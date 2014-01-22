package org.infinispan.protostream;

/**
 * Translates a Java enum into an {@code int} value that is suitable for serializing with protobuf. The integer value
 * must be one of the values defined in the .proto file.
 *
 * @author anistor@redhat.com
 * @since 1.0
 */
public interface EnumMarshaller<E extends Enum<E>> extends BaseMarshaller<E> {

   /**
    * Decodes an integer enum value read from a protobuf encoded stream into a Java enum instance. If the numeric value
    * is not recognized the method must return {@code null} in order to signal this to the library and allow the
    * unrecognized data to be preserved.
    *
    * @param enumValue the protobuf enum value to decode
    * @return a Java enum instance if the value is recognized or {@code null} otherwise.
    */
   E decode(int enumValue);

   /**
    * Encodes a Java enum into its corresponding protobuf numeric value.
    *
    * @param e an Enum instance
    * @return the corresponding numeric value from the protobuf definition of the enum.
    * @throws IllegalArgumentException if the given Enum is of an unexpected type or its value has no correspondence to
    *                                  a protobuf enum value (programming error).
    */
   int encode(E e) throws IllegalArgumentException;
}
