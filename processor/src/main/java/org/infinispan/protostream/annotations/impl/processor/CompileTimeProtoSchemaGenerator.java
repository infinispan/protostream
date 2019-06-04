package org.infinispan.protostream.annotations.impl.processor;

import java.util.Map;
import java.util.Set;

import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilderException;
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

   private final AnnotatedClassScanner classScanner;

   CompileTimeProtoSchemaGenerator(UnifiedTypeFactory typeFactory, GeneratedFilesWriter generatedFilesWriter,
                                   SerializationContext serializationContext, String generator,
                                   String fileName, String packageName, Map<XClass, String> dependencies,
                                   Set<XClass> classes, boolean autoImportClasses, AnnotatedClassScanner classScanner) {
      super(typeFactory, serializationContext, generator, fileName, packageName, classes, autoImportClasses);
      this.dependencies = dependencies;
      this.marshallerSourceCodeGenerator = new MarshallerSourceCodeGenerator(generatedFilesWriter, typeFactory, packageName);
      this.classScanner = classScanner;
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
      boolean isUnknownClass = !dependencies.containsKey(c) && super.isUnknownClass(c);

      if (isUnknownClass) {
         if (!classScanner.isClassIncluded(c.getCanonicalName())) {
            throw new ProtoSchemaBuilderException("Found a reference to class " + c.getName() +
                  " which was not explicitly included by the @AutoProtoSchemaBuilder and the annotation attributes do not allow it to be included.");
         }
         // it's unknown up to this point but annotation attributes allow us to auto-import this newly found type
         isUnknownClass = false;
      }

      return isUnknownClass;
   }

   public Set<String> getGeneratedMarshallerClasses() {
      return marshallerSourceCodeGenerator.getGeneratedClasses();
   }
}
