package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoFactory;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@AutoProtoSchemaBuilder(
      schemaFileName = "empty.proto",
      schemaFilePath = "/protostream/google/protobuf",
      schemaPackageName = "google.protobuf",
      includeClasses = EmptySchema.Empty.class
)
public interface EmptySchema extends GeneratedSchema {

   final class Empty {

      @ProtoFactory
      public Empty() {
      }
   }
}
