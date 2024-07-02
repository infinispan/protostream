package org.infinispan.protostream.annotations.impl.processor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.lang.model.type.TypeMirror;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoSchemaBuilderException;
import org.infinispan.protostream.annotations.ProtoSyntax;
import org.infinispan.protostream.annotations.impl.AbstractMarshallerCodeGenerator;
import org.infinispan.protostream.annotations.impl.BaseProtoSchemaGenerator;
import org.infinispan.protostream.annotations.impl.ImportedProtoTypeMetadata;
import org.infinispan.protostream.annotations.impl.ProtoTypeMetadata;
import org.infinispan.protostream.annotations.impl.processor.dependency.CompileTimeDependency;
import org.infinispan.protostream.annotations.impl.processor.types.MirrorTypeFactory;
import org.infinispan.protostream.annotations.impl.types.XClass;
import org.infinispan.protostream.annotations.impl.types.XTypeFactory;
import org.infinispan.protostream.descriptors.GenericDescriptor;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
final class CompileTimeProtoSchemaGenerator extends BaseProtoSchemaGenerator {

   private final Map<XClass, CompileTimeDependency> dependencies;

   private final MarshallerSourceCodeGenerator marshallerSourceCodeGenerator;

   private final AnnotatedClassScanner classScanner;

   private final Map<XClass, XClass> adapterMap = new HashMap<>();

   CompileTimeProtoSchemaGenerator(XTypeFactory typeFactory, GeneratedFilesWriter generatedFilesWriter,
                                   SerializationContext serializationContext, String generator,
                                   String fileName, String packageName, Map<XClass, CompileTimeDependency> dependencies,
                                   Set<XClass> classes, boolean autoImportClasses, ProtoSyntax syntax, boolean allowNullFields,
                                   AnnotatedClassScanner classScanner) {
      super(typeFactory, serializationContext, generator, fileName, packageName, classes, autoImportClasses, syntax, allowNullFields);
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
      XClass targetClass = getTargetClass(javaType);
      if (!targetClass.equals(javaType)) {
         adapterMap.put(targetClass, javaType);
      }

      return new CompileTimeProtoMessageTypeMetadata(this, javaType, targetClass);
   }

   @Override
   protected ProtoTypeMetadata importProtoTypeMetadata(XClass javaType) {
      if (javaType == typeFactory.fromClass(WrappedMessage.class)) {
         GenericDescriptor descriptor = serializationContext.getDescriptorByName(WrappedMessage.PROTOBUF_TYPE_NAME);
         BaseMarshaller<WrappedMessage> marshaller = serializationContext.getMarshaller(WrappedMessage.PROTOBUF_TYPE_NAME);
         return new ImportedProtoTypeMetadata(descriptor, marshaller, javaType);
      }

      CompileTimeDependency dependency = dependencies.get(javaType);
      if (dependency != null) {
         String packageName = serializationContext.getFileDescriptors().get(dependency.getFileName()).getPackage();
         return new CompileTimeImportedProtoTypeMetadata(makeTypeMetadata(dependency.getUseToMakeTypeMetadata()),
               packageName, dependency.getFileName());
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
   protected XClass getAdapterFor(XClass annotatedClass) {
      ProtoAdapter protoAdapter;
      try {
         protoAdapter = annotatedClass.getAnnotation(ProtoAdapter.class);
         if (protoAdapter == null) {
            return null;
         }
      } catch (ClassCastException e) {
         // javac soiling pants
         throw new ProtoSchemaBuilderException("The class referenced by the ProtoAdapter annotation " +
               "does not exist, possibly due to compilation errors in your source code or due to " +
               "incremental compilation issues caused by your build system. Please try a clean rebuild.");

      }
      // TODO [anistor] also ensure that typeMirror is not part of current serCtxInit and is not scanned for @ProtoXyz annotations even if present
      TypeMirror typeMirror = DangerousActions.getTypeMirror(protoAdapter, ProtoAdapter::value);
      XClass target = ((MirrorTypeFactory) typeFactory).fromTypeMirror(typeMirror);
      if (target == annotatedClass) {
         throw new ProtoSchemaBuilderException(annotatedClass.getName() + " has an invalid @ProtoAdapter annotation pointing to self");
      }
      return target;
   }

   public XClass getOriginalClass(XClass targetClass) {
      XClass xClass = adapterMap.get(targetClass);
      if (xClass == null) {
         return targetClass;
      }
      return xClass;
   }

   public Set<String> getGeneratedMarshallerClasses() {
      return marshallerSourceCodeGenerator.getGeneratedClasses();
   }
}
