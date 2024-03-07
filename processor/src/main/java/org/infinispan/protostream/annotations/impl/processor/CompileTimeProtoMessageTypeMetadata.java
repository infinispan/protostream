package org.infinispan.protostream.annotations.impl.processor;

import java.util.Collection;
import java.util.Map;

import javax.lang.model.type.TypeMirror;

import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.impl.ProtoMessageTypeMetadata;
import org.infinispan.protostream.annotations.impl.processor.types.MirrorTypeFactory;
import org.infinispan.protostream.annotations.impl.types.XClass;

/**
 * A ProtoMessageTypeMetadata for compile time, using javax.lang.model instead of reflection.
 */
class CompileTimeProtoMessageTypeMetadata extends ProtoMessageTypeMetadata {

   CompileTimeProtoMessageTypeMetadata(CompileTimeProtoSchemaGenerator protoSchemaGenerator, XClass annotatedClass, XClass javaClass) {
      super(protoSchemaGenerator, annotatedClass, javaClass);
   }

   @Override
   protected XClass getCollectionImplementationFromAnnotation(ProtoField annotation) {
      if (annotation == null) {
         return typeFactory.fromClass(Collection.class);
      } else {
         TypeMirror typeMirror = DangerousActions.getTypeMirror(annotation, ProtoField::collectionImplementation);
         return ((MirrorTypeFactory) typeFactory).fromTypeMirror(typeMirror);
      }
   }

   @Override
   protected XClass getMapImplementationFromAnnotation(ProtoField annotation) {
      if (annotation == null) {
         return typeFactory.fromClass(Map.class);
      } else {
         TypeMirror typeMirror = DangerousActions.getTypeMirror(annotation, ProtoField::mapImplementation);
         return ((MirrorTypeFactory) typeFactory).fromTypeMirror(typeMirror);
      }
   }

   @Override
   protected XClass getJavaTypeFromAnnotation(ProtoField annotation) {
      if (annotation == null) {
         return typeFactory.fromClass(void.class);
      } else {
         TypeMirror typeMirror = DangerousActions.getTypeMirror(annotation, ProtoField::javaType);
         return ((MirrorTypeFactory) typeFactory).fromTypeMirror(typeMirror);
      }
   }
}
