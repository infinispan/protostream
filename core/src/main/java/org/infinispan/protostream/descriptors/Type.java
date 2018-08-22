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

   public JavaType getJavaType() {
      return javaType;
   }

   public int getWireType() {
      return wireType;
   }
}
