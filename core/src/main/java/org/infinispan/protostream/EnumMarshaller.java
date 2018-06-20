package org.infinispan.protostream;

/**
 * Contract to be implemented by marshallers of {@link Enum} types. Translates a Java enum into an {@code int} value
 * that is suitable for serializing with protobuf. The returned integer value must be one of the values defined in the
 * .proto schema file. The marshaller implementation must be stateless and thread-safe.
 *
 * @author anistor@redhat.com
 * @since 1.0
 */
public interface EnumMarshaller<E extends Enum<E>> extends BaseMarshaller<E> {

   /**
    * Decodes an integer enum value read from a protobuf encoded stream into a Java enum instance.
    * <p>
    * If the numeric value is not recognized the method must return {@code null} to signal this to the library and allow
    * the unrecognized data to be preserved. No exception should be thrown in this case.
    *
    * @param enumValue the protobuf enum value to decode
    * @return a Java {@link Enum} instance if the value is recognized or {@code null} otherwise.
    */
   E decode(int enumValue);

   /**
    * Encodes a Java {@link Enum} into its corresponding protobuf numeric value.
    *
    * @param e an {@link Enum} instance
    * @return the corresponding numeric value from the protobuf definition of the enum.
    * @throws IllegalArgumentException if the given Enum argument is of an unexpected type or its value has no
    *                                  correspondence to a protobuf enum value (this is a programming error, not a
    *                                  schema evolution issue).
    */
   int encode(E e) throws IllegalArgumentException;
}
