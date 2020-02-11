package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.annotations.ProtoField;

public class UInt64Value {

   private long value;

   public UInt64Value() {
   }

   public UInt64Value(long value) {
      this.value = value;
   }

   @ProtoField(value = 1, defaultValue = "0")
   public long getValue() {
      return value;
   }

   public void setValue(long value) {
      this.value = value;
   }
}
