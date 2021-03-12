package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

public final class UInt32Value {

   private final int value;

   @ProtoFactory
   public UInt32Value(int value) {
      this.value = value;
   }

   @ProtoField(value = 1, defaultValue = "0")
   public int getValue() {
      return value;
   }
}
