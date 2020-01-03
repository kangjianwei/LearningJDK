/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ObjectStreamClass.WeakClassKey;
import java.lang.ref.ReferenceQueue;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import sun.reflect.misc.ReflectUtil;
import sun.security.action.GetBooleanAction;

/**
 * An ObjectOutputStream writes primitive data types and graphs of Java objects
 * to an OutputStream.  The objects can be read (reconstituted) using an
 * ObjectInputStream.  Persistent storage of objects can be accomplished by
 * using a file for the stream.  If the stream is a network socket stream, the
 * objects can be reconstituted on another host or in another process.
 *
 * <p>Only objects that support the java.io.Serializable interface can be
 * written to streams.  The class of each serializable object is encoded
 * including the class name and signature of the class, the values of the
 * object's fields and arrays, and the closure of any other objects referenced
 * from the initial objects.
 *
 * <p>The method writeObject is used to write an object to the stream.  Any
 * object, including Strings and arrays, is written with writeObject. Multiple
 * objects or primitives can be written to the stream.  The objects must be
 * read back from the corresponding ObjectInputstream with the same types and
 * in the same order as they were written.
 *
 * <p>Primitive data types can also be written to the stream using the
 * appropriate methods from DataOutput. Strings can also be written using the
 * writeUTF method.
 *
 * <p>The default serialization mechanism for an object writes the class of the
 * object, the class signature, and the values of all non-transient and
 * non-static fields.  References to other objects (except in transient or
 * static fields) cause those objects to be written also. Multiple references
 * to a single object are encoded using a reference sharing mechanism so that
 * graphs of objects can be restored to the same shape as when the original was
 * written.
 *
 * <p>For example to write an object that can be read by the example in
 * ObjectInputStream:
 * <br>
 * <pre>
 *      FileOutputStream fos = new FileOutputStream("t.tmp");
 *      ObjectOutputStream oos = new ObjectOutputStream(fos);
 *
 *      oos.writeInt(12345);
 *      oos.writeObject("Today");
 *      oos.writeObject(new Date());
 *
 *      oos.close();
 * </pre>
 *
 * <p>Classes that require special handling during the serialization and
 * deserialization process must implement special methods with these exact
 * signatures:
 * <br>
 * <pre>
 * private void readObject(java.io.ObjectInputStream stream)
 *     throws IOException, ClassNotFoundException;
 * private void writeObject(java.io.ObjectOutputStream stream)
 *     throws IOException
 * private void readObjectNoData()
 *     throws ObjectStreamException;
 * </pre>
 *
 * <p>The writeObject method is responsible for writing the state of the object
 * for its particular class so that the corresponding readObject method can
 * restore it.  The method does not need to concern itself with the state
 * belonging to the object's superclasses or subclasses.  State is saved by
 * writing the individual fields to the ObjectOutputStream using the
 * writeObject method or by using the methods for primitive data types
 * supported by DataOutput.
 *
 * <p>Serialization does not write out the fields of any object that does not
 * implement the java.io.Serializable interface.  Subclasses of Objects that
 * are not serializable can be serializable. In this case the non-serializable
 * class must have a no-arg constructor to allow its fields to be initialized.
 * In this case it is the responsibility of the subclass to save and restore
 * the state of the non-serializable class. It is frequently the case that the
 * fields of that class are accessible (public, package, or protected) or that
 * there are get and set methods that can be used to restore the state.
 *
 * <p>Serialization of an object can be prevented by implementing writeObject
 * and readObject methods that throw the NotSerializableException.  The
 * exception will be caught by the ObjectOutputStream and abort the
 * serialization process.
 *
 * <p>Implementing the Externalizable interface allows the object to assume
 * complete control over the contents and format of the object's serialized
 * form.  The methods of the Externalizable interface, writeExternal and
 * readExternal, are called to save and restore the objects state.  When
 * implemented by a class they can write and read their own state using all of
 * the methods of ObjectOutput and ObjectInput.  It is the responsibility of
 * the objects to handle any versioning that occurs.
 *
 * <p>Enum constants are serialized differently than ordinary serializable or
 * externalizable objects.  The serialized form of an enum constant consists
 * solely of its name; field values of the constant are not transmitted.  To
 * serialize an enum constant, ObjectOutputStream writes the string returned by
 * the constant's name method.  Like other serializable or externalizable
 * objects, enum constants can function as the targets of back references
 * appearing subsequently in the serialization stream.  The process by which
 * enum constants are serialized cannot be customized; any class-specific
 * writeObject and writeReplace methods defined by enum types are ignored
 * during serialization.  Similarly, any serialPersistentFields or
 * serialVersionUID field declarations are also ignored--all enum types have a
 * fixed serialVersionUID of 0L.
 *
 * <p>Primitive data, excluding serializable fields and externalizable data, is
 * written to the ObjectOutputStream in block-data records. A block data record
 * is composed of a header and data. The block data header consists of a marker
 * and the number of bytes to follow the header.  Consecutive primitive data
 * writes are merged into one block-data record.  The blocking factor used for
 * a block-data record will be 1024 bytes.  Each block-data record will be
 * filled up to 1024 bytes, or be written whenever there is a termination of
 * block-data mode.  Calls to the ObjectOutputStream methods writeObject,
 * defaultWriteObject and writeFields initially terminate any existing
 * block-data record.
 *
 * @author      Mike Warres
 * @author      Roger Riggs
 * @see java.io.DataOutput
 * @see java.io.ObjectInputStream
 * @see java.io.Serializable
 * @see java.io.Externalizable
 * @see <a href="{@docRoot}/../specs/serialization/output.html">Object Serialization Specification, Section 2, Object Output Classes</a>
 * @since       1.1
 */
/*
 * 对象输出流，参与序列化过程
 *
 * 对于实现了Externalizable接口的类，需要自行实现序列化逻辑
 */
public class ObjectOutputStream extends OutputStream implements ObjectOutput, ObjectStreamConstants {
    
    /** stream protocol version */
    private int protocol = PROTOCOL_VERSION_2;  // 序列化版本
    
    /** recursion depth */
    private int depth;  // 序列化嵌套深度
    
    /** filter stream for handling block data conversion */
    private final BlockDataOutputStream bout;   // 块数据输出流
    
    /** obj -> wire handle map */
    private final HandleTable handles;  // 共享对象哈希表
    
    /** obj -> replacement obj map */
    private final ReplaceTable subs;    // 替换对象哈希表
    
    /** buffer for writing primitive field values */
    private byte[] primVals;    // 待序列化的基本类型字段的值
    
    /** if true, invoke writeObjectOverride() instead of writeObject() */
    private final boolean enableOverride;   // 是否使用子类实现的writeObjectOverride()方法，而不是使用默认的序列化逻辑
    
    /** if true, invoke replaceObject() */
    private boolean enableReplace;  // 是否需要调用replaceObject()方法，默认为false
    
    /* values below valid only during upcalls to writeObject()/writeExternal() */
    
    /**
     * Context during upcalls to class-defined writeObject methods; holds
     * object currently being serialized and descriptor for current class.
     * Null when not during writeObject upcall.
     */
    private SerialCallbackContext curContext;   // 序列化上下文
    
    /** current PutField object */
    private PutFieldImpl curPut;    // 当前待序列化的字段集
    
    /** custom storage for debug trace info */
    private final DebugTraceInfoStack debugInfoStack;
    
    /**
     * value of "sun.io.serialization.extendedDebugInfo" property,
     * as true or false for extended information about exception's place
     */
    private static final boolean extendedDebugInfo = AccessController.doPrivileged(
        new GetBooleanAction("sun.io.serialization.extendedDebugInfo")
    );
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates an ObjectOutputStream that writes to the specified OutputStream.
     * This constructor writes the serialization stream header to the
     * underlying stream; callers may wish to flush the stream immediately to
     * ensure that constructors for receiving ObjectInputStreams will not block
     * when reading the header.
     *
     * <p>If a security manager is installed, this constructor will check for
     * the "enableSubclassImplementation" SerializablePermission when invoked
     * directly or indirectly by the constructor of a subclass which overrides
     * the ObjectOutputStream.putFields or ObjectOutputStream.writeUnshared
     * methods.
     *
     * @param   out output stream to write to
     * @throws  IOException if an I/O error occurs while writing stream header
     * @throws  SecurityException if untrusted subclass illegally overrides
     *          security-sensitive methods
     * @throws  NullPointerException if <code>out</code> is <code>null</code>
     * @since   1.4
     * @see     ObjectOutputStream#ObjectOutputStream()
     * @see     ObjectOutputStream#putFields()
     * @see     ObjectInputStream#ObjectInputStream(InputStream)
     */
    public ObjectOutputStream(OutputStream out) throws IOException {
        verifySubclass();
        
        bout = new BlockDataOutputStream(out);
        handles = new HandleTable(10, (float) 3.00);
        subs = new ReplaceTable(10, (float) 3.00);
        
        enableOverride = false;
        
        // 写入序列化头（包含一个魔数和一个版本号）
        writeStreamHeader();
        
        // 设置待写数据处于块模式下
        bout.setBlockDataMode(true);
        
        if (extendedDebugInfo) {
            debugInfoStack = new DebugTraceInfoStack();
        } else {
            debugInfoStack = null;
        }
    }
    
    /**
     * Provide a way for subclasses that are completely reimplementing
     * ObjectOutputStream to not have to allocate private data just used by
     * this implementation of ObjectOutputStream.
     *
     * <p>If there is a security manager installed, this method first calls the
     * security manager's <code>checkPermission</code> method with a
     * <code>SerializablePermission("enableSubclassImplementation")</code>
     * permission to ensure it's ok to enable subclassing.
     *
     * @throws  SecurityException if a security manager exists and its
     *          <code>checkPermission</code> method denies enabling
     *          subclassing.
     * @throws  IOException if an I/O error occurs while creating this stream
     * @see SecurityManager#checkPermission
     * @see java.io.SerializablePermission
     */
    protected ObjectOutputStream() throws IOException, SecurityException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(SUBCLASS_IMPLEMENTATION_PERMISSION);
        }
        
        bout = null;
        handles = null;
        subs = null;
        enableOverride = true;
        debugInfoStack = null;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 写 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Writes a boolean.
     *
     * @param val the boolean to be written
     *
     * @throws IOException if I/O errors occur while writing to the underlying
     *                     stream
     */
    // 向最终输出流写入boolean
    public void writeBoolean(boolean val) throws IOException {
        bout.writeBoolean(val);
    }
    
    /**
     * Writes a 16 bit char.
     *
     * @param val the char value to be written
     *
     * @throws IOException if I/O errors occur while writing to the underlying
     *                     stream
     */
    // 向最终输出流写入char
    public void writeChar(int val) throws IOException {
        bout.writeChar(val);
    }
    
    /**
     * Writes an 8 bit byte.
     *
     * @param val the byte value to be written
     *
     * @throws IOException if I/O errors occur while writing to the underlying
     *                     stream
     */
    // 向最终输出流写入byte
    public void writeByte(int val) throws IOException {
        bout.writeByte(val);
    }
    
    /**
     * Writes a 16 bit short.
     *
     * @param val the short value to be written
     *
     * @throws IOException if I/O errors occur while writing to the underlying
     *                     stream
     */
    // 向最终输出流写入short
    public void writeShort(int val) throws IOException {
        bout.writeShort(val);
    }
    
    /**
     * Writes a 32 bit int.
     *
     * @param val the integer value to be written
     *
     * @throws IOException if I/O errors occur while writing to the underlying
     *                     stream
     */
    // 向最终输出流写入int
    public void writeInt(int val) throws IOException {
        bout.writeInt(val);
    }
    
    /**
     * Writes a 64 bit long.
     *
     * @param val the long value to be written
     *
     * @throws IOException if I/O errors occur while writing to the underlying
     *                     stream
     */
    // 向最终输出流写入long
    public void writeLong(long val) throws IOException {
        bout.writeLong(val);
    }
    
    /**
     * Writes a 32 bit float.
     *
     * @param val the float value to be written
     *
     * @throws IOException if I/O errors occur while writing to the underlying
     *                     stream
     */
    // 向最终输出流写入float
    public void writeFloat(float val) throws IOException {
        bout.writeFloat(val);
    }
    
    /**
     * Writes a 64 bit double.
     *
     * @param val the double value to be written
     *
     * @throws IOException if I/O errors occur while writing to the underlying
     *                     stream
     */
    // 向最终输出流写入double
    public void writeDouble(double val) throws IOException {
        bout.writeDouble(val);
    }
    
    /**
     * Writes a String as a sequence of bytes.
     *
     * @param str the String of bytes to be written
     *
     * @throws IOException if I/O errors occur while writing to the underlying
     *                     stream
     */
    // 向最终输出流写入字符串中的byte信息(只取char的低8位)
    public void writeBytes(String str) throws IOException {
        bout.writeBytes(str);
    }
    
    /**
     * Writes a String as a sequence of chars.
     *
     * @param str the String of chars to be written
     *
     * @throws IOException if I/O errors occur while writing to the underlying
     *                     stream
     */
    // 向最终输出流写入字符串中的char信息
    public void writeChars(String str) throws IOException {
        bout.writeChars(str);
    }
    
    
    /**
     * Writes a byte. This method will block until the byte is actually written.
     *
     * @param val the byte to be written to the stream
     *
     * @throws IOException If an I/O error has occurred.
     */
    // 向块数据输出流写入byte信息
    public void write(int val) throws IOException {
        bout.write(val);
    }
    
    /**
     * Writes an array of bytes. This method will block until the bytes are
     * actually written.
     *
     * @param buf the data to be written
     *
     * @throws IOException If an I/O error has occurred.
     */
    // 向块数据输出流写入字节数组b中的数据
    public void write(byte[] buf) throws IOException {
        bout.write(buf, 0, buf.length, false);
    }
    
    /**
     * Writes a sub array of bytes.
     *
     * @param buf the data to be written
     * @param off the start offset in the data
     * @param len the number of bytes that are written
     *
     * @throws IOException If an I/O error has occurred.
     */
    // 向块数据输出流写入字节数组b中指定范围的的数据
    public void write(byte[] buf, int off, int len) throws IOException {
        if(buf == null) {
            throw new NullPointerException();
        }
        
        int endoff = off + len;
        if(off<0 || len<0 || endoff>buf.length || endoff<0) {
            throw new IndexOutOfBoundsException();
        }
        
        bout.write(buf, off, len, false);
    }
    
    
    /**
     * Primitive data write of this String in
     * <a href="DataInput.html#modified-utf-8">modified UTF-8</a>
     * format.  Note that there is a
     * significant difference between writing a String into the stream as
     * primitive data or as an Object. A String instance written by writeObject
     * is written into the stream as a String initially. Future writeObject()
     * calls write references to the string into the stream.
     *
     * @param str the String to be written
     *
     * @throws IOException if I/O errors occur while writing to the underlying
     *                     stream
     */
    // 向最终输出流写入字符串的utf8形式
    public void writeUTF(String str) throws IOException {
        bout.writeUTF(str);
    }
    
    /*▲ 写 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Write the specified object to the ObjectOutputStream.  The class of the
     * object, the signature of the class, and the values of the non-transient
     * and non-static fields of the class and all of its supertypes are
     * written.  Default serialization for a class can be overridden using the
     * writeObject and the readObject methods.  Objects referenced by this
     * object are written transitively so that a complete equivalent graph of
     * objects can be reconstructed by an ObjectInputStream.
     *
     * <p>Exceptions are thrown for problems with the OutputStream and for
     * classes that should not be serialized.  All exceptions are fatal to the
     * OutputStream, which is left in an indeterminate state, and it is up to
     * the caller to ignore or recover the stream state.
     *
     * @throws InvalidClassException    Something is wrong with a class used by
     *                                  serialization.
     * @throws NotSerializableException Some object to be serialized does not
     *                                  implement the java.io.Serializable interface.
     * @throws IOException              Any exception thrown by the underlying
     *                                  OutputStream.
     */
    // 将指定的共享对象obj写入输出流
    public final void writeObject(Object obj) throws IOException {
        if(enableOverride) {
            // 使用子类实现的序列化逻辑
            writeObjectOverride(obj);
            return;
        }
        
        try {
            // 使用默认的序列化逻辑：对共享对象obj进行序列化
            writeObject0(obj, false);
        } catch(IOException ex) {
            if(depth == 0) {
                writeFatalException(ex);
            }
            throw ex;
        }
    }
    
    /**
     * Writes an "unshared" object to the ObjectOutputStream.  This method is
     * identical to writeObject, except that it always writes the given object
     * as a new, unique object in the stream (as opposed to a back-reference
     * pointing to a previously serialized instance).  Specifically:
     * <ul>
     *   <li>An object written via writeUnshared is always serialized in the
     *       same manner as a newly appearing object (an object that has not
     *       been written to the stream yet), regardless of whether or not the
     *       object has been written previously.
     *
     *   <li>If writeObject is used to write an object that has been previously
     *       written with writeUnshared, the previous writeUnshared operation
     *       is treated as if it were a write of a separate object.  In other
     *       words, ObjectOutputStream will never generate back-references to
     *       object data written by calls to writeUnshared.
     * </ul>
     * While writing an object via writeUnshared does not in itself guarantee a
     * unique reference to the object when it is deserialized, it allows a
     * single object to be defined multiple times in a stream, so that multiple
     * calls to readUnshared by the receiver will not conflict.  Note that the
     * rules described above only apply to the base-level object written with
     * writeUnshared, and not to any transitively referenced sub-objects in the
     * object graph to be serialized.
     *
     * <p>ObjectOutputStream subclasses which override this method can only be
     * constructed in security contexts possessing the
     * "enableSubclassImplementation" SerializablePermission; any attempt to
     * instantiate such a subclass without this permission will cause a
     * SecurityException to be thrown.
     *
     * @param obj object to write to stream
     *
     * @throws NotSerializableException if an object in the graph to be
     *                                  serialized does not implement the Serializable interface
     * @throws InvalidClassException    if a problem exists with the class of an
     *                                  object to be serialized
     * @throws IOException              if an I/O error occurs during serialization
     * @since 1.4
     */
    // 将指定的非共享对象obj写入输出流
    public void writeUnshared(Object obj) throws IOException {
        try {
            writeObject0(obj, true);
        } catch(IOException ex) {
            if(depth == 0) {
                writeFatalException(ex);
            }
            throw ex;
        }
    }
    
    /**
     * Write the non-static and non-transient fields of the current class to this stream.
     * This may only be called from the writeObject method of the class being serialized.
     * It will throw the NotActiveException if it is called otherwise.
     *
     * @throws IOException if I/O errors occur while writing to the underlying <code>OutputStream</code>
     */
    /*
     * 默认序列化
     *
     * 将当前类的非static和非transient字段序列化到输出流
     * 只能从待序列化的类的writeObject()方法中调用此方法，否则会抛异常。
     */
    public void defaultWriteObject() throws IOException {
        SerialCallbackContext ctx = curContext;
        if(ctx == null) {
            throw new NotActiveException("not in call to writeObject");
        }
        
        Object curObj = ctx.getObj();
        
        // 获取待序列化对象的序列化描述符
        ObjectStreamClass curDesc = ctx.getDesc();
        
        bout.setBlockDataMode(false);
        
        // 对指定的对象curObj执行默认的序列化过程
        defaultWriteFields(curObj, curDesc);
        
        bout.setBlockDataMode(true);
    }
    
    
    /**
     * Method used by subclasses to override the default writeObject method.
     * This method is called by trusted subclasses of ObjectInputStream that
     * constructed ObjectInputStream using the protected no-arg constructor.
     * The subclass is expected to provide an override method with the modifier
     * "final".
     *
     * @param obj object to be written to the underlying stream
     *
     * @throws IOException if there are I/O errors while writing to the
     *                     underlying stream
     * @see #ObjectOutputStream()
     * @see #writeObject(Object)
     * @since 1.2
     */
    // [子类覆盖]如果开启了enableOverride，则会调用该方法来替代默认的序列化逻辑
    protected void writeObjectOverride(Object obj) throws IOException {
    }
    
    /**
     * Subclasses may implement this method to allow class data to be stored in the stream.
     * By default this method does nothing.
     * The corresponding method in ObjectInputStream is resolveClass.
     * This method is called exactly once for each unique class in the stream.
     * The class name and signature will have already been written to the stream.
     * This method may make free use of the ObjectOutputStream to save any representation of
     * the class it deems suitable (for example, the bytes of the class file).
     * The resolveClass method in the corresponding subclass of
     * ObjectInputStream must read and use any data or objects written by annotateClass.
     *
     * @param cl the class to annotate custom data for
     *
     * @throws IOException Any exception thrown by the underlying OutputStream.
     * @see ObjectInputStream#resolveClass(ObjectStreamClass)
     */
    // [子类覆盖]由子类重写的回调方法，在写入非代理类序列化描述符之后被调用
    protected void annotateClass(Class<?> cl) throws IOException {
    }
    
    /**
     * Subclasses may implement this method to store custom data in the stream
     * along with descriptors for dynamic proxy classes.
     *
     * <p>This method is called exactly once for each unique proxy class
     * descriptor in the stream.  The default implementation of this method in
     * <code>ObjectOutputStream</code> does nothing.
     *
     * <p>The corresponding method in <code>ObjectInputStream</code> is <code>resolveProxyClass</code>.
     * For a given subclass of <code>ObjectOutputStream</code> that overrides this method,
     * the <code>resolveProxyClass</code> method in the corresponding subclass of
     * <code>ObjectInputStream</code> must read any data or objects written by <code>annotateProxyClass</code>.
     *
     * @param cl the proxy class to annotate custom data for
     *
     * @throws IOException any exception thrown by the underlying <code>OutputStream</code>
     * @see ObjectInputStream#resolveProxyClass(String[])
     * @since 1.3
     */
    // [子类覆盖]由子类重写的回调方法，在写入代理类序列化描述符之后被调用
    protected void annotateProxyClass(Class<?> cl) throws IOException {
    }
    
    /**
     * This method will allow trusted subclasses of ObjectOutputStream to
     * substitute one object for another during serialization. Replacing
     * objects is disabled until enableReplaceObject is called. The
     * enableReplaceObject method checks that the stream requesting to do
     * replacement can be trusted.  The first occurrence of each object written
     * into the serialization stream is passed to replaceObject.  Subsequent
     * references to the object are replaced by the object returned by the
     * original call to replaceObject.  To ensure that the private state of
     * objects is not unintentionally exposed, only trusted streams may use
     * replaceObject.
     *
     * <p>The ObjectOutputStream.writeObject method takes a parameter of type
     * Object (as opposed to type Serializable) to allow for cases where
     * non-serializable objects are replaced by serializable ones.
     *
     * <p>When a subclass is replacing objects it must insure that either a
     * complementary substitution must be made during deserialization or that
     * the substituted object is compatible with every field where the
     * reference will be stored.  Objects whose type is not a subclass of the
     * type of the field or array element abort the serialization by raising an
     * exception and the object is not be stored.
     *
     * <p>This method is called only once when each object is first
     * encountered.  All subsequent references to the object will be redirected
     * to the new object. This method should return the object to be
     * substituted or the original object.
     *
     * <p>Null can be returned as the object to be substituted, but may cause
     * NullReferenceException in classes that contain references to the
     * original object since they may be expecting an object instead of
     * null.
     *
     * @param obj the object to be replaced
     *
     * @return the alternate object that replaced the specified one
     *
     * @throws IOException Any exception thrown by the underlying
     *                     OutputStream.
     */
    // [子类覆盖]如果写入的是普通对象，且开启了enableReplace，则在序列化前会调用该方法进行对象替换
    protected Object replaceObject(Object obj) throws IOException {
        return obj;
    }
    
    /**
     * The writeStreamHeader method is provided so subclasses can append or prepend their own header to the stream.
     * It writes the magic number and version to the stream.
     *
     * @throws IOException if I/O errors occur while writing to the underlying stream
     */
    // [子类覆盖]写入序列化头（包含一个魔数和一个版本号）
    protected void writeStreamHeader() throws IOException {
        bout.writeShort(STREAM_MAGIC);
        bout.writeShort(STREAM_VERSION);
    }
    
    
    /**
     * Underlying writeObject/writeUnshared implementation.
     */
    // 默认的序列化逻辑：将指定的对象obj写入输出流，unshared指示该对象是否为非共享
    private void writeObject0(Object obj, boolean unshared) throws IOException {
        // 设置块数据模式为false，并返回旧的模式
        boolean oldMode = bout.setBlockDataMode(false);
        
        depth++;
        
        try {
            // handle previously written and non-replaceable objects
            int h;
            
            // 获取obj的替换对象，如果不存在，返回其自身
            obj = subs.lookup(obj);
            
            if(obj == null) {
                writeNull();    // 向最终输出流写入null
                return;
                
                // 如果obj是共享对象，且obj已存在于对象顺序表中
            } else if(!unshared && (h = handles.lookup(obj)) != -1) {
                writeHandle(h); // 向最终输出流写入共享对象在对象顺序表中的位置
                return;
                
                // 如果obj是类对象
            } else if(obj instanceof Class) {
                writeClass((Class) obj, unshared);  // 向最终输出流写入类对象obj的序列化描述符信息
                return;
            } else if(obj instanceof ObjectStreamClass) {
                writeClassDesc((ObjectStreamClass) obj, unshared);
                return;
            }
            
            /* check for replacement object */
            // 记录待序列化对象的原始形式
            Object orig = obj;
            
            // 待序列化对象的类型
            Class<?> cl = obj.getClass();
            
            // 待序列化类的序列化描述符
            ObjectStreamClass desc;
            
            for(; ; ) {
                // REMIND: skip this check for strings/arrays?
                Class<?> repCl;
                
                // 获取类对象cl的序列化描述符，返回之前会先去缓存中查找
                desc = ObjectStreamClass.lookup(cl, true);
                
                if(!desc.hasWriteReplaceMethod()    // 是否包含writeReplace方法
                    || (obj = desc.invokeWriteReplace(obj)) == null // 调用obj对象的writeReplace方法，生成的对象是否为null
                    || (repCl = obj.getClass()) == cl) {    // 被替换之后的对象的类型是否维持原样
                    break;
                }
                
                cl = repCl;
            }
            
            // 如果需要调用replaceObject()方法(默认为false)
            if(enableReplace) {
                // 替换对象
                Object rep = replaceObject(obj);
                
                // 如果对象发生了变化
                if(rep != obj && rep != null) {
                    cl = rep.getClass();
                    // 获取类对象cl的序列化描述符，返回之前会先去缓存中查找。
                    desc = ObjectStreamClass.lookup(cl, true);
                }
                
                obj = rep;
            }
            
            /* if object replaced, run through original checks a second time */
            // 如果待序列化对象已被替换，需要再次检查对象类型
            if(obj != orig) {
                // 将orig插入到HandleTable的对象顺序表，并将obj插入到ReplaceTable的替换对象顺序表中
                subs.assign(orig, obj);
                
                if(obj == null) {
                    writeNull();
                    return;
                } else if(!unshared && (h = handles.lookup(obj)) != -1) {
                    writeHandle(h);
                    return;
                } else if(obj instanceof Class) {
                    writeClass((Class) obj, unshared);
                    return;
                } else if(obj instanceof ObjectStreamClass) {
                    writeClassDesc((ObjectStreamClass) obj, unshared);
                    return;
                }
            }
            
            /* remaining cases */
            // 对String类型的对象进行序列化
            if(obj instanceof String) {
                writeString((String) obj, unshared);
                
                // 对数组类型的对象进行序列化
            } else if(cl.isArray()) {
                writeArray(obj, desc, unshared);
                
                // 对枚举类型的对象进行序列化
            } else if(obj instanceof Enum) {
                writeEnum((Enum<?>) obj, desc, unshared);
                
                // 对Serializable类型(包含Externalizable类型)的对象进行序列化
            } else if(obj instanceof Serializable) {
                writeOrdinaryObject(obj, desc, unshared);
            } else {
                if(extendedDebugInfo) {
                    throw new NotSerializableException(cl.getName() + "\n" + debugInfoStack.toString());
                } else {
                    throw new NotSerializableException(cl.getName());
                }
            }
        } finally {
            depth--;
            bout.setBlockDataMode(oldMode);
        }
    }
    
    /**
     * Fetches and writes values of serializable fields of given object to
     * stream.  The given class descriptor specifies which field values to
     * write, and in which order they should be written.
     */
    // 对指定的对象obj执行默认的序列化过程
    private void defaultWriteFields(Object obj, ObjectStreamClass desc) throws IOException {
        Class<?> cl = desc.forClass();
        if(cl != null && obj != null && !cl.isInstance(obj)) {
            throw new ClassCastException();
        }
        
        // 确保序列化描述符已初始化，且其中不包含无效字段
        desc.checkDefaultSerialize();
        
        // 获取fields中基本类型字段所占字节数
        int primDataSize = desc.getPrimDataSize();
        if(primDataSize>0) {
            if(primVals == null || primVals.length<primDataSize) {
                primVals = new byte[primDataSize];
            }
            
            // 获取obj中所有待序列化的基本类型字段的值
            desc.getPrimFieldValues(obj, primVals);
            
            // 向最终输出流（包括块数据缓冲区）写入字节数组b中的数据(此处会直接将数据写入最终输出流)
            bout.write(primVals, 0, primDataSize, false);
        }
        
        // 获取fields中引用类型字段数量
        int numObjFields = desc.getNumObjFields();
        if(numObjFields>0) {
            // 获取待序列化字段
            ObjectStreamField[] fields = desc.getFields(false);
            
            // 存储引用类型字段值
            Object[] objVals = new Object[numObjFields];
            
            // 计算基本类型字段数量
            int numPrimFields = fields.length - objVals.length;
            
            // 获取待序列化的引用类型字段的值
            desc.getObjFieldValues(obj, objVals);
            
            // 遍历引用类型字段
            for(int i = 0; i<objVals.length; i++) {
                if(extendedDebugInfo) {
                    debugInfoStack.push("field (class \"" + desc.getName() + "\", name: \"" + fields[numPrimFields + i].getName() + "\", type: \"" + fields[numPrimFields + i].getType() + "\")");
                }
                
                try {
                    // 获取i处的对象是否非共享
                    boolean unshared = fields[numPrimFields + i].isUnshared();
                    // 默认的序列化逻辑：将指定的对象obj写入输出流，unshared指示该对象是否为非共享
                    writeObject0(objVals[i], unshared);
                } finally {
                    if(extendedDebugInfo) {
                        debugInfoStack.pop();
                    }
                }
            }
        }
    }
    
    
    
    /**
     * Retrieve the object used to buffer persistent fields to be written to the stream.
     * The fields will be written to the stream when writeFields method is called.
     *
     * @return an instance of the class Putfield that holds the serializable fields
     *
     * @throws IOException if I/O errors occur
     * @since 1.2
     */
    /*
     * 返回一个PutField对象以存放待序列化字段
     *
     * 注：该方法需要在待序列化对象自行实现的writeObject()方法内被调用
     */
    public ObjectOutputStream.PutField putFields() throws IOException {
        if(curPut == null) {
            SerialCallbackContext ctx = curContext;
            
            // 只有待序列化的对象自己实现了writeObject()方法，这里的ctx才非空(参见writeSerialData)
            if(ctx == null) {
                throw new NotActiveException("not in call to writeObject");
            }
            
            ctx.checkAndSetUsed();
            
            // 获取待序列化对象的序列化描述符
            ObjectStreamClass curDesc = ctx.getDesc();
            
            // 构造一个PutField对象以存放待序列化字段
            curPut = new PutFieldImpl(curDesc);
        }
        
        return curPut;
    }
    
    /**
     * Write the buffered fields to the stream.
     *
     * @throws IOException        if I/O errors occur while writing to the underlying
     *                            stream
     * @throws NotActiveException Called when a classes writeObject method was
     *                            not called to write the state of the object.
     * @since 1.2
     */
    // 向当前输出流写入待序列化的字段
    public void writeFields() throws IOException {
        if(curPut == null) {
            throw new NotActiveException("no current PutField object");
        }
        
        bout.setBlockDataMode(false);
        curPut.writeFields();   // 向当前输出流写入待序列化的字段
        bout.setBlockDataMode(true);
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 序列化-内部实现 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Writes null code to stream.
     */
    // 向最终输出流写入null
    private void writeNull() throws IOException {
        // 向最终输出流写入TC_NULL标记(null)
        bout.writeByte(TC_NULL);
    }
    
    /**
     * Writes given object handle to stream.
     */
    // 向最终输出流写入共享对象在对象顺序表中的位置
    private void writeHandle(int handle) throws IOException {
        // 向最终输出流写入TC_REFERENCE标记(对已写入流中的对象的引用)
        bout.writeByte(TC_REFERENCE);
        
        // 写入该对象在对象顺序表中的位置
        bout.writeInt(baseWireHandle + handle);
    }
    
    /**
     * Writes representation of given class descriptor to stream.
     */
    // 将给定的序列化描述符写到输出流中，unshared指示该对象是否共享
    private void writeClassDesc(ObjectStreamClass desc, boolean unshared) throws IOException {
        int handle;
        
        // 如果是null
        if(desc == null) {
            // 向最终输出流写入null
            writeNull();
            
            // 如果是共享对象，则查找其在对象顺序表中的位置
        } else if(!unshared && (handle = handles.lookup(desc)) != -1) {
            // 向最终输出流写入共享对象desc在对象顺序表中的位置
            writeHandle(handle);
            
            // 如果是代理对象
        } else if(desc.isProxy()) {
            // 向最终输出流写入代理对象desc，unshared指示该对象是否共享
            writeProxyDesc(desc, unshared);
            
            // 如果是普通对象(非null，非共享，非代理)
        } else {
            writeNonProxyDesc(desc, unshared);
        }
    }
    
    /**
     * Writes representation of given class to stream.
     */
    // 向最终输出流写入类对象cl的序列化描述符信息
    private void writeClass(Class<?> cl, boolean unshared) throws IOException {
        // 向最终输出流写入TC_CLASS标记(类对象的序列化描述符)
        bout.writeByte(TC_CLASS);
        
        // 获取类对象cl的序列化描述符
        ObjectStreamClass objectStreamClass = ObjectStreamClass.lookup(cl, true);
        
        writeClassDesc(objectStreamClass, false);
        
        // 将cl信息插入到HandleTable中，如果unshared为true，则插入null
        handles.assign(unshared ? null : cl);
    }
    
    /**
     * Writes class descriptor representing a dynamic proxy class to stream.
     */
    // 向最终输出流写入代理对象的序列化描述符desc，unshared指示该对象是否共享
    private void writeProxyDesc(ObjectStreamClass desc, boolean unshared) throws IOException {
        // 向最终输出流写入TC_PROXYCLASSDESC标记(代理对象)
        bout.writeByte(TC_PROXYCLASSDESC);
        
        // 将desc信息插入到HandleTable中
        handles.assign(unshared ? null : desc);
        
        // 获取待序列化的类型
        Class<?> cl = desc.forClass();
        
        // 获取cl类的父接口
        Class<?>[] ifaces = cl.getInterfaces();
        
        // 向块数据输出流写入接口数量（按大端法写入）
        bout.writeInt(ifaces.length);
        
        // 向块数据输出流写入接口名称
        for(Class<?> iface : ifaces) {
            bout.writeUTF(iface.getName());
        }
        
        // 启用待写数据的块模式
        bout.setBlockDataMode(true);
        
        // 如果当前类是自定义的ObjectOutputStream的子类
        if(isCustomSubclass()) {
            // 检查当前类对cl所在的包的访问权限
            ReflectUtil.checkPackageAccess(cl);
        }
        
        annotateProxyClass(cl);
        
        // 禁用待写数据的块模式
        bout.setBlockDataMode(false);
        
        bout.writeByte(TC_ENDBLOCKDATA);
        
        // 获取cl的父类的序列化描述符
        ObjectStreamClass superDesc = desc.getSuperDesc();
        // 将给定的序列化描述符写到当前输出流中，unshared指示该对象是否共享
        writeClassDesc(superDesc, false);
    }
    
    /**
     * Writes class descriptor representing a standard (i.e., not a dynamic proxy) class to stream.
     */
    // 向最终输出流写入非代理对象的序列化描述符desc，unshared指示该对象是否共享
    private void writeNonProxyDesc(ObjectStreamClass desc, boolean unshared) throws IOException {
        // 向块数据输出流写入TC_CLASSDESC标记：普通对象(非null，非共享，非代理)的序列化描述符
        bout.writeByte(TC_CLASSDESC);
        
        // 如果desc是共享对象，则将其信息插入到HandleTable中
        handles.assign(unshared ? null : desc);
        
        // 向当前输出流写入非代理对象的序列化描述符
        if(protocol == PROTOCOL_VERSION_1) {
            /* do not invoke class descriptor write hook with old protocol */
            desc.writeNonProxy(this);   // 版本1中只能使用默认的序列化逻辑
        } else {
            writeClassDescriptor(desc); // 版本2中允许对默认的序列化逻辑进行重写
        }
        
        // 获取待序列化的类型
        Class<?> cl = desc.forClass();
        
        // 启用待写数据的块模式
        bout.setBlockDataMode(true);
        
        if(cl != null && isCustomSubclass()) {
            ReflectUtil.checkPackageAccess(cl);
        }
        
        annotateClass(cl);
        
        // 禁用待写数据的块模式
        bout.setBlockDataMode(false);
        
        bout.writeByte(TC_ENDBLOCKDATA);
        
        // 获取cl的父类的序列化描述符
        ObjectStreamClass superDesc = desc.getSuperDesc();
        // 将给定的序列化描述符写到当前输出流中，unshared指示该对象是否共享
        writeClassDesc(superDesc, false);
    }
    
    /**
     * Write the specified class descriptor to the ObjectOutputStream.  Class
     * descriptors are used to identify the classes of objects written to the
     * stream.  Subclasses of ObjectOutputStream may override this method to
     * customize the way in which class descriptors are written to the
     * serialization stream.  The corresponding method in ObjectInputStream,
     * <code>readClassDescriptor</code>, should then be overridden to
     * reconstitute the class descriptor from its custom stream representation.
     * By default, this method writes class descriptors according to the format
     * defined in the Object Serialization specification.
     *
     * <p>Note that this method will only be called if the ObjectOutputStream
     * is not using the old serialization stream format (set by calling
     * ObjectOutputStream's <code>useProtocolVersion</code> method).  If this
     * serialization stream is using the old format
     * (<code>PROTOCOL_VERSION_1</code>), the class descriptor will be written
     * internally in a manner that cannot be overridden or customized.
     *
     * @param desc class descriptor to write to the stream
     *
     * @throws IOException If an I/O error has occurred.
     * @see java.io.ObjectInputStream#readClassDescriptor()
     * @see #useProtocolVersion(int)
     * @see java.io.ObjectStreamConstants#PROTOCOL_VERSION_1
     * @since 1.3
     */
    // 向当前输出流写入非代理对象的序列化描述符desc(允许实现类去重写该方法)
    protected void writeClassDescriptor(ObjectStreamClass desc) throws IOException {
        desc.writeNonProxy(this);
    }
    
    /**
     * Writes given string to stream, using standard or long UTF format
     * depending on string length.
     */
    // 对String类型的对象进行序列化
    private void writeString(String str, boolean unshared) throws IOException {
        handles.assign(unshared ? null : str);
        
        // 获取字符串s的UTF8形式的字节数量
        long utflen = bout.getUTFLength(str);
        
        // 小字符串：utf8字节数量在0xFFFF范围内
        if(utflen<=0xFFFF) {
            bout.writeByte(TC_STRING);
            bout.writeUTF(str, utflen);
            
            // 大字符串：utf8字节数量超出0xFFFF范围
        } else {
            bout.writeByte(TC_LONGSTRING);
            bout.writeLongUTF(str, utflen);
        }
    }
    
    /**
     * Writes given array object to stream.
     */
    // 对数组类型的对象进行序列化
    private void writeArray(Object array, ObjectStreamClass desc, boolean unshared) throws IOException {
        bout.writeByte(TC_ARRAY);
        
        writeClassDesc(desc, false);
        
        handles.assign(unshared ? null : array);
        
        Class<?> ccl = desc.forClass().getComponentType();
        
        if(ccl.isPrimitive()) {
            if(ccl == Integer.TYPE) {
                int[] ia = (int[]) array;
                bout.writeInt(ia.length);
                bout.writeInts(ia, 0, ia.length);
            } else if(ccl == Byte.TYPE) {
                byte[] ba = (byte[]) array;
                bout.writeInt(ba.length);
                bout.write(ba, 0, ba.length, true);
            } else if(ccl == Long.TYPE) {
                long[] ja = (long[]) array;
                bout.writeInt(ja.length);
                bout.writeLongs(ja, 0, ja.length);
            } else if(ccl == Float.TYPE) {
                float[] fa = (float[]) array;
                bout.writeInt(fa.length);
                bout.writeFloats(fa, 0, fa.length);
            } else if(ccl == Double.TYPE) {
                double[] da = (double[]) array;
                bout.writeInt(da.length);
                bout.writeDoubles(da, 0, da.length);
            } else if(ccl == Short.TYPE) {
                short[] sa = (short[]) array;
                bout.writeInt(sa.length);
                bout.writeShorts(sa, 0, sa.length);
            } else if(ccl == Character.TYPE) {
                char[] ca = (char[]) array;
                bout.writeInt(ca.length);
                bout.writeChars(ca, 0, ca.length);
            } else if(ccl == Boolean.TYPE) {
                boolean[] za = (boolean[]) array;
                bout.writeInt(za.length);
                bout.writeBooleans(za, 0, za.length);
            } else {
                throw new InternalError();
            }
        } else {
            Object[] objs = (Object[]) array;
            int len = objs.length;
            bout.writeInt(len);
            if(extendedDebugInfo) {
                debugInfoStack.push("array (class \"" + array.getClass().getName() + "\", size: " + len + ")");
            }
            try {
                for(int i = 0; i<len; i++) {
                    if(extendedDebugInfo) {
                        debugInfoStack.push("element of array (index: " + i + ")");
                    }
                    try {
                        // 将指定的共享对象objs[i]写入输出流
                        writeObject0(objs[i], false);
                    } finally {
                        if(extendedDebugInfo) {
                            debugInfoStack.pop();
                        }
                    }
                }
            } finally {
                if(extendedDebugInfo) {
                    debugInfoStack.pop();
                }
            }
        }
    }
    
    /**
     * Writes given enum constant to stream.
     */
    // 对枚举类型的对象进行序列化
    private void writeEnum(Enum<?> en, ObjectStreamClass desc, boolean unshared) throws IOException {
        bout.writeByte(TC_ENUM);
        ObjectStreamClass sdesc = desc.getSuperDesc();
        writeClassDesc((sdesc.forClass() == Enum.class) ? desc : sdesc, false);
        handles.assign(unshared ? null : en);
        writeString(en.name(), false);
    }
    
    /**
     * Writes representation of a "ordinary" (i.e., not a String, Class,
     * ObjectStreamClass, array, or enum constant) serializable object to the
     * stream.
     */
    // 对Serializable类型(包含Externalizable类型)的对象进行序列化
    private void writeOrdinaryObject(Object obj, ObjectStreamClass desc, boolean unshared) throws IOException {
        if(extendedDebugInfo) {
            debugInfoStack.push((depth == 1 ? "root " : "") + "object (class \"" + obj.getClass().getName() + "\", " + obj.toString() + ")");
        }
        
        try {
            desc.checkSerialize();
            
            bout.writeByte(TC_OBJECT);
            
            writeClassDesc(desc, false);
            
            handles.assign(unshared ? null : obj);
            
            // 如果待序列化对象为Externalizable类的非代理实现类
            if(desc.isExternalizable() && !desc.isProxy()) {
                // 对Externalizable类型的对象进行序列化
                writeExternalData((Externalizable) obj);
            } else {
                // 对Serializable类型(非Externalizable类型)的对象进行序列化
                writeSerialData(obj, desc);
            }
        } finally {
            if(extendedDebugInfo) {
                debugInfoStack.pop();
            }
        }
    }
    
    /**
     * Writes externalizable data of given object by invoking its writeExternal() method.
     */
    // 对Externalizable类型的对象进行序列化
    private void writeExternalData(Externalizable obj) throws IOException {
        if(extendedDebugInfo) {
            debugInfoStack.push("writeExternal data");
        }
        
        PutFieldImpl oldPut = curPut;   // 记录旧的(外层)PutField对象(因为在writeExternal()中可能会被改变)
        curPut = null;
        
        SerialCallbackContext oldContext = curContext;  // 记录旧的(外层)SerialCallbackContext对象
        
        try {
            curContext = null;
            
            if(protocol == PROTOCOL_VERSION_1) {
                obj.writeExternal(this);        // 调用obj对象实现的序列化逻辑
            } else {
                bout.setBlockDataMode(true);    // 启用待写数据的块模式
                obj.writeExternal(this);        // 调用obj对象实现的序列化逻辑
                bout.setBlockDataMode(false);   // 禁用待写数据的块模式
                bout.writeByte(TC_ENDBLOCKDATA);
            }
        } finally {
            curContext = oldContext;    // 恢复旧的SerialCallbackContext对象
            if(extendedDebugInfo) {
                debugInfoStack.pop();
            }
        }
        
        curPut = oldPut;    // 恢复旧的PutField对象
    }
    
    /**
     * Writes instance data for each serializable class of given object, from
     * superclass to subclass.
     */
    // 对Serializable类型(非Externalizable类型)的对象进行序列化
    private void writeSerialData(Object obj, ObjectStreamClass desc) throws IOException {
        // 返回数据槽：包含从当前类到最上层实现了Serializable接口的父类的所有序列化描述符
        ObjectStreamClass.ClassDataSlot[] slots = desc.getClassDataLayout();
        
        // 遍历数据槽（父类的数据槽靠前）
        for(ObjectStreamClass.ClassDataSlot slot : slots) {
            // 获取序列化描述符
            ObjectStreamClass slotDesc = slot.desc;
            
            // 当前Serializable实现类中包含writeObject方法
            if(slotDesc.hasWriteObjectMethod()) {
                if(extendedDebugInfo) {
                    debugInfoStack.push("custom writeObject data (class \"" + slotDesc.getName() + "\")");
                }
                
                PutFieldImpl oldPut = curPut;   // 记录旧的(外层)PutField对象(因为在writeObject()中可能会被改变)
                curPut = null;
                
                SerialCallbackContext oldContext = curContext;  // 记录旧的(外层)SerialCallbackContext对象
                
                try {
                    curContext = new SerialCallbackContext(obj, slotDesc);
                    
                    bout.setBlockDataMode(true);    // 设置块数据模式为true
                    
                    // 调用obj对象的writeObject()方法
                    slotDesc.invokeWriteObject(obj, this);
                    
                    bout.setBlockDataMode(false);   // 设置块数据模式为false
                    
                    // 向最终输出流写入TC_ENDBLOCKDATA标记(对象的可选数据块的结尾)
                    bout.writeByte(TC_ENDBLOCKDATA);
                } finally {
                    curContext.setUsed();
                    curContext = oldContext;    // 恢复旧的SerialCallbackContext对象
                    if(extendedDebugInfo) {
                        debugInfoStack.pop();
                    }
                }
                
                curPut = oldPut;    // 恢复旧的PutField对象
                
                // 当前Serializable实现类中不包含writeObject方法
            } else {
                // 对指定的对象obj执行默认的序列化过程
                defaultWriteFields(obj, slotDesc);
            }
        }
    }
    
    /**
     * Attempts to write to stream fatal IOException that has caused
     * serialization to abort.
     */
    // 向输出流写入异常信息
    private void writeFatalException(IOException ex) throws IOException {
        /*
         * Note: the serialization specification states that if a second
         * IOException occurs while attempting to serialize the original fatal
         * exception to the stream, then a StreamCorruptedException should be
         * thrown (section 2.1).  However, due to a bug in previous
         * implementations of serialization, StreamCorruptedExceptions were
         * rarely (if ever) actually thrown--the "root" exceptions from
         * underlying streams were thrown instead.  This historical behavior is
         * followed here for consistency.
         */
        clear();
        boolean oldMode = bout.setBlockDataMode(false);
        try {
            bout.writeByte(TC_EXCEPTION);
            // 将指定的共享对象objs[i]写入输出流
            writeObject0(ex, false);
            clear();
        } finally {
            bout.setBlockDataMode(oldMode);
        }
    }
    
    /**
     * Writes string without allowing it to be replaced in stream.  Used by
     * ObjectStreamClass to write class descriptor type strings.
     */
    // 向输出流写入字符串信息
    void writeTypeString(String str) throws IOException {
        int handle;
        if(str == null) {
            writeNull();
        } else if((handle = handles.lookup(str)) != -1) {
            writeHandle(handle);
        } else {
            writeString(str, false);
        }
    }
    
    /*▲ 序列化-内部实现 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼  ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Drain any buffered data in ObjectOutputStream.  Similar to flush but
     * does not propagate the flush to the underlying stream.
     *
     * @throws IOException if I/O errors occur while writing to the underlying
     *                     stream
     */
    // 将块数据缓冲区中的待写数据全部写入到最终输出流out
    protected void drain() throws IOException {
        bout.drain();
    }
    
    /**
     * Flushes the stream. This will write any buffered output bytes and flush
     * through to the underlying stream.
     *
     * @throws IOException If an I/O error has occurred.
     */
    // 将内外缓冲区中的数据写入到最终输出流
    public void flush() throws IOException {
        bout.flush();
    }
    
    /**
     * Reset will disregard the state of any objects already written to the
     * stream.  The state is reset to be the same as a new ObjectOutputStream.
     * The current point in the stream is marked as reset so the corresponding
     * ObjectInputStream will be reset at the same point.  Objects previously
     * written to the stream will not be referred to as already being in the
     * stream.  They will be written to the stream again.
     *
     * @throws IOException if reset() is invoked while serializing an object.
     */
    public void reset() throws IOException {
        if(depth != 0) {
            throw new IOException("stream active");
        }
        
        bout.setBlockDataMode(false);
        bout.writeByte(TC_RESET);
        clear();
        bout.setBlockDataMode(true);
    }
    
    /**
     * Closes the stream. This method must be called to release any resources
     * associated with the stream.
     *
     * @throws IOException If an I/O error has occurred.
     */
    public void close() throws IOException {
        flush();
        clear();
        bout.close();
    }
    
    /**
     * Clears internal data structures.
     */
    private void clear() {
        subs.clear();
        handles.clear();
    }
    
    /*▲  ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Enables the stream to do replacement of objects written to the stream.  When
     * enabled, the {@link #replaceObject} method is called for every object being
     * serialized.
     *
     * <p>If object replacement is currently not enabled, and
     * {@code enable} is true, and there is a security manager installed,
     * this method first calls the security manager's
     * {@code checkPermission} method with the
     * {@code SerializablePermission("enableSubstitution")} permission to
     * ensure that the caller is permitted to enable the stream to do replacement
     * of objects written to the stream.
     *
     * @param enable true for enabling use of {@code replaceObject} for
     *               every object being serialized
     *
     * @return the previous setting before this method was invoked
     *
     * @throws SecurityException if a security manager exists and its
     *                           {@code checkPermission} method denies enabling the stream
     *                           to do replacement of objects written to the stream.
     * @see SecurityManager#checkPermission
     * @see java.io.SerializablePermission
     */
    // 设置是否需要调用replaceObject()方法
    protected boolean enableReplaceObject(boolean enable) throws SecurityException {
        if(enable == enableReplace) {
            return enable;
        }
        
        if(enable) {
            SecurityManager sm = System.getSecurityManager();
            if(sm != null) {
                sm.checkPermission(SUBSTITUTION_PERMISSION);
            }
        }
        
        enableReplace = enable;
        
        return !enableReplace;
    }
    
    /**
     * Specify stream protocol version to use when writing the stream.
     *
     * <p>This routine provides a hook to enable the current version of
     * Serialization to write in a format that is backwards compatible to a
     * previous version of the stream format.
     *
     * <p>Every effort will be made to avoid introducing additional
     * backwards incompatibilities; however, sometimes there is no
     * other alternative.
     *
     * @param version use ProtocolVersion from java.io.ObjectStreamConstants.
     *
     * @throws IllegalStateException    if called after any objects
     *                                  have been serialized.
     * @throws IllegalArgumentException if invalid version is passed in.
     * @throws IOException              if I/O errors occur
     * @see java.io.ObjectStreamConstants#PROTOCOL_VERSION_1
     * @see java.io.ObjectStreamConstants#PROTOCOL_VERSION_2
     * @since 1.2
     */
    // 指定序列化版本信息
    public void useProtocolVersion(int version) throws IOException {
        if(handles.size() != 0) {
            // REMIND: implement better check for pristine stream?
            throw new IllegalStateException("stream non-empty");
        }
        
        switch(version) {
            case PROTOCOL_VERSION_1:
            case PROTOCOL_VERSION_2:
                protocol = version;
                break;
            
            default:
                throw new IllegalArgumentException("unknown version: " + version);
        }
    }
    
    /**
     * Returns protocol version in use.
     */
    // 获取序列化版本信息
    int getProtocolVersion() {
        return protocol;
    }
    
    // 判断当前类是否为自定义的ObjectOutputStream的子类
    private boolean isCustomSubclass() {
        // Return true if this class is a custom subclass of ObjectOutputStream
        return getClass().getClassLoader() != ObjectOutputStream.class.getClassLoader();
    }
    
    /**
     * Verifies that this (possibly subclass) instance can be constructed
     * without violating security constraints: the subclass must not override
     * security-sensitive non-final methods, or else the
     * "enableSubclassImplementation" SerializablePermission is checked.
     */
    private void verifySubclass() {
        Class<?> cl = getClass();
        if(cl == ObjectOutputStream.class) {
            return;
        }
        SecurityManager sm = System.getSecurityManager();
        if(sm == null) {
            return;
        }
        ObjectStreamClass.processQueue(Caches.subclassAuditsQueue, Caches.subclassAudits);
        WeakClassKey key = new WeakClassKey(cl, Caches.subclassAuditsQueue);
        Boolean result = Caches.subclassAudits.get(key);
        if(result == null) {
            result = auditSubclass(cl);
            Caches.subclassAudits.putIfAbsent(key, result);
        }
        if(!result) {
            sm.checkPermission(SUBCLASS_IMPLEMENTATION_PERMISSION);
        }
    }
    
    /**
     * Performs reflective checks on given subclass to verify that it doesn't
     * override security-sensitive non-final methods.  Returns TRUE if subclass
     * is "safe", FALSE otherwise.
     */
    private static Boolean auditSubclass(Class<?> subcl) {
        return AccessController.doPrivileged(new PrivilegedAction<>() {
            public Boolean run() {
                for(Class<?> cl = subcl; cl != ObjectOutputStream.class; cl = cl.getSuperclass()) {
                    try {
                        cl.getDeclaredMethod("writeUnshared", Object.class);
                        return Boolean.FALSE;
                    } catch(NoSuchMethodException ex) {
                    }
                    try {
                        cl.getDeclaredMethod("putFields", (Class<?>[]) null);
                        return Boolean.FALSE;
                    } catch(NoSuchMethodException ex) {
                    }
                }
                return Boolean.TRUE;
            }
        });
    }
    
    /**
     * Converts specified span of float values into byte values.
     */
    // REMIND: remove once hotspot inlines Float.floatToIntBits
    private static native void floatsToBytes(float[] src, int srcpos, byte[] dst, int dstpos, int nfloats);
    
    /**
     * Converts specified span of double values into byte values.
     */
    // REMIND: remove once hotspot inlines Double.doubleToLongBits
    private static native void doublesToBytes(double[] src, int srcpos, byte[] dst, int dstpos, int ndoubles);
    
    
    
    
    
    
    /**
     * Provide programmatic access to the persistent fields to be written to ObjectOutput.
     *
     * @since 1.2
     */
    // 待序列化字段集
    public abstract static class PutField {
        
        /**
         * Put the value of the named boolean field into the persistent field.
         *
         * @param name the name of the serializable field
         * @param val  the value to assign to the field
         *
         * @throws IllegalArgumentException if <code>name</code> does not
         *                                  match the name of a serializable field for the class whose fields
         *                                  are being written, or if the type of the named field is not
         *                                  <code>boolean</code>
         */
        public abstract void put(String name, boolean val);
        
        /**
         * Put the value of the named byte field into the persistent field.
         *
         * @param name the name of the serializable field
         * @param val  the value to assign to the field
         *
         * @throws IllegalArgumentException if <code>name</code> does not
         *                                  match the name of a serializable field for the class whose fields
         *                                  are being written, or if the type of the named field is not
         *                                  <code>byte</code>
         */
        public abstract void put(String name, byte val);
        
        /**
         * Put the value of the named char field into the persistent field.
         *
         * @param name the name of the serializable field
         * @param val  the value to assign to the field
         *
         * @throws IllegalArgumentException if <code>name</code> does not
         *                                  match the name of a serializable field for the class whose fields
         *                                  are being written, or if the type of the named field is not
         *                                  <code>char</code>
         */
        public abstract void put(String name, char val);
        
        /**
         * Put the value of the named short field into the persistent field.
         *
         * @param name the name of the serializable field
         * @param val  the value to assign to the field
         *
         * @throws IllegalArgumentException if <code>name</code> does not
         *                                  match the name of a serializable field for the class whose fields
         *                                  are being written, or if the type of the named field is not
         *                                  <code>short</code>
         */
        public abstract void put(String name, short val);
        
        /**
         * Put the value of the named int field into the persistent field.
         *
         * @param name the name of the serializable field
         * @param val  the value to assign to the field
         *
         * @throws IllegalArgumentException if <code>name</code> does not
         *                                  match the name of a serializable field for the class whose fields
         *                                  are being written, or if the type of the named field is not
         *                                  <code>int</code>
         */
        public abstract void put(String name, int val);
        
        /**
         * Put the value of the named long field into the persistent field.
         *
         * @param name the name of the serializable field
         * @param val  the value to assign to the field
         *
         * @throws IllegalArgumentException if <code>name</code> does not
         *                                  match the name of a serializable field for the class whose fields
         *                                  are being written, or if the type of the named field is not
         *                                  <code>long</code>
         */
        public abstract void put(String name, long val);
        
        /**
         * Put the value of the named float field into the persistent field.
         *
         * @param name the name of the serializable field
         * @param val  the value to assign to the field
         *
         * @throws IllegalArgumentException if <code>name</code> does not
         *                                  match the name of a serializable field for the class whose fields
         *                                  are being written, or if the type of the named field is not
         *                                  <code>float</code>
         */
        public abstract void put(String name, float val);
        
        /**
         * Put the value of the named double field into the persistent field.
         *
         * @param name the name of the serializable field
         * @param val  the value to assign to the field
         *
         * @throws IllegalArgumentException if <code>name</code> does not
         *                                  match the name of a serializable field for the class whose fields
         *                                  are being written, or if the type of the named field is not
         *                                  <code>double</code>
         */
        public abstract void put(String name, double val);
        
        /**
         * Put the value of the named Object field into the persistent field.
         *
         * @param name the name of the serializable field
         * @param val  the value to assign to the field
         *             (which may be <code>null</code>)
         *
         * @throws IllegalArgumentException if <code>name</code> does not
         *                                  match the name of a serializable field for the class whose fields
         *                                  are being written, or if the type of the named field is not a
         *                                  reference type
         */
        public abstract void put(String name, Object val);
        
        /**
         * Write the data and fields to the specified ObjectOutput stream,
         * which must be the same stream that produced this
         * <code>PutField</code> object.
         *
         * @param out the stream to write the data and fields to
         *
         * @throws IOException              if I/O errors occur while writing to the
         *                                  underlying stream
         * @throws IllegalArgumentException if the specified stream is not
         *                                  the same stream that produced this <code>PutField</code>
         *                                  object
         * @deprecated This method does not write the values contained by this
         * <code>PutField</code> object in a proper format, and may
         * result in corruption of the serialization stream.  The
         * correct way to write <code>PutField</code> data is by
         * calling the {@link java.io.ObjectOutputStream#writeFields()}
         * method.
         */
        @Deprecated
        public abstract void write(ObjectOutput out) throws IOException;
    }
    
    /**
     * Default PutField implementation.
     */
    // 待序列化字段集的默认实现
    private class PutFieldImpl extends PutField {
        
        /** class descriptor describing serializable fields */
        private final ObjectStreamClass desc;   // 待序列化对象的序列化描述符
        
        /** primitive field values */
        private final byte[] primVals;          // 待序列化的基本类型字段
        
        /** object field values */
        private final Object[] objVals;         // 待序列化的引用类型的字段
        
        /**
         * Creates PutFieldImpl object for writing fields defined in given class descriptor.
         */
        PutFieldImpl(ObjectStreamClass desc) {
            this.desc = desc;
            primVals = new byte[desc.getPrimDataSize()];
            objVals = new Object[desc.getNumObjFields()];
        }
        
        // 添加boolean类型的字段
        public void put(String name, boolean val) {
            Bits.putBoolean(primVals, getFieldOffset(name, Boolean.TYPE), val);
        }
        
        // 添加boolean类型的字段
        public void put(String name, byte val) {
            primVals[getFieldOffset(name, Byte.TYPE)] = val;
        }
        
        // 添加char类型的字段
        public void put(String name, char val) {
            Bits.putChar(primVals, getFieldOffset(name, Character.TYPE), val);
        }
        
        // 添加short类型的字段
        public void put(String name, short val) {
            Bits.putShort(primVals, getFieldOffset(name, Short.TYPE), val);
        }
        
        // 添加int类型的字段
        public void put(String name, int val) {
            Bits.putInt(primVals, getFieldOffset(name, Integer.TYPE), val);
        }
        
        // 添加float类型的字段
        public void put(String name, float val) {
            Bits.putFloat(primVals, getFieldOffset(name, Float.TYPE), val);
        }
        
        // 添加long类型的字段
        public void put(String name, long val) {
            Bits.putLong(primVals, getFieldOffset(name, Long.TYPE), val);
        }
        
        // 添加double类型的字段
        public void put(String name, double val) {
            Bits.putDouble(primVals, getFieldOffset(name, Double.TYPE), val);
        }
        
        // 添加引用类型的字段
        public void put(String name, Object val) {
            objVals[getFieldOffset(name, Object.class)] = val;
        }
        
        /* deprecated in ObjectOutputStream.PutField */
        // 已过时，由writeFields()替代
        public void write(ObjectOutput out) throws IOException {
            /*
             * Applications should *not* use this method to write PutField
             * data, as it will lead to stream corruption if the PutField
             * object writes any primitive data (since block data mode is not
             * unset/set properly, as is done in OOS.writeFields()).  This
             * broken implementation is being retained solely for behavioral
             * compatibility, in order to support applications which use
             * OOS.PutField.write() for writing only non-primitive data.
             *
             * Serialization of unshared objects is not implemented here since
             * it is not necessary for backwards compatibility; also, unshared
             * semantics may not be supported by the given ObjectOutput
             * instance.  Applications which write unshared objects using the
             * PutField API must use OOS.writeFields().
             */
            if(ObjectOutputStream.this != out) {
                throw new IllegalArgumentException("wrong stream");
            }
            out.write(primVals, 0, primVals.length);
            
            ObjectStreamField[] fields = desc.getFields(false);
            int numPrimFields = fields.length - objVals.length;
            // REMIND: warn if numPrimFields > 0?
            for(int i = 0; i<objVals.length; i++) {
                if(fields[numPrimFields + i].isUnshared()) {
                    throw new IOException("cannot write unshared object");
                }
                out.writeObject(objVals[i]);
            }
        }
        
        /**
         * Writes buffered primitive data and object fields to stream.
         */
        // 向当前输出流写入待序列化的字段
        void writeFields() throws IOException {
            // 向最终输出流（包括块数据缓冲区）写入字节数组b中的数据(此处会直接将数据写入最终输出流)
            bout.write(primVals, 0, primVals.length, false);
            
            // 获取待序列化字段
            ObjectStreamField[] fields = desc.getFields(false);
            
            // 计算基本类型字段数量
            int numPrimFields = fields.length - objVals.length;
            
            for(int i = 0; i<objVals.length; i++) {
                if(extendedDebugInfo) {
                    debugInfoStack.push("field (class \"" + desc.getName() + "\", name: \"" + fields[numPrimFields + i].getName() + "\", type: \"" + fields[numPrimFields + i].getType() + "\")");
                }
                try {
                    // 获取i处的对象是否非共享
                    boolean unshared = fields[numPrimFields + i].isUnshared();
                    // 默认的序列化逻辑：将指定的对象obj写入输出流，unshared指示该对象是否为非共享
                    writeObject0(objVals[i], unshared);
                } finally {
                    if(extendedDebugInfo) {
                        debugInfoStack.pop();
                    }
                }
            }
        }
        
        /**
         * Returns offset of field with given name and type.  A specified type
         * of null matches all types, Object.class matches all non-primitive
         * types, and any other non-null type matches assignable types only.
         * Throws IllegalArgumentException if no matching field found.
         */
        // 返回指定名称与类型的待序列化字段的偏移量
        private int getFieldOffset(String name, Class<?> type) {
            // 获取指定名称与类型的待序列化字段
            ObjectStreamField field = desc.getField(name, type);
            if(field == null) {
                throw new IllegalArgumentException("no such field " + name + " with type " + type);
            }
            
            // 返回字段的偏移量
            return field.getOffset();
        }
        
    }
    
    /**
     * Buffered output stream with two modes: in default mode,
     * outputs data in same format as DataOutputStream; in "block data" mode,
     * outputs data bracketed by block data markers (see object serialization specification for details).
     */
    // 块数据输出流，包含了最终输出流与基础数据输出流
    private static class BlockDataOutputStream extends OutputStream implements DataOutput {
        
        /** maximum data block length */
        private static final int MAX_BLOCK_SIZE = 1024;             // 块数据缓冲区的最大容量
        
        /** maximum data block header length */
        private static final int MAX_HEADER_SIZE = 5;
        
        /** (tunable) length of char buffer (for writing strings) */
        private static final int CHAR_BUF_SIZE = 256;
        
        /** current offset into buf */
        private int pos = 0;    // 记录块数据缓冲区中的字节数量
        
        /** buffer for writing general/block data */
        private final byte[] buf = new byte[MAX_BLOCK_SIZE];        // 块数据缓冲区
        
        /** buffer for writing block data headers */
        private final byte[] hbuf = new byte[MAX_HEADER_SIZE];      // 块数据头缓冲区
        
        /** char buffer for fast string writes */
        private final char[] cbuf = new char[CHAR_BUF_SIZE];        // 字符数据缓冲区(只取byte部分)
        
        /** loopback stream (for data writes that span data blocks) */
        private final DataOutputStream dout;    // 包装了当前块数据输出流this的【基础数据输出流】
        
        /** underlying output stream */
        private final OutputStream out;         // 【最终输出流】：数据写入的最终目的地，比如文件
        
        /** block data mode */
        private boolean blkmode = false;        // 待写数据是否处于块模式下（在块模式下写入数据时，会先写入一个包含长度信息的头信息）
        
        
        /**
         * Creates new BlockDataOutputStream on top of given underlying stream.
         * Block data mode is turned off by default.
         */
        BlockDataOutputStream(OutputStream out) {
            this.out = out;
            dout = new DataOutputStream(this);
        }
        
        
        // 向块数据输出流写入byte信息
        public void write(int b) throws IOException {
            // 如果块数据缓冲区已满
            if(pos >= MAX_BLOCK_SIZE) {
                drain();    // 将块数据缓冲区中的待写数据全部写入到最终输出流out
            }
            
            // 向块数据缓冲区写入byte值
            buf[pos++] = (byte) b;
        }
        
        // 向最终输出流（包括块数据缓冲区）写入字节数组b中的全部数据
        public void write(byte[] b) throws IOException {
            write(b, 0, b.length, false);
        }
        
        // 向最终输出流（包括块数据缓冲区）写入字节数组b中的数据，其中copy为false，即可能需要考虑到数据块头信息
        public void write(byte[] b, int off, int len) throws IOException {
            write(b, off, len, false);
        }
        
        /**
         * Writes specified span of byte values from given array.
         * If copy is true, copies the values to an intermediate buffer before writing them to underlying stream
         * (to avoid exposing a reference to the original byte array).
         */
        /*
         * 向最终输出流（包括块数据缓冲区）写入字节数组b中的数据
         *
         * 如果copy为true，则待写数据会先复制到块数据缓冲区，然后再转入最终输出流。
         * 如果copy为false，则遇到一整块(MAX_BLOCK_SIZE个字节)数据时，需要先向最终输出流写入数据块头信息，再向其写入待写数据
         * 当copy跟blkmode均为false时，不需要考虑数据块头信息，而是直接向最终输出流写入数据
         */
        void write(byte[] b, int off, int len, boolean copy) throws IOException {
            /*
             * 如果待写数据不需要复制到块数据缓冲区，
             * 且待写数据也没有处在块模式下，
             * 则直接将字节数组b中的数据写入最终输出流
             */
            if(!copy && !blkmode) {
                drain();                // 将块数据缓冲区中的待写数据全部写入到最终输出流out
                out.write(b, off, len); // 向最终输出流写入字节数组b中的数据
                return;
            }
            
            while(len>0) {
                // 如果块数据缓冲区已满
                if(pos >= MAX_BLOCK_SIZE) {
                    drain();    // 将块数据缓冲区中的待写数据全部写入到最终输出流out
                }
                
                /*
                 * 如果剩余待写数据超出块数据缓冲区的容量，
                 * 且待写数据不需要写入块数据缓冲区，
                 * 且块数据缓冲区为空，
                 * 则直接向最终输出流写入字节数组b中的前MAX_BLOCK_SIZE个字节
                 */
                if(len >= MAX_BLOCK_SIZE && !copy && pos == 0) {
                    /* avoid unnecessary copy */
                    writeBlockHeader(MAX_BLOCK_SIZE);   // 向最终输出流写入包含长度信息MAX_BLOCK_SIZE的数据块头信息
                    out.write(b, off, MAX_BLOCK_SIZE);  // 向最终输出流写入字节数组b中的前MAX_BLOCK_SIZE个字节
                    off += MAX_BLOCK_SIZE;
                    len -= MAX_BLOCK_SIZE;
                } else {
                    int wlen = Math.min(len, MAX_BLOCK_SIZE - pos); // 计算块数据缓冲区剩余容量
                    System.arraycopy(b, off, buf, pos, wlen);
                    pos += wlen;
                    off += wlen;
                    len -= wlen;
                }
            }
        }
        
        // 向当前块数据输出流写入boolean信息
        public void writeBoolean(boolean v) throws IOException {
            // 如果块数据缓冲区已满
            if(pos >= MAX_BLOCK_SIZE) {
                drain();    // 将块数据缓冲区中的待写数据全部写入到最终输出流out
            }
            
            // 向块数据缓冲区写入boolean值
            Bits.putBoolean(buf, pos++, v);
        }
        
        // 向当前块数据输出流写入byte信息
        public void writeByte(int v) throws IOException {
            // 如果块数据缓冲区已满
            if(pos >= MAX_BLOCK_SIZE) {
                drain();    // 将块数据缓冲区中的待写数据全部写入到最终输出流out
            }
            
            // 向块数据缓冲区写入byte值
            buf[pos++] = (byte) v;
        }
        
        // 向当前块数据输出流写入char信息（按大端法写入）
        public void writeChar(int v) throws IOException {
            // 如果块数据缓冲区未满
            if(pos + 2<=MAX_BLOCK_SIZE) {
                // 向块数据缓冲区写入char值（按大端法写入）
                Bits.putChar(buf, pos, (char) v);
                pos += 2;
            } else {
                /*
                 * 向基础数据输出流写入char值（按大端法写入）
                 * 内部会先将待写数据分解为字节，然后调用块数据输出流的write(int)方法将其写入块数据缓冲区/最终输出流
                 */
                dout.writeChar(v);
            }
        }
        
        // 向当前块数据输出流写入short信息（按大端法写入）
        public void writeShort(int v) throws IOException {
            // 如果块数据缓冲区未满
            if(pos + 2<=MAX_BLOCK_SIZE) {
                // 向块数据缓冲区写入short值（按大端法写入）
                Bits.putShort(buf, pos, (short) v);
                pos += 2;
            } else {
                /*
                 * 向基础数据输出流写入short值（按大端法写入）
                 * 内部会先将待写数据分解为字节，然后调用块数据输出流的write(int)方法将其写入块数据缓冲区/最终输出流
                 */
                dout.writeShort(v);
            }
        }
        
        // 向当前块数据输出流写入int信息（按大端法写入）
        public void writeInt(int v) throws IOException {
            // 如果块数据缓冲区未满
            if(pos + 4<=MAX_BLOCK_SIZE) {
                // 向块数据缓冲区写入int值（按大端法写入）
                Bits.putInt(buf, pos, v);
                pos += 4;
            } else {
                /*
                 * 向基础数据输出流写入int值（按大端法写入）
                 * 内部会先将待写数据分解为字节，然后调用块数据输出流的write(int)方法将其写入块数据缓冲区/最终输出流
                 */
                dout.writeInt(v);
            }
        }
        
        // 向当前块数据输出流写入long信息（按大端法写入）
        public void writeLong(long v) throws IOException {
            // 如果块数据缓冲区未满
            if(pos + 8<=MAX_BLOCK_SIZE) {
                // 向块数据缓冲区写入long值（按大端法写入）
                Bits.putLong(buf, pos, v);
                pos += 8;
            } else {
                /*
                 * 向基础数据输出流写入long值（按大端法写入）
                 * 内部会先将待写数据分解为字节，然后调用块数据输出流的write(int)方法将其写入块数据缓冲区/最终输出流
                 */
                dout.writeLong(v);
            }
        }
        
        // 向当前块数据输出流写入float信息（按大端法写入）
        public void writeFloat(float v) throws IOException {
            // 如果块数据缓冲区未满
            if(pos + 4<=MAX_BLOCK_SIZE) {
                // 向块数据缓冲区写入float值（按大端法写入）
                Bits.putFloat(buf, pos, v);
                pos += 4;
            } else {
                /*
                 * 向基础数据输出流写入float值（按大端法写入）
                 * 内部会先将待写数据分解为字节，然后调用块数据输出流的write(int)方法将其写入块数据缓冲区/最终输出流
                 */
                dout.writeFloat(v);
            }
        }
        
        // 向当前块数据输出流写入double信息（按大端法写入）
        public void writeDouble(double v) throws IOException {
            // 如果块数据缓冲区未满
            if(pos + 8<=MAX_BLOCK_SIZE) {
                // 向块数据缓冲区写入double值（按大端法写入）
                Bits.putDouble(buf, pos, v);
                pos += 8;
            } else {
                /*
                 * 向基础数据输出流写入double值（按大端法写入）
                 * 内部会先将待写数据分解为字节，然后调用块数据输出流的write(int)方法将其写入块数据缓冲区/最终输出流
                 */
                dout.writeDouble(v);
            }
        }
        
        // 向当前块数据输出流批量写入boolean信息
        void writeBooleans(boolean[] v, int off, int len) throws IOException {
            int endoff = off + len;
            
            while(off<endoff) {
                if(pos >= MAX_BLOCK_SIZE) {
                    drain();
                }
                
                int stop = Math.min(endoff, off + (MAX_BLOCK_SIZE - pos));
                
                while(off<stop) {
                    Bits.putBoolean(buf, pos++, v[off++]);
                }
            }
        }
        
        // 向当前块数据输出流批量写入字符串s中的byte信息(只取char的低8位)
        public void writeBytes(String s) throws IOException {
            int endoff = s.length();
            int cpos = 0;
            int csize = 0;
            
            int off = 0;
            while(off<endoff) {
                if(cpos >= csize) {
                    cpos = 0;
                    csize = Math.min(endoff - off, CHAR_BUF_SIZE);
                    s.getChars(off, off + csize, cbuf, 0);
                }
                
                if(pos >= MAX_BLOCK_SIZE) {
                    drain();
                }
                
                int n = Math.min(csize - cpos, MAX_BLOCK_SIZE - pos);
                int stop = pos + n;
                while(pos<stop) {
                    buf[pos++] = (byte) cbuf[cpos++];
                }
                off += n;
            }
        }
        
        // 向当前块数据输出流批量写入字符串s中的char信息
        public void writeChars(String s) throws IOException {
            int endoff = s.length();
            
            int off = 0;
            while(off<endoff) {
                int csize = Math.min(endoff - off, CHAR_BUF_SIZE);
                s.getChars(off, off + csize, cbuf, 0);
                writeChars(cbuf, 0, csize);
                off += csize;
            }
        }
        
        // 向当前块数据输出流批量写入char信息
        void writeChars(char[] v, int off, int len) throws IOException {
            int limit = MAX_BLOCK_SIZE - 2;
            int endoff = off + len;
            while(off<endoff) {
                if(pos<=limit) {
                    int avail = (MAX_BLOCK_SIZE - pos) >> 1;
                    int stop = Math.min(endoff, off + avail);
                    while(off<stop) {
                        Bits.putChar(buf, pos, v[off++]);
                        pos += 2;
                    }
                } else {
                    dout.writeChar(v[off++]);
                }
            }
        }
        
        // 向当前块数据输出流批量写入short信息
        void writeShorts(short[] v, int off, int len) throws IOException {
            int limit = MAX_BLOCK_SIZE - 2;
            int endoff = off + len;
            while(off<endoff) {
                if(pos<=limit) {
                    int avail = (MAX_BLOCK_SIZE - pos) >> 1;
                    int stop = Math.min(endoff, off + avail);
                    while(off<stop) {
                        Bits.putShort(buf, pos, v[off++]);
                        pos += 2;
                    }
                } else {
                    dout.writeShort(v[off++]);
                }
            }
        }
        
        // 向当前块数据输出流批量写入int信息
        void writeInts(int[] v, int off, int len) throws IOException {
            int limit = MAX_BLOCK_SIZE - 4;
            int endoff = off + len;
            while(off<endoff) {
                if(pos<=limit) {
                    int avail = (MAX_BLOCK_SIZE - pos) >> 2;
                    int stop = Math.min(endoff, off + avail);
                    while(off<stop) {
                        Bits.putInt(buf, pos, v[off++]);
                        pos += 4;
                    }
                } else {
                    dout.writeInt(v[off++]);
                }
            }
        }
        
        // 向当前块数据输出流批量写入long信息
        void writeLongs(long[] v, int off, int len) throws IOException {
            int limit = MAX_BLOCK_SIZE - 8;
            int endoff = off + len;
            while(off<endoff) {
                if(pos<=limit) {
                    int avail = (MAX_BLOCK_SIZE - pos) >> 3;
                    int stop = Math.min(endoff, off + avail);
                    while(off<stop) {
                        Bits.putLong(buf, pos, v[off++]);
                        pos += 8;
                    }
                } else {
                    dout.writeLong(v[off++]);
                }
            }
        }
        
        // 向当前块数据输出流批量写入float信息
        void writeFloats(float[] v, int off, int len) throws IOException {
            int limit = MAX_BLOCK_SIZE - 4;
            int endoff = off + len;
            while(off<endoff) {
                if(pos<=limit) {
                    int avail = (MAX_BLOCK_SIZE - pos) >> 2;
                    int chunklen = Math.min(endoff - off, avail);
                    floatsToBytes(v, off, buf, pos, chunklen);
                    off += chunklen;
                    pos += chunklen << 2;
                } else {
                    dout.writeFloat(v[off++]);
                }
            }
        }
        
        // 向当前块数据输出流批量写入double信息
        void writeDoubles(double[] v, int off, int len) throws IOException {
            int limit = MAX_BLOCK_SIZE - 8;
            int endoff = off + len;
            while(off<endoff) {
                if(pos<=limit) {
                    int avail = (MAX_BLOCK_SIZE - pos) >> 3;
                    int chunklen = Math.min(endoff - off, avail);
                    doublesToBytes(v, off, buf, pos, chunklen);
                    off += chunklen;
                    pos += chunklen << 3;
                } else {
                    dout.writeDouble(v[off++]);
                }
            }
        }
        
        /**
         * Returns the length in bytes of the UTF encoding of the given string.
         */
        // 获取字符串s的UTF8形式的字节数量
        long getUTFLength(String s) {
            int len = s.length();
            long utflen = 0;
            int off = 0;
            while(off<len) {
                int csize = Math.min(len - off, CHAR_BUF_SIZE);
                s.getChars(off, off + csize, cbuf, 0);
                for(int cpos = 0; cpos<csize; cpos++) {
                    char c = cbuf[cpos];
                    if(c >= 0x0001 && c<=0x007F) {
                        utflen++;
                    } else if(c>0x07FF) {
                        utflen += 3;
                    } else {
                        utflen += 2;
                    }
                }
                off += csize;
            }
            return utflen;
        }
        
        // 以字节形式向当前块数据输出流写入字符串s的utf8形式（字符串s包含的utf8字节数量在short范围内）
        public void writeUTF(String s) throws IOException {
            writeUTF(s, getUTFLength(s));
        }
        
        /**
         * Writes the given string in UTF format.  This method is used in
         * situations where the UTF encoding length of the string is already
         * known; specifying it explicitly avoids a prescan of the string to
         * determine its UTF length.
         */
        // 以字节形式向当前块数据输出流写入字符串s的utf8形式的前utflen个字节（utflen在short范围内）
        void writeUTF(String s, long utflen) throws IOException {
            if(utflen>0xFFFFL) {
                throw new UTFDataFormatException();
            }
            
            // 向当前块数据输出流写入代表utf8字节数量的short信息（按大端法写入）
            writeShort((int) utflen);
            
            // 如果s中都是单字节字符
            if(utflen == (long) s.length()) {
                writeBytes(s);
            } else {
                writeUTFBody(s);
            }
        }
        
        /**
         * Writes given string in "long" UTF format.  "Long" UTF format is
         * identical to standard UTF, except that it uses an 8 byte header
         * (instead of the standard 2 bytes) to convey the UTF encoding length.
         */
        // 以字节形式向当前块数据输出流写入字符串s的utf8形式（字符串s包含的utf8字节数量在long范围内）
        void writeLongUTF(String s) throws IOException {
            writeLongUTF(s, getUTFLength(s));
        }
        
        /**
         * Writes given string in "long" UTF format, where the UTF encoding
         * length of the string is already known.
         */
        // 以字节形式向当前块数据输出流写入字符串s的utf8形式的前utflen个字节（utflen在long范围内）
        void writeLongUTF(String s, long utflen) throws IOException {
            // 向当前块数据输出流写入代表utf8字节数量的long信息（按大端法写入）
            writeLong(utflen);
            
            // 如果s中都是单字节字符
            if(utflen == (long) s.length()) {
                writeBytes(s);
            } else {
                writeUTFBody(s);
            }
        }
        
        /**
         * Writes the "body" (i.e., the UTF representation minus the 2-byte or 8-byte length header) of the UTF encoding for the given string.
         */
        // 向当前块数据输出流写入字符串s中包含的UTF8字节信息
        private void writeUTFBody(String s) throws IOException {
            int limit = MAX_BLOCK_SIZE - 3;
            int len = s.length();
            for(int off = 0; off<len; ) {
                int csize = Math.min(len - off, CHAR_BUF_SIZE);
                s.getChars(off, off + csize, cbuf, 0);
                for(int cpos = 0; cpos<csize; cpos++) {
                    char c = cbuf[cpos];
                    if(pos<=limit) {
                        if(c<=0x007F && c != 0) {
                            buf[pos++] = (byte) c;
                        } else if(c>0x07FF) {
                            buf[pos + 2] = (byte) (0x80 | ((c >> 0) & 0x3F));
                            buf[pos + 1] = (byte) (0x80 | ((c >> 6) & 0x3F));
                            buf[pos + 0] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                            pos += 3;
                        } else {
                            buf[pos + 1] = (byte) (0x80 | ((c >> 0) & 0x3F));
                            buf[pos + 0] = (byte) (0xC0 | ((c >> 6) & 0x1F));
                            pos += 2;
                        }
                    } else {    // write one byte at a time to normalize block
                        if(c<=0x007F && c != 0) {
                            write(c);
                        } else if(c>0x07FF) {
                            write(0xE0 | ((c >> 12) & 0x0F));
                            write(0x80 | ((c >> 6) & 0x3F));
                            write(0x80 | ((c >> 0) & 0x3F));
                        } else {
                            write(0xC0 | ((c >> 6) & 0x1F));
                            write(0x80 | ((c >> 0) & 0x3F));
                        }
                    }
                }
                off += csize;
            }
        }
        
        /**
         * Writes block data header.
         * Data blocks shorter than 256 bytes are prefixed with a 2-byte header;
         * all others start with a 5-byte header.
         */
        // 向最终输出流写入包含长度信息len的数据块头信息
        private void writeBlockHeader(int len) throws IOException {
            if(len<=0xFF) {
                hbuf[0] = TC_BLOCKDATA;
                hbuf[1] = (byte) len;
                // 将数据块头信息(总计2个字节)写入最终输出流
                out.write(hbuf, 0, 2);
            } else {
                hbuf[0] = TC_BLOCKDATALONG;
                // 向hbuf[1]处写入int类型的len值（大端法）
                Bits.putInt(hbuf, 1, len);
                // 将数据块头信息(总计5个字节)写入最终输出流
                out.write(hbuf, 0, 5);
            }
        }
        
        /**
         * Writes all buffered data from this stream to the underlying stream,
         * but does not flush underlying stream.
         */
        // 将块数据缓冲区中的待写数据全部写入到最终输出流out
        void drain() throws IOException {
            if(pos == 0) {
                return;
            }
            
            // 如果处于块模式下
            if(blkmode) {
                // 向最终输出流写入包含长度信息pos的数据块头信息
                writeBlockHeader(pos);
            }
            
            // 将字节数组buf[0, pos]范围的字节写入到输出流out
            out.write(buf, 0, pos);
            
            pos = 0;
        }
        
        // 将内外缓冲区中的数据写入到最终输出流
        public void flush() throws IOException {
            drain();        // 将块数据缓冲区中的待写数据全部写入到最终输出流out
            
            out.flush();    // 将输出流缓冲区中的字节刷新到输出流
        }
        
        // 关闭输出流
        public void close() throws IOException {
            flush();
            out.close();
        }
        
        /**
         * Sets block data mode to the given mode (true == on, false == off) and returns the previous mode value.
         * If the new mode is the same as the old mode, no action is taken.
         * If the new mode differs from the old mode, any buffered data is flushed before switching to the new mode.
         */
        // 设置待写数据是否处于块模式下
        boolean setBlockDataMode(boolean mode) throws IOException {
            if(blkmode == mode) {
                return blkmode;
            }
            
            // 将块数据缓冲区中的待写数据全部写入到最终输出流out
            drain();
            
            blkmode = mode;
            
            return !blkmode;
        }
        
        /**
         * Returns true if the stream is currently in block data mode, false otherwise.
         */
        // 判断待写数据是否处于块模式下
        boolean getBlockDataMode() {
            return blkmode;
        }
        
    }
    
    /**
     * Lightweight identity hash table which maps objects to integer handles, assigned in ascending order.
     */
    // 共享对象哈希表(对于非共享对象，存入的是null)
    private static class HandleTable {
        
        /* maps handle value -> associated object */
        private Object[] objs;  // 对象顺序表，用来顺序存储指定的obj对象
        
        /* maps hash value -> candidate handle value */
        private int[] spine;    // 存储指定对象在objs中的位置信息（类似哈希表）
        
        /* maps handle value -> next candidate handle value */
        private int[] next;     // 记录下一个拥有相同哈希值的obj对象的位置
        
        /* number of mappings in table/next available handle */
        private int size;       // 哈希表容量
        
        /* factor for computing size threshold */
        private final float loadFactor; // 负载因子
        
        /* size threshold determining when to expand hash spine */
        private int threshold;  // 扩容阈值
        
        /**
         * Creates new HandleTable with given capacity and load factor.
         */
        HandleTable(int initialCapacity, float loadFactor) {
            this.loadFactor = loadFactor;
            spine = new int[initialCapacity];
            next = new int[initialCapacity];
            objs = new Object[initialCapacity];
            threshold = (int) (initialCapacity * loadFactor);
            clear();
        }
        
        /**
         * Assigns next available handle to given object, and returns handle value.
         * Handles are assigned in ascending order starting at 0.
         */
        // 将obj信息插入到HandleTable中，并返回当前已插入的对象数量（也是obj在对象表中的位置）
        int assign(Object obj) {
            if(size >= next.length) {
                growEntries();
            }
            
            if(size >= threshold) {
                growSpine();
            }
            
            // 将obj信息插入到HandleTable中
            insert(obj, size);
            
            return size++;
        }
        
        /**
         * Looks up and returns handle associated with given object, or -1 if no mapping found.
         */
        // 获取obj在对象顺序表中的位置
        int lookup(Object obj) {
            if(size == 0) {
                return -1;
            }
            
            // 计算obj的哈希值
            int index = hash(obj) % spine.length;
            
            // 要搜索其他相同哈希值的元素
            for(int i = spine[index]; i >= 0; i = next[i]) {
                if(objs[i] == obj) {
                    return i;
                }
            }
            
            return -1;
        }
        
        /**
         * Resets table to its initial (empty) state.
         */
        void clear() {
            Arrays.fill(spine, -1);
            Arrays.fill(objs, 0, size, null);
            size = 0;
        }
        
        /**
         * Returns the number of mappings currently in table.
         */
        int size() {
            return size;
        }
        
        /**
         * Inserts mapping object -> handle mapping into table.
         * Assumes table is large enough to accommodate new mapping.
         */
        // 将obj信息插入到HandleTable中
        private void insert(Object obj, int handle) {
            // 计算obj的哈希值
            int index = hash(obj) % spine.length;
            objs[handle] = obj;             // 顺序存储obj对象
            next[handle] = spine[index];    // 记录下一个拥有相同哈希值的obj对象的位置
            spine[index] = handle;          // 存储当前obj对象的位置信息
        }
        
        /**
         * Expands the hash "spine" -- equivalent to increasing the number of
         * buckets in a conventional hash table.
         */
        private void growSpine() {
            spine = new int[(spine.length << 1) + 1];
            threshold = (int) (spine.length * loadFactor);
            Arrays.fill(spine, -1);
            for(int i = 0; i<size; i++) {
                insert(objs[i], i);
            }
        }
        
        /**
         * Increases hash table capacity by lengthening entry arrays.
         */
        private void growEntries() {
            int newLength = (next.length << 1) + 1;
            int[] newNext = new int[newLength];
            System.arraycopy(next, 0, newNext, 0, size);
            next = newNext;
            
            Object[] newObjs = new Object[newLength];
            System.arraycopy(objs, 0, newObjs, 0, size);
            objs = newObjs;
        }
        
        /**
         * Returns hash value for given object.
         */
        private int hash(Object obj) {
            return System.identityHashCode(obj) & 0x7FFFFFFF;
        }
    }
    
    /**
     * Lightweight identity hash table which maps objects to replacement objects.
     */
    // 替换对象哈希表
    private static class ReplaceTable {
        
        /* maps object -> index */
        private final HandleTable htab;
        
        /* maps index -> replacement object */
        private Object[] reps;  // 替换对象顺序表
        
        /**
         * Creates new ReplaceTable with given capacity and load factor.
         */
        ReplaceTable(int initialCapacity, float loadFactor) {
            htab = new HandleTable(initialCapacity, loadFactor);
            reps = new Object[initialCapacity];
        }
        
        /**
         * Enters mapping from object to replacement object.
         */
        // 将obj信息插入到HandleTable的对象顺序表，并将rep插入到ReplaceTable的替换对象顺序表中
        void assign(Object obj, Object rep) {
            // 将obj信息插入到HandleTable中，并返回当前已插入的对象数量（也是obj在对象表中的位置）
            int index = htab.assign(obj);
            
            while(index >= reps.length) {
                grow();
            }
            
            reps[index] = rep;
        }
        
        /**
         * Looks up and returns replacement for given object.  If no
         * replacement is found, returns the lookup object itself.
         */
        // 获取obj的替换对象，如果不存在，返回其自身
        Object lookup(Object obj) {
            int index = htab.lookup(obj);
            return (index >= 0) ? reps[index] : obj;
        }
        
        /**
         * Resets table to its initial (empty) state.
         */
        void clear() {
            Arrays.fill(reps, 0, htab.size(), null);
            htab.clear();
        }
        
        /**
         * Returns the number of mappings currently in table.
         */
        int size() {
            return htab.size();
        }
        
        /**
         * Increases table capacity.
         */
        private void grow() {
            Object[] newReps = new Object[(reps.length << 1) + 1];
            System.arraycopy(reps, 0, newReps, 0, reps.length);
            reps = newReps;
        }
    }
    
    private static class Caches {
        /** cache of subclass security audit results */
        static final ConcurrentMap<WeakClassKey, Boolean> subclassAudits = new ConcurrentHashMap<>();
        
        /** queue for WeakReferences to audited subclasses */
        static final ReferenceQueue<Class<?>> subclassAuditsQueue = new ReferenceQueue<>();
    }
    
    /**
     * Stack to keep debug information about the state of the
     * serialization process, for embedding in exception messages.
     */
    private static class DebugTraceInfoStack {
        private final List<String> stack;
        
        DebugTraceInfoStack() {
            stack = new ArrayList<>();
        }
        
        /**
         * Returns a string representation of this object
         */
        public String toString() {
            StringJoiner sj = new StringJoiner("\n");
            for(int i = stack.size() - 1; i >= 0; i--) {
                sj.add(stack.get(i));
            }
            return sj.toString();
        }
        
        /**
         * Removes all of the elements from enclosed list.
         */
        void clear() {
            stack.clear();
        }
        
        /**
         * Removes the object at the top of enclosed list.
         */
        void pop() {
            stack.remove(stack.size() - 1);
        }
        
        /**
         * Pushes a String onto the top of enclosed list.
         */
        void push(String entry) {
            stack.add("\t- " + entry);
        }
    }
    
}
