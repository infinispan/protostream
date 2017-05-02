package org.infinispan.protostream.impl;

/**
 * Defines numeric constants for wire types. Also helps extracting the wire type and field number out of a tag.
 *
 * @author anistor@redhat.com
 * @deprecated replaced by {@link org.infinispan.protostream.descriptors.WireType}. To be removed in version 5.
 */
@Deprecated
public final class WireFormat {

   public static final int WIRETYPE_VARINT = 0;
   public static final int WIRETYPE_FIXED64 = 1;
   public static final int WIRETYPE_LENGTH_DELIMITED = 2;
   public static final int WIRETYPE_START_GROUP = 3;
   public static final int WIRETYPE_END_GROUP = 4;
   public static final int WIRETYPE_FIXED32 = 5;

   /**
    * Makes a tag value given a field number and wire type.
    */
   public static int makeTag(int fieldNumber, int wireType) {
      return (fieldNumber << 3) | wireType;
   }

   /**
    * Given a tag value, determines the wire type (the lower 3 bits).
    */
   public static int getTagWireType(int tag) {
      return tag & ((1 << 3) - 1);
   }

   /**
    * Given a tag value, determines the field number (the upper 29 bits).
    */
   public static int getTagFieldNumber(int tag) {
      return tag >>> 3;
   }

   /**
    * Prevent instantiation.
    */
   private WireFormat() {
   }
}
