package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.annotations.ProtoField;

public class BoolValue {

   private boolean value;

   public BoolValue() {
   }

   public BoolValue(boolean value) {
      this.value = value;
   }

   @ProtoField(value = 1, defaultValue = "false")
   public boolean getValue() {
      return value;
   }

   public void setValue(boolean value) {
      this.value = value;
   }
}
