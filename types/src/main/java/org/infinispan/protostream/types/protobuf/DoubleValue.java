package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

public final class DoubleValue {

   private final double value;

   @ProtoFactory
   public DoubleValue(double value) {
      this.value = value;
   }

   @ProtoField(value = 1, defaultValue = "0")
   public double getValue() {
      return value;
   }
}
