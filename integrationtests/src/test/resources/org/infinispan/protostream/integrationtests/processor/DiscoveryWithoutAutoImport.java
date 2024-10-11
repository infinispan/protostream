package test_discovery_without_auto_import;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoField;

@AutoProtoSchemaBuilder(includeClasses = OuterMessage1.class)
public interface DiscoveryWithoutAutoImport extends SerializationContextInitializer {
}

abstract class AbstractMessage {

   @ProtoField(1)
   String baseField1;
}

class InnerMessage1 extends AbstractMessage {
}

class OuterMessage1 {

   @ProtoField(1)
   InnerMessage1 inner;
}
