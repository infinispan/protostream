package org.infinispan.protostream.descriptors;

import static com.google.protobuf.WireFormat.*;

/**
 * Type of a field in protobuf, can be any value defined in <a href="https://developers.google.com/protocol-buffers/docs/proto#scalar">https://developers.google.com/protocol-buffers/docs/proto#scalar</a>
 * plus composite types group, message and enum
 *
 * @author gustavonalle
 * @since 2.0
 */
public enum Type {
   DOUBLE(JavaType.DOUBLE, WIRETYPE_FIXED64),
   FLOAT(JavaType.FLOAT, WIRETYPE_FIXED32),
   INT64(JavaType.LONG, WIRETYPE_VARINT),
   UINT64(JavaType.LONG, WIRETYPE_VARINT),
   INT32(JavaType.INT, WIRETYPE_VARINT),
   FIXED64(JavaType.LONG, WIRETYPE_FIXED64),
   FIXED32(JavaType.INT, WIRETYPE_FIXED32),
   BOOL(JavaType.BOOLEAN, WIRETYPE_VARINT),
   STRING(JavaType.STRING, WIRETYPE_LENGTH_DELIMITED),
   GROUP(JavaType.MESSAGE, WIRETYPE_START_GROUP),
   MESSAGE(JavaType.MESSAGE, WIRETYPE_LENGTH_DELIMITED),
   BYTES(JavaType.BYTE_STRING, WIRETYPE_LENGTH_DELIMITED),
   UINT32(JavaType.INT, WIRETYPE_VARINT),
   ENUM(JavaType.ENUM, WIRETYPE_VARINT),
   SFIXED32(JavaType.INT, WIRETYPE_FIXED32),
   SFIXED64(JavaType.LONG, WIRETYPE_FIXED64),
   SINT32(JavaType.INT, WIRETYPE_VARINT),
   SINT64(JavaType.LONG, WIRETYPE_VARINT);
   private final JavaType javaType;
   private final int wireType;

   Type(JavaType javaType, int wireType) {
      this.javaType = javaType;
      this.wireType = wireType;
   }

   JavaType getJavaType() {
      return javaType;
   }

   public int getWireType() {
      return wireType;
   }
}
  