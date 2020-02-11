package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.annotations.ProtoField;

public class Int64Value {

   private long value;

   public Int64Value() {
   }

   public Int64Value(long value) {
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
