package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoSchema;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@ProtoSchema(
      schemaFileName = "empty.proto",
      schemaFilePath = "org/infinispan/protostream/types/protobuf",
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
