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
import java.lang.System.Logger;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.Unsafe;
import jdk.internal.misc.VM;
import sun.reflect.misc.ReflectUtil;

/**
 * An ObjectInputStream deserializes primitive data and objects previously
 * written using an ObjectOutputStream.
 *
 * <p><strong>Warning: Deserialization of untrusted data is inherently dangerous
 * and should be avoided. Untrusted data should be carefully validated according to the
 * "Serialization and Deserialization" section of the
 * {@extLink secure_coding_guidelines_javase Secure Coding Guidelines for Java SE}.
 * {@extLink serialization_filter_guide Serialization Filtering} describes best
 * practices for defensive use of serial filters.
 * </strong></p>
 *
 * <p>ObjectOutputStream and ObjectInputStream can provide an application with
 * persistent storage for graphs of objects when used with a FileOutputStream
 * and FileInputStream respectively.  ObjectInputStream is used to recover
 * those objects previously serialized. Other uses include passing objects
 * between hosts using a socket stream or for marshaling and unmarshaling
 * arguments and parameters in a remote communication system.
 *
 * <p>ObjectInputStream ensures that the types of all objects in the graph
 * created from the stream match the classes present in the Java Virtual
 * Machine.  Classes are loaded as required using the standard mechanisms.
 *
 * <p>Only objects that support the java.io.Serializable or
 * java.io.Externalizable interface can be read from streams.
 *
 * <p>The method <code>readObject</code> is used to read an object from the
 * stream.  Java's safe casting should be used to get the desired type.  In
 * Java, strings and arrays are objects and are treated as objects during
 * serialization. When read they need to be cast to the expected type.
 *
 * <p>Primitive data types can be read from the stream using the appropriate
 * method on DataInput.
 *
 * <p>The default deserialization mechanism for objects restores the contents
 * of each field to the value and type it had when it was written.  Fields
 * declared as transient or static are ignored by the deserialization process.
 * References to other objects cause those objects to be read from the stream
 * as necessary.  Graphs of objects are restored correctly using a reference
 * sharing mechanism.  New objects are always allocated when deserializing,
 * which prevents existing objects from being overwritten.
 *
 * <p>Reading an object is analogous to running the constructors of a new
 * object.  Memory is allocated for the object and initialized to zero (NULL).
 * No-arg constructors are invoked for the non-serializable classes and then
 * the fields of the serializable classes are restored from the stream starting
 * with the serializable class closest to java.lang.object and finishing with
 * the object's most specific class.
 *
 * <p>For example to read from a stream as written by the example in
 * ObjectOutputStream:
 * <br>
 * <pre>
 *      FileInputStream fis = new FileInputStream("t.tmp");
 *      ObjectInputStream ois = new ObjectInputStream(fis);
 *
 *      int i = ois.readInt();
 *      String today = (String) ois.readObject();
 *      Date date = (Date) ois.readObject();
 *
 *      ois.close();
 * </pre>
 *
 * <p>Classes control how they are serialized by implementing either the
 * java.io.Serializable or java.io.Externalizable interfaces.
 *
 * <p>Implementing the Serializable interface allows object serialization to
 * save and restore the entire state of the object and it allows classes to
 * evolve between the time the stream is written and the time it is read.  It
 * automatically traverses references between objects, saving and restoring
 * entire graphs.
 *
 * <p>Serializable classes that require special handling during the
 * serialization and deserialization process should implement the following
 * methods:
 *
 * <pre>
 * private void writeObject(java.io.ObjectOutputStream stream)
 *     throws IOException;
 * private void readObject(java.io.ObjectInputStream stream)
 *     throws IOException, ClassNotFoundException;
 * private void readObjectNoData()
 *     throws ObjectStreamException;
 * </pre>
 *
 * <p>The readObject method is responsible for reading and restoring the state
 * of the object for its particular class using data written to the stream by
 * the corresponding writeObject method.  The method does not need to concern
 * itself with the state belonging to its superclasses or subclasses.  State is
 * restored by reading data from the ObjectInputStream for the individual
 * fields and making assignments to the appropriate fields of the object.
 * Reading primitive data types is supported by DataInput.
 *
 * <p>Any attempt to read object data which exceeds the boundaries of the
 * custom data written by the corresponding writeObject method will cause an
 * OptionalDataException to be thrown with an eof field value of true.
 * Non-object reads which exceed the end of the allotted data will reflect the
 * end of data in the same way that they would indicate the end of the stream:
 * bytewise reads will return -1 as the byte read or number of bytes read, and
 * primitive reads will throw EOFExceptions.  If there is no corresponding
 * writeObject method, then the end of default serialized data marks the end of
 * the allotted data.
 *
 * <p>Primitive and object read calls issued from within a readExternal method
 * behave in the same manner--if the stream is already positioned at the end of
 * data written by the corresponding writeExternal method, object reads will
 * throw OptionalDataExceptions with eof set to true, bytewise reads will
 * return -1, and primitive reads will throw EOFExceptions.  Note that this
 * behavior does not hold for streams written with the old
 * <code>ObjectStreamConstants.PROTOCOL_VERSION_1</code> protocol, in which the
 * end of data written by writeExternal methods is not demarcated, and hence
 * cannot be detected.
 *
 * <p>The readObjectNoData method is responsible for initializing the state of
 * the object for its particular class in the event that the serialization
 * stream does not list the given class as a superclass of the object being
 * deserialized.  This may occur in cases where the receiving party uses a
 * different version of the deserialized instance's class than the sending
 * party, and the receiver's version extends classes that are not extended by
 * the sender's version.  This may also occur if the serialization stream has
 * been tampered; hence, readObjectNoData is useful for initializing
 * deserialized objects properly despite a "hostile" or incomplete source
 * stream.
 *
 * <p>Serialization does not read or assign values to the fields of any object
 * that does not implement the java.io.Serializable interface.  Subclasses of
 * Objects that are not serializable can be serializable. In this case the
 * non-serializable class must have a no-arg constructor to allow its fields to
 * be initialized.  In this case it is the responsibility of the subclass to
 * save and restore the state of the non-serializable class. It is frequently
 * the case that the fields of that class are accessible (public, package, or
 * protected) or that there are get and set methods that can be used to restore
 * the state.
 *
 * <p>The contents of the stream can be filtered during deserialization.
 * If a {@linkplain #setObjectInputFilter(ObjectInputFilter) filter is set}
 * on an ObjectInputStream, the {@link ObjectInputFilter} can check that
 * the classes, array lengths, number of references in the stream, depth, and
 * number of bytes consumed from the input stream are allowed and
 * if not, can terminate deserialization.
 * A {@linkplain ObjectInputFilter.Config#setSerialFilter(ObjectInputFilter) process-wide filter}
 * can be configured that is applied to each {@code ObjectInputStream} unless replaced
 * using {@link #setObjectInputFilter(ObjectInputFilter) setObjectInputFilter}.
 *
 * <p>Any exception that occurs while deserializing an object will be caught by
 * the ObjectInputStream and abort the reading process.
 *
 * <p>Implementing the Externalizable interface allows the object to assume
 * complete control over the contents and format of the object's serialized
 * form.  The methods of the Externalizable interface, writeExternal and
 * readExternal, are called to save and restore the objects state.  When
 * implemented by a class they can write and read their own state using all of
 * the methods of ObjectOutput and ObjectInput.  It is the responsibility of
 * the objects to handle any versioning that occurs.
 *
 * <p>Enum constants are deserialized differently than ordinary serializable or
 * externalizable objects.  The serialized form of an enum constant consists
 * solely of its name; field values of the constant are not transmitted.  To
 * deserialize an enum constant, ObjectInputStream reads the constant name from
 * the stream; the deserialized constant is then obtained by calling the static
 * method <code>Enum.valueOf(Class, String)</code> with the enum constant's
 * base type and the received constant name as arguments.  Like other
 * serializable or externalizable objects, enum constants can function as the
 * targets of back references appearing subsequently in the serialization
 * stream.  The process by which enum constants are deserialized cannot be
 * customized: any class-specific readObject, readObjectNoData, and readResolve
 * methods defined by enum types are ignored during deserialization.
 * Similarly, any serialPersistentFields or serialVersionUID field declarations
 * are also ignored--all enum types have a fixed serialVersionUID of 0L.
 *
 * @author      Mike Warres
 * @author      Roger Riggs
 * @see java.io.DataInput
 * @see java.io.ObjectOutputStream
 * @see java.io.Serializable
 * @see <a href="{@docRoot}/../specs/serialization/input.html">
 *     Object Serialization Specification, Section 3, Object Input Classes</a>
 * @since   1.1
 */
/*
 * 对象输入流，参与反序列化过程
 *
 * 对于实现了Externalizable接口的类，需要自行实现反序列化逻辑
 */
public class ObjectInputStream extends InputStream implements ObjectInput, ObjectStreamConstants {
    
    /** handle value representing null */
    private static final int NULL_HANDLE = -1;
    
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    
    /** marker for unshared objects in internal handle table */
    private static final Object unsharedMarker = new Object();
    
    /**
     * immutable table mapping primitive type names to corresponding
     * class objects
     */
    private static final Map<String, Class<?>> primClasses = Map.of(
        "boolean", boolean.class,
        "byte", byte.class,
        "char", char.class,
        "short", short.class,
        "int", int.class,
        "long", long.class,
        "float", float.class,
        "double", double.class,
        "void", void.class
    );
    
    /** recursion depth */
    private long depth;     // 序列化嵌套深度
    
    /** filter stream for handling block data conversion */
    private final BlockDataInputStream bin;     // 块数据输入流
    
    /** wire handle -> obj/exception map */
    private final HandleTable handles;          // 共享对象哈希表
    
    /** validation callback list */
    private final ValidationList vlist; // 验证回调列表
    
    /** Total number of references to any type of object, class, enum, proxy, etc. */
    private long totalObjectRefs;   // 总计反序列化的对象数量
    
    /** whether stream is closed */
    private boolean closed;
    
    /** scratch field for passing handle values up/down call stack */
    private int passHandle = NULL_HANDLE;
    
    /** flag set when at end of field value block with no TC_ENDBLOCKDATA */
    private boolean defaultDataEnd = false;
    
    /** if true, invoke readObjectOverride() instead of readObject() */
    private final boolean enableOverride;   // 是否使用子类实现的readObjectOverride()方法，而不是使用默认的反序列化逻辑
    
    /** if true, invoke resolveObject() */
    private boolean enableResolve;  // 是否需要调用resolveObject()方法，默认为false
    
    /**
     * Context during upcalls to class-defined readObject methods; holds
     * object currently being deserialized and descriptor for current class.
     * Null when not during readObject upcall.
     */
    private SerialCallbackContext curContext;   // 序列化上下文
    
    /**
     * Filter of class descriptors and classes read from the stream;
     * may be null.
     */
    private ObjectInputFilter serialFilter; // 反序列化过滤器
    
    
    static {
        SharedSecrets.setJavaObjectInputStreamAccess(ObjectInputStream::checkArray);
    }
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates an ObjectInputStream that reads from the specified InputStream.
     * A serialization stream header is read from the stream and verified.
     * This constructor will block until the corresponding ObjectOutputStream
     * has written and flushed the header.
     *
     * <p>The serialization filter is initialized to the value of
     * {@linkplain ObjectInputFilter.Config#getSerialFilter() the process-wide filter}.
     *
     * <p>If a security manager is installed, this constructor will check for
     * the "enableSubclassImplementation" SerializablePermission when invoked
     * directly or indirectly by the constructor of a subclass which overrides
     * the ObjectInputStream.readFields or ObjectInputStream.readUnshared
     * methods.
     *
     * @param in input stream to read from
     *
     * @throws StreamCorruptedException if the stream header is incorrect
     * @throws IOException              if an I/O error occurs while reading stream header
     * @throws SecurityException        if untrusted subclass illegally overrides
     *                                  security-sensitive methods
     * @throws NullPointerException     if <code>in</code> is <code>null</code>
     * @see ObjectInputStream#ObjectInputStream()
     * @see ObjectInputStream#readFields()
     * @see ObjectOutputStream#ObjectOutputStream(OutputStream)
     */
    public ObjectInputStream(InputStream in) throws IOException {
        verifySubclass();
        
        bin = new BlockDataInputStream(in);
        handles = new HandleTable(10);
        vlist = new ValidationList();
        
        enableOverride = false;
        
        serialFilter = ObjectInputFilter.Config.getSerialFilter();
        
        // 读取序列化头（包含一个魔数和一个版本号）
        readStreamHeader();
        
        // 设置待写数据处于块模式下
        bin.setBlockDataMode(true);
    }
    
    /**
     * Provide a way for subclasses that are completely reimplementing
     * ObjectInputStream to not have to allocate private data just used by this
     * implementation of ObjectInputStream.
     *
     * <p>The serialization filter is initialized to the value of
     * {@linkplain ObjectInputFilter.Config#getSerialFilter() the process-wide filter}.
     *
     * <p>If there is a security manager installed, this method first calls the
     * security manager's <code>checkPermission</code> method with the
     * <code>SerializablePermission("enableSubclassImplementation")</code>
     * permission to ensure it's ok to enable subclassing.
     *
     * @throws SecurityException if a security manager exists and its
     *                           <code>checkPermission</code> method denies enabling
     *                           subclassing.
     * @throws IOException       if an I/O error occurs while creating this stream
     * @see SecurityManager#checkPermission
     * @see java.io.SerializablePermission
     */
    protected ObjectInputStream() throws IOException, SecurityException {
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkPermission(SUBCLASS_IMPLEMENTATION_PERMISSION);
        }
        
        bin = null;
        handles = null;
        vlist = null;
        serialFilter = ObjectInputFilter.Config.getSerialFilter();
        enableOverride = true;
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 读 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reads in a boolean.
     *
     * @return the boolean read.
     *
     * @throws EOFException If end of file is reached.
     * @throws IOException  If other I/O error has occurred.
     */
    // 从块数据输入流读取boolean值
    public boolean readBoolean() throws IOException {
        return bin.readBoolean();
    }
    
    /**
     * Reads a 16 bit char.
     *
     * @return the 16 bit char read.
     *
     * @throws EOFException If end of file is reached.
     * @throws IOException  If other I/O error has occurred.
     */
    // 从块数据输入流读取char值
    public char readChar() throws IOException {
        return bin.readChar();
    }
    
    /**
     * Reads an 8 bit byte.
     *
     * @return the 8 bit byte read.
     *
     * @throws EOFException If end of file is reached.
     * @throws IOException  If other I/O error has occurred.
     */
    // 从块数据输入流读取byte值
    public byte readByte() throws IOException {
        return bin.readByte();
    }
    
    /**
     * Reads an unsigned 8 bit byte.
     *
     * @return the 8 bit byte read.
     *
     * @throws EOFException If end of file is reached.
     * @throws IOException  If other I/O error has occurred.
     */
    // 从块数据输入流读取byte值
    public int readUnsignedByte() throws IOException {
        return bin.readUnsignedByte();
    }
    
    /**
     * Reads a 16 bit short.
     *
     * @return the 16 bit short read.
     *
     * @throws EOFException If end of file is reached.
     * @throws IOException  If other I/O error has occurred.
     */
    // 从块数据输入流读取short值
    public short readShort() throws IOException {
        return bin.readShort();
    }
    
    /**
     * Reads an unsigned 16 bit short.
     *
     * @return the 16 bit short read.
     *
     * @throws EOFException If end of file is reached.
     * @throws IOException  If other I/O error has occurred.
     */
    // 从块数据输入流读取无符号short值
    public int readUnsignedShort() throws IOException {
        return bin.readUnsignedShort();
    }
    
    /**
     * Reads a 32 bit int.
     *
     * @return the 32 bit integer read.
     *
     * @throws EOFException If end of file is reached.
     * @throws IOException  If other I/O error has occurred.
     */
    // 从块数据输入流读取int值
    public int readInt() throws IOException {
        return bin.readInt();
    }
    
    /**
     * Reads a 64 bit long.
     *
     * @return the read 64 bit long.
     *
     * @throws EOFException If end of file is reached.
     * @throws IOException  If other I/O error has occurred.
     */
    // 从块数据输入流读取long值
    public long readLong() throws IOException {
        return bin.readLong();
    }
    
    /**
     * Reads a 32 bit float.
     *
     * @return the 32 bit float read.
     *
     * @throws EOFException If end of file is reached.
     * @throws IOException  If other I/O error has occurred.
     */
    // 从块数据输入流读取float值
    public float readFloat() throws IOException {
        return bin.readFloat();
    }
    
    /**
     * Reads a 64 bit double.
     *
     * @return the 64 bit double read.
     *
     * @throws EOFException If end of file is reached.
     * @throws IOException  If other I/O error has occurred.
     */
    // 从块数据输入流读取double值
    public double readDouble() throws IOException {
        return bin.readDouble();
    }
    
    
    /**
     * Reads a byte of data. This method will block if no input is available.
     *
     * @return the byte read, or -1 if the end of the stream is reached.
     *
     * @throws IOException If an I/O error has occurred.
     */
    // 从块数据输入流读取一个字节，返回-1表示读取结束
    public int read() throws IOException {
        return bin.read();
    }
    
    /**
     * Reads into an array of bytes.  This method will block until some input
     * is available. Consider using java.io.DataInputStream.readFully to read
     * exactly 'length' bytes.
     *
     * @param buf the buffer into which the data is read
     * @param off the start offset in the destination array {@code buf}
     * @param len the maximum number of bytes read
     *
     * @return the actual number of bytes read, -1 is returned when the end of
     * the stream is reached.
     *
     * @throws NullPointerException      if {@code buf} is {@code null}.
     * @throws IndexOutOfBoundsException if {@code off} is negative,
     *                                   {@code len} is negative, or {@code len} is greater than
     *                                   {@code buf.length - off}.
     * @throws IOException               If an I/O error has occurred.
     * @see java.io.DataInputStream#readFully(byte[], int, int)
     */
    // 从块数据输入流读取len个字节，并将读到的内容插入到字节数组b的off索引处
    public int read(byte[] buf, int off, int len) throws IOException {
        if(buf == null) {
            throw new NullPointerException();
        }
        
        int endoff = off + len;
        if(off<0 || len<0 || endoff>buf.length || endoff<0) {
            throw new IndexOutOfBoundsException();
        }
        
        return bin.read(buf, off, len, false);
    }
    
    
    /**
     * Reads bytes, blocking until all bytes are read.
     *
     * @param buf the buffer into which the data is read
     *
     * @throws NullPointerException If {@code buf} is {@code null}.
     * @throws EOFException         If end of file is reached.
     * @throws IOException          If other I/O error has occurred.
     */
    // 从块数据输入流读取足够字节填满buf
    public void readFully(byte[] buf) throws IOException {
        bin.readFully(buf, 0, buf.length, false);
    }
    
    /**
     * Reads bytes, blocking until all bytes are read.
     *
     * @param buf the buffer into which the data is read
     * @param off the start offset into the data array {@code buf}
     * @param len the maximum number of bytes to read
     *
     * @throws NullPointerException      If {@code buf} is {@code null}.
     * @throws IndexOutOfBoundsException If {@code off} is negative,
     *                                   {@code len} is negative, or {@code len} is greater than
     *                                   {@code buf.length - off}.
     * @throws EOFException              If end of file is reached.
     * @throws IOException               If other I/O error has occurred.
     */
    // 从块数据输入流读取len个字节写入buf的off处
    public void readFully(byte[] buf, int off, int len) throws IOException {
        int endoff = off + len;
        if(off<0 || len<0 || endoff>buf.length || endoff<0) {
            throw new IndexOutOfBoundsException();
        }
        
        bin.readFully(buf, off, len, false);
    }
    
    
    /**
     * Reads a String in <a href="DataInput.html#modified-utf-8">modified UTF-8</a> format.
     *
     * @return the String.
     *
     * @throws IOException            if there are I/O errors while reading from the
     *                                underlying <code>InputStream</code>
     * @throws UTFDataFormatException if read bytes do not represent a valid
     *                                modified UTF-8 encoding of a string
     */
    // 读取一个UTF8编码格式的小字符串
    public String readUTF() throws IOException {
        return bin.readUTF();
    }
    
    
    /**
     * Reads in a line that has been terminated by a \n, \r, \r\n or EOF.
     *
     * @return a String copy of the line.
     *
     * @throws IOException if there are I/O errors while reading from the
     *                     underlying <code>InputStream</code>
     * @deprecated This method does not properly convert bytes to characters.
     * see DataInputStream for the details and alternatives.
     */
    // 读取一行数据：已过时
    @Deprecated
    public String readLine() throws IOException {
        return bin.readLine();
    }
    
    /*▲ 读 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 反序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Read an object from the ObjectInputStream.  The class of the object, the
     * signature of the class, and the values of the non-transient and
     * non-static fields of the class and all of its supertypes are read.
     * Default deserializing for a class can be overridden using the writeObject
     * and readObject methods.  Objects referenced by this object are read
     * transitively so that a complete equivalent graph of objects is
     * reconstructed by readObject.
     *
     * <p>The root object is completely restored when all of its fields and the
     * objects it references are completely restored.  At this point the object
     * validation callbacks are executed in order based on their registered
     * priorities. The callbacks are registered by objects (in the readObject
     * special methods) as they are individually restored.
     *
     * <p>The serialization filter, when not {@code null}, is invoked for
     * each object (regular or class) read to reconstruct the root object.
     * See {@link #setObjectInputFilter(ObjectInputFilter) setObjectInputFilter} for details.
     *
     * <p>Exceptions are thrown for problems with the InputStream and for
     * classes that should not be deserialized.  All exceptions are fatal to
     * the InputStream and leave it in an indeterminate state; it is up to the
     * caller to ignore or recover the stream state.
     *
     * @throws ClassNotFoundException   Class of a serialized object cannot be
     *                                  found.
     * @throws InvalidClassException    Something is wrong with a class used by
     *                                  serialization.
     * @throws StreamCorruptedException Control information in the
     *                                  stream is inconsistent.
     * @throws OptionalDataException    Primitive data was found in the
     *                                  stream instead of objects.
     * @throws IOException              Any of the usual Input/Output related exceptions.
     */
    // 从输入流读取共享对象
    public final Object readObject() throws IOException, ClassNotFoundException {
        if(enableOverride) {
            // 使用子类实现的反序列化逻辑
            return readObjectOverride();
        }
        
        // if nested read, passHandle contains handle of enclosing object
        int outerHandle = passHandle;
        
        try {
            // 使用默认的反序列化逻辑，读取共享对象obj
            Object obj = readObject0(false);
            
            handles.markDependency(outerHandle, passHandle);
            
            ClassNotFoundException ex = handles.lookupException(passHandle);
            if(ex != null) {
                throw ex;
            }
            
            // 反序列化完成后，对反序列化的对象执行验证
            if(depth == 0) {
                // 从前往后，按优先级从高到低执行回调逻辑
                vlist.doCallbacks();
                freeze();
            }
            
            return obj;
        } finally {
            passHandle = outerHandle;
            if(closed && depth == 0) {
                clear();
            }
        }
    }
    
    /**
     * Reads an "unshared" object from the ObjectInputStream.  This method is
     * identical to readObject, except that it prevents subsequent calls to
     * readObject and readUnshared from returning additional references to the
     * deserialized instance obtained via this call.  Specifically:
     * <ul>
     *   <li>If readUnshared is called to deserialize a back-reference (the
     *       stream representation of an object which has been written
     *       previously to the stream), an ObjectStreamException will be
     *       thrown.
     *
     *   <li>If readUnshared returns successfully, then any subsequent attempts
     *       to deserialize back-references to the stream handle deserialized
     *       by readUnshared will cause an ObjectStreamException to be thrown.
     * </ul>
     * Deserializing an object via readUnshared invalidates the stream handle
     * associated with the returned object.  Note that this in itself does not
     * always guarantee that the reference returned by readUnshared is unique;
     * the deserialized object may define a readResolve method which returns an
     * object visible to other parties, or readUnshared may return a Class
     * object or enum constant obtainable elsewhere in the stream or through
     * external means. If the deserialized object defines a readResolve method
     * and the invocation of that method returns an array, then readUnshared
     * returns a shallow clone of that array; this guarantees that the returned
     * array object is unique and cannot be obtained a second time from an
     * invocation of readObject or readUnshared on the ObjectInputStream,
     * even if the underlying data stream has been manipulated.
     *
     * <p>The serialization filter, when not {@code null}, is invoked for
     * each object (regular or class) read to reconstruct the root object.
     * See {@link #setObjectInputFilter(ObjectInputFilter) setObjectInputFilter} for details.
     *
     * <p>ObjectInputStream subclasses which override this method can only be
     * constructed in security contexts possessing the
     * "enableSubclassImplementation" SerializablePermission; any attempt to
     * instantiate such a subclass without this permission will cause a
     * SecurityException to be thrown.
     *
     * @return reference to deserialized object
     *
     * @throws ClassNotFoundException   if class of an object to deserialize
     *                                  cannot be found
     * @throws StreamCorruptedException if control information in the stream
     *                                  is inconsistent
     * @throws ObjectStreamException    if object to deserialize has already
     *                                  appeared in stream
     * @throws OptionalDataException    if primitive data is next in stream
     * @throws IOException              if an I/O error occurs during deserialization
     * @since 1.4
     */
    // 从输入流读取非共享对象
    public Object readUnshared() throws IOException, ClassNotFoundException {
        // if nested read, passHandle contains handle of enclosing object
        int outerHandle = passHandle;
        
        try {
            Object obj = readObject0(true);
            
            handles.markDependency(outerHandle, passHandle);
            
            ClassNotFoundException ex = handles.lookupException(passHandle);
            if(ex != null) {
                throw ex;
            }
            
            // 反序列化完成后，对反序列化的对象执行验证
            if(depth == 0) {
                // 从前往后，按优先级从高到低执行回调逻辑
                vlist.doCallbacks();
                freeze();
            }
            
            return obj;
        } finally {
            passHandle = outerHandle;
            if(closed && depth == 0) {
                clear();
            }
        }
    }
    
    /**
     * Read the non-static and non-transient fields of the current class from
     * this stream.  This may only be called from the readObject method of the
     * class being deserialized. It will throw the NotActiveException if it is
     * called otherwise.
     *
     * @throws ClassNotFoundException if the class of a serialized object
     *                                could not be found.
     * @throws IOException            if an I/O error occurs.
     * @throws NotActiveException     if the stream is not currently reading
     *                                objects.
     */
    /*
     * 默认反序列化
     *
     * 读取目标类的非static和非transient字段
     * 只能从readObject()中调用此方法，否则会抛异常
     */
    public void defaultReadObject() throws IOException, ClassNotFoundException {
        SerialCallbackContext ctx = curContext;
        if(ctx == null) {
            throw new NotActiveException("not in call to readObject");
        }
        
        Object curObj = ctx.getObj();
        
        ObjectStreamClass curDesc = ctx.getDesc();
        
        bin.setBlockDataMode(false);
        
        FieldValues vals = defaultReadFields(curObj, curDesc);
        
        if(curObj != null) {
            defaultCheckFieldValues(curObj, curDesc, vals);
            defaultSetFieldValues(curObj, curDesc, vals);
        }
        
        bin.setBlockDataMode(true);
        
        if(!curDesc.hasWriteObjectData()) {
            /*
             * Fix for 4360508: since stream does not contain terminating TC_ENDBLOCKDATA tag,
             * set flag so that reading code elsewhere knows to simulate end-of-custom-data behavior.
             */
            defaultDataEnd = true;
        }
        
        ClassNotFoundException ex = handles.lookupException(passHandle);
        if(ex != null) {
            throw ex;
        }
    }
    
    
    /**
     * This method is called by trusted subclasses of ObjectOutputStream that
     * constructed ObjectOutputStream using the protected no-arg constructor.
     * The subclass is expected to provide an override method with the modifier
     * "final".
     *
     * @return the Object read from the stream.
     *
     * @throws ClassNotFoundException Class definition of a serialized object
     *                                cannot be found.
     * @throws OptionalDataException  Primitive data was found in the stream
     *                                instead of objects.
     * @throws IOException            if I/O errors occurred while reading from the
     *                                underlying stream
     * @see #ObjectInputStream()
     * @see #readObject()
     * @since 1.2
     */
    // [子类覆盖]如果开启了enableOverride，则会调用该方法来替代默认的反序列化逻辑
    protected Object readObjectOverride() throws IOException, ClassNotFoundException {
        return null;
    }
    
    /**
     * Load the local class equivalent of the specified stream class
     * description.  Subclasses may implement this method to allow classes to
     * be fetched from an alternate source.
     *
     * <p>The corresponding method in <code>ObjectOutputStream</code> is
     * <code>annotateClass</code>.  This method will be invoked only once for
     * each unique class in the stream.  This method can be implemented by
     * subclasses to use an alternate loading mechanism but must return a
     * <code>Class</code> object. Once returned, if the class is not an array
     * class, its serialVersionUID is compared to the serialVersionUID of the
     * serialized class, and if there is a mismatch, the deserialization fails
     * and an {@link InvalidClassException} is thrown.
     *
     * <p>The default implementation of this method in
     * <code>ObjectInputStream</code> returns the result of calling
     * <pre>
     *     Class.forName(desc.getName(), false, loader)
     * </pre>
     * where <code>loader</code> is the first class loader on the current
     * thread's stack (starting from the currently executing method) that is
     * neither the {@linkplain ClassLoader#getPlatformClassLoader() platform
     * class loader} nor its ancestor; otherwise, <code>loader</code> is the
     * <em>platform class loader</em>. If this call results in a
     * <code>ClassNotFoundException</code> and the name of the passed
     * <code>ObjectStreamClass</code> instance is the Java language keyword
     * for a primitive type or void, then the <code>Class</code> object
     * representing that primitive type or void will be returned
     * (e.g., an <code>ObjectStreamClass</code> with the name
     * <code>"int"</code> will be resolved to <code>Integer.TYPE</code>).
     * Otherwise, the <code>ClassNotFoundException</code> will be thrown to
     * the caller of this method.
     *
     * @param desc an instance of class <code>ObjectStreamClass</code>
     *
     * @return a <code>Class</code> object corresponding to <code>desc</code>
     *
     * @throws IOException            any of the usual Input/Output exceptions.
     * @throws ClassNotFoundException if class of a serialized object cannot
     *                                be found.
     */
    // 解析非代理类对象的序列化描述符
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        String name = desc.getName();
        try {
            return Class.forName(name, false, latestUserDefinedLoader());
        } catch(ClassNotFoundException ex) {
            Class<?> cl = primClasses.get(name);
            if(cl != null) {
                return cl;
            } else {
                throw ex;
            }
        }
    }
    
    /**
     * Returns a proxy class that implements the interfaces named in a proxy
     * class descriptor; subclasses may implement this method to read custom
     * data from the stream along with the descriptors for dynamic proxy
     * classes, allowing them to use an alternate loading mechanism for the
     * interfaces and the proxy class.
     *
     * <p>This method is called exactly once for each unique proxy class
     * descriptor in the stream.
     *
     * <p>The corresponding method in <code>ObjectOutputStream</code> is
     * <code>annotateProxyClass</code>.  For a given subclass of
     * <code>ObjectInputStream</code> that overrides this method, the
     * <code>annotateProxyClass</code> method in the corresponding subclass of
     * <code>ObjectOutputStream</code> must write any data or objects read by
     * this method.
     *
     * <p>The default implementation of this method in
     * <code>ObjectInputStream</code> returns the result of calling
     * <code>Proxy.getProxyClass</code> with the list of <code>Class</code>
     * objects for the interfaces that are named in the <code>interfaces</code>
     * parameter.  The <code>Class</code> object for each interface name
     * <code>i</code> is the value returned by calling
     * <pre>
     *     Class.forName(i, false, loader)
     * </pre>
     * where <code>loader</code> is the first class loader on the current
     * thread's stack (starting from the currently executing method) that is
     * neither the {@linkplain ClassLoader#getPlatformClassLoader() platform
     * class loader} nor its ancestor; otherwise, <code>loader</code> is the
     * <em>platform class loader</em>.
     * Unless any of the resolved interfaces are non-public, this same value
     * of <code>loader</code> is also the class loader passed to
     * <code>Proxy.getProxyClass</code>; if non-public interfaces are present,
     * their class loader is passed instead (if more than one non-public
     * interface class loader is encountered, an
     * <code>IllegalAccessError</code> is thrown).
     * If <code>Proxy.getProxyClass</code> throws an
     * <code>IllegalArgumentException</code>, <code>resolveProxyClass</code>
     * will throw a <code>ClassNotFoundException</code> containing the
     * <code>IllegalArgumentException</code>.
     *
     * @param interfaces the list of interface names that were
     *                   deserialized in the proxy class descriptor
     *
     * @return a proxy class for the specified interfaces
     *
     * @throws IOException            any exception thrown by the underlying
     *                                <code>InputStream</code>
     * @throws ClassNotFoundException if the proxy class or any of the
     *                                named interfaces could not be found
     * @see ObjectOutputStream#annotateProxyClass(Class)
     * @since 1.3
     */
    // 解析代理类的类对象，interfaces是代理接口
    protected Class<?> resolveProxyClass(String[] interfaces) throws IOException, ClassNotFoundException {
        ClassLoader latestLoader = latestUserDefinedLoader();
        ClassLoader nonPublicLoader = null;
        boolean hasNonPublicInterface = false;
        
        // define proxy in class loader of non-public interface(s), if any
        Class<?>[] classObjs = new Class<?>[interfaces.length];
        for(int i = 0; i<interfaces.length; i++) {
            Class<?> cl = Class.forName(interfaces[i], false, latestLoader);
            if((cl.getModifiers() & Modifier.PUBLIC) == 0) {
                if(hasNonPublicInterface) {
                    if(nonPublicLoader != cl.getClassLoader()) {
                        throw new IllegalAccessError("conflicting non-public interface class loaders");
                    }
                } else {
                    nonPublicLoader = cl.getClassLoader();
                    hasNonPublicInterface = true;
                }
            }
            classObjs[i] = cl;
        }
        
        try {
            // 返回代理类的类对象
            @SuppressWarnings("deprecation")
            Class<?> proxyClass = Proxy.getProxyClass(hasNonPublicInterface ? nonPublicLoader : latestLoader, classObjs);
            return proxyClass;
        } catch(IllegalArgumentException e) {
            throw new ClassNotFoundException(null, e);
        }
    }
    
    /**
     * This method will allow trusted subclasses of ObjectInputStream to
     * substitute one object for another during deserialization. Replacing
     * objects is disabled until enableResolveObject is called. The
     * enableResolveObject method checks that the stream requesting to resolve
     * object can be trusted. Every reference to serializable objects is passed
     * to resolveObject.  To insure that the private state of objects is not
     * unintentionally exposed only trusted streams may use resolveObject.
     *
     * <p>This method is called after an object has been read but before it is
     * returned from readObject.  The default resolveObject method just returns
     * the same object.
     *
     * <p>When a subclass is replacing objects it must insure that the
     * substituted object is compatible with every field where the reference
     * will be stored.  Objects whose type is not a subclass of the type of the
     * field or array element abort the serialization by raising an exception
     * and the object is not be stored.
     *
     * <p>This method is called only once when each object is first
     * encountered.  All subsequent references to the object will be redirected
     * to the new object.
     *
     * @param obj object to be substituted
     *
     * @return the substituted object
     *
     * @throws IOException Any of the usual Input/Output exceptions.
     */
    // 解析对象
    protected Object resolveObject(Object obj) throws IOException {
        return obj;
    }
    
    /**
     * The readStreamHeader method is provided to allow subclasses to read and
     * verify their own stream headers. It reads and verifies the magic number
     * and version number.
     *
     * @throws IOException              if there are I/O errors while reading from the
     *                                  underlying <code>InputStream</code>
     * @throws StreamCorruptedException if control information in the stream
     *                                  is inconsistent
     */
    // [子类覆盖]读取序列化头（包含一个魔数和一个版本号）
    protected void readStreamHeader() throws IOException, StreamCorruptedException {
        short s0 = bin.readShort();
        short s1 = bin.readShort();
        if(s0 != STREAM_MAGIC || s1 != STREAM_VERSION) {
            throw new StreamCorruptedException(String.format("invalid stream header: %04X%04X", s0, s1));
        }
    }
    
    
    /**
     * Underlying readObject implementation.
     */
    // 默认的反序列化逻辑：从输入流读取指定的对象，unshared指示该对象是否为非共享
    private Object readObject0(boolean unshared) throws IOException {
        // 获取当前的块模式
        boolean oldMode = bin.getBlockDataMode();
        
        // 如果处在块模式下
        if(oldMode) {
            // 获取当前块剩余未读数据量
            int remain = bin.currentBlockRemaining();
            
            if(remain>0) {
                throw new OptionalDataException(remain);
            } else if(defaultDataEnd) {
                /*
                 * Fix for 4360508: stream is currently at the end of a field
                 * value block written via default serialization; since there
                 * is no terminating TC_ENDBLOCKDATA tag, simulate
                 * end-of-custom-data behavior explicitly.
                 */
                throw new OptionalDataException(true);
            }
            
            bin.setBlockDataMode(false);
        }
        
        byte tc;
        
        while((tc = bin.peekByte()) == TC_RESET) {
            bin.readByte();
            handleReset();
        }
        
        depth++;
        
        totalObjectRefs++;
        
        try {
            switch(tc) {
                case TC_NULL:
                    return readNull();
                
                case TC_REFERENCE:
                    return readHandle(unshared);
                
                case TC_CLASS:
                    return readClass(unshared);
                
                case TC_CLASSDESC:
                case TC_PROXYCLASSDESC:
                    return readClassDesc(unshared);
                
                case TC_STRING:
                case TC_LONGSTRING:
                    return checkResolve(readString(unshared));
                
                case TC_ARRAY:
                    return checkResolve(readArray(unshared));
                
                case TC_ENUM:
                    return checkResolve(readEnum(unshared));
                
                case TC_OBJECT:
                    return checkResolve(readOrdinaryObject(unshared));
                
                case TC_EXCEPTION:
                    IOException ex = readFatalException();
                    throw new WriteAbortedException("writing aborted", ex);
                
                case TC_BLOCKDATA:
                case TC_BLOCKDATALONG:
                    if(oldMode) {
                        bin.setBlockDataMode(true);
                        bin.peek();             // force header read
                        throw new OptionalDataException(bin.currentBlockRemaining());
                    } else {
                        throw new StreamCorruptedException("unexpected block data");
                    }
                
                case TC_ENDBLOCKDATA:
                    if(oldMode) {
                        throw new OptionalDataException(true);
                    } else {
                        throw new StreamCorruptedException("unexpected end of block data");
                    }
                
                default:
                    throw new StreamCorruptedException(String.format("invalid type code: %02X", tc));
            }
        } finally {
            depth--;
            bin.setBlockDataMode(oldMode);
        }
    }
    
    /**
     * Reads in values of serializable fields declared by given class
     * descriptor. Expects that passHandle is set to obj's handle before this
     * method is called.
     */
    // 对指定的对象obj执行默认的反序列化过程
    private FieldValues defaultReadFields(Object obj, ObjectStreamClass desc) throws IOException {
        Class<?> cl = desc.forClass();
        if(cl != null && obj != null && !cl.isInstance(obj)) {
            throw new ClassCastException();
        }
        
        byte[] primVals = null;
        int primDataSize = desc.getPrimDataSize();
        if(primDataSize>0) {
            primVals = new byte[primDataSize];
            bin.readFully(primVals, 0, primDataSize, false);
        }
        
        Object[] objVals = null;
        int numObjFields = desc.getNumObjFields();
        
        if(numObjFields>0) {
            int objHandle = passHandle;
            ObjectStreamField[] fields = desc.getFields(false);
            objVals = new Object[numObjFields];
            int numPrimFields = fields.length - objVals.length;
            for(int i = 0; i<objVals.length; i++) {
                ObjectStreamField f = fields[numPrimFields + i];
                objVals[i] = readObject0(f.isUnshared());
                if(f.getField() != null) {
                    handles.markDependency(objHandle, passHandle);
                }
            }
            
            passHandle = objHandle;
        }
        
        return new FieldValues(primVals, objVals);
    }
    
    
    /**
     * Reads the persistent fields from the stream and makes them available by
     * name.
     *
     * @return the <code>GetField</code> object representing the persistent
     * fields of the object being deserialized
     *
     * @throws ClassNotFoundException if the class of a serialized object
     *                                could not be found.
     * @throws IOException            if an I/O error occurs.
     * @throws NotActiveException     if the stream is not currently reading
     *                                objects.
     * @since 1.2
     */
    /*
     * 返回一个PutField对象，其中存放了反序列化字段
     *
     * 注：该方法需要在待反序列化对象自行实现的readObject()方法内被调用
     */
    public ObjectInputStream.GetField readFields() throws IOException, ClassNotFoundException {
        SerialCallbackContext ctx = curContext;
        if(ctx == null) {
            throw new NotActiveException("not in call to readObject");
        }
        
        ctx.checkAndSetUsed();
        ObjectStreamClass curDesc = ctx.getDesc();
        bin.setBlockDataMode(false);
        GetFieldImpl getField = new GetFieldImpl(curDesc);
        getField.readFields();
        bin.setBlockDataMode(true);
        if(!curDesc.hasWriteObjectData()) {
            /*
             * Fix for 4360508: since stream does not contain terminating
             * TC_ENDBLOCKDATA tag, set flag so that reading code elsewhere
             * knows to simulate end-of-custom-data behavior.
             */
            defaultDataEnd = true;
        }
        
        return getField;
    }
    
    /*▲ 反序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 序列化-内部实现 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Reads in null code, sets passHandle to NULL_HANDLE and returns null.
     */
    // 读取null
    private Object readNull() throws IOException {
        if(bin.readByte() != TC_NULL) {
            throw new InternalError();
        }
        
        passHandle = NULL_HANDLE;
        
        return null;
    }
    
    /**
     * Reads in object handle, sets passHandle to the read handle, and returns object associated with the handle.
     */
    // 读取共享对象在对象顺序表中的位置，unshared必须为false
    private Object readHandle(boolean unshared) throws IOException {
        if(bin.readByte() != TC_REFERENCE) {
            throw new InternalError();
        }
        
        passHandle = bin.readInt() - baseWireHandle;
        if(passHandle<0 || passHandle >= handles.size()) {
            throw new StreamCorruptedException(String.format("invalid handle value: %08X", passHandle + baseWireHandle));
        }
        
        if(unshared) {
            // REMIND: what type of exception to throw here?
            throw new InvalidObjectException("cannot read back reference as unshared");
        }
        
        Object obj = handles.lookupObject(passHandle);
        if(obj == unsharedMarker) {
            // REMIND: what type of exception to throw here?
            throw new InvalidObjectException("cannot read back reference to unshared object");
        }
        
        filterCheck(null, -1);       // just a check for number of references, depth, no class
        
        return obj;
    }
    
    /**
     * Reads in and returns (possibly null) class descriptor.  Sets passHandle
     * to class descriptor's assigned handle.  If class descriptor cannot be
     * resolved to a class in the local VM, a ClassNotFoundException is
     * associated with the class descriptor's handle.
     */
    // 读取序列化描述符
    private ObjectStreamClass readClassDesc(boolean unshared) throws IOException {
        byte tc = bin.peekByte();
        
        ObjectStreamClass descriptor;
        
        switch(tc) {
            case TC_NULL:
                descriptor = (ObjectStreamClass) readNull();
                break;
            case TC_REFERENCE:
                descriptor = (ObjectStreamClass) readHandle(unshared);
                break;
            case TC_PROXYCLASSDESC:
                descriptor = readProxyDesc(unshared);
                break;
            case TC_CLASSDESC:
                descriptor = readNonProxyDesc(unshared);
                break;
            default:
                throw new StreamCorruptedException(String.format("invalid type code: %02X", tc));
        }
        
        return descriptor;
    }
    
    /**
     * Reads in and returns class object.  Sets passHandle to class object's
     * assigned handle.  Returns null if class is unresolvable (in which case a
     * ClassNotFoundException will be associated with the class' handle in the
     * handle table).
     */
    // 读取类对象，unshared指示该对象是否非共享
    private Class<?> readClass(boolean unshared) throws IOException {
        if(bin.readByte() != TC_CLASS) {
            throw new InternalError();
        }
        
        ObjectStreamClass desc = readClassDesc(false);
        Class<?> cl = desc.forClass();
        passHandle = handles.assign(unshared ? unsharedMarker : cl);
        
        ClassNotFoundException resolveEx = desc.getResolveException();
        if(resolveEx != null) {
            handles.markException(passHandle, resolveEx);
        }
        
        handles.finish(passHandle);
        
        return cl;
    }
    
    /**
     * Reads in and returns class descriptor for a dynamic proxy class.  Sets
     * passHandle to proxy class descriptor's assigned handle.  If proxy class
     * descriptor cannot be resolved to a class in the local VM, a
     * ClassNotFoundException is associated with the descriptor's handle.
     */
    // 读取代理类对象的序列化描述符
    private ObjectStreamClass readProxyDesc(boolean unshared) throws IOException {
        if(bin.readByte() != TC_PROXYCLASSDESC) {
            throw new InternalError();
        }
        
        ObjectStreamClass desc = new ObjectStreamClass();
        
        int descHandle = handles.assign(unshared ? unsharedMarker : desc);
        
        passHandle = NULL_HANDLE;
        
        int numIfaces = bin.readInt();
        if(numIfaces>65535) {
            throw new InvalidObjectException("interface limit exceeded: " + numIfaces);
        }
        
        // 读取代理接口信息
        String[] ifaces = new String[numIfaces];
        for(int i = 0; i<numIfaces; i++) {
            ifaces[i] = bin.readUTF();
        }
        
        Class<?> cl = null;
        ClassNotFoundException resolveEx = null;
        
        bin.setBlockDataMode(true);
        
        try {
            // 解析代理类的类对象，interfaces是代理接口
            cl = resolveProxyClass(ifaces);
            
            if(cl == null) {
                resolveEx = new ClassNotFoundException("null class");
            } else if(!Proxy.isProxyClass(cl)) {
                throw new InvalidClassException("Not a proxy");
            } else {
                // ReflectUtil.checkProxyPackageAccess makes a test
                // equivalent to isCustomSubclass so there's no need
                // to condition this call to isCustomSubclass == true here.
                ReflectUtil.checkProxyPackageAccess(getClass().getClassLoader(), cl.getInterfaces());
                // Filter the interfaces
                for(Class<?> clazz : cl.getInterfaces()) {
                    filterCheck(clazz, -1);
                }
            }
        } catch(ClassNotFoundException ex) {
            resolveEx = ex;
        }
        
        // Call filterCheck on the class before reading anything else
        filterCheck(cl, -1);
        
        skipCustomData();
        
        try {
            totalObjectRefs++;
            depth++;
            desc.initProxy(cl, resolveEx, readClassDesc(false));
        } finally {
            depth--;
        }
        
        handles.finish(descHandle);
        passHandle = descHandle;
        return desc;
    }
    
    /**
     * Reads in and returns class descriptor for a class that is not a dynamic
     * proxy class.  Sets passHandle to class descriptor's assigned handle.  If
     * class descriptor cannot be resolved to a class in the local VM, a
     * ClassNotFoundException is associated with the descriptor's handle.
     */
    // 读取非代理类对象的序列化描述符
    private ObjectStreamClass readNonProxyDesc(boolean unshared) throws IOException {
        if(bin.readByte() != TC_CLASSDESC) {
            throw new InternalError();
        }
        
        ObjectStreamClass desc = new ObjectStreamClass();
        
        int descHandle = handles.assign(unshared ? unsharedMarker : desc);
        
        passHandle = NULL_HANDLE;
        
        ObjectStreamClass readDesc;
        try {
            // 读取非代理对象序列化描述符
            readDesc = readClassDescriptor();
        } catch(ClassNotFoundException ex) {
            throw (IOException) new InvalidClassException("failed to read class descriptor").initCause(ex);
        }
        
        Class<?> cl = null;
        ClassNotFoundException resolveEx = null;
        
        bin.setBlockDataMode(true);
        
        final boolean checksRequired = isCustomSubclass();
        
        try {
            // 解析非代理类对象
            cl = resolveClass(readDesc);
            
            if(cl == null) {
                resolveEx = new ClassNotFoundException("null class");
            } else if(checksRequired) {
                ReflectUtil.checkPackageAccess(cl);
            }
        } catch(ClassNotFoundException ex) {
            resolveEx = ex;
        }
        
        // Call filterCheck on the class before reading anything else
        filterCheck(cl, -1);
        
        skipCustomData();
        
        try {
            totalObjectRefs++;
            depth++;
            desc.initNonProxy(readDesc, cl, resolveEx, readClassDesc(false));
        } finally {
            depth--;
        }
        
        handles.finish(descHandle);
        passHandle = descHandle;
        
        return desc;
    }
    
    /**
     * Read a class descriptor from the serialization stream.  This method is
     * called when the ObjectInputStream expects a class descriptor as the next
     * item in the serialization stream.  Subclasses of ObjectInputStream may
     * override this method to read in class descriptors that have been written
     * in non-standard formats (by subclasses of ObjectOutputStream which have
     * overridden the <code>writeClassDescriptor</code> method).  By default,
     * this method reads class descriptors according to the format defined in
     * the Object Serialization specification.
     *
     * @return the class descriptor read
     *
     * @throws IOException            If an I/O error has occurred.
     * @throws ClassNotFoundException If the Class of a serialized object used
     *                                in the class descriptor representation cannot be found
     * @see java.io.ObjectOutputStream#writeClassDescriptor(java.io.ObjectStreamClass)
     * @since 1.3
     */
    // 读取非代理对象的序列化描述符
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        ObjectStreamClass desc = new ObjectStreamClass();
        desc.readNonProxy(this);
        return desc;
    }
    
    /**
     * Reads in and returns new string.  Sets passHandle to new string's assigned handle.
     */
    // 对String类型的对象进行反序列化
    private String readString(boolean unshared) throws IOException {
        String str;
        
        byte tc = bin.readByte();
        switch(tc) {
            // 读取小字符串
            case TC_STRING:
                str = bin.readUTF();
                break;
            
            // 读取大字符串
            case TC_LONGSTRING:
                str = bin.readLongUTF();
                break;
            
            default:
                throw new StreamCorruptedException(String.format("invalid type code: %02X", tc));
        }
        
        passHandle = handles.assign(unshared ? unsharedMarker : str);
        handles.finish(passHandle);
        return str;
    }
    
    /**
     * Reads in and returns array object, or null if array class is unresolvable.
     * Sets passHandle to array's assigned handle.
     */
    // 对数组类型的对象进行反序列化
    private Object readArray(boolean unshared) throws IOException {
        if(bin.readByte() != TC_ARRAY) {
            throw new InternalError();
        }
        
        ObjectStreamClass desc = readClassDesc(false);
        int len = bin.readInt();
        
        filterCheck(desc.forClass(), len);
        
        Object array = null;
        Class<?> cl, ccl = null;
        if((cl = desc.forClass()) != null) {
            ccl = cl.getComponentType();
            array = Array.newInstance(ccl, len);
        }
        
        int arrayHandle = handles.assign(unshared ? unsharedMarker : array);
        ClassNotFoundException resolveEx = desc.getResolveException();
        if(resolveEx != null) {
            handles.markException(arrayHandle, resolveEx);
        }
        
        if(ccl == null) {
            for(int i = 0; i<len; i++) {
                readObject0(false);
            }
        } else if(ccl.isPrimitive()) {
            if(ccl == Integer.TYPE) {
                bin.readInts((int[]) array, 0, len);
            } else if(ccl == Byte.TYPE) {
                bin.readFully((byte[]) array, 0, len, true);
            } else if(ccl == Long.TYPE) {
                bin.readLongs((long[]) array, 0, len);
            } else if(ccl == Float.TYPE) {
                bin.readFloats((float[]) array, 0, len);
            } else if(ccl == Double.TYPE) {
                bin.readDoubles((double[]) array, 0, len);
            } else if(ccl == Short.TYPE) {
                bin.readShorts((short[]) array, 0, len);
            } else if(ccl == Character.TYPE) {
                bin.readChars((char[]) array, 0, len);
            } else if(ccl == Boolean.TYPE) {
                bin.readBooleans((boolean[]) array, 0, len);
            } else {
                throw new InternalError();
            }
        } else {
            Object[] oa = (Object[]) array;
            for(int i = 0; i<len; i++) {
                oa[i] = readObject0(false);
                handles.markDependency(arrayHandle, passHandle);
            }
        }
        
        handles.finish(arrayHandle);
        passHandle = arrayHandle;
        return array;
    }
    
    /**
     * Reads in and returns enum constant, or null if enum type is
     * unresolvable.  Sets passHandle to enum constant's assigned handle.
     */
    // 对枚举类型的对象进行反序列化
    private Enum<?> readEnum(boolean unshared) throws IOException {
        if(bin.readByte() != TC_ENUM) {
            throw new InternalError();
        }
        
        ObjectStreamClass desc = readClassDesc(false);
        if(!desc.isEnum()) {
            throw new InvalidClassException("non-enum class: " + desc);
        }
        
        int enumHandle = handles.assign(unshared ? unsharedMarker : null);
        ClassNotFoundException resolveEx = desc.getResolveException();
        if(resolveEx != null) {
            handles.markException(enumHandle, resolveEx);
        }
        
        String name = readString(false);
        Enum<?> result = null;
        Class<?> cl = desc.forClass();
        if(cl != null) {
            try {
                @SuppressWarnings("unchecked")
                Enum<?> en = Enum.valueOf((Class) cl, name);
                result = en;
            } catch(IllegalArgumentException ex) {
                throw (IOException) new InvalidObjectException("enum constant " + name + " does not exist in " + cl).initCause(ex);
            }
            if(!unshared) {
                handles.setObject(enumHandle, result);
            }
        }
        
        handles.finish(enumHandle);
        passHandle = enumHandle;
        return result;
    }
    
    /**
     * Reads and returns "ordinary" (i.e., not a String, Class,
     * ObjectStreamClass, array, or enum constant) object, or null if object's
     * class is unresolvable (in which case a ClassNotFoundException will be
     * associated with object's handle).  Sets passHandle to object's assigned
     * handle.
     */
    // 对Serializable类型(包含Externalizable类型)的对象进行反序列化
    private Object readOrdinaryObject(boolean unshared) throws IOException {
        if(bin.readByte() != TC_OBJECT) {
            throw new InternalError();
        }
        
        ObjectStreamClass desc = readClassDesc(false);
        desc.checkDeserialize();
        
        Class<?> cl = desc.forClass();
        if(cl == String.class || cl == Class.class || cl == ObjectStreamClass.class) {
            throw new InvalidClassException("invalid class descriptor");
        }
        
        Object obj;
        try {
            obj = desc.isInstantiable() ? desc.newInstance() : null;
        } catch(Exception ex) {
            throw (IOException) new InvalidClassException(desc.forClass().getName(), "unable to create instance").initCause(ex);
        }
        
        passHandle = handles.assign(unshared ? unsharedMarker : obj);
        ClassNotFoundException resolveEx = desc.getResolveException();
        if(resolveEx != null) {
            handles.markException(passHandle, resolveEx);
        }
        
        if(desc.isExternalizable()) {
            readExternalData((Externalizable) obj, desc);
        } else {
            readSerialData(obj, desc);
        }
        
        handles.finish(passHandle);
        
        if(obj != null && handles.lookupException(passHandle) == null && desc.hasReadResolveMethod()) {
            Object rep = desc.invokeReadResolve(obj);
            if(unshared && rep.getClass().isArray()) {
                rep = cloneArray(rep);
            }
            if(rep != obj) {
                // Filter the replacement object
                if(rep != null) {
                    if(rep.getClass().isArray()) {
                        filterCheck(rep.getClass(), Array.getLength(rep));
                    } else {
                        filterCheck(rep.getClass(), -1);
                    }
                }
                handles.setObject(passHandle, obj = rep);
            }
        }
        
        return obj;
    }
    
    /**
     * If obj is non-null, reads externalizable data by invoking readExternal()
     * method of obj; otherwise, attempts to skip over externalizable data.
     * Expects that passHandle is set to obj's handle before this method is
     * called.
     */
    // 对Externalizable类型的对象进行反序列化
    private void readExternalData(Externalizable obj, ObjectStreamClass desc) throws IOException {
        SerialCallbackContext oldContext = curContext;
        if(oldContext != null)
            oldContext.check();
        curContext = null;
        try {
            boolean blocked = desc.hasBlockExternalData();
            if(blocked) {
                bin.setBlockDataMode(true);
            }
            if(obj != null) {
                try {
                    obj.readExternal(this);
                } catch(ClassNotFoundException ex) {
                    /*
                     * In most cases, the handle table has already propagated
                     * a CNFException to passHandle at this point; this mark
                     * call is included to address cases where the readExternal
                     * method has cons'ed and thrown a new CNFException of its
                     * own.
                     */
                    handles.markException(passHandle, ex);
                }
            }
            if(blocked) {
                skipCustomData();
            }
        } finally {
            if(oldContext != null)
                oldContext.check();
            curContext = oldContext;
        }
        /*
         * At this point, if the externalizable data was not written in
         * block-data form and either the externalizable class doesn't exist
         * locally (i.e., obj == null) or readExternal() just threw a
         * CNFException, then the stream is probably in an inconsistent state,
         * since some (or all) of the externalizable data may not have been
         * consumed.  Since there's no "correct" action to take in this case,
         * we mimic the behavior of past serialization implementations and
         * blindly hope that the stream is in sync; if it isn't and additional
         * externalizable data remains in the stream, a subsequent read will
         * most likely throw a StreamCorruptedException.
         */
    }
    
    /**
     * Reads (or attempts to skip, if obj is null or is tagged with a
     * ClassNotFoundException) instance data for each serializable class of
     * object in stream, from superclass to subclass.  Expects that passHandle
     * is set to obj's handle before this method is called.
     */
    // 对Serializable类型(非Externalizable类型)的对象进行反序列化
    private void readSerialData(Object obj, ObjectStreamClass desc) throws IOException {
        ObjectStreamClass.ClassDataSlot[] slots = desc.getClassDataLayout();
        // Best effort Failure Atomicity; slotValues will be non-null if field
        // values can be set after reading all field data in the hierarchy.
        // Field values can only be set after reading all data if there are no
        // user observable methods in the hierarchy, readObject(NoData). The
        // top most Serializable class in the hierarchy can be skipped.
        FieldValues[] slotValues = null;
        
        boolean hasSpecialReadMethod = false;
        for(int i = 1; i<slots.length; i++) {
            ObjectStreamClass slotDesc = slots[i].desc;
            if(slotDesc.hasReadObjectMethod() || slotDesc.hasReadObjectNoDataMethod()) {
                hasSpecialReadMethod = true;
                break;
            }
        }
        // No special read methods, can store values and defer setting.
        if(!hasSpecialReadMethod)
            slotValues = new FieldValues[slots.length];
        
        for(int i = 0; i<slots.length; i++) {
            ObjectStreamClass slotDesc = slots[i].desc;
            
            if(slots[i].hasData) {
                if(obj == null || handles.lookupException(passHandle) != null) {
                    defaultReadFields(null, slotDesc); // skip field values
                } else if(slotDesc.hasReadObjectMethod()) {
                    ThreadDeath t = null;
                    boolean reset = false;
                    SerialCallbackContext oldContext = curContext;
                    if(oldContext != null)
                        oldContext.check();
                    try {
                        curContext = new SerialCallbackContext(obj, slotDesc);
                        
                        bin.setBlockDataMode(true);
                        slotDesc.invokeReadObject(obj, this);
                    } catch(ClassNotFoundException ex) {
                        /*
                         * In most cases, the handle table has already
                         * propagated a CNFException to passHandle at this
                         * point; this mark call is included to address cases
                         * where the custom readObject method has cons'ed and
                         * thrown a new CNFException of its own.
                         */
                        handles.markException(passHandle, ex);
                    } finally {
                        do {
                            try {
                                curContext.setUsed();
                                if(oldContext != null)
                                    oldContext.check();
                                curContext = oldContext;
                                reset = true;
                            } catch(ThreadDeath x) {
                                t = x;  // defer until reset is true
                            }
                        } while(!reset);
                        if(t != null)
                            throw t;
                    }
                    
                    /*
                     * defaultDataEnd may have been set indirectly by custom
                     * readObject() method when calling defaultReadObject() or
                     * readFields(); clear it to restore normal read behavior.
                     */
                    defaultDataEnd = false;
                } else {
                    FieldValues vals = defaultReadFields(obj, slotDesc);
                    if(slotValues != null) {
                        slotValues[i] = vals;
                    } else if(obj != null) {
                        defaultCheckFieldValues(obj, slotDesc, vals);
                        defaultSetFieldValues(obj, slotDesc, vals);
                    }
                }
                
                if(slotDesc.hasWriteObjectData()) {
                    skipCustomData();
                } else {
                    bin.setBlockDataMode(false);
                }
            } else {
                if(obj != null && slotDesc.hasReadObjectNoDataMethod() && handles.lookupException(passHandle) == null) {
                    slotDesc.invokeReadObjectNoData(obj);
                }
            }
        }
        
        if(obj != null && slotValues != null) {
            // Check that the non-primitive types are assignable for all slots
            // before assigning.
            for(int i = 0; i<slots.length; i++) {
                if(slotValues[i] != null)
                    defaultCheckFieldValues(obj, slots[i].desc, slotValues[i]);
            }
            for(int i = 0; i<slots.length; i++) {
                if(slotValues[i] != null)
                    defaultSetFieldValues(obj, slots[i].desc, slotValues[i]);
            }
        }
    }
    
    /**
     * Reads in and returns IOException that caused serialization to abort.
     * All stream state is discarded prior to reading in fatal exception.  Sets
     * passHandle to fatal exception's handle.
     */
    // 读取异常信息
    private IOException readFatalException() throws IOException {
        if(bin.readByte() != TC_EXCEPTION) {
            throw new InternalError();
        }
        clear();
        return (IOException) readObject0(false);
    }
    
    /*▲ 序列化-内部实现 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Closes the input stream. Must be called to release any resources
     * associated with the stream.
     *
     * @throws IOException If an I/O error has occurred.
     */
    // 关闭输入流
    public void close() throws IOException {
        /*
         * Even if stream already closed, propagate redundant close to
         * underlying stream to stay consistent with previous implementations.
         */
        closed = true;
        
        if(depth == 0) {
            clear();
        }
        
        bin.close();
    }
    
    /**
     * Returns the number of bytes that can be read without blocking.
     *
     * @return the number of available bytes.
     *
     * @throws IOException if there are I/O errors while reading from the
     *                     underlying <code>InputStream</code>
     */
    // 返回剩余可不被阻塞地读取（或跳过）的字节数（估计值）
    public int available() throws IOException {
        return bin.available();
    }
    
    /**
     * Skips bytes.
     *
     * @param len the number of bytes to be skipped
     *
     * @return the actual number of bytes skipped.
     *
     * @throws IOException If an I/O error has occurred.
     */
    // 跳过len个字节
    public int skipBytes(int len) throws IOException {
        return bin.skipBytes(len);
    }
    
    /**
     * If recursion depth is 0, clears internal data structures; otherwise,
     * throws a StreamCorruptedException.  This method is called when a
     * TC_RESET typecode is encountered.
     */
    private void handleReset() throws StreamCorruptedException {
        if(depth>0) {
            throw new StreamCorruptedException("unexpected reset; recursion depth: " + depth);
        }
        
        clear();
    }
    
    /**
     * Clears internal data structures.
     */
    private void clear() {
        handles.clear();
        vlist.clear();
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 过滤器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the serialization filter for this stream.
     * The serialization filter is the most recent filter set in
     * {@link #setObjectInputFilter setObjectInputFilter} or
     * the initial process-wide filter from
     * {@link ObjectInputFilter.Config#getSerialFilter() ObjectInputFilter.Config.getSerialFilter}.
     *
     * @return the serialization filter for the stream; may be null
     *
     * @since 9
     */
    // 返回反序列化过滤器
    public final ObjectInputFilter getObjectInputFilter() {
        return serialFilter;
    }
    
    /**
     * Set the serialization filter for the stream.
     * The filter's {@link ObjectInputFilter#checkInput checkInput} method is called
     * for each class and reference in the stream.
     * The filter can check any or all of the class, the array length, the number
     * of references, the depth of the graph, and the size of the input stream.
     * The depth is the number of nested {@linkplain #readObject readObject}
     * calls starting with the reading of the root of the graph being deserialized
     * and the current object being deserialized.
     * The number of references is the cumulative number of objects and references
     * to objects already read from the stream including the current object being read.
     * The filter is invoked only when reading objects from the stream and for
     * not primitives.
     * <p>
     * If the filter returns {@link ObjectInputFilter.Status#REJECTED Status.REJECTED},
     * {@code null} or throws a {@link RuntimeException},
     * the active {@code readObject} or {@code readUnshared}
     * throws {@link InvalidClassException}, otherwise deserialization
     * continues uninterrupted.
     * <p>
     * The serialization filter is initialized to the value of
     * {@link ObjectInputFilter.Config#getSerialFilter() ObjectInputFilter.Config.getSerialFilter}
     * when the {@code  ObjectInputStream} is constructed and can be set
     * to a custom filter only once.
     *
     * @param filter the filter, may be null
     *
     * @throws SecurityException     if there is security manager and the
     *                               {@code SerializablePermission("serialFilter")} is not granted
     * @throws IllegalStateException if the {@linkplain #getObjectInputFilter() current filter}
     *                               is not {@code null} and is not the process-wide filter
     * @implSpec The filter, when not {@code null}, is invoked during {@link #readObject readObject}
     * and {@link #readUnshared readUnshared} for each object (regular or class) in the stream.
     * Strings are treated as primitives and do not invoke the filter.
     * The filter is called for:
     * <ul>
     *     <li>each object reference previously deserialized from the stream
     *     (class is {@code null}, arrayLength is -1),
     *     <li>each regular class (class is not {@code null}, arrayLength is -1),
     *     <li>each interface of a dynamic proxy and the dynamic proxy class itself
     *     (class is not {@code null}, arrayLength is -1),
     *     <li>each array is filtered using the array type and length of the array
     *     (class is the array type, arrayLength is the requested length),
     *     <li>each object replaced by its class' {@code readResolve} method
     *         is filtered using the replacement object's class, if not {@code null},
     *         and if it is an array, the arrayLength, otherwise -1,
     *     <li>and each object replaced by {@link #resolveObject resolveObject}
     *         is filtered using the replacement object's class, if not {@code null},
     *         and if it is an array, the arrayLength, otherwise -1.
     * </ul>
     *
     * When the {@link ObjectInputFilter#checkInput checkInput} method is invoked
     * it is given access to the current class, the array length,
     * the current number of references already read from the stream,
     * the depth of nested calls to {@link #readObject readObject} or
     * {@link #readUnshared readUnshared},
     * and the implementation dependent number of bytes consumed from the input stream.
     * <p>
     * Each call to {@link #readObject readObject} or
     * {@link #readUnshared readUnshared} increases the depth by 1
     * before reading an object and decreases by 1 before returning
     * normally or exceptionally.
     * The depth starts at {@code 1} and increases for each nested object and
     * decrements when each nested call returns.
     * The count of references in the stream starts at {@code 1} and
     * is increased before reading an object.
     * @since 9
     */
    // 设置反序列化过滤器
    public final void setObjectInputFilter(ObjectInputFilter filter) {
        SecurityManager sm = System.getSecurityManager();
        if(sm != null) {
            sm.checkPermission(ObjectStreamConstants.SERIAL_FILTER_PERMISSION);
        }
        
        // Allow replacement of the process-wide filter if not already set
        if(serialFilter != null && serialFilter != ObjectInputFilter.Config.getSerialFilter()) {
            throw new IllegalStateException("filter can not be set more than once");
        }
        
        this.serialFilter = filter;
    }
    
    /*▲ 过滤器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /**
     * Invoke the serialization filter if non-null.
     * If the filter rejects or an exception is thrown, throws InvalidClassException.
     *
     * @param clazz       the class; may be null
     * @param arrayLength the array length requested; use {@code -1} if not creating an array
     *
     * @throws InvalidClassException if it rejected by the filter or a {@link RuntimeException} is thrown
     */
    // 过滤检查；arrayLength指示数组长度，如果不是数组，其值为-1
    private void filterCheck(Class<?> clazz, int arrayLength) throws InvalidClassException {
        if(serialFilter == null) {
            return;
        }
        
        RuntimeException ex = null;
        ObjectInputFilter.Status status;
        
        // Info about the stream is not available if overridden by subclass, return 0
        long bytesRead = (bin == null) ? 0 : bin.getBytesRead();
        
        try {
            FilterValues filterValues = new FilterValues(clazz, arrayLength, totalObjectRefs, depth, bytesRead);
            status = serialFilter.checkInput(filterValues);
        } catch(RuntimeException e) {
            // Preventive interception of an exception to log
            status = ObjectInputFilter.Status.REJECTED;
            ex = e;
        }
        
        if(Logging.filterLogger != null) {
            // Debug logging of filter checks that fail; Tracing for those that succeed
            Logging.filterLogger.log(status == null || status == ObjectInputFilter.Status.REJECTED ? Logger.Level.DEBUG : Logger.Level.TRACE, "ObjectInputFilter {0}: {1}, array length: {2}, nRefs: {3}, depth: {4}, bytes: {5}, ex: {6}", status, clazz, arrayLength, totalObjectRefs, depth, bytesRead, Objects.toString(ex, "n/a"));
        }
        
        if(status == null || status == ObjectInputFilter.Status.REJECTED) {
            InvalidClassException ice = new InvalidClassException("filter status: " + status);
            ice.initCause(ex);
            throw ice;
        }
    }
    
    /**
     * Checks the given array type and length to ensure that creation of such
     * an array is permitted by this ObjectInputStream. The arrayType argument
     * must represent an actual array type.
     *
     * This private method is called via SharedSecrets.
     *
     * @param arrayType   the array type
     * @param arrayLength the array length
     *
     * @throws NullPointerException       if arrayType is null
     * @throws IllegalArgumentException   if arrayType isn't actually an array type
     * @throws NegativeArraySizeException if arrayLength is negative
     * @throws InvalidClassException      if the filter rejects creation
     */
    private void checkArray(Class<?> arrayType, int arrayLength) throws InvalidClassException {
        if(!arrayType.isArray()) {
            throw new IllegalArgumentException("not an array type");
        }
        
        if(arrayLength<0) {
            throw new NegativeArraySizeException();
        }
        
        filterCheck(arrayType, arrayLength);
    }
    
    /**
     * If resolveObject has been enabled and given object does not have an exception associated with it,
     * calls resolveObject to determine replacement for object, and updates handle table accordingly.
     * Returns replacement object, or echoes provided object if no replacement occurred.
     * Expects that passHandle is set to given object's handle prior to calling this method.
     */
    /*
     * 如果启用了resolveObject并且给定对象没有与之关联的异常，
     * 则调用resolveObject以确定对象的替换，并相应地更新句柄表。
     *
     * 返回替换对象，如果未发生替换，则回显提供的对象。
     * 期望在调用此方法之前将passHandle设置为给定对象的句柄。
     */
    private Object checkResolve(Object obj) throws IOException {
        if(!enableResolve || handles.lookupException(passHandle) != null) {
            return obj;
        }
        
        Object rep = resolveObject(obj);
        
        if(rep != obj) {
            /*
             * The type of the original object has been filtered but resolveObject may have replaced it;
             * filter the replacement's type.
             */
            if(rep != null) {
                if(rep.getClass().isArray()) {
                    filterCheck(rep.getClass(), Array.getLength(rep));
                } else {
                    filterCheck(rep.getClass(), -1);
                }
            }
            
            handles.setObject(passHandle, rep);
        }
        
        return rep;
    }
    
    /** Throws ClassCastException if any value is not assignable. */
    private void defaultCheckFieldValues(Object obj, ObjectStreamClass desc, FieldValues values) {
        Object[] objectValues = values.objValues;
        if(objectValues != null) {
            desc.checkObjFieldValueTypes(obj, objectValues);
        }
    }
    
    
    
    /**
     * Register an object to be validated before the graph is returned.  While
     * similar to resolveObject these validations are called after the entire
     * graph has been reconstituted.  Typically, a readObject method will
     * register the object with the stream so that when all of the objects are
     * restored a final set of validations can be performed.
     *
     * @param obj  the object to receive the validation callback.
     * @param priority controls the order of callbacks;zero is a good default.
     *             Use higher numbers to be called back earlier, lower numbers for
     *             later callbacks. Within a priority, callbacks are processed in
     *             no particular order.
     *
     * @throws NotActiveException     The stream is not currently reading objects
     *                                so it is invalid to register a callback.
     * @throws InvalidObjectException The validation object is null.
     */
    // 注册验证回调，priority指示当前回调的优先级，priority越大，优先级越高(排在调用链前面)
    public void registerValidation(ObjectInputValidation obj, int priority) throws NotActiveException, InvalidObjectException {
        if(depth == 0) {
            throw new NotActiveException("stream inactive");
        }
        
        vlist.register(obj, priority);
    }
    
    /**
     * Enables the stream to do replacement of objects read from the stream. When
     * enabled, the {@link #resolveObject} method is called for every object being
     * deserialized.
     *
     * <p>If object replacement is currently not enabled, and
     * {@code enable} is true, and there is a security manager installed,
     * this method first calls the security manager's
     * {@code checkPermission} method with the
     * {@code SerializablePermission("enableSubstitution")} permission to
     * ensure that the caller is permitted to enable the stream to do replacement
     * of objects read from the stream.
     *
     * @param enable true for enabling use of {@code resolveObject} for
     *               every object being deserialized
     *
     * @return the previous setting before this method was invoked
     *
     * @throws SecurityException if a security manager exists and its
     *                           {@code checkPermission} method denies enabling the stream
     *                           to do replacement of objects read from the stream.
     * @see SecurityManager#checkPermission
     * @see java.io.SerializablePermission
     */
    protected boolean enableResolveObject(boolean enable) throws SecurityException {
        if(enable == enableResolve) {
            return enable;
        }
        
        if(enable) {
            SecurityManager sm = System.getSecurityManager();
            if(sm != null) {
                sm.checkPermission(SUBSTITUTION_PERMISSION);
            }
        }
        
        enableResolve = enable;
        
        return !enableResolve;
    }
    
    /**
     * Reads string without allowing it to be replaced in stream.  Called from
     * within ObjectStreamClass.read().
     */
    String readTypeString() throws IOException {
        int oldHandle = passHandle;
        try {
            byte tc = bin.peekByte();
            switch(tc) {
                case TC_NULL:
                    return (String) readNull();
                
                case TC_REFERENCE:
                    return (String) readHandle(false);
                
                case TC_STRING:
                case TC_LONGSTRING:
                    return readString(false);
                
                default:
                    throw new StreamCorruptedException(String.format("invalid type code: %02X", tc));
            }
        } finally {
            passHandle = oldHandle;
        }
    }
    
    private boolean isCustomSubclass() {
        // Return true if this class is a custom subclass of ObjectInputStream
        return getClass().getClassLoader() != ObjectInputStream.class.getClassLoader();
    }
    
    /**
     * Skips over all block data and objects until TC_ENDBLOCKDATA is
     * encountered.
     */
    private void skipCustomData() throws IOException {
        int oldHandle = passHandle;
        
        for(; ; ) {
            if(bin.getBlockDataMode()) {
                bin.skipBlockData();
                bin.setBlockDataMode(false);
            }
            switch(bin.peekByte()) {
                case TC_BLOCKDATA:
                case TC_BLOCKDATALONG:
                    bin.setBlockDataMode(true);
                    break;
                
                case TC_ENDBLOCKDATA:
                    bin.readByte();
                    passHandle = oldHandle;
                    return;
                
                default:
                    readObject0(false);
                    break;
            }
        }
    }
    
    /** Sets field values in obj. */
    private void defaultSetFieldValues(Object obj, ObjectStreamClass desc, FieldValues values) {
        byte[] primValues = values.primValues;
        Object[] objectValues = values.objValues;
        
        if(primValues != null) {
            desc.setPrimFieldValues(obj, primValues);
        }
        
        if(objectValues != null) {
            desc.setObjFieldValues(obj, objectValues);
        }
    }
    
    /**
     * Returns the first non-null and non-platform class loader (not counting
     * class loaders of generated reflection implementation classes) up the
     * execution stack, or the platform class loader if only code from the
     * bootstrap and platform class loader is on the stack.
     */
    private static ClassLoader latestUserDefinedLoader() {
        return VM.latestUserDefinedLoader();
    }
    
    /**
     * Verifies that this (possibly subclass) instance can be constructed without violating security constraints:
     * the subclass must not override security-sensitive non-final methods,
     * or else the "enableSubclassImplementation" SerializablePermission is checked.
     */
    private void verifySubclass() {
        Class<?> cl = getClass();
        if(cl == ObjectInputStream.class) {
            return;
        }
        
        SecurityManager sm = System.getSecurityManager();
        if(sm == null) {
            return;
        }
        
        // 从subclassAudits中移除subclassAuditsQueue中包含的元素
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
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            public Boolean run() {
                for(Class<?> cl = subcl; cl != ObjectInputStream.class; cl = cl.getSuperclass()) {
                    try {
                        cl.getDeclaredMethod("readUnshared", (Class[]) null);
                        return Boolean.FALSE;
                    } catch(NoSuchMethodException ex) {
                    }
                    
                    try {
                        cl.getDeclaredMethod("readFields", (Class[]) null);
                        return Boolean.FALSE;
                    } catch(NoSuchMethodException ex) {
                    }
                }
                
                return Boolean.TRUE;
            }
        });
    }
    
    /**
     * Converts specified span of bytes into float values.
     */
    // REMIND: remove once hotspot inlines Float.intBitsToFloat
    private static native void bytesToFloats(byte[] src, int srcpos, float[] dst, int dstpos, int nfloats);
    
    /**
     * Converts specified span of bytes into double values.
     */
    // REMIND: remove once hotspot inlines Double.longBitsToDouble
    private static native void bytesToDoubles(byte[] src, int srcpos, double[] dst, int dstpos, int ndoubles);
    
    /**
     * Method for cloning arrays in case of using unsharing reading
     */
    private static Object cloneArray(Object array) {
        if(array instanceof Object[]) {
            return ((Object[]) array).clone();
        } else if(array instanceof boolean[]) {
            return ((boolean[]) array).clone();
        } else if(array instanceof byte[]) {
            return ((byte[]) array).clone();
        } else if(array instanceof char[]) {
            return ((char[]) array).clone();
        } else if(array instanceof double[]) {
            return ((double[]) array).clone();
        } else if(array instanceof float[]) {
            return ((float[]) array).clone();
        } else if(array instanceof int[]) {
            return ((int[]) array).clone();
        } else if(array instanceof long[]) {
            return ((long[]) array).clone();
        } else if(array instanceof short[]) {
            return ((short[]) array).clone();
        } else {
            throw new AssertionError();
        }
    }
    
    /**
     * Performs a "freeze" action, required to adhere to final field semantics.
     *
     * <p> This method can be called unconditionally before returning the graph,
     * from the topmost readObject call, since it is expected that the
     * additional cost of the freeze action is negligible compared to
     * reconstituting even the most simple graph.
     *
     * <p> Nested calls to readObject do not issue freeze actions because the
     * sub-graph returned from a nested call is not guaranteed to be fully
     * initialized yet (possible cycles).
     */
    private void freeze() {
        // Issue a StoreStore|StoreLoad fence, which is at least sufficient to provide final-freeze semantics.
        UNSAFE.storeFence();
    }
    
    
    
    
    
    
    /**
     * Provide access to the persistent fields read from the input stream.
     */
    // 待反序列化字段集
    public abstract static class GetField {
        
        /**
         * Get the ObjectStreamClass that describes the fields in the stream.
         *
         * @return the descriptor class that describes the serializable fields
         */
        public abstract ObjectStreamClass getObjectStreamClass();
        
        /**
         * Return true if the named field is defaulted and has no value in this
         * stream.
         *
         * @param name the name of the field
         *
         * @return true, if and only if the named field is defaulted
         *
         * @throws IOException              if there are I/O errors while reading from
         *                                  the underlying <code>InputStream</code>
         * @throws IllegalArgumentException if <code>name</code> does not
         *                                  correspond to a serializable field
         */
        public abstract boolean defaulted(String name) throws IOException;
        
        /**
         * Get the value of the named boolean field from the persistent field.
         *
         * @param name the name of the field
         * @param val  the default value to use if <code>name</code> does not
         *             have a value
         *
         * @return the value of the named <code>boolean</code> field
         *
         * @throws IOException              if there are I/O errors while reading from the
         *                                  underlying <code>InputStream</code>
         * @throws IllegalArgumentException if type of <code>name</code> is
         *                                  not serializable or if the field type is incorrect
         */
        public abstract boolean get(String name, boolean val) throws IOException;
        
        /**
         * Get the value of the named byte field from the persistent field.
         *
         * @param name the name of the field
         * @param val  the default value to use if <code>name</code> does not
         *             have a value
         *
         * @return the value of the named <code>byte</code> field
         *
         * @throws IOException              if there are I/O errors while reading from the
         *                                  underlying <code>InputStream</code>
         * @throws IllegalArgumentException if type of <code>name</code> is
         *                                  not serializable or if the field type is incorrect
         */
        public abstract byte get(String name, byte val) throws IOException;
        
        /**
         * Get the value of the named char field from the persistent field.
         *
         * @param name the name of the field
         * @param val  the default value to use if <code>name</code> does not
         *             have a value
         *
         * @return the value of the named <code>char</code> field
         *
         * @throws IOException              if there are I/O errors while reading from the
         *                                  underlying <code>InputStream</code>
         * @throws IllegalArgumentException if type of <code>name</code> is
         *                                  not serializable or if the field type is incorrect
         */
        public abstract char get(String name, char val) throws IOException;
        
        /**
         * Get the value of the named short field from the persistent field.
         *
         * @param name the name of the field
         * @param val  the default value to use if <code>name</code> does not
         *             have a value
         *
         * @return the value of the named <code>short</code> field
         *
         * @throws IOException              if there are I/O errors while reading from the
         *                                  underlying <code>InputStream</code>
         * @throws IllegalArgumentException if type of <code>name</code> is
         *                                  not serializable or if the field type is incorrect
         */
        public abstract short get(String name, short val) throws IOException;
        
        /**
         * Get the value of the named int field from the persistent field.
         *
         * @param name the name of the field
         * @param val  the default value to use if <code>name</code> does not
         *             have a value
         *
         * @return the value of the named <code>int</code> field
         *
         * @throws IOException              if there are I/O errors while reading from the
         *                                  underlying <code>InputStream</code>
         * @throws IllegalArgumentException if type of <code>name</code> is
         *                                  not serializable or if the field type is incorrect
         */
        public abstract int get(String name, int val) throws IOException;
        
        /**
         * Get the value of the named long field from the persistent field.
         *
         * @param name the name of the field
         * @param val  the default value to use if <code>name</code> does not
         *             have a value
         *
         * @return the value of the named <code>long</code> field
         *
         * @throws IOException              if there are I/O errors while reading from the
         *                                  underlying <code>InputStream</code>
         * @throws IllegalArgumentException if type of <code>name</code> is
         *                                  not serializable or if the field type is incorrect
         */
        public abstract long get(String name, long val) throws IOException;
        
        /**
         * Get the value of the named float field from the persistent field.
         *
         * @param name the name of the field
         * @param val  the default value to use if <code>name</code> does not
         *             have a value
         *
         * @return the value of the named <code>float</code> field
         *
         * @throws IOException              if there are I/O errors while reading from the
         *                                  underlying <code>InputStream</code>
         * @throws IllegalArgumentException if type of <code>name</code> is
         *                                  not serializable or if the field type is incorrect
         */
        public abstract float get(String name, float val) throws IOException;
        
        /**
         * Get the value of the named double field from the persistent field.
         *
         * @param name the name of the field
         * @param val  the default value to use if <code>name</code> does not
         *             have a value
         *
         * @return the value of the named <code>double</code> field
         *
         * @throws IOException              if there are I/O errors while reading from the
         *                                  underlying <code>InputStream</code>
         * @throws IllegalArgumentException if type of <code>name</code> is
         *                                  not serializable or if the field type is incorrect
         */
        public abstract double get(String name, double val) throws IOException;
        
        /**
         * Get the value of the named Object field from the persistent field.
         *
         * @param name the name of the field
         * @param val  the default value to use if <code>name</code> does not
         *             have a value
         *
         * @return the value of the named <code>Object</code> field
         *
         * @throws IOException              if there are I/O errors while reading from the
         *                                  underlying <code>InputStream</code>
         * @throws IllegalArgumentException if type of <code>name</code> is
         *                                  not serializable or if the field type is incorrect
         */
        public abstract Object get(String name, Object val) throws IOException;
    }
    
    /**
     * Default GetField implementation.
     */
    // 待反序列化字段集的默认实现
    private class GetFieldImpl extends GetField {
        
        /** class descriptor describing serializable fields */
        private final ObjectStreamClass desc;
        /** primitive field values */
        private final byte[] primVals;
        /** object field values */
        private final Object[] objVals;
        /** object field value handles */
        private final int[] objHandles;
        
        /**
         * Creates GetFieldImpl object for reading fields defined in given
         * class descriptor.
         */
        GetFieldImpl(ObjectStreamClass desc) {
            this.desc = desc;
            primVals = new byte[desc.getPrimDataSize()];
            objVals = new Object[desc.getNumObjFields()];
            objHandles = new int[objVals.length];
        }
        
        public ObjectStreamClass getObjectStreamClass() {
            return desc;
        }
        
        public boolean defaulted(String name) throws IOException {
            return (getFieldOffset(name, null)<0);
        }
        
        public boolean get(String name, boolean val) throws IOException {
            int off = getFieldOffset(name, Boolean.TYPE);
            return (off >= 0) ? Bits.getBoolean(primVals, off) : val;
        }
        
        public byte get(String name, byte val) throws IOException {
            int off = getFieldOffset(name, Byte.TYPE);
            return (off >= 0) ? primVals[off] : val;
        }
        
        public char get(String name, char val) throws IOException {
            int off = getFieldOffset(name, Character.TYPE);
            return (off >= 0) ? Bits.getChar(primVals, off) : val;
        }
        
        public short get(String name, short val) throws IOException {
            int off = getFieldOffset(name, Short.TYPE);
            return (off >= 0) ? Bits.getShort(primVals, off) : val;
        }
        
        public int get(String name, int val) throws IOException {
            int off = getFieldOffset(name, Integer.TYPE);
            return (off >= 0) ? Bits.getInt(primVals, off) : val;
        }
        
        public float get(String name, float val) throws IOException {
            int off = getFieldOffset(name, Float.TYPE);
            return (off >= 0) ? Bits.getFloat(primVals, off) : val;
        }
        
        public long get(String name, long val) throws IOException {
            int off = getFieldOffset(name, Long.TYPE);
            return (off >= 0) ? Bits.getLong(primVals, off) : val;
        }
        
        public double get(String name, double val) throws IOException {
            int off = getFieldOffset(name, Double.TYPE);
            return (off >= 0) ? Bits.getDouble(primVals, off) : val;
        }
        
        public Object get(String name, Object val) throws IOException {
            int off = getFieldOffset(name, Object.class);
            if(off >= 0) {
                int objHandle = objHandles[off];
                handles.markDependency(passHandle, objHandle);
                return (handles.lookupException(objHandle) == null) ? objVals[off] : null;
            } else {
                return val;
            }
        }
        
        /**
         * Reads primitive and object field values from stream.
         */
        void readFields() throws IOException {
            bin.readFully(primVals, 0, primVals.length, false);
            
            int oldHandle = passHandle;
            ObjectStreamField[] fields = desc.getFields(false);
            int numPrimFields = fields.length - objVals.length;
            for(int i = 0; i<objVals.length; i++) {
                objVals[i] = readObject0(fields[numPrimFields + i].isUnshared());
                objHandles[i] = passHandle;
            }
            passHandle = oldHandle;
        }
        
        /**
         * Returns offset of field with given name and type.  A specified type
         * of null matches all types, Object.class matches all non-primitive
         * types, and any other non-null type matches assignable types only.
         * If no matching field is found in the (incoming) class
         * descriptor but a matching field is present in the associated local
         * class descriptor, returns -1.  Throws IllegalArgumentException if
         * neither incoming nor local class descriptor contains a match.
         */
        private int getFieldOffset(String name, Class<?> type) {
            ObjectStreamField field = desc.getField(name, type);
            if(field != null) {
                return field.getOffset();
            } else if(desc.getLocalDesc().getField(name, type) != null) {
                return -1;
            } else {
                throw new IllegalArgumentException("no such field " + name + " with type " + type);
            }
        }
    }
    
    /**
     * Input stream with two modes: in default mode, inputs data written in the
     * same format as DataOutputStream; in "block data" mode, inputs data
     * bracketed by block data markers (see object serialization specification
     * for details).  Buffering depends on block data mode: when in default
     * mode, no data is buffered in advance; when in block data mode, all data
     * for the current data block is read in at once (and buffered).
     */
    // 块数据输入流
    private class BlockDataInputStream extends InputStream implements DataInput {
        
        /** maximum data block length */
        private static final int MAX_BLOCK_SIZE = 1024;
        /** maximum data block header length */
        private static final int MAX_HEADER_SIZE = 5;
        /** (tunable) length of char buffer (for reading strings) */
        private static final int CHAR_BUF_SIZE = 256;
        /** readBlockHeader() return value indicating header read may block */
        private static final int HEADER_BLOCKED = -2;
        
        /** buffer for reading general/block data */
        private final byte[] buf = new byte[MAX_BLOCK_SIZE];    // 块数据缓冲区
        /** buffer for reading block data headers */
        private final byte[] hbuf = new byte[MAX_HEADER_SIZE];  // 块数据头缓冲区
        /** char buffer for fast string reads */
        private final char[] cbuf = new char[CHAR_BUF_SIZE];    // 字符数据缓冲区(只取byte部分)
        
        // block data state fields; values meaningful only when blkmode true
        /** loopback stream (for data reads that span data blocks) */
        private final DataInputStream din;  // 包装了当前块数据输入流this的【基础数据输入流】
        
        /** underlying stream (wrapped in peekable filter stream) */
        private final PeekInputStream in;   // 【最终输入流】：包含数据读取的源头，比如文件
        
        /** block data mode */
        private boolean blkmode = false;    // 待读数据是否处于块模式下（在块模式下读取数据时，会先读取一个包含长度信息的头信息）
        
        /** current offset into buf */
        private int pos = 0;    // 指向buf的游标，标记为缓冲区未读数据的起始位置
        
        /** end offset of valid data in buf, or -1 if no more block data */
        private int end = -1;   // 指向buf的游标，标记为缓冲区未读数据的末尾
        
        /** number of bytes in current block yet to be read from stream */
        private int unread = 0; // 当前数据块未读的数据(一部分已经读到了buf中)
        
        
        /**
         * Creates new BlockDataInputStream on top of given underlying stream.
         * Block data mode is turned off by default.
         */
        BlockDataInputStream(InputStream in) {
            this.in = new PeekInputStream(in);
            din = new DataInputStream(this);
        }
        
        
        // 从当前块数据输入流读取一个字节，返回-1表示读取结束
        public int read() throws IOException {
            // 处在块模式下
            if(blkmode) {
                // 如果缓冲区为空
                if(pos == end) {
                    refill();   // 用块数据重新填充内部缓冲区buf(会先清空缓冲区现有数据)
                }
                
                // 从缓冲区读取数据
                return (end >= 0) ? (buf[pos++] & 0xFF) : -1;
            } else {
                // 从【最终输入流】读取数据
                return in.read();
            }
        }
        
        // 从当前块数据输入流读取len个字节，并将读到的内容插入到字节数组b的off索引处
        public int read(byte[] b, int off, int len) throws IOException {
            return read(b, off, len, false);
        }
        
        /**
         * Attempts to read len bytes into byte array b at offset off.  Returns
         * the number of bytes read, or -1 if the end of stream/block data has
         * been reached.  If copy is true, reads values into an intermediate
         * buffer before copying them to b (to avoid exposing a reference to
         * b).
         */
        /*
         * 从当前块数据输入流读取len个字节，并将读到的内容插入到字节数组b的off索引处
         * copy指示在非块模式下，是否需要将最终输入流的数据先填充到缓冲区
         */
        int read(byte[] b, int off, int len, boolean copy) throws IOException {
            if(len == 0) {
                return 0;
                
                // 处在块模式下
            } else if(blkmode) {
                // 如果缓冲区为空
                if(pos == end) {
                    refill();   // 用块数据重新填充内部缓冲区buf(会先清空缓冲区现有数据)
                }
                
                // 没有可读数据
                if(end<0) {
                    return -1;
                }
                
                int nread = Math.min(len, end - pos);
                System.arraycopy(buf, pos, b, off, nread);
                pos += nread;
                return nread;
                
                // 不在块模式，且copy为true时，需要先将最终输入流的数据填充到缓冲区
            } else if(copy) {
                int nread = in.read(buf, 0, Math.min(len, MAX_BLOCK_SIZE));
                if(nread>0) {
                    System.arraycopy(buf, 0, b, off, nread);
                }
                return nread;
                
                // 直接从【最终输入流】读取
            } else {
                return in.read(b, off, len);
            }
        }
        
        /*
         * 从当前块数据输入流读取数据以填满字节数组b
         * 填不满字节数组b不会返回，除非读取过程中发生了异常
         */
        public void readFully(byte[] b) throws IOException {
            readFully(b, 0, b.length, false);
        }
        
        /*
         * 从当前块数据输入流读取len个字节，并将读到的内容插入到字节数组b的off索引处
         * 读不够len个字节不会返回，除非读取过程中发生了异常
         */
        public void readFully(byte[] b, int off, int len) throws IOException {
            readFully(b, off, len, false);
        }
        
        /*
         * 从当前块数据输入流读取len个字节，并将读到的内容插入到字节数组b的off索引处
         * 读不够len个字节不会返回，除非读取过程中发生了异常
         * copy指示在非块模式下，是否需要将最终输入流的数据先填充到缓冲区
         */
        public void readFully(byte[] b, int off, int len, boolean copy) throws IOException {
            while(len>0) {
                int n = read(b, off, len, copy);
                if(n<0) {
                    throw new EOFException();
                }
                off += n;
                len -= n;
            }
        }
        
        // 从当前块数据输入流读取boolean值
        public boolean readBoolean() throws IOException {
            int v = read();
            if(v<0) {
                throw new EOFException();
            }
            return (v != 0);
        }
        
        // 从当前块数据输入流读取char值
        public char readChar() throws IOException {
            // 未处在块模式
            if(!blkmode) {
                pos = 0;
                // 尝试从【最终输入流】中读取2个字节存储到buf中
                in.readFully(buf, 0, 2);
                
                // 处在块模式下，且缓冲区中有足够数据
            } else if(pos + 2>end) {
                // 从基础数据输入流中读取char并返回
                return din.readChar();
            }
            
            // 从字节数组buf的pos处读取char值
            char v = Bits.getChar(buf, pos);
            pos += 2;
            return v;
        }
        
        // 从当前块数据输入流读取byte值
        public byte readByte() throws IOException {
            int v = read();
            if(v<0) {
                throw new EOFException();
            }
            return (byte) v;
        }
        
        // 从当前块数据输入流读取无符号byte值
        public int readUnsignedByte() throws IOException {
            int v = read();
            if(v<0) {
                throw new EOFException();
            }
            return v;
        }
        
        // 从当前块数据输入流读取short值
        public short readShort() throws IOException {
            // 未处在块模式
            if(!blkmode) {
                pos = 0;
                // 尝试从【最终输入流】中读取2个字节存储到buf中
                in.readFully(buf, 0, 2);
                
                // 处在块模式下，且缓冲区中有足够数据
            } else if(pos + 2>end) {
                return din.readShort();
            }
            
            // 从字节数组buf的pos处读取short值
            short v = Bits.getShort(buf, pos);
            pos += 2;
            return v;
        }
        
        // 从当前块数据输入流读取无符号short值
        public int readUnsignedShort() throws IOException {
            // 未处在块模式
            if(!blkmode) {
                pos = 0;
                // 尝试从【最终输入流】中读取2个字节存储到buf中
                in.readFully(buf, 0, 2);
                
                // 处在块模式下，且缓冲区中有足够数据
            } else if(pos + 2>end) {
                return din.readUnsignedShort();
            }
            
            // 从字节数组buf的pos处读取无符号short值
            int v = Bits.getShort(buf, pos) & 0xFFFF;
            pos += 2;
            return v;
        }
        
        // 从当前块数据输入流读取int值
        public int readInt() throws IOException {
            // 未处在块模式
            if(!blkmode) {
                pos = 0;
                // 尝试从【最终输入流】中读取4个字节存储到buf中
                in.readFully(buf, 0, 4);
                
                // 处在块模式下，且缓冲区中有足够数据
            } else if(pos + 4>end) {
                return din.readInt();
            }
            
            int v = Bits.getInt(buf, pos);
            pos += 4;
            return v;
        }
        
        // 从当前块数据输入流读取long值
        public long readLong() throws IOException {
            // 未处在块模式
            if(!blkmode) {
                pos = 0;
                // 尝试从【最终输入流】中读取8个字节存储到buf中
                in.readFully(buf, 0, 8);
                
                // 处在块模式下，且缓冲区中有足够数据
            } else if(pos + 8>end) {
                return din.readLong();
            }
            
            long v = Bits.getLong(buf, pos);
            pos += 8;
            return v;
        }
        
        // 从当前块数据输入流读取float值
        public float readFloat() throws IOException {
            // 未处在块模式
            if(!blkmode) {
                pos = 0;
                // 尝试从【最终输入流】中读取4个字节存储到buf中
                in.readFully(buf, 0, 4);
                
                // 处在块模式下，且缓冲区中有足够数据
            } else if(pos + 4>end) {
                return din.readFloat();
            }
            
            float v = Bits.getFloat(buf, pos);
            pos += 4;
            return v;
        }
        
        // 从当前块数据输入流读取double值
        public double readDouble() throws IOException {
            // 未处在块模式
            if(!blkmode) {
                pos = 0;
                // 尝试从【最终输入流】中读取8个字节存储到buf中
                in.readFully(buf, 0, 8);
                
                // 处在块模式下，且缓冲区中有足够数据
            } else if(pos + 8>end) {
                return din.readDouble();
            }
            
            double v = Bits.getDouble(buf, pos);
            pos += 8;
            return v;
        }
        
        // 从当前块数据输入流读取len个boolean值填充到数组off处
        void readBooleans(boolean[] v, int off, int len) throws IOException {
            int stop, endoff = off + len;
            
            while(off<endoff) {
                // 未处在块模式
                if(!blkmode) {
                    int span = Math.min(endoff - off, MAX_BLOCK_SIZE);
                    in.readFully(buf, 0, span);
                    stop = off + span;
                    pos = 0;
                    
                    // 处在块模式下，且缓冲区中有足够数据
                } else if(pos + 1>end) {
                    v[off++] = din.readBoolean();
                    continue;
                } else {
                    stop = Math.min(endoff, off + end - pos);
                }
                
                while(off<stop) {
                    v[off++] = Bits.getBoolean(buf, pos++);
                }
            }
        }
        
        // 从当前块数据输入流读取len个char值填充到数组off处
        void readChars(char[] v, int off, int len) throws IOException {
            int stop, endoff = off + len;
            while(off<endoff) {
                // 未处在块模式
                if(!blkmode) {
                    int span = Math.min(endoff - off, MAX_BLOCK_SIZE >> 1);
                    in.readFully(buf, 0, span << 1);
                    stop = off + span;
                    pos = 0;
                    
                    // 处在块模式下，且缓冲区中有足够数据
                } else if(pos + 2>end) {
                    v[off++] = din.readChar();
                    continue;
                } else {
                    stop = Math.min(endoff, off + ((end - pos) >> 1));
                }
                
                while(off<stop) {
                    v[off++] = Bits.getChar(buf, pos);
                    pos += 2;
                }
            }
        }
        
        // 从当前块数据输入流读取len个short值填充到数组off处
        void readShorts(short[] v, int off, int len) throws IOException {
            int stop, endoff = off + len;
            while(off<endoff) {
                // 未处在块模式
                if(!blkmode) {
                    int span = Math.min(endoff - off, MAX_BLOCK_SIZE >> 1);
                    in.readFully(buf, 0, span << 1);
                    stop = off + span;
                    pos = 0;
                    
                    // 处在块模式下，且缓冲区中有足够数据
                } else if(pos + 2>end) {
                    v[off++] = din.readShort();
                    continue;
                } else {
                    stop = Math.min(endoff, off + ((end - pos) >> 1));
                }
                
                while(off<stop) {
                    v[off++] = Bits.getShort(buf, pos);
                    pos += 2;
                }
            }
        }
        
        // 从当前块数据输入流读取len个int值填充到数组off处
        void readInts(int[] v, int off, int len) throws IOException {
            int stop, endoff = off + len;
            while(off<endoff) {
                // 未处在块模式
                if(!blkmode) {
                    int span = Math.min(endoff - off, MAX_BLOCK_SIZE >> 2);
                    in.readFully(buf, 0, span << 2);
                    stop = off + span;
                    pos = 0;
                    
                    // 处在块模式下，且缓冲区中有足够数据
                } else if(pos + 4>end) {
                    v[off++] = din.readInt();
                    continue;
                } else {
                    stop = Math.min(endoff, off + ((end - pos) >> 2));
                }
                
                while(off<stop) {
                    v[off++] = Bits.getInt(buf, pos);
                    pos += 4;
                }
            }
        }
        
        // 从当前块数据输入流读取len个float值填充到数组off处
        void readFloats(float[] v, int off, int len) throws IOException {
            int span, endoff = off + len;
            while(off<endoff) {
                // 未处在块模式
                if(!blkmode) {
                    span = Math.min(endoff - off, MAX_BLOCK_SIZE >> 2);
                    in.readFully(buf, 0, span << 2);
                    pos = 0;
                    
                    // 处在块模式下，且缓冲区中有足够数据
                } else if(pos + 4>end) {
                    v[off++] = din.readFloat();
                    continue;
                } else {
                    span = Math.min(endoff - off, ((end - pos) >> 2));
                }
                
                bytesToFloats(buf, pos, v, off, span);
                off += span;
                pos += span << 2;
            }
        }
        
        // 从当前块数据输入流读取len个long值填充到数组off处
        void readLongs(long[] v, int off, int len) throws IOException {
            int stop, endoff = off + len;
            while(off<endoff) {
                // 未处在块模式
                if(!blkmode) {
                    int span = Math.min(endoff - off, MAX_BLOCK_SIZE >> 3);
                    in.readFully(buf, 0, span << 3);
                    stop = off + span;
                    pos = 0;
                    
                    // 处在块模式下，且缓冲区中有足够数据
                } else if(pos + 8>end) {
                    v[off++] = din.readLong();
                    continue;
                } else {
                    stop = Math.min(endoff, off + ((end - pos) >> 3));
                }
                
                while(off<stop) {
                    v[off++] = Bits.getLong(buf, pos);
                    pos += 8;
                }
            }
        }
        
        // 从当前块数据输入流读取len个double值填充到数组off处
        void readDoubles(double[] v, int off, int len) throws IOException {
            int span, endoff = off + len;
            while(off<endoff) {
                // 未处在块模式
                if(!blkmode) {
                    span = Math.min(endoff - off, MAX_BLOCK_SIZE >> 3);
                    in.readFully(buf, 0, span << 3);
                    pos = 0;
                    
                    // 处在块模式下，且缓冲区中有足够数据
                } else if(pos + 8>end) {
                    v[off++] = din.readDouble();
                    continue;
                } else {
                    span = Math.min(endoff - off, ((end - pos) >> 3));
                }
                
                bytesToDoubles(buf, pos, v, off, span);
                off += span;
                pos += span << 3;
            }
        }
        
        // 读取一个UTF8编码格式的小字符串
        public String readUTF() throws IOException {
            return readUTFBody(readUnsignedShort());
        }
        
        /**
         * Reads in string written in "long" UTF format.  "Long" UTF format is
         * identical to standard UTF, except that it uses an 8 byte header
         * (instead of the standard 2 bytes) to convey the UTF encoding length.
         */
        // 读取一个UTF8编码格式的大字符串
        String readLongUTF() throws IOException {
            return readUTFBody(readLong());
        }
        
        /**
         * Reads in the "body" (i.e., the UTF representation minus the 2-byte or 8-byte length header)
         * of a UTF encoding, which occupies the next utflen bytes.
         */
        // 读取utflen个utf8格式的字节，并将其解码为字符串
        private String readUTFBody(long utflen) throws IOException {
            StringBuilder sbuf;
            
            if(utflen>0 && utflen<Integer.MAX_VALUE) {
                // a reasonable initial capacity based on the UTF length
                int initialCapacity = Math.min((int) utflen, 0xFFFF);
                sbuf = new StringBuilder(initialCapacity);
            } else {
                sbuf = new StringBuilder();
            }
            
            if(!blkmode) {
                end = pos = 0;
            }
            
            while(utflen>0) {
                int avail = end - pos;
                if(avail >= 3 || (long) avail == utflen) {
                    utflen -= readUTFSpan(sbuf, utflen);
                } else {
                    if(blkmode) {
                        // near block boundary, read one byte at a time
                        utflen -= readUTFChar(sbuf, utflen);
                    } else {
                        // shift and refill buffer manually
                        if(avail>0) {
                            System.arraycopy(buf, pos, buf, 0, avail);
                        }
                        pos = 0;
                        end = (int) Math.min(MAX_BLOCK_SIZE, utflen);
                        in.readFully(buf, avail, end - avail);
                    }
                }
            }
            
            return sbuf.toString();
        }
        
        /**
         * Reads span of UTF-encoded characters out of internal buffer
         * (starting at offset pos and ending at or before offset end),
         * consuming no more than utflen bytes.
         * Appends read characters to sbuf.
         * Returns the number of bytes consumed.
         */
        private long readUTFSpan(StringBuilder sbuf, long utflen) throws IOException {
            int cpos = 0;
            int start = pos;
            int avail = Math.min(end - pos, CHAR_BUF_SIZE);
            // stop short of last char unless all of utf bytes in buffer
            int stop = pos + ((utflen>avail) ? avail - 2 : (int) utflen);
            boolean outOfBounds = false;
            
            try {
                while(pos<stop) {
                    int b1, b2, b3;
                    b1 = buf[pos++] & 0xFF;
                    switch(b1 >> 4) {
                        case 0:
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                        case 5:
                        case 6:
                        case 7:   // 1 byte format: 0xxxxxxx
                            cbuf[cpos++] = (char) b1;
                            break;
                        
                        case 12:
                        case 13:  // 2 byte format: 110xxxxx 10xxxxxx
                            b2 = buf[pos++];
                            if((b2 & 0xC0) != 0x80) {
                                throw new UTFDataFormatException();
                            }
                            cbuf[cpos++] = (char) (((b1 & 0x1F) << 6) | ((b2 & 0x3F) << 0));
                            break;
                        
                        case 14:  // 3 byte format: 1110xxxx 10xxxxxx 10xxxxxx
                            b3 = buf[pos + 1];
                            b2 = buf[pos + 0];
                            pos += 2;
                            if((b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80) {
                                throw new UTFDataFormatException();
                            }
                            cbuf[cpos++] = (char) (((b1 & 0x0F) << 12) | ((b2 & 0x3F) << 6) | ((b3 & 0x3F) << 0));
                            break;
                        
                        default:  // 10xx xxxx, 1111 xxxx
                            throw new UTFDataFormatException();
                    }
                }
            } catch(ArrayIndexOutOfBoundsException ex) {
                outOfBounds = true;
            } finally {
                if(outOfBounds || (pos - start)>utflen) {
                    /*
                     * Fix for 4450867: if a malformed utf char causes the
                     * conversion loop to scan past the expected end of the utf
                     * string, only consume the expected number of utf bytes.
                     */
                    pos = start + (int) utflen;
                    throw new UTFDataFormatException();
                }
            }
            
            sbuf.append(cbuf, 0, cpos);
            return pos - start;
        }
        
        /**
         * Reads in single UTF-encoded character one byte at a time, appends
         * the character to sbuf, and returns the number of bytes consumed.
         * This method is used when reading in UTF strings written in block
         * data mode to handle UTF-encoded characters which (potentially)
         * straddle block-data boundaries.
         */
        private int readUTFChar(StringBuilder sbuf, long utflen) throws IOException {
            int b1, b2, b3;
            b1 = readByte() & 0xFF;
            switch(b1 >> 4) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:     // 1 byte format: 0xxxxxxx
                    sbuf.append((char) b1);
                    return 1;
                
                case 12:
                case 13:    // 2 byte format: 110xxxxx 10xxxxxx
                    if(utflen<2) {
                        throw new UTFDataFormatException();
                    }
                    b2 = readByte();
                    if((b2 & 0xC0) != 0x80) {
                        throw new UTFDataFormatException();
                    }
                    sbuf.append((char) (((b1 & 0x1F) << 6) | ((b2 & 0x3F) << 0)));
                    return 2;
                
                case 14:    // 3 byte format: 1110xxxx 10xxxxxx 10xxxxxx
                    if(utflen<3) {
                        if(utflen == 2) {
                            readByte();         // consume remaining byte
                        }
                        throw new UTFDataFormatException();
                    }
                    b2 = readByte();
                    b3 = readByte();
                    if((b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80) {
                        throw new UTFDataFormatException();
                    }
                    sbuf.append((char) (((b1 & 0x0F) << 12) | ((b2 & 0x3F) << 6) | ((b3 & 0x3F) << 0)));
                    return 3;
                
                default:   // 10xx xxxx, 1111 xxxx
                    throw new UTFDataFormatException();
            }
        }
        
        // 读取一行数据：已过时
        @SuppressWarnings("deprecation")
        public String readLine() throws IOException {
            return din.readLine();      // deprecated, not worth optimizing
        }
        
        /**
         * Attempts to read in the next block data header (if any).
         * If canBlock is false and a full header cannot be read without possibly blocking, returns HEADER_BLOCKED,
         * else if the next element in the stream is a block data header, returns the block data length specified by the header,
         * else returns -1.
         */
        // 尝试读取下一个块数据头，如果canBlock为false，则当没有可用字节时，返回HEADER_BLOCKED
        private int readBlockHeader(boolean canBlock) throws IOException {
            if(defaultDataEnd) {
                /*
                 * Fix for 4360508: stream is currently at the end of a field
                 * value block written via default serialization; since there
                 * is no terminating TC_ENDBLOCKDATA tag, simulate
                 * end-of-custom-data behavior explicitly.
                 */
                return -1;
            }
            
            try {
                for(; ; ) {
                    // 如果当前已经没有可用字节，则返回HEADER_BLOCKED
                    int avail = canBlock ? Integer.MAX_VALUE : in.available();
                    if(avail == 0) {
                        return HEADER_BLOCKED;
                    }
                    
                    // 查看下一个字节
                    int tc = in.peek();
                    switch(tc) {
                        // 小数据块标记
                        case TC_BLOCKDATA:
                            if(avail<2) {
                                return HEADER_BLOCKED;
                            }
                            // 读取2个字节：小数据块标记，数据所占字节数量在byte范围内
                            in.readFully(hbuf, 0, 2);
                            return hbuf[1] & 0xFF;
                        
                        // 大数据块标记
                        case TC_BLOCKDATALONG:
                            if(avail<5) {
                                return HEADER_BLOCKED;
                            }
                            
                            // 读取5个字节：大数据块标记，数据所占字节数量在int范围内
                            in.readFully(hbuf, 0, 5);
                            int len = Bits.getInt(hbuf, 1);
                            if(len<0) {
                                throw new StreamCorruptedException("illegal block data header length: " + len);
                            }
                            return len;
                        
                        /*
                         * TC_RESETs may occur in between data blocks.
                         * Unfortunately, this case must be parsed at a lower
                         * level than other typecodes, since primitive data
                         * reads may span data blocks separated by a TC_RESET.
                         */
                        // 重置输入流/输出流，即丢弃之前缓存的读取信息
                        case TC_RESET:
                            in.read();
                            handleReset();
                            break;
                        
                        default:
                            if(tc >= 0 && (tc<TC_BASE || tc>TC_MAX)) {
                                throw new StreamCorruptedException(String.format("invalid type code: %02X", tc));
                            }
                            return -1;
                    }
                }
            } catch(EOFException ex) {
                throw new StreamCorruptedException("unexpected EOF while reading block data header");
            }
        }
        
        /**
         * Refills internal buffer buf with block data.
         * Any data in buf at the time of the call is considered consumed.
         * Sets the pos, end, and unread fields to reflect the new amount of available block data;
         * if the next element in the stream is not a data block, sets pos and unread to 0 and end to -1.
         */
        // 用块数据重新填充内部缓冲区buf(会先清空缓冲区现有数据)
        private void refill() throws IOException {
            try {
                do {
                    pos = 0;
                    
                    // 如果存在下一块未读数据
                    if(unread>0) {
                        // 取出一部分数据填充缓冲区
                        int n = in.read(buf, 0, Math.min(unread, MAX_BLOCK_SIZE));
                        if(n >= 0) {
                            end = n;
                            unread -= n;
                        } else {
                            throw new StreamCorruptedException("unexpected EOF in middle of data block");
                        }
                    } else {
                        // 读取下一个块数据头(可阻塞的)
                        int n = readBlockHeader(true);
                        
                        // 读到了下一个块数据的长度
                        if(n >= 0) {
                            end = 0;
                            unread = n;
                            
                            // 无效的块数据头
                        } else {
                            end = -1;
                            unread = 0;
                        }
                    }
                } while(pos == end);
            } catch(IOException ex) {
                pos = 0;
                end = -1;
                unread = 0;
                throw ex;
            }
        }
        
        /**
         * Sets block data mode to the given mode (true == on, false == off)
         * and returns the previous mode value.  If the new mode is the same as
         * the old mode, no action is taken.  Throws IllegalStateException if
         * block data mode is being switched from on to off while unconsumed
         * block data is still present in the stream.
         */
        boolean setBlockDataMode(boolean newmode) throws IOException {
            if(blkmode == newmode) {
                return blkmode;
            }
            if(newmode) {
                pos = 0;
                end = 0;
                unread = 0;
            } else if(pos<end) {
                throw new IllegalStateException("unread block data");
            }
            blkmode = newmode;
            return !blkmode;
        }
        
        /**
         * Returns true if the stream is currently in block data mode, false
         * otherwise.
         */
        // 返回当前的块模式
        boolean getBlockDataMode() {
            return blkmode;
        }
        
        /**
         * If in block data mode, skips to the end of the current group of data
         * blocks (but does not unset block data mode).  If not in block data
         * mode, throws an IllegalStateException.
         */
        void skipBlockData() throws IOException {
            if(!blkmode) {
                throw new IllegalStateException("not in block data mode");
            }
            while(end >= 0) {
                refill();
            }
        }
        
        /**
         * If in block data mode, returns the number of unconsumed bytes
         * remaining in the current data block.  If not in block data mode,
         * throws an IllegalStateException.
         */
        // 返回当前块剩余未读数据量
        int currentBlockRemaining() {
            if(blkmode) {
                return (end >= 0) ? (end - pos) + unread : 0;
            } else {
                throw new IllegalStateException();
            }
        }
        
        /**
         * Peeks at (but does not consume) and returns the next byte value in
         * the stream, or -1 if the end of the stream/block data (if in block
         * data mode) has been reached.
         */
        int peek() throws IOException {
            if(blkmode) {
                if(pos == end) {
                    refill();
                }
                return (end >= 0) ? (buf[pos] & 0xFF) : -1;
            } else {
                return in.peek();
            }
        }
        
        /**
         * Peeks at (but does not consume) and returns the next byte value in
         * the stream, or throws EOFException if end of stream/block data has
         * been reached.
         */
        byte peekByte() throws IOException {
            int val = peek();
            if(val<0) {
                throw new EOFException();
            }
            return (byte) val;
        }
        
        /**
         * Returns the number of bytes read from the input stream.
         *
         * @return the number of bytes read from the input stream
         */
        long getBytesRead() {
            return in.getBytesRead();
        }
        
        public long skip(long len) throws IOException {
            long remain = len;
            
            while(remain>0) {
                if(blkmode) {
                    if(pos == end) {
                        refill();
                    }
                    
                    if(end<0) {
                        break;
                    }
                    int nread = (int) Math.min(remain, end - pos);
                    remain -= nread;
                    pos += nread;
                } else {
                    int nread = (int) Math.min(remain, MAX_BLOCK_SIZE);
                    if((nread = in.read(buf, 0, nread))<0) {
                        break;
                    }
                    remain -= nread;
                }
            }
            
            return len - remain;
        }
        
        public int skipBytes(int n) throws IOException {
            return din.skipBytes(n);
        }
        
        public int available() throws IOException {
            if(!blkmode) {
                return in.available();
            }
            
            if((pos == end) && (unread == 0)) {
                int n;
                
                while((n = readBlockHeader(false)) == 0)
                    ;
                
                switch(n) {
                    case HEADER_BLOCKED:
                        break;
                    
                    case -1:
                        pos = 0;
                        end = -1;
                        break;
                    
                    default:
                        pos = 0;
                        end = 0;
                        unread = n;
                        break;
                }
            }
            
            // avoid unnecessary call to in.available() if possible
            int unreadAvail = (unread>0) ? Math.min(in.available(), unread) : 0;
            
            return (end >= 0) ? (end - pos) + unreadAvail : 0;
        }
        
        public void close() throws IOException {
            if(blkmode) {
                pos = 0;
                end = -1;
                unread = 0;
            }
            in.close();
        }
    }
    
    /**
     * Unsynchronized table which tracks wire handle to object mappings, as
     * well as ClassNotFoundExceptions associated with deserialized objects.
     * This class implements an exception-propagation algorithm for
     * determining which objects should have ClassNotFoundExceptions associated
     * with them, taking into account cycles and discontinuities (e.g., skipped
     * fields) in the object graph.
     *
     * <p>General use of the table is as follows: during deserialization, a
     * given object is first assigned a handle by calling the assign method.
     * This method leaves the assigned handle in an "open" state, wherein
     * dependencies on the exception status of other handles can be registered
     * by calling the markDependency method, or an exception can be directly
     * associated with the handle by calling markException.  When a handle is
     * tagged with an exception, the HandleTable assumes responsibility for
     * propagating the exception to any other objects which depend
     * (transitively) on the exception-tagged object.
     *
     * <p>Once all exception information/dependencies for the handle have been
     * registered, the handle should be "closed" by calling the finish method
     * on it.  The act of finishing a handle allows the exception propagation
     * algorithm to aggressively prune dependency links, lessening the
     * performance/memory impact of exception tracking.
     *
     * <p>Note that the exception propagation algorithm used depends on handles
     * being assigned/finished in LIFO order; however, for simplicity as well
     * as memory conservation, it does not enforce this constraint.
     */
    private static class HandleTable {
        
        /* status codes indicating whether object has associated exception */
        private static final byte STATUS_OK = 1;
        private static final byte STATUS_UNKNOWN = 2;
        private static final byte STATUS_EXCEPTION = 3;
        
        /** array mapping handle -> object status */
        byte[] status;
        
        /** array mapping handle -> object/exception (depending on status) */
        Object[] entries;
        
        /** array mapping handle -> list of dependent handles (if any) */
        HandleList[] deps;
        
        /** lowest unresolved dependency */
        int lowDep = -1;
        
        /** number of handles in table */
        int size = 0;
        
        /**
         * Creates handle table with the given initial capacity.
         */
        HandleTable(int initialCapacity) {
            status = new byte[initialCapacity];
            entries = new Object[initialCapacity];
            deps = new HandleList[initialCapacity];
        }
        
        /**
         * Assigns next available handle to given object, and returns assigned
         * handle.  Once object has been completely deserialized (and all
         * dependencies on other objects identified), the handle should be
         * "closed" by passing it to finish().
         */
        int assign(Object obj) {
            if(size >= entries.length) {
                grow();
            }
            
            status[size] = STATUS_UNKNOWN;
            entries[size] = obj;
            return size++;
        }
        
        /**
         * Registers a dependency (in exception status) of one handle on
         * another.  The dependent handle must be "open" (i.e., assigned, but
         * not finished yet).  No action is taken if either dependent or target
         * handle is NULL_HANDLE. Additionally, no action is taken if the
         * dependent and target are the same.
         */
        void markDependency(int dependent, int target) {
            if(dependent == target || dependent == NULL_HANDLE || target == NULL_HANDLE) {
                return;
            }
            
            switch(status[dependent]) {
                case STATUS_UNKNOWN:
                    switch(status[target]) {
                        case STATUS_OK:
                            // ignore dependencies on objs with no exception
                            break;
                        
                        case STATUS_EXCEPTION:
                            // eagerly propagate exception
                            markException(dependent, (ClassNotFoundException) entries[target]);
                            break;
                        
                        case STATUS_UNKNOWN:
                            // add to dependency list of target
                            if(deps[target] == null) {
                                deps[target] = new HandleList();
                            }
                            deps[target].add(dependent);
                            
                            // remember lowest unresolved target seen
                            if(lowDep<0 || lowDep>target) {
                                lowDep = target;
                            }
                            break;
                        
                        default:
                            throw new InternalError();
                    }
                    break;
                
                case STATUS_EXCEPTION:
                    break;
                
                default:
                    throw new InternalError();
            }
        }
        
        /**
         * Associates a ClassNotFoundException (if one not already associated)
         * with the currently active handle and propagates it to other
         * referencing objects as appropriate.  The specified handle must be
         * "open" (i.e., assigned, but not finished yet).
         */
        void markException(int handle, ClassNotFoundException ex) {
            switch(status[handle]) {
                case STATUS_UNKNOWN:
                    status[handle] = STATUS_EXCEPTION;
                    entries[handle] = ex;
                    
                    // propagate exception to dependents
                    HandleList dlist = deps[handle];
                    if(dlist != null) {
                        int ndeps = dlist.size();
                        for(int i = 0; i<ndeps; i++) {
                            markException(dlist.get(i), ex);
                        }
                        deps[handle] = null;
                    }
                    break;
                
                case STATUS_EXCEPTION:
                    break;
                
                default:
                    throw new InternalError();
            }
        }
        
        /**
         * Marks given handle as finished, meaning that no new dependencies
         * will be marked for handle.  Calls to the assign and finish methods
         * must occur in LIFO order.
         */
        void finish(int handle) {
            int end;
            if(lowDep<0) {
                // no pending unknowns, only resolve current handle
                end = handle + 1;
            } else if(lowDep >= handle) {
                // pending unknowns now clearable, resolve all upward handles
                end = size;
                lowDep = -1;
            } else {
                // unresolved backrefs present, can't resolve anything yet
                return;
            }
            
            // change STATUS_UNKNOWN -> STATUS_OK in selected span of handles
            for(int i = handle; i<end; i++) {
                switch(status[i]) {
                    case STATUS_UNKNOWN:
                        status[i] = STATUS_OK;
                        deps[i] = null;
                        break;
                    
                    case STATUS_OK:
                    case STATUS_EXCEPTION:
                        break;
                    
                    default:
                        throw new InternalError();
                }
            }
        }
        
        /**
         * Assigns a new object to the given handle.  The object previously
         * associated with the handle is forgotten.  This method has no effect
         * if the given handle already has an exception associated with it.
         * This method may be called at any time after the handle is assigned.
         */
        void setObject(int handle, Object obj) {
            switch(status[handle]) {
                case STATUS_UNKNOWN:
                case STATUS_OK:
                    entries[handle] = obj;
                    break;
                
                case STATUS_EXCEPTION:
                    break;
                
                default:
                    throw new InternalError();
            }
        }
        
        /**
         * Looks up and returns object associated with the given handle.
         * Returns null if the given handle is NULL_HANDLE, or if it has an
         * associated ClassNotFoundException.
         */
        Object lookupObject(int handle) {
            return (handle != NULL_HANDLE && status[handle] != STATUS_EXCEPTION) ? entries[handle] : null;
        }
        
        /**
         * Looks up and returns ClassNotFoundException associated with the given handle.
         * Returns null if the given handle is NULL_HANDLE, or if there is no ClassNotFoundException associated with the handle.
         */
        ClassNotFoundException lookupException(int handle) {
            return (handle != NULL_HANDLE && status[handle] == STATUS_EXCEPTION) ? (ClassNotFoundException) entries[handle] : null;
        }
        
        /**
         * Resets table to its initial state.
         */
        void clear() {
            Arrays.fill(status, 0, size, (byte) 0);
            Arrays.fill(entries, 0, size, null);
            Arrays.fill(deps, 0, size, null);
            lowDep = -1;
            size = 0;
        }
        
        /**
         * Returns number of handles registered in table.
         */
        int size() {
            return size;
        }
        
        /**
         * Expands capacity of internal arrays.
         */
        private void grow() {
            int newCapacity = (entries.length << 1) + 1;
            
            byte[] newStatus = new byte[newCapacity];
            Object[] newEntries = new Object[newCapacity];
            HandleList[] newDeps = new HandleList[newCapacity];
            
            System.arraycopy(status, 0, newStatus, 0, size);
            System.arraycopy(entries, 0, newEntries, 0, size);
            System.arraycopy(deps, 0, newDeps, 0, size);
            
            status = newStatus;
            entries = newEntries;
            deps = newDeps;
        }
        
        /**
         * Simple growable list of (integer) handles.
         */
        private static class HandleList {
            private int[] list = new int[4];
            private int size = 0;
            
            public HandleList() {
            }
            
            public void add(int handle) {
                if(size >= list.length) {
                    int[] newList = new int[list.length << 1];
                    System.arraycopy(list, 0, newList, 0, list.length);
                    list = newList;
                }
                list[size++] = handle;
            }
            
            public int get(int index) {
                if(index >= size) {
                    throw new ArrayIndexOutOfBoundsException();
                }
                return list[index];
            }
            
            public int size() {
                return size;
            }
        }
    }
    
    private static class Caches {
        /** cache of subclass security audit results */
        static final ConcurrentMap<WeakClassKey, Boolean> subclassAudits = new ConcurrentHashMap<>();
        
        /** queue for WeakReferences to audited subclasses */
        static final ReferenceQueue<Class<?>> subclassAuditsQueue = new ReferenceQueue<>();
    }
    
    private class FieldValues {
        final byte[] primValues;
        final Object[] objValues;
        
        FieldValues(byte[] primValues, Object[] objValues) {
            this.primValues = primValues;
            this.objValues = objValues;
        }
    }
    
    /**
     * Hold a snapshot of values to be passed to an ObjectInputFilter.
     */
    static class FilterValues implements ObjectInputFilter.FilterInfo {
        final Class<?> clazz;
        final long arrayLength;
        final long totalObjectRefs;
        final long depth;
        final long streamBytes;
        
        public FilterValues(Class<?> clazz, long arrayLength, long totalObjectRefs, long depth, long streamBytes) {
            this.clazz = clazz;
            this.arrayLength = arrayLength;
            this.totalObjectRefs = totalObjectRefs;
            this.depth = depth;
            this.streamBytes = streamBytes;
        }
        
        @Override
        public Class<?> serialClass() {
            return clazz;
        }
        
        @Override
        public long arrayLength() {
            return arrayLength;
        }
        
        @Override
        public long references() {
            return totalObjectRefs;
        }
        
        @Override
        public long depth() {
            return depth;
        }
        
        @Override
        public long streamBytes() {
            return streamBytes;
        }
    }
    
    /**
     * Input stream supporting single-byte peek operations.
     */
    private static class PeekInputStream extends InputStream {
        
        /** underlying stream */
        private final InputStream in;   // 真正的最终输入流，会从此读取数据
        
        /** total bytes read from the stream */
        private long totalBytesRead = 0;    // 统计已读取过(包括跳过)的字节数量
        
        /** peeked byte */
        private int peekb = -1; // 预存储下一个字节
        
        /**
         * Creates new PeekInputStream on top of given underlying stream.
         */
        PeekInputStream(InputStream in) {
            this.in = in;
        }
        
        // 尝试从最终输入流中读取一个字节，读取成功直接返回，读取失败返回-1
        public int read() throws IOException {
            // 如果已经包含预存储的字节，直接返回它
            if(peekb >= 0) {
                int v = peekb;
                peekb = -1;
                return v;
                
                // 全新的读取
            } else {
                int nbytes = in.read();
                totalBytesRead += nbytes >= 0 ? 1 : 0;
                return nbytes;
            }
        }
        
        // 尝试从最终输入流中读取len个字节，并将读到的内容插入到字节数组b的off索引处
        public int read(byte[] b, int off, int len) throws IOException {
            int nbytes;
            
            if(len == 0) {
                return 0;
                
                // 全新的读取
            } else if(peekb<0) {
                nbytes = in.read(b, off, len);
                totalBytesRead += nbytes >= 0 ? nbytes : 0;
                return nbytes;
                
                // 如果已经包含预存储的字节，则需要考虑它
            } else {
                b[off++] = (byte) peekb;
                len--;
                peekb = -1;
                nbytes = in.read(b, off, len);
                totalBytesRead += nbytes >= 0 ? nbytes : 0;
                return (nbytes >= 0) ? (nbytes + 1) : 1;
            }
        }
        
        // 尝试从最终输入流中读取len个字节(读不够不返回，除非读到了流的末尾或遇到了其他异常)
        void readFully(byte[] b, int off, int len) throws IOException {
            int n = 0;
            
            while(n<len) {
                int count = read(b, off + n, len - n);
                if(count<0) {
                    throw new EOFException();
                }
                
                n += count;
            }
        }
        
        // 读取中跳过n个字节，返回实际跳过的字节数
        public long skip(long n) throws IOException {
            if(n<=0) {
                return 0;
            }
            
            int skipped = 0;
            if(peekb >= 0) {
                peekb = -1;
                skipped++;
                n--;
            }
            
            n = skipped + in.skip(n);
            
            totalBytesRead += n;
            
            return n;
        }
        
        // 返回剩余可不被阻塞地读取（或跳过）的字节数（估计值）
        public int available() throws IOException {
            return in.available() + ((peekb >= 0) ? 1 : 0);
        }
        
        /**
         * Peeks at next byte value in stream.
         * Similar to read(), except that it does not consume the read value.
         */
        // 查看下一个字节
        int peek() throws IOException {
            if(peekb >= 0) {
                return peekb;
            }
            
            // 读取一个字节
            peekb = in.read();
            totalBytesRead += peekb >= 0 ? 1 : 0;
            return peekb;
        }
        
        // 关闭输入流
        public void close() throws IOException {
            in.close();
        }
        
        // 返回已读取过的字节数量
        public long getBytesRead() {
            return totalBytesRead;
        }
        
    }
    
    /**
     * Prioritized list of callbacks to be performed once object graph has been
     * completely deserialized.
     */
    // 验证回调列表
    private static class ValidationList {
        
        /** linked list of callbacks */
        private Callback list;  // 回调链，在验证对象是从前到后依次触发
        
        /**
         * Creates new (empty) ValidationList.
         */
        ValidationList() {
        }
        
        /**
         * Resets the callback list to its initial (empty) state.
         */
        public void clear() {
            list = null;
        }
        
        /**
         * Registers callback.
         * Throws InvalidObjectException if callback object is null.
         */
        // 注册验证回调，priority指示当前回调的优先级，priority越大，优先级越高(排在调用链前面)
        void register(ObjectInputValidation obj, int priority) throws InvalidObjectException {
            if(obj == null) {
                throw new InvalidObjectException("null callback");
            }
            
            Callback prev = null, cur = list;
            while(cur != null && priority<cur.priority) {
                prev = cur;
                cur = cur.next;
            }
            
            AccessControlContext acc = AccessController.getContext();
            
            if(prev != null) {
                prev.next = new Callback(obj, priority, cur, acc);
            } else {
                list = new Callback(obj, priority, list, acc);
            }
        }
        
        /**
         * Invokes all registered callbacks and clears the callback list.
         * Callbacks with higher priorities are called first; those with equal
         * priorities may be called in any order.  If any of the callbacks
         * throws an InvalidObjectException, the callback process is terminated
         * and the exception propagated upwards.
         */
        // 从前往后，按优先级从高到低执行回调逻辑
        void doCallbacks() throws InvalidObjectException {
            try {
                while(list != null) {
                    AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                        public Void run() throws InvalidObjectException {
                            list.obj.validateObject();
                            return null;
                        }
                    }, list.acc);
                    
                    list = list.next;
                }
            } catch(PrivilegedActionException ex) {
                list = null;
                throw (InvalidObjectException) ex.getException();
            }
        }
        
        private static class Callback {
            final AccessControlContext acc;
            final ObjectInputValidation obj;
            final int priority;
            Callback next;  // 指向下一个优先级较低的回调
            
            Callback(ObjectInputValidation obj, int priority, Callback next, AccessControlContext acc) {
                this.obj = obj;
                this.priority = priority;
                this.next = next;
                this.acc = acc;
            }
        }
    }
    
    /*
     * Separate class to defer initialization of logging until needed.
     */
    private static class Logging {
        /*
         * Logger for ObjectInputFilter results.
         * Setup the filter logger if it is set to DEBUG or TRACE.
         * (Assuming it will not change).
         */
        static final System.Logger filterLogger;
        
        static {
            Logger filterLog = System.getLogger("java.io.serialization");
            filterLogger = (filterLog.isLoggable(Logger.Level.DEBUG) || filterLog.isLoggable(Logger.Level.TRACE)) ? filterLog : null;
        }
    }
    
}
