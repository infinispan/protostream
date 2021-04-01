package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@AutoProtoSchemaBuilder(
      schemaFileName = "duration.proto",
      schemaFilePath = "/google/protobuf",
      schemaPackageName = "google.protobuf",
      includeClasses = DurationSchema.Duration.class
)
public interface DurationSchema extends GeneratedSchema {

   final class Duration {

      private final long seconds;

      private final long nanos;

      @ProtoFactory
      public Duration(long seconds, long nanos) {
         this.seconds = seconds;
         this.nanos = nanos;
      }

      @ProtoField(value = 1, defaultValue = "0")
      public long getSeconds() {
         return seconds;
      }

      @ProtoField(value = 2, defaultValue = "0")
      public long getNanos() {
         return nanos;
      }
   }
}
