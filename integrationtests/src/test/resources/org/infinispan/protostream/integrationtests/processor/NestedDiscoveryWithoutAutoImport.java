package test_nested_discovery_without_auto_import;

import org.infinispan.protostream.*;
import org.infinispan.protostream.annotations.*;

@AutoProtoSchemaBuilder(includeClasses = {OuterMessage2.class, OuterMessage3.class}, autoImportClasses = false, schemaFilePath = "/")
public interface NestedDiscoveryWithoutAutoImport extends SerializationContextInitializer {
}

abstract class AbstractMessage {

   @ProtoField(number = 1)
   String baseField1;
}

class OuterMessage2 {

   static class InnerMessage2 extends AbstractMessage {
      @ProtoField(number = 2)
      String field2;
   }

   @ProtoField(number = 1)
   InnerMessage2 inner;
}

class OuterMessage3 {

   // this class is nested but not referenced from the outer class or the builder, so it does not get included
   static class InnerMessage3 extends AbstractMessage {
      @ProtoField(number = 2)
      String field2;
   }

   @ProtoField(number = 1)
   String field1;
}
