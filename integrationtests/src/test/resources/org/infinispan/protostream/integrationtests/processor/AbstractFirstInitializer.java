package test_basic_stuff;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(schemaFilePath = "first_initializer", schemaFileName = "FirstInitializer.proto",
      className = "FirstInitializer",
      basePackages = {"org.infinispan.protostream.integrationtests", "test_basic_stuff"},
      service = true)
public abstract class AbstractFirstInitializer implements SerializationContextInitializer {
}
