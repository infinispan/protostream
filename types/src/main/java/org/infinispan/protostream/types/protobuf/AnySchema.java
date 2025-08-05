package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoSchema;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@ProtoSchema(
      schemaFileName = "any.proto",
      schemaFilePath = "org/infinispan/protostream/types/protobuf",
      schemaPackageName = "google.protobuf",
      includeClasses = AnySchema.Any.class
)
public interface AnySchema extends GeneratedSchema {

   final class Any {

      private final String typeUrl;

      private final byte[] value;

      @ProtoFactory
      public Any(String typeUrl, byte[] value) {
         this.typeUrl = typeUrl;
         this.value = value;
      }

      @ProtoField(value = 1, name = "type_url")
      public String getTypeUrl() {
         return typeUrl;
      }

      @ProtoField(2)
      public byte[] getValue() {
         return value;
      }
   }
}
