/*
 * Copyright (c) 1996, 2015, Oracle and/or its affiliates. All rights reserved.
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

package java.io;

import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;
import sun.reflect.misc.ReflectUtil;

import java.lang.reflect.Field;

/**
 * A description of a Serializable field from a Serializable class.  An array
 * of ObjectStreamFields is used to declare the Serializable fields of a class.
 *
 * @author Mike Warres
 * @author Roger Riggs
 * @see ObjectStreamClass
 * @since 1.2
 */
/*
 * 代表可被序列化的字段。即指定哪些字段将参与序列化，并封装这些参与序列化的字段信息。
 * 所有可被序列化的字段需要封装为一个ObjectStreamField数组，且数组变量名必须为serialPersistentFields。
 */
public class ObjectStreamField implements Comparable<Object> {
    
    /** corresponding reflective field object, if any */
    // 封装的字段(可能为null)
    private final Field field;
    
    /** field name */
    // 字段名称
    private final String name;
    
    /** field type (Object.class if unknown non-primitive type) */
    // 字段类型
    private final Class<?> type;
    
    /** canonical JVM signature of field type, if given */
    // JVM规范签名
    private final String signature;
    
    /** lazily constructed signature for the type, if no explicit signature */
    // JVM规范签名(懒加载生成)
    private String typeSignature;
    
    /** whether or not to (de)serialize field values as unshared */
    // 字段是否非共享(共享字段会共用一段序列化信息)
    private final boolean unshared;
    
    /** offset of field value in enclosing field group */
    // 当前字段在所有待序列化字段中的偏移量(以字节为偏移单位)
    private int offset;
    
    
    /*▼ 构造方法 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Create a Serializable field with the specified type.  This field should
     * be documented with a <code>serialField</code> tag.
     *
     * @param name the name of the serializable field
     * @param type the <code>Class</code> object of the serializable field
     */
    public ObjectStreamField(String name, Class<?> type) {
        this(name, type, false);
    }
    
    /**
     * Creates an ObjectStreamField representing a serializable field with the
     * given name and type.  If unshared is false, values of the represented
     * field are serialized and deserialized in the default manner--if the
     * field is non-primitive, object values are serialized and deserialized as
     * if they had been written and read by calls to writeObject and
     * readObject.  If unshared is true, values of the represented field are
     * serialized and deserialized as if they had been written and read by
     * calls to writeUnshared and readUnshared.
     *
     * @param name     field name
     * @param type     field type
     * @param unshared if false, write/read field values in the same manner
     *                 as writeObject/readObject; if true, write/read in the same
     *                 manner as writeUnshared/readUnshared
     *
     * @since 1.4
     */
    public ObjectStreamField(String name, Class<?> type, boolean unshared) {
        if(name == null) {
            throw new NullPointerException();
        }
        this.name = name;
        this.type = type;
        this.unshared = unshared;
        this.field = null;
        this.signature = null;
    }
    
    /**
     * Creates an ObjectStreamField representing a field with the given name,
     * signature and unshared setting.
     */
    ObjectStreamField(String name, String signature, boolean unshared) {
        if(name == null) {
            throw new NullPointerException();
        }
        this.name = name;
        this.signature = signature.intern();
        this.unshared = unshared;
        this.field = null;
        
        switch(signature.charAt(0)) {
            case 'Z':
                type = Boolean.TYPE;
                break;
            case 'B':
                type = Byte.TYPE;
                break;
            case 'C':
                type = Character.TYPE;
                break;
            case 'S':
                type = Short.TYPE;
                break;
            case 'I':
                type = Integer.TYPE;
                break;
            case 'J':
                type = Long.TYPE;
                break;
            case 'F':
                type = Float.TYPE;
                break;
            case 'D':
                type = Double.TYPE;
                break;
            case 'L':
            case '[':
                type = Object.class;
                break;
            default:
                throw new IllegalArgumentException("illegal signature");
        }
    }
    
    /**
     * Creates an ObjectStreamField representing the given field with the
     * specified unshared setting.  For compatibility with the behavior of
     * earlier serialization implementations, a "showType" parameter is
     * necessary to govern whether or not a getType() call on this
     * ObjectStreamField (if non-primitive) will return Object.class (as
     * opposed to a more specific reference type).
     */
    ObjectStreamField(Field field, boolean unshared, boolean showType) {
        this.field = field;
        this.unshared = unshared;
        name = field.getName();
        Class<?> ftype = field.getType();
        type = (showType || ftype.isPrimitive()) ? ftype : Object.class;
        signature = getClassSignature(ftype).intern();
    }
    
    /*▲ 构造方法 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Returns field represented by this ObjectStreamField, or null if
     * ObjectStreamField is not associated with an actual field.
     */
    // 获取允许反序列化的字段
    Field getField() {
        return field;
    }
    
    /**
     * Get the name of this field.
     *
     * @return a <code>String</code> representing the name of the serializable
     * field
     */
    // 返回字段的名称
    public String getName() {
        return name;
    }
    
    /**
     * Get the type of the field.  If the type is non-primitive and this
     * <code>ObjectStreamField</code> was obtained from a deserialized {@link
     * ObjectStreamClass} instance, then <code>Object.class</code> is returned.
     * Otherwise, the <code>Class</code> object for the type of the field is
     * returned.
     *
     * @return a <code>Class</code> object representing the type of the
     * serializable field
     */
    // 返回字段的类型
    @CallerSensitive
    public Class<?> getType() {
        if(System.getSecurityManager() != null) {
            Class<?> caller = Reflection.getCallerClass();
            if(ReflectUtil.needsPackageAccessCheck(caller.getClassLoader(), type.getClassLoader())) {
                ReflectUtil.checkPackageAccess(type);
            }
        }
        return type;
    }
    
    /**
     * Returns character encoding of field type.  The encoding is as follows:
     * <blockquote><pre>
     * B            byte
     * C            char
     * D            double
     * F            float
     * I            int
     * J            long
     * L            class or interface
     * S            short
     * Z            boolean
     * [            array
     * </pre></blockquote>
     *
     * @return the typecode of the serializable field
     *
     * REMIND: deprecate?
     */
    // 返回字段签名
    public char getTypeCode() {
        return getSignature().charAt(0);
    }
    
    /**
     * Return the JVM type signature.
     *
     * @return null if this field has a primitive type.
     *
     * REMIND: deprecate?
     */
    // 返回字段签名，对于基本类型，返回null
    public String getTypeString() {
        return isPrimitive() ? null : getSignature();
    }
    
    /**
     * Offset of field within instance data.
     *
     * @return the offset of this field
     *
     * @see #setOffset
     *
     * REMIND: deprecate?
     */
    // 返回字段的偏移量
    public int getOffset() {
        return offset;
    }
    
    /**
     * Offset within instance data.
     *
     * @param offset the offset of the field
     *
     * @see #getOffset
     *
     * REMIND: deprecate?
     */
    // 设置字段的偏移量
    protected void setOffset(int offset) {
        this.offset = offset;
    }
    
    /**
     * Return true if this field has a primitive type.
     *
     * @return true if and only if this field corresponds to a primitive type
     *
     * REMIND: deprecate?
     */
    // 当前字段是否为基本类型
    public boolean isPrimitive() {
        char tcode = getTypeCode();
        return ((tcode != 'L') && (tcode != '['));
    }
    
    /**
     * Returns boolean value indicating whether or not the serializable field
     * represented by this ObjectStreamField instance is unshared.
     *
     * @return {@code true} if this field is unshared
     *
     * @since 1.4
     */
    // 判断当前字段是否非共享
    public boolean isUnshared() {
        return unshared;
    }
    
    
    
    /*▼ 签名 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns JVM type signature of field (similar to getTypeString, except
     * that signature strings are returned for primitive fields as well).
     */
    // 返回字段签名
    String getSignature() {
        if(signature != null) {
            return signature;
        }
        
        String sig = typeSignature;
        // This lazy calculation is safe since signature can be null iff one
        // of the public constructors are used, in which case type is always
        // initialized to the exact type we want the signature to represent.
        if(sig == null) {
            typeSignature = sig = getClassSignature(type).intern();
        }
        
        return sig;
    }
    
    /**
     * Returns JVM type signature for given class.
     */
    // 获取类型对应的签名
    static String getClassSignature(Class<?> cl) {
        if(cl.isPrimitive()) {
            return getPrimitiveSignature(cl);
        } else {
            return appendClassSignature(new StringBuilder(), cl).toString();
        }
    }
    
    static StringBuilder appendClassSignature(StringBuilder sbuf, Class<?> cl) {
        while(cl.isArray()) {
            sbuf.append('[');
            cl = cl.getComponentType();
        }
        
        if(cl.isPrimitive()) {
            sbuf.append(getPrimitiveSignature(cl));
        } else {
            sbuf.append('L').append(cl.getName().replace('.', '/')).append(';');
        }
        
        return sbuf;
    }
    
    /**
     * Returns JVM type signature for given primitive.
     */
    // 基本类型的签名
    private static String getPrimitiveSignature(Class<?> cl) {
        if(cl == Integer.TYPE) {
            return "I";
        } else if(cl == Byte.TYPE) {
            return "B";
        } else if(cl == Long.TYPE) {
            return "J";
        } else if(cl == Float.TYPE) {
            return "F";
        } else if(cl == Double.TYPE) {
            return "D";
        } else if(cl == Short.TYPE) {
            return "S";
        } else if(cl == Character.TYPE) {
            return "C";
        } else if(cl == Boolean.TYPE) {
            return "Z";
        } else if(cl == Void.TYPE) {
            return "V";
        } else {
            throw new InternalError();
        }
    }
    
    /*▲ 签名 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Compare this field with another <code>ObjectStreamField</code>.  Return
     * -1 if this is smaller, 0 if equal, 1 if greater.  Types that are
     * primitives are "smaller" than object types.  If equal, the field names
     * are compared.
     */
    // REMIND: deprecate?
    @Override
    public int compareTo(Object obj) {
        ObjectStreamField other = (ObjectStreamField) obj;
        boolean isPrim = isPrimitive();
        if(isPrim != other.isPrimitive()) {
            return isPrim ? -1 : 1;
        }
        return name.compareTo(other.name);
    }
    
    /**
     * Return a string that describes this field.
     */
    @Override
    public String toString() {
        return getSignature() + ' ' + name;
    }
    
}
