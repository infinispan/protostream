package org.infinispan.protostream.annotations.impl.processor;

import javax.lang.model.type.TypeMirror;

import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.impl.ProtoMessageTypeMetadata;
import org.infinispan.protostream.annotations.impl.processor.types.MirrorClassFactory;
import org.infinispan.protostream.annotations.impl.types.XClass;

/**
 * A ProtoMessageTypeMetadata for compile time, using javax.lang.model instead of reflection.
 */
class CompileTimeProtoMessageTypeMetadata extends ProtoMessageTypeMetadata {

   CompileTimeProtoMessageTypeMetadata(CompileTimeProtoSchemaGenerator protoSchemaGenerator, XClass annotatedClass) {
      super(protoSchemaGenerator, annotatedClass);
   }

   @Override
   protected XClass getCollectionImplementationFromAnnotation(ProtoField annotation) {
      TypeMirror typeMirror = DangerousActions.getTypeMirror(annotation, ProtoField::collectionImplementation);
      return ((MirrorClassFactory) typeFactory).fromTypeMirror(typeMirror);
   }

   @Override
   protected XClass getJavaTypeFromAnnotation(ProtoField annotation) {
      TypeMirror typeMirror = DangerousActions.getTypeMirror(annotation, ProtoField::javaType);
      return ((MirrorClassFactory) typeFactory).fromTypeMirror(typeMirror);
   }
}
