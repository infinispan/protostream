package test_bad_schema_file_name;

@org.infinispan.protostream.annotations.AutoProtoSchemaBuilder(schemaFileName = "ThisIsNotAProtoFile.pdf")
public interface BadSchemaFileName extends org.infinispan.protostream.SerializationContextInitializer {
}
