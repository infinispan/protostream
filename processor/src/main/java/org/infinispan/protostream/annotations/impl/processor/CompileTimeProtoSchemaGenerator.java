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

   private final Map<XClass, String> dependencies;

   private final MarshallerSourceCodeGenerator marshallerSourceCodeGenerator;

   CompileTimeProtoSchemaGenerator(UnifiedTypeFactory typeFactory, GeneratedFilesWriter generatedFilesWriter,
                                   SerializationContext serializationContext, String fileName, String packageName,
                                   Map<XClass, String> dependencies,
                                   Set<XClass> classes, boolean autoImportClasses) {
      super(typeFactory, serializationContext, fileName, packageName, classes, autoImportClasses);
      this.dependencies = dependencies;
      this.marshallerSourceCodeGenerator = new MarshallerSourceCodeGenerator(generatedFilesWriter, typeFactory, packageName);
   }

   @Override
   protected AbstractMarshallerCodeGenerator makeCodeGenerator() {
      return marshallerSourceCodeGenerator;
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
   protected boolean isUnknownClass(XClass c) {
      return !dependencies.containsKey(c) && super.isUnknownClass(c);
   }

   public Set<String> getGeneratedMarshallerClasses() {
      return marshallerSourceCodeGenerator.getGeneratedClasses();
   }
}
