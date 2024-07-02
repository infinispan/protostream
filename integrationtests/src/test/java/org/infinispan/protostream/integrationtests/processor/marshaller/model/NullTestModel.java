package org.infinispan.protostream.integrationtests.processor.marshaller.model;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;


public class NullTestModel {

   @ProtoField(1)
   public String string;

   @ProtoField(2)
   public Integer boxedInt;

   @ProtoField(3)
   public int primitiveInt;

   @ProtoField(4)
   public byte[] bytes;

   @ProtoField(5)
   public SimpleEnum simpleEnum;

   public NullTestModel() {
      this(null, null, 0, null, null);
   }

   @ProtoFactory
   public NullTestModel(String string, Integer boxedInt, int primitiveInt, byte[] bytes, SimpleEnum simpleEnum) {
      this.string = string;
      this.boxedInt = boxedInt;
      this.primitiveInt = primitiveInt;
      this.bytes = bytes;
      this.simpleEnum = simpleEnum;
   }
}
