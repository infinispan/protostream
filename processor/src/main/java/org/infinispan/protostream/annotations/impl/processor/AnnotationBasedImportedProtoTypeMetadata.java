package org.infinispan.protostream.annotations.impl.processor;

import org.infinispan.protostream.annotations.impl.ProtoEnumValueMetadata;
import org.infinispan.protostream.annotations.impl.ProtoTypeMetadata;

/**
 * A {@link ProtoTypeMetadata} for a message or enum type that is defined in an external protobuf schema and its
 * definition was not created based on annotations during the current execution of a {@link
 * org.infinispan.protostream.annotations.ProtoSchemaBuilder}.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
final class AnnotationBasedImportedProtoTypeMetadata extends ProtoTypeMetadata {

   private final ProtoTypeMetadata protoTypeMetadata;

   private final String fileName;

   private final String fullName;

   AnnotationBasedImportedProtoTypeMetadata(ProtoTypeMetadata protoTypeMetadata, String packageName, String fileName) {
      super(protoTypeMetadata.getName(), protoTypeMetadata.getJavaClass());
      this.protoTypeMetadata = protoTypeMetadata;
      this.fileName = fileName;
      this.fullName = packageName == null ? protoTypeMetadata.getFullName() : packageName + '.' + protoTypeMetadata.getFullName();
   }

   @Override
   public boolean isImported() {
      return true;
   }

   @Override
   public String getFullName() {
      return fullName;
   }

   @Override
   public boolean isEnum() {
      return protoTypeMetadata.isEnum();
   }

   @Override
   public ProtoEnumValueMetadata getEnumMemberByName(String name) {
      return protoTypeMetadata.getEnumMemberByName(name);
   }

   @Override
   public String getFileName() {
      return fileName;
   }
}
