/*
 * Copyright (c) 1996, 2011, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

/**
 * The {@code Void} class is an uninstantiable placeholder class to hold a reference to the {@code Class} object representing the Java keyword void.
 *
 * @author unascribed
 * @since 1.1
 *
 * Void类是一个不可实例化的占位符类，用于表示对Java关键字void的类对象的引用。
 *
 * 可以用于判断某个方法的返回值是否为void：
 * public void foo() {}
 * ...
 * if (getClass().getMethod("foo").getReturnType() == Void.TYPE)
 * ...
 *
 *
 * 也可用在泛型中，如需要一个返回值为void的泛型。
 * abstract class Foo<T> {
 *     abstract T bar();
 * }
 *
 * class Bar extends Foo<Void> {
 *     Void bar() {
 *         return (null);
 *     }
 * }
 *
 * 还可用在形参中，仅仅表示占位。
 */
public final class Void {
    
    /**
     * The {@code Class} object representing the pseudo-type corresponding to the keyword {@code void}.
     */
    @SuppressWarnings("unchecked")
    public static final Class<Void> TYPE = (Class<Void>) Class.getPrimitiveClass("void");
    
    /**
     * The Void class cannot be instantiated.
     */
    // 无法被实例化
    private Void() {
    }
}
