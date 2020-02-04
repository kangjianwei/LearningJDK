/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.reflect;

/**
 * This interface provides the declarations for the accessor methods
 * of java.lang.reflect.Field. Each Field object is configured with a
 * (possibly dynamically-generated) class which implements this
 * interface.
 */
// 字段访问器
public interface FieldAccessor {
    
    /** Matches specification in {@link java.lang.reflect.Field} */
    // 返回obj中当前字段的值，要求当前字段为boolean类型
    boolean getBoolean(Object obj) throws IllegalArgumentException;
    
    /** Matches specification in {@link java.lang.reflect.Field} */
    // 返回obj中当前字段的值，要求当前字段为char类型
    char getChar(Object obj) throws IllegalArgumentException;
    
    /** Matches specification in {@link java.lang.reflect.Field} */
    // 返回obj中当前字段的值，要求当前字段为byte类型
    byte getByte(Object obj) throws IllegalArgumentException;
    
    /** Matches specification in {@link java.lang.reflect.Field} */
    // 返回obj中当前字段的值，要求当前字段为short类型
    short getShort(Object obj) throws IllegalArgumentException;
    
    /** Matches specification in {@link java.lang.reflect.Field} */
    // 返回obj中当前字段的值，要求当前字段为int类型
    int getInt(Object obj) throws IllegalArgumentException;
    
    /** Matches specification in {@link java.lang.reflect.Field} */
    // 返回obj中当前字段的值，要求当前字段为long类型
    long getLong(Object obj) throws IllegalArgumentException;
    
    /** Matches specification in {@link java.lang.reflect.Field} */
    // 返回obj中当前字段的值，要求当前字段为float类型
    float getFloat(Object obj) throws IllegalArgumentException;
    
    /** Matches specification in {@link java.lang.reflect.Field} */
    // 返回obj中当前字段的值，要求当前字段为double类型
    double getDouble(Object obj) throws IllegalArgumentException;
    
    /** Matches specification in {@link java.lang.reflect.Field} */
    // 返回obj中当前字段的值，要求当前字段为对象类型
    Object get(Object obj) throws IllegalArgumentException;
    
    
    /** Matches specification in {@link java.lang.reflect.Field} */
    // 将obj对象中的当前字段设置为指定的值，要求当前字段为boolean类型
    void setBoolean(Object obj, boolean value) throws IllegalArgumentException, IllegalAccessException;
    
    /** Matches specification in {@link java.lang.reflect.Field} */
    // 将obj对象中的当前字段设置为指定的值，要求当前字段为char类型
    void setChar(Object obj, char value) throws IllegalArgumentException, IllegalAccessException;
    
    /** Matches specification in {@link java.lang.reflect.Field} */
    // 将obj对象中的当前字段设置为指定的值，要求当前字段为byte类型
    void setByte(Object obj, byte value) throws IllegalArgumentException, IllegalAccessException;
    
    /** Matches specification in {@link java.lang.reflect.Field} */
    // 将obj对象中的当前字段设置为指定的值，要求当前字段为short类型
    void setShort(Object obj, short value) throws IllegalArgumentException, IllegalAccessException;
    
    /** Matches specification in {@link java.lang.reflect.Field} */
    // 将obj对象中的当前字段设置为指定的值，要求当前字段为int类型
    void setInt(Object obj, int value) throws IllegalArgumentException, IllegalAccessException;
    
    /** Matches specification in {@link java.lang.reflect.Field} */
    // 将obj对象中的当前字段设置为指定的值，要求当前字段为long类型
    void setLong(Object obj, long value) throws IllegalArgumentException, IllegalAccessException;
    
    /** Matches specification in {@link java.lang.reflect.Field} */
    // 将obj对象中的当前字段设置为指定的值，要求当前字段为float类型
    void setFloat(Object obj, float value) throws IllegalArgumentException, IllegalAccessException;
    
    /** Matches specification in {@link java.lang.reflect.Field} */
    // 将obj对象中的当前字段设置为指定的值，要求当前字段为double类型
    void setDouble(Object obj, double value) throws IllegalArgumentException, IllegalAccessException;
    
    /** Matches specification in {@link java.lang.reflect.Field} */
    // 将obj对象中的当前字段设置为指定的值，要求当前字段为对象类型
    void set(Object obj, Object value) throws IllegalArgumentException, IllegalAccessException;
    
}
