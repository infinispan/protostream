package test_basic_stuff_dependent;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoField;

import test_basic_stuff.TestMessage;

@AutoProtoSchemaBuilder(schemaFilePath = "/", dependsOn = test_basic_stuff.AbstractFirstInitializer.class,
         includeClasses = DependentInitializer.A.class, service = true)
interface DependentInitializer extends SerializationContextInitializer {

   class A {
      @ProtoField(number = 1)
      public TestMessage testMessage;
   }

   // This method will be overridden by generated code and a warning will be issued so the user is aware of this
   default String getProtoFileName() {
      return null;
   }
}
