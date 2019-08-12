/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.atomic;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * An {@code AtomicMarkableReference} maintains an object reference
 * along with a mark bit, that can be updated atomically.
 *
 * <p>Implementation note: This implementation maintains markable
 * references by creating internal objects representing "boxed"
 * [reference, boolean] pairs.
 *
 * @param <V> The type of object referred to by this reference
 *
 * @author Doug Lea
 * @since 1.5
 */
// 捆绑了boolean标记的reference，在特定场合下可以解决CAS中的ABA问题
public class AtomicMarkableReference<V> {
    // 捆绑了boolean标记的reference
    private volatile Pair<V> pair;
    
    // VarHandle mechanics
    private static final VarHandle PAIR;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            PAIR = l.findVarHandle(AtomicMarkableReference.class, "pair", Pair.class);
        } catch(ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates a new {@code AtomicMarkableReference} with the given
     * initial values.
     *
     * @param initialRef  the initial reference
     * @param initialMark the initial mark
     */
    public AtomicMarkableReference(V initialRef, boolean initialMark) {
        pair = Pair.of(initialRef, initialMark);
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 获取值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the current values of both the reference and the mark.
     * Typical usage is {@code boolean[1] holder; ref = v.get(holder); }.
     *
     * @param markHolder an array of size of at least one. On return, {@code markHolder[0]} will hold the value of the mark.
     *
     * @return the current value of the reference
     */
    // 获取reference值（返回值）和mark值（形参）
    public V get(boolean[] markHolder) {
        Pair<V> pair = this.pair;
        markHolder[0] = pair.mark;
        return pair.reference;
    }
    
    /**
     * Returns the current value of the reference.
     *
     * @return the current value of the reference
     */
    // 获取reference值（返回值）
    public V getReference() {
        return pair.reference;
    }
    
    /**
     * Returns the current value of the mark.
     *
     * @return the current value of the mark
     */
    // 获取mark值（返回值）
    public boolean isMarked() {
        return pair.mark;
    }
    
    /*▲ 获取值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 设置值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Unconditionally sets the value of both the reference and mark.
     *
     * @param newReference the new value for the reference
     * @param newMark      the new value for the mark
     */
    // 使用newReference和newMark无条件地更新pair
    public void set(V newReference, boolean newMark) {
        Pair<V> current = pair;
        
        if(newReference != current.reference || newMark != current.mark) {
            this.pair = Pair.of(newReference, newMark);
        }
    }
    
    /*▲ 设置值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ CAS ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 原子地更新pair，返回值指示是否更新成功
    private boolean casPair(Pair<V> cmp, Pair<V> val) {
        return PAIR.compareAndSet(this, cmp, val);
    }
    
    /**
     * Atomically sets the value of the mark to the given update value
     * if the current reference is {@code ==} to the expected
     * reference.  Any given invocation of this operation may fail
     * (return {@code false}) spuriously, but repeated invocation
     * when the current value holds the expected value and no other
     * thread is also attempting to set the value will eventually
     * succeed.
     *
     * @param expectedReference the expected value of the reference
     * @param newMark           the new value for the mark
     *
     * @return {@code true} if successful
     */
    // 如果当前引用reference与期望引用expectedReference一致，则使用newMark原子地更新pair
    public boolean attemptMark(V expectedReference, boolean newMark) {
        Pair<V> current = pair;
        return expectedReference == current.reference
            && (newMark == current.mark || casPair(current, Pair.of(expectedReference, newMark)));
    }
    
    /**
     * Atomically sets the value of both the reference and mark
     * to the given update values if the
     * current reference is {@code ==} to the expected reference
     * and the current mark is equal to the expected mark.
     *
     * @param expectedReference the expected value of the reference
     * @param newReference      the new value for the reference
     * @param expectedMark      the expected value of the mark
     * @param newMark           the new value for the mark
     *
     * @return {@code true} if successful
     */
    // 使用newReference和newMark原子地更新Pair（更新前需要先与期望值作比对）
    public boolean compareAndSet(V expectedReference, V newReference, boolean expectedMark, boolean newMark) {
        Pair<V> current = pair;
        return expectedReference == current.reference
            && expectedMark == current.mark
            && ((newReference == current.reference && newMark == current.mark) || casPair(current, Pair.of(newReference, newMark)));
    }
    
    /**
     * Atomically sets the value of both the reference and mark
     * to the given update values if the
     * current reference is {@code ==} to the expected reference
     * and the current mark is equal to the expected mark.
     *
     * <p><a href="package-summary.html#weakCompareAndSet">May fail
     * spuriously and does not provide ordering guarantees</a>, so is
     * only rarely an appropriate alternative to {@code compareAndSet}.
     *
     * @param expectedReference the expected value of the reference
     * @param newReference      the new value for the reference
     * @param expectedMark      the expected value of the mark
     * @param newMark           the new value for the mark
     *
     * @return {@code true} if successful
     */
    // 使用newReference和newMark原子地更新Pair（更新前需要先与期望值作比对）
    public boolean weakCompareAndSet(V expectedReference, V newReference, boolean expectedMark, boolean newMark) {
        return compareAndSet(expectedReference, newReference, expectedMark, newMark);
    }
    
    /*▲ CAS ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    // 当前类的内部实现，Pair中包含了捆绑了boolean标记的reference
    private static class Pair<T> {
        final T reference;
        final boolean mark;
        
        private Pair(T reference, boolean mark) {
            this.reference = reference;
            this.mark = mark;
        }
        
        static <T> Pair<T> of(T reference, boolean mark) {
            return new Pair<T>(reference, mark);
        }
    }
}
