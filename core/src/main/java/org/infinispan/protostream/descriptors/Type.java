package org.infinispan.protostream.descriptors;

import org.infinispan.protostream.impl.WireFormat;

/**
 * Type of a field in Protobuf, can be any value defined in <a href="https://developers.google.com/protocol-buffers/docs/proto3#scalar">https://developers.google.com/protocol-buffers/docs/proto3#scalar</a>
 * plus composite types group, message and enum.
 *
 * @author gustavonalle
 * @since 2.0
 */
public enum Type {

   DOUBLE(JavaType.DOUBLE, WireFormat.WIRETYPE_FIXED64),

   FLOAT(JavaType.FLOAT, WireFormat.WIRETYPE_FIXED32),

   INT64(JavaType.LONG, WireFormat.WIRETYPE_VARINT),

   UINT64(JavaType.LONG, WireFormat.WIRETYPE_VARINT),

   INT32(JavaType.INT, WireFormat.WIRETYPE_VARINT),

   FIXED64(JavaType.LONG, WireFormat.WIRETYPE_FIXED64),

   FIXED32(JavaType.INT, WireFormat.WIRETYPE_FIXED32),

   BOOL(JavaType.BOOLEAN, WireFormat.WIRETYPE_VARINT),

   STRING(JavaType.STRING, WireFormat.WIRETYPE_LENGTH_DELIMITED),

   BYTES(JavaType.BYTE_STRING, WireFormat.WIRETYPE_LENGTH_DELIMITED),

   UINT32(JavaType.INT, WireFormat.WIRETYPE_VARINT),

   SFIXED32(JavaType.INT, WireFormat.WIRETYPE_FIXED32),

   SFIXED64(JavaType.LONG, WireFormat.WIRETYPE_FIXED64),

   SINT32(JavaType.INT, WireFormat.WIRETYPE_VARINT),

   SINT64(JavaType.LONG, WireFormat.WIRETYPE_VARINT),

   GROUP(JavaType.MESSAGE, WireFormat.WIRETYPE_START_GROUP),

   MESSAGE(JavaType.MESSAGE, WireFormat.WIRETYPE_LENGTH_DELIMITED),

   ENUM(JavaType.ENUM, WireFormat.WIRETYPE_VARINT);

   private final JavaType javaType;

   private final int wireType;

   Type(JavaType javaType, int wireType) {
      this.javaType = javaType;
      this.wireType = wireType;
   }

   /**
    * If the type name is a primitive, it returns the corresponding enum constant, otherwise it returns {@code null}.
    */
   public static Type primitiveFromString(String typeName) {
      switch (typeName) {
         case "double":
            return DOUBLE;
         case "float":
            return FLOAT;
         case "int64":
            return INT64;
         case "uint64":
            return UINT64;
         case "sint64":
            return SINT64;
         case "fixed64":
            return FIXED64;
         case "sfixed64":
            return SFIXED64;
         case "int32":
            return INT32;
         case "uint32":
            return UINT32;
         case "sint32":
            return SINT32;
         case "fixed32":
            return FIXED32;
         case "sfixed32":
            return SFIXED32;
         case "bool":
            return BOOL;
         case "string":
            return STRING;
         case "bytes":
            return BYTES;
         default:
            // unknown type, not a primitive for sure
            return null;
      }
   }

   public JavaType getJavaType() {
      return javaType;
   }

   public int getWireType() {
      return wireType;
   }

   /**
    * Returns {@code true} only if the type is an unsigned numeric type, {@code false} otherwise.
    */
   public boolean isUnsigned() {
      return this == UINT32 ||
            this == UINT64 ||
            this == FIXED32 ||
            this == FIXED64;
   }

   @Override
   public String toString() {
      return name().toLowerCase();
   }
}
