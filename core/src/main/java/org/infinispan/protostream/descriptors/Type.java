package org.infinispan.protostream.descriptors;

/**
 * Type of a field in Protobuf, can be any value defined in <a href="https://developers.google.com/protocol-buffers/docs/proto3#scalar">https://developers.google.com/protocol-buffers/docs/proto3#scalar</a>
 * or a group, message and enum.
 *
 * @author gustavonalle
 * @since 2.0
 */
public enum Type {

   DOUBLE(JavaType.DOUBLE, WireType.FIXED64),

   FLOAT(JavaType.FLOAT, WireType.FIXED32),

   INT64(JavaType.LONG, WireType.VARINT),

   UINT64(JavaType.LONG, WireType.VARINT),

   INT32(JavaType.INT, WireType.VARINT),

   FIXED64(JavaType.LONG, WireType.FIXED64),

   FIXED32(JavaType.INT, WireType.FIXED32),

   BOOL(JavaType.BOOLEAN, WireType.VARINT),

   STRING(JavaType.STRING, WireType.LENGTH_DELIMITED),

   BYTES(JavaType.BYTE_STRING, WireType.LENGTH_DELIMITED),

   UINT32(JavaType.INT, WireType.VARINT),

   SFIXED32(JavaType.INT, WireType.FIXED32),

   SFIXED64(JavaType.LONG, WireType.FIXED64),

   SINT32(JavaType.INT, WireType.VARINT),

   SINT64(JavaType.LONG, WireType.VARINT),

   GROUP(JavaType.MESSAGE, WireType.START_GROUP),

   MESSAGE(JavaType.MESSAGE, WireType.LENGTH_DELIMITED),

   ENUM(JavaType.ENUM, WireType.VARINT),

   MAP(JavaType.MAP, WireType.LENGTH_DELIMITED);

   private final JavaType javaType;

   private final WireType wireType;

   Type(JavaType javaType, WireType wireType) {
      this.javaType = javaType;
      this.wireType = wireType;
   }

   /**
    * If the type name is a primitive, it returns the corresponding enum constant, otherwise it returns {@code null}.
    */
   public static Type primitiveFromString(String typeName) {
      return switch (typeName) {
         case "double" -> DOUBLE;
         case "float" -> FLOAT;
         case "int64" -> INT64;
         case "uint64" -> UINT64;
         case "sint64" -> SINT64;
         case "fixed64" -> FIXED64;
         case "sfixed64" -> SFIXED64;
         case "int32" -> INT32;
         case "uint32" -> UINT32;
         case "sint32" -> SINT32;
         case "fixed32" -> FIXED32;
         case "sfixed32" -> SFIXED32;
         case "bool" -> BOOL;
         case "string" -> STRING;
         case "bytes" -> BYTES;
         default ->
            // unknown type, not a primitive for sure
               null;
      };
   }

   public JavaType getJavaType() {
      return javaType;
   }

   public WireType getWireType() {
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

   public boolean isValidMapKey() {
      return switch (this) {
         case UINT32, UINT64, SINT32, SINT64, SFIXED32, SFIXED64, INT32, INT64, STRING, BOOL, FIXED32, FIXED64 -> true;
         default -> false;
      };
   }

   @Override
   public String toString() {
      return name().toLowerCase();
   }
}
