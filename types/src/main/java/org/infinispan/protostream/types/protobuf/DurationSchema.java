package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@AutoProtoSchemaBuilder(
      schemaFileName = "duration.proto",
      schemaFilePath = "/google/protobuf",
      schemaPackageName = "google.protobuf",
      includeClasses = Duration.class
)
public interface DurationSchema extends GeneratedSchema {
}
