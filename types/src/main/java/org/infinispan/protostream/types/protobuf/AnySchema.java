package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@AutoProtoSchemaBuilder(
      schemaFileName = "any.proto",
      schemaFilePath = "/google/protobuf",
      schemaPackageName = "google.protobuf",
      includeClasses = Any.class
)
public interface AnySchema extends GeneratedSchema {
}
