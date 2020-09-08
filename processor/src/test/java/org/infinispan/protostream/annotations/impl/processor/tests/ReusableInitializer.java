package org.infinispan.protostream.annotations.impl.processor.tests;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoField;

@AutoProtoSchemaBuilder(includeClasses = {ReusableInitializer.A.class, ReusableInitializer.B.class})
public interface ReusableInitializer extends SerializationContextInitializer {

   class A {
      @ProtoField(number = 1, required = true)
      public boolean flag;
   }

   class B {
      @ProtoField(number = 1, required = true)
      public boolean flag;
   }
}
