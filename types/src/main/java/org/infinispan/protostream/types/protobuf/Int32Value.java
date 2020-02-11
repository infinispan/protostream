package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.annotations.ProtoField;

public class Int32Value {

   private int value;

   public Int32Value() {
   }

   public Int32Value(int value) {
      this.value = value;
   }

   @ProtoField(value = 1, defaultValue = "0")
   public int getValue() {
      return value;
   }

   public void setValue(int value) {
      this.value = value;
   }
}
