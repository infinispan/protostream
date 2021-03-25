package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
public final class Any {

   private final String typeUrl;

   private final byte[] value;

   @ProtoFactory
   public Any(String typeUrl, byte[] value) {
      this.typeUrl = typeUrl;
      this.value = value;
   }

   @ProtoField(value = 1, name = "type_url")
   public String getTypeUrl() {
      return typeUrl;
   }

   @ProtoField(2)
   public byte[] getValue() {
      return value;
   }
}
