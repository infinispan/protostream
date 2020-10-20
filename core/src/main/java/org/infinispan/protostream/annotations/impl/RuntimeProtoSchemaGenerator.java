package org.infinispan.protostream.annotations.impl;

import java.util.Set;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilderException;
import org.infinispan.protostream.annotations.impl.types.XClass;
import org.infinispan.protostream.annotations.impl.types.XTypeFactory;
import org.infinispan.protostream.descriptors.GenericDescriptor;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

/**
 * This class is not to be directly invoked by users. See {@link org.infinispan.protostream.annotations.ProtoSchemaBuilder}
 * instead.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
public final class RuntimeProtoSchemaGenerator extends BaseProtoSchemaGenerator {

   private final ClassPool classPool;

   public RuntimeProtoSchemaGenerator(XTypeFactory typeFactory, SerializationContext serializationContext,
                                      String generator, String fileName, String packageName,
                                      Set<XClass> classes, boolean autoImportClasses, ClassLoader classLoader) {
      super(typeFactory, serializationContext, generator, fileName, packageName, classes, autoImportClasses);
      if (classes.isEmpty()) {
         throw new ProtoSchemaBuilderException("At least one class must be specified");
      }
      classPool = getClassPool(classes, classLoader);
   }

   /**
    * Return an imported ProtoTypeMetadata implementation or null if it cannot be imported.
    */
   @Override
   protected ProtoTypeMetadata importProtoTypeMetadata(XClass javaType) {
      // check if this is already marshallable in current SerializationContext
      BaseMarshaller<?> marshaller;
      try {
         marshaller = serializationContext.getMarshaller(javaType.asClass());
      } catch (Exception e) {
         // ignore
         return null;
      }

      // this is an already known type, defined in another schema file that we'll just need to import; nothing gets generated for it
      GenericDescriptor descriptor = serializationContext.getDescriptorByName(marshaller.getTypeName());
      return new ImportedProtoTypeMetadata(descriptor, marshaller, javaType);
   }

   @Override
   protected AbstractMarshallerCodeGenerator makeMarshallerCodeGenerator() {
      try {
         return new MarshallerByteCodeGenerator(typeFactory, packageName, classPool);
      } catch (NotFoundException e) {
         throw new ProtoSchemaBuilderException(e);
      }
   }

   private static ClassPool getClassPool(Set<XClass> classes, ClassLoader classLoader) {
      ClassPool cp = classLoader == null ? new ClassPool(ClassPool.getDefault()) : new ClassPool(ClassPool.getDefault()) {
         @Override
         public ClassLoader getClassLoader() {
            return classLoader;
         }
      };
      for (XClass c : classes) {
         cp.appendClassPath(new ClassClassPath(c.asClass()));
      }
      ClassLoader myCL = RuntimeProtoSchemaGenerator.class.getClassLoader();
      cp.appendClassPath(new LoaderClassPath(myCL));
      if (classLoader != myCL) {
         cp.appendClassPath(new LoaderClassPath(classLoader));
      }
      return cp;
   }
}
