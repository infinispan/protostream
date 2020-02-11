package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.annotations.ProtoField;

public class StringValue {

   private String value;

   public StringValue() {
   }

   public StringValue(String value) {
      this.value = value;
   }

   @ProtoField(value = 1)
   public String getValue() {
      return value;
   }

   public void setValue(String value) {
      this.value = value;
   }
}
