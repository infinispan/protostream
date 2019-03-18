package org.infinispan.protostream.annotations.impl.processor;

import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;

import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.impl.AbstractMarshallerCodeGenerator;
import org.infinispan.protostream.annotations.impl.BaseProtoSchemaGenerator;
import org.infinispan.protostream.annotations.impl.types.UnifiedTypeFactory;
import org.infinispan.protostream.annotations.impl.types.XClass;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
final class CompileTimeProtoSchemaGenerator extends BaseProtoSchemaGenerator {

   private final ProcessingEnvironment processingEnv;

   private final Set<String> generatedClasses;

   CompileTimeProtoSchemaGenerator(UnifiedTypeFactory typeFactory, ProcessingEnvironment processingEnv,
                                   SerializationContext serializationContext, String fileName, String packageName,
                                   Set<XClass> classes, boolean autoImportClasses, Set<String> generatedClasses) {
      super(typeFactory, serializationContext, fileName, packageName, classes, autoImportClasses);
      this.processingEnv = processingEnv;
      this.generatedClasses = generatedClasses;
   }

   @Override
   protected AbstractMarshallerCodeGenerator makeCodeGenerator() {
      return new MarshallerSourceCodeGenerator(processingEnv, typeFactory, packageName, generatedClasses);
   }
}
