package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

public final class FloatValue {

   private final float value;

   @ProtoFactory
   public FloatValue(float value) {
      this.value = value;
   }

   @ProtoField(value = 1, defaultValue = "0")
   public float getValue() {
      return value;
   }
}
