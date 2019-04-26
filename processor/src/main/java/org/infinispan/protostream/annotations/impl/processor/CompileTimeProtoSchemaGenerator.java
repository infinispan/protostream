package org.infinispan.protostream.annotations.impl.processor;

import java.util.Map;
import java.util.Set;

import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.impl.AbstractMarshallerCodeGenerator;
import org.infinispan.protostream.annotations.impl.BaseProtoSchemaGenerator;
import org.infinispan.protostream.annotations.impl.ProtoTypeMetadata;
import org.infinispan.protostream.annotations.impl.types.UnifiedTypeFactory;
import org.infinispan.protostream.annotations.impl.types.XClass;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
final class CompileTimeProtoSchemaGenerator extends BaseProtoSchemaGenerator {

   private final SourceFileWriter sourceFileWriter;

   private final Map<XClass, String> dependencies;

   CompileTimeProtoSchemaGenerator(UnifiedTypeFactory typeFactory, SourceFileWriter sourceFileWriter,
                                   SerializationContext serializationContext, String fileName, String packageName,
                                   Map<XClass, String> dependencies,
                                   Set<XClass> classes, boolean autoImportClasses) {
      super(typeFactory, serializationContext, fileName, packageName, classes, autoImportClasses);
      this.sourceFileWriter = sourceFileWriter;
      this.dependencies = dependencies;
   }

   @Override
   protected AbstractMarshallerCodeGenerator makeCodeGenerator() {
      return new MarshallerSourceCodeGenerator(sourceFileWriter, typeFactory, packageName);
   }

   @Override
   protected ProtoTypeMetadata importProtoTypeMetadata(XClass javaType) {
      String fileName = dependencies.get(javaType);
      if (fileName != null) {
         String packageName = serializationContext.getFileDescriptors().get(fileName).getPackage();
         return new AnnotationBasedImportedProtoTypeMetadata(makeProtoTypeMetadata(javaType), packageName, fileName);
      }
      return null;
   }

   @Override
   protected boolean isKnownClass(XClass c) {
      return dependencies.containsKey(c) || super.isKnownClass(c);
   }
}
