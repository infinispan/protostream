package test_discovery_without_auto_import;

import org.infinispan.protostream.*;
import org.infinispan.protostream.annotations.*;

@AutoProtoSchemaBuilder(includeClasses = OuterMessage1.class, autoImportClasses = false)
public interface DiscoveryWithoutAutoImport extends SerializationContextInitializer {
}

abstract class AbstractMessage {

   @ProtoField(number = 1)
   String baseField1;
}

class InnerMessage1 extends AbstractMessage {
}

class OuterMessage1 {

   @ProtoField(number = 1)
   InnerMessage1 inner;
}
