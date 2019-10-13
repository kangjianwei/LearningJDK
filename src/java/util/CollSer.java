package java.util;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import jdk.internal.misc.SharedSecrets;

/**
 * A unified serialization proxy class for the immutable collections.
 *
 * @serial
 * @since 9
 */
// ImmutableCollections中使用序列化代理，仅限内部使用
final class CollSer implements Serializable {
    private static final long serialVersionUID = 6309168927139932177L;
    
    static final int IMM_LIST = 1;
    static final int IMM_SET = 2;
    static final int IMM_MAP = 3;
    
    /**
     * Indicates the type of collection that is serialized.
     * The low order 8 bits have the value 1 for an immutable
     * {@code List}, 2 for an immutable {@code Set}, and 3 for
     * an immutable {@code Map}. Any other value causes an
     * {@link InvalidObjectException} to be thrown. The high
     * order 24 bits are zero when an instance is serialized,
     * and they are ignored when an instance is deserialized.
     * They can thus be used by future implementations without
     * causing compatibility issues.
     *
     * <p>The tag value also determines the interpretation of the
     * transient {@code Object[] array} field.
     * For {@code List} and {@code Set}, the array's length is the size
     * of the collection, and the array contains the elements of the collection.
     * Null elements are not allowed. For {@code Set}, duplicate elements
     * are not allowed.
     *
     * <p>For {@code Map}, the array's length is twice the number of mappings
     * present in the map. The array length is necessarily even.
     * The array contains a succession of key and value pairs:
     * {@code k1, v1, k2, v2, ..., kN, vN.} Nulls are not allowed,
     * and duplicate keys are not allowed.
     *
     * @serial
     * @since 9
     */
    private final int tag;
    
    /**
     * @serial
     * @since 9
     */
    private transient Object[] array;
    
    CollSer(int t, Object... a) {
        tag = t;
        array = a;
    }
    
    /**
     * Reads objects from the stream and stores them
     * in the transient {@code Object[] array} field.
     *
     * @param ois the ObjectInputStream from which data is read
     *
     * @throws IOException            if an I/O error occurs
     * @throws ClassNotFoundException if a serialized class cannot be loaded
     * @throws InvalidObjectException if the count is negative
     * @serialData A nonnegative int, indicating the count of objects,
     * followed by that many objects.
     * @since 9
     */
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        int len = ois.readInt();
        
        if(len<0) {
            throw new InvalidObjectException("negative length " + len);
        }
        
        SharedSecrets.getJavaObjectInputStreamAccess().checkArray(ois, Object[].class, len);
        
        Object[] a = new Object[len];
        for(int i = 0; i<len; i++) {
            a[i] = ois.readObject();
        }
        
        array = a;
    }
    
    /**
     * Writes objects to the stream from
     * the transient {@code Object[] array} field.
     *
     * @param oos the ObjectOutputStream to which data is written
     *
     * @throws IOException if an I/O error occurs
     * @serialData A nonnegative int, indicating the count of objects,
     * followed by that many objects.
     * @since 9
     */
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        oos.writeInt(array.length);
        for(Object o : array) {
            oos.writeObject(o);
        }
    }
    
    /**
     * Creates and returns an immutable collection from this proxy class.
     * The instance returned is created as if by calling one of the
     * static factory methods for
     * <a href="List.html#immutable">List</a>,
     * <a href="Map.html#immutable">Map</a>, or
     * <a href="Set.html#immutable">Set</a>.
     * This proxy class is the serial form for all immutable collection instances,
     * regardless of implementation type. This is necessary to ensure that the
     * existence of any particular implementation type is kept out of the
     * serialized form.
     *
     * @return a collection created from this proxy object
     *
     * @throws InvalidObjectException if the tag value is illegal or if an exception
     *                                is thrown during creation of the collection
     * @throws ObjectStreamException  if another serialization error has occurred
     * @since 9
     */
    private Object readResolve() throws ObjectStreamException {
        try {
            if(array == null) {
                throw new InvalidObjectException("null array");
            }
            
            /*
             * use low order 8 bits to indicate "kind"
             * ignore high order 24 bits
             */
            switch(tag & 0xff) {
                case IMM_LIST:
                    return List.of(array);
                case IMM_SET:
                    return Set.of(array);
                case IMM_MAP:
                    if(array.length == 0) {
                        return ImmutableCollections.emptyMap();
                    } else if(array.length == 2) {
                        return new ImmutableCollections.Map1<>(array[0], array[1]);
                    } else {
                        return new ImmutableCollections.MapN<>(array);
                    }
                default:
                    throw new InvalidObjectException(String.format("invalid flags 0x%x", tag));
            }
        } catch(NullPointerException | IllegalArgumentException ex) {
            InvalidObjectException ioe = new InvalidObjectException("invalid object");
            ioe.initCause(ex);
            throw ioe;
        }
    }
    
}

