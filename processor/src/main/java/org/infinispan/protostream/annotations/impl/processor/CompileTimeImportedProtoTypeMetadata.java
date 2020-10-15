package org.infinispan.protostream.annotations.impl.processor;

import org.infinispan.protostream.annotations.impl.ProtoEnumValueMetadata;
import org.infinispan.protostream.annotations.impl.ProtoTypeMetadata;

/**
 * A {@link ProtoTypeMetadata} for a message or enum type that is not present in the current set of classes and is
 * expected to be defined in another protobuf schema that was created based on annotations during the processing on a
 * different module, so it just gets to be imported from the file that defines it.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
final class CompileTimeImportedProtoTypeMetadata extends ProtoTypeMetadata {

   private final ProtoTypeMetadata protoTypeMetadata;

   /**
    * The file that defines this.
    */
   private final String fileName;

   /**
    * Fully qualified protobuf type name.
    */
   private final String fullName;

   CompileTimeImportedProtoTypeMetadata(ProtoTypeMetadata protoTypeMetadata, String packageName, String fileName) {
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

   @Override
   public String toString() {
      return "AnnotationBasedImportedProtoTypeMetadata{" +
            "name='" + name + '\'' +
            ", javaClass=" + javaClass +
            ", protoTypeMetadata=" + protoTypeMetadata.getName() +
            ", fileName='" + fileName + '\'' +
            ", fullName='" + fullName + '\'' +
            '}';
   }
}
