package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.annotations.ProtoField;

public class Any {

   private String typeUrl;

   private byte[] value;

   public Any() {
   }

   public Any(String typeUrl, byte[] value) {
      this.typeUrl = typeUrl;
      this.value = value;
   }

   @ProtoField(value = 1, name = "type_url")
   public String getTypeUrl() {
      return typeUrl;
   }

   public void setTypeUrl(String typeUrl) {
      this.typeUrl = typeUrl;
   }

   @ProtoField(2)
   public byte[] getValue() {
      return value;
   }

   public void setValue(byte[] value) {
      this.value = value;
   }
}
