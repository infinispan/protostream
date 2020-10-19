package org.infinispan.protostream.annotations.impl.processor;

import java.util.Map;
import java.util.Set;

import javax.lang.model.type.TypeMirror;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.annotations.ProtoBridgeFor;
import org.infinispan.protostream.annotations.ProtoSchemaBuilderException;
import org.infinispan.protostream.annotations.impl.AbstractMarshallerCodeGenerator;
import org.infinispan.protostream.annotations.impl.BaseProtoSchemaGenerator;
import org.infinispan.protostream.annotations.impl.ImportedProtoTypeMetadata;
import org.infinispan.protostream.annotations.impl.ProtoTypeMetadata;
import org.infinispan.protostream.annotations.impl.processor.types.MirrorClassFactory;
import org.infinispan.protostream.annotations.impl.types.XClass;
import org.infinispan.protostream.annotations.impl.types.XTypeFactory;
import org.infinispan.protostream.descriptors.GenericDescriptor;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
final class CompileTimeProtoSchemaGenerator extends BaseProtoSchemaGenerator {

   private final Map<XClass, String> dependencies;

   private final MarshallerSourceCodeGenerator marshallerSourceCodeGenerator;

   private final AnnotatedClassScanner classScanner;

   CompileTimeProtoSchemaGenerator(XTypeFactory typeFactory, GeneratedFilesWriter generatedFilesWriter,
                                   SerializationContext serializationContext, String generator,
                                   String fileName, String packageName, Map<XClass, String> dependencies,
                                   Set<XClass> classes, boolean autoImportClasses, AnnotatedClassScanner classScanner) {
      super(typeFactory, serializationContext, generator, fileName, packageName, classes, autoImportClasses);
      this.dependencies = dependencies;
      this.marshallerSourceCodeGenerator = new MarshallerSourceCodeGenerator(generatedFilesWriter, typeFactory, packageName);
      this.classScanner = classScanner;
   }

   @Override
   protected AbstractMarshallerCodeGenerator makeMarshallerCodeGenerator() {
      return marshallerSourceCodeGenerator;
   }

   @Override
   protected ProtoTypeMetadata makeMessageTypeMetadata(XClass javaType) {
      return new CompileTimeProtoMessageTypeMetadata(this, javaType, getMessageClass(javaType));
   }

   private XClass getMessageClass(XClass annotatedClass) {
      ProtoBridgeFor bridgeFor = annotatedClass.getAnnotation(ProtoBridgeFor.class);
      if (bridgeFor == null) {
         return annotatedClass;
      }
      TypeMirror typeMirror = DangerousActions.getTypeMirror(bridgeFor, ProtoBridgeFor::value);
      return ((MirrorClassFactory) typeFactory).fromTypeMirror(typeMirror);
   }

   @Override
   protected ProtoTypeMetadata importProtoTypeMetadata(XClass javaType) {
      if (javaType == typeFactory.fromClass(WrappedMessage.class)) {
         GenericDescriptor descriptor = serializationContext.getDescriptorByName(WrappedMessage.PROTOBUF_TYPE_NAME);
         BaseMarshaller<WrappedMessage> marshaller = serializationContext.getMarshaller(WrappedMessage.PROTOBUF_TYPE_NAME);
         return new ImportedProtoTypeMetadata(descriptor, marshaller, javaType);
      }

      String fileName = dependencies.get(javaType);
      if (fileName != null) {
         String packageName = serializationContext.getFileDescriptors().get(fileName).getPackage();
         return new CompileTimeImportedProtoTypeMetadata(makeTypeMetadata(javaType), packageName, fileName);
      }
      return null;
   }

   @Override
   protected boolean isUnknownClass(XClass c) {
      if (super.isUnknownClass(c) && !dependencies.containsKey(c) && !classScanner.isClassAcceptable(c)) {
         throw new ProtoSchemaBuilderException("Found a reference to class " + c.getCanonicalName() +
               " which was not explicitly included by @AutoProtoSchemaBuilder and the combination of" +
               " relevant attributes (basePackages, includeClasses, excludeClasses, autoImportClasses)" +
               " do not allow it to be included.");
      }

      // it may have been unknown up to this point but annotation attributes allow us to auto-import this newly found type, so go ahead
      return false;
   }

   @Override
   protected XClass getBridgeFor(XClass c) {
      ProtoBridgeFor annotation;
      try {
         annotation = c.getAnnotation(ProtoBridgeFor.class);
         if (annotation == null) {
            return null;
         }
      } catch (ClassCastException e) {
         // javac soiling pants
         throw new ProtoSchemaBuilderException("The class referenced by the ProtoBridgeFor annotation " +
               "do not exist, possibly due to compilation errors in your source code or due to " +
               "incremental compilation issues caused by your build system. Please try a clean rebuild.");

      }
      // TODO [anistor] also ensure that typeMirror is not part of current serCtxInit and is not scanned for @ProtoXyz annotations even if present
      TypeMirror typeMirror = DangerousActions.getTypeMirror(annotation, ProtoBridgeFor::value);
      return ((MirrorClassFactory) typeFactory).fromTypeMirror(typeMirror);
   }

   public Set<String> getGeneratedMarshallerClasses() {
      return marshallerSourceCodeGenerator.getGeneratedClasses();
   }
}
