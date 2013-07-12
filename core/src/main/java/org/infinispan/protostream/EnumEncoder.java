package org.infinispan.protostream;

/**
 * Encodes a Java enum into an int value that is suitable for serializing with protobuf and back again.
 *
 * @author anistor@redhat.com
 */
public interface EnumEncoder<E extends Enum<E>> {

   /**
    * Returns the full name of the enum as declared in the protobuf file.
    *
    * @return the full name of the enum type declared in the protobuf file.
    */
   String getFullName();

   /**
    * Decodes a an integer enum value read from the protobuf stream into a Java enum instance.
    *
    * @param enumValue the wire value to decode
    * @return a Java enum instance if the value is recognized or {@code null} otherwise.
    */
   E decode(int enumValue);

   int encode(E e);
}
