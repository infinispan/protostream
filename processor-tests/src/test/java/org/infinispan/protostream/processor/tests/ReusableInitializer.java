package org.infinispan.protostream.processor.tests;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoSchema;
import org.infinispan.protostream.annotations.ProtoSyntax;

@ProtoSchema(includeClasses = {ReusableInitializer.A.class, ReusableInitializer.B.class}, syntax = ProtoSyntax.PROTO3)
public interface ReusableInitializer extends SerializationContextInitializer {

   class A {
      @ProtoField(number = 1, defaultValue = "false")
      public boolean flag;
   }

   class B {
      @ProtoField(number = 1, defaultValue = "false")
      public boolean flag;
   }
}
