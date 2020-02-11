package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.annotations.ProtoField;

public class FloatValue {

   private float value;

   public FloatValue() {
   }

   public FloatValue(float value) {
      this.value = value;
   }

   @ProtoField(value = 1, defaultValue = "0")
   public float getValue() {
      return value;
   }

   public void setValue(float value) {
      this.value = value;
   }
}
