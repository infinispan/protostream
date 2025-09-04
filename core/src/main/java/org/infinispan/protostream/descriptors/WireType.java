package org.infinispan.protostream.descriptors;

import org.infinispan.protostream.MalformedProtobufException;

/**
 * Protobuf wire encoding type. Also provide helper functions for extracting the wire type and field number out of a tag.
 *
 * @author anistor@redhat.com
 * @since 4.4
 */
public enum WireType {

   VARINT(0),
   FIXED64(1),
   LENGTH_DELIMITED(2),
   START_GROUP(3),
   END_GROUP(4),
   FIXED32(5);

   private static final WireType[] VALUES = {VARINT, FIXED64, LENGTH_DELIMITED, START_GROUP, END_GROUP, FIXED32};

   // same as the enum values above, but as int constants for usage in case statements and other places requiring compile-time constants
   public static final int WIRETYPE_VARINT = 0;
   public static final int WIRETYPE_FIXED64 = 1;
   public static final int WIRETYPE_LENGTH_DELIMITED = 2;
   public static final int WIRETYPE_START_GROUP = 3;
   public static final int WIRETYPE_END_GROUP = 4;
   public static final int WIRETYPE_FIXED32 = 5;

   public static final int FIXED_32_SIZE = 4;
   public static final int FIXED_64_SIZE = 8;
   public static final int MAX_VARINT_SIZE = 10;
   // Similar to MAX_VARINT_SIZE except to be used for fields that are only ever used as Java primitive int values
   public static final int MAX_INT_VARINT_SIZE = 5;

   /**
    * The lower 3 bits of the 32 bit tag are used for encoding the wire type.
    */
   public static final int TAG_TYPE_NUM_BITS = 3;

   /**
    * Bit mask used for extracting the lower 3 bits of a tag, which represent the wire type of the field.
    */
   public static final int TAG_TYPE_BIT_MASK = (1 << TAG_TYPE_NUM_BITS) - 1;

   /**
    * Bit mask used to extract the higher 4 bits of a tag, which represents the field number. Note the most significant
    * bit is currently not used as field numbers are greater than 0. If required it may be possible to use unsigned int
    * to allow fields to support 5 bits.
    */
   public static final int FIELD_NUMBER_BIT_MASK = 0x7FFFFFFF ^ TAG_TYPE_BIT_MASK;

   /**
    * The protobuf protocol wire type.
    */
   public final int value;

   WireType(int value) {
      this.value = value;
   }

   /**
    * Gets the WireType enum value corresponding to a numeric wire type.
    */
   public static WireType fromValue(int wireType) throws MalformedProtobufException {
      if (wireType < 0 || wireType >= VALUES.length) {
         throw new MalformedProtobufException("Invalid wire type " + wireType);
      }
      return VALUES[wireType];
   }

   /**
    * Extracts the WireType from a numeric tag.
    */
   public static WireType fromTag(int tag) throws MalformedProtobufException {
      int wireType = getTagWireType(tag);
      if (wireType < 0 || wireType >= VALUES.length) {
         throw new MalformedProtobufException("Invalid wire type " + wireType + " in tag " + tag);
      }
      return VALUES[wireType];
   }

   /**
    * Makes a tag value given a field number and wire type.
    */
   public static int makeTag(int fieldNumber, WireType wireType) {
      return (fieldNumber << TAG_TYPE_NUM_BITS) | wireType.value;
   }

   public static int makeTag(int fieldNumber, int wireType) {
      return (fieldNumber << TAG_TYPE_NUM_BITS) | wireType;
   }

   /**
    * Given a tag value, determines the wire type (the lower 3 bits). Does not validate the resulting value.
    */
   public static int getTagWireType(int tag) {
      return tag & TAG_TYPE_BIT_MASK;
   }

   /**
    * Given a tag value, determines the field number (the upper 29 bits).
    */
   public static int getTagFieldNumber(int tag) {
      return tag >>> TAG_TYPE_NUM_BITS;
   }
}
