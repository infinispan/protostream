package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(
      schemaFileName = "wrappers.proto",
      schemaFilePath = "/google/protobuf",
      schemaPackageName = "google.protobuf",
      includeClasses = {
            DoubleValue.class,
            FloatValue.class,
            Int64Value.class,
            UInt64Value.class,
            Int32Value.class,
            UInt32Value.class,
            BoolValue.class,
            StringValue.class,
            BytesValue.class
      }
)
public interface WrappersSchema extends GeneratedSchema {
}
