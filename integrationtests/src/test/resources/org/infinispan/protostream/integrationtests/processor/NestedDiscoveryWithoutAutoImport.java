package test_nested_discovery_without_auto_import;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoField;

@AutoProtoSchemaBuilder(includeClasses = {OuterMessage2.class, OuterMessage3.class},
      schemaFilePath = "/")
interface NestedDiscoveryWithoutAutoImport extends SerializationContextInitializer {
}

@AutoProtoSchemaBuilder(basePackages = "test_nested_discovery_without_auto_import",
      schemaFilePath = "/")
interface NestedDiscoveryWithoutAutoImport2 extends SerializationContextInitializer {
}

@AutoProtoSchemaBuilder(schemaFilePath = "/")
interface NestedDiscoveryWithoutAutoImport3 extends SerializationContextInitializer {
}

abstract class AbstractMessage {

   @ProtoField(1)
   String baseField1;
}

class OuterMessage2 {

   static class InnerMessage2 extends AbstractMessage {
      @ProtoField(2)
      String field2;
   }

   @ProtoField(1)
   InnerMessage2 inner;
}

class OuterMessage3 {

   // this class is nested but not referenced from the outer class or the builder's includeClasses, so it does not get included
   static class InnerMessage3 extends AbstractMessage {
      @ProtoField(2)
      String field2;
   }

   @ProtoField(1)
   String field1;
}

class OuterMessage4 {

   // not referenced from the outer class or the builder, but it does get included because of basePackages
   static class InnerMessage4 extends AbstractMessage {
      @ProtoField(2)
      String field2;
   }

   @ProtoField(1)
   String field1;
}
