package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.annotations.ProtoField;

public class BytesValue {

   private byte[] value;

   public BytesValue() {
   }

   public BytesValue(byte[] value) {
      this.value = value;
   }

   @ProtoField(value = 1)
   public byte[] getValue() {
      return value;
   }

   public void setValue(byte[] value) {
      this.value = value;
   }
}
