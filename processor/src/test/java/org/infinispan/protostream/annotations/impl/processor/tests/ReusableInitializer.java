package org.infinispan.protostream.annotations.impl.processor.tests;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoSyntax;

@AutoProtoSchemaBuilder(includeClasses = {ReusableInitializer.A.class, ReusableInitializer.B.class}, syntax = ProtoSyntax.PROTO3)
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
