package org.infinispan.protostream.descriptors;

/**
 * Type of a field in Protobuf, can be any value defined in <a href="https://developers.google.com/protocol-buffers/docs/proto3#scalar">https://developers.google.com/protocol-buffers/docs/proto3#scalar</a>
 * or a message or an enum.
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

   ENUM(JavaType.ENUM, WireType.VARINT);

   private final JavaType javaType;

   private final WireType wireType;

   Type(JavaType javaType, WireType wireType) {
      this.javaType = javaType;
      this.wireType = wireType;
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

   @Override
   public String toString() {
      return name().toLowerCase();
   }
}
