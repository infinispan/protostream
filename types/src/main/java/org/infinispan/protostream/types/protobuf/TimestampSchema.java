package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@AutoProtoSchemaBuilder(
      schemaFileName = "timestamp.proto",
      schemaFilePath = "/google/protobuf",
      schemaPackageName = "google.protobuf",
      includeClasses = Timestamp.class
)
public interface TimestampSchema extends GeneratedSchema {
}
