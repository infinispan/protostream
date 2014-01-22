package org.infinispan.protostream.impl;

/**
 * Defines numeric constants for wire types. Also helps extracting the wire type and field number out of a tag.
 *
 * @author anistor@redhat.com
 */
public final class WireFormat {

   public static final int WIRETYPE_VARINT = com.google.protobuf.WireFormat.WIRETYPE_VARINT;
   public static final int WIRETYPE_FIXED64 = com.google.protobuf.WireFormat.WIRETYPE_FIXED64;
   public static final int WIRETYPE_LENGTH_DELIMITED = com.google.protobuf.WireFormat.WIRETYPE_LENGTH_DELIMITED;
   public static final int WIRETYPE_START_GROUP = com.google.protobuf.WireFormat.WIRETYPE_START_GROUP;
   public static final int WIRETYPE_END_GROUP = com.google.protobuf.WireFormat.WIRETYPE_END_GROUP;
   public static final int WIRETYPE_FIXED32 = com.google.protobuf.WireFormat.WIRETYPE_FIXED32;

   /**
    * The lower 3 bits of the 32 bit tag are used for encoding the wire type.
    */
   private static final int TAG_TYPE_BITS = 3;

   /**
    * Bit mask used for extracting the lower 3 bits which represent the wire type.
    */
   private static final int TAG_TYPE_MASK = (1 << TAG_TYPE_BITS) - 1;

   /**
    * Makes a tag value given a field number and wire type.
    */
   public static int makeTag(int fieldNumber, int wireType) {
      return (fieldNumber << TAG_TYPE_BITS) | wireType;
   }

   /**
    * Given a tag value, determines the wire type (the lower 3 bits).
    */
   public static int getTagWireType(int tag) {
      return tag & TAG_TYPE_MASK;
   }

   /**
    * Given a tag value, determines the field number (the upper 29 bits).
    */
   public static int getTagFieldNumber(final int tag) {
      return tag >>> TAG_TYPE_BITS;
   }

   /**
    * Prevent instantiation.
    */
   private WireFormat() {
   }
}
