package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

public final class StringValue {

   private final String value;

   @ProtoFactory
   public StringValue(String value) {
      this.value = value;
   }

   @ProtoField(value = 1)
   public String getValue() {
      return value;
   }
}
