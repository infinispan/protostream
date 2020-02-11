package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.annotations.ProtoField;

public class DoubleValue {

   private double value;

   public DoubleValue() {
   }

   public DoubleValue(double value) {
      this.value = value;
   }

   @ProtoField(value = 1, defaultValue = "0")
   public double getValue() {
      return value;
   }

   public void setValue(double value) {
      this.value = value;
   }
}
