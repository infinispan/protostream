package org.infinispan.protostream.annotations.impl.processor;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.lang.model.type.TypeMirror;

import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoSchema;
import org.infinispan.protostream.annotations.ProtoSyntax;

class ProtoSchemaAnnotation {

   private final boolean autoImportClasses;
   private final String[] basePackages;
   private final String className;
   private final boolean marshallersOnly;
   private final String schemaFileName;
   private final String schemaFilePath;
   private final String schemaPackageName;
   private final boolean service;
   private final ProtoSyntax syntax;
   private final String[] value;
   private final Annotation annotation;
   private final String annotationName;

   public ProtoSchemaAnnotation(AutoProtoSchemaBuilder annotation) {
      this.annotation = annotation;
      this.annotationName = AutoProtoSchemaBuilder.class.getSimpleName();
      autoImportClasses = annotation.autoImportClasses();
      basePackages = annotation.basePackages();
      className = annotation.className();
      marshallersOnly = annotation.marshallersOnly();
      schemaFileName = annotation.schemaFileName();
      schemaFilePath = annotation.schemaFilePath();
      schemaPackageName = annotation.schemaPackageName();
      service = annotation.service();
      syntax = annotation.syntax();
      value = annotation.value();
   }

   public ProtoSchemaAnnotation(ProtoSchema annotation) {
      this.annotation = annotation;
      this.annotationName = ProtoSchema.class.getSimpleName();
      autoImportClasses = false;
      basePackages = annotation.basePackages();
      className = annotation.className();
      marshallersOnly = annotation.marshallersOnly();
      schemaFileName = annotation.schemaFileName();
      schemaFilePath = annotation.schemaFilePath();
      schemaPackageName = annotation.schemaPackageName();
      service = true;
      syntax = annotation.syntax();
      value = annotation.value();
   }

   public boolean autoImportClasses() {
      return autoImportClasses;
   }

   public String[] basePackages() {
      return basePackages;
   }

   public String className() {
      return className;
   }

   public List<? extends TypeMirror> dependsOn() {
      if (annotation instanceof AutoProtoSchemaBuilder a) {
         return DangerousActions.getTypeMirrors(a, AutoProtoSchemaBuilder::dependsOn);
      } else {
         return DangerousActions.getTypeMirrors((ProtoSchema) annotation, ProtoSchema::dependsOn);
      }
   }

   public List<? extends TypeMirror> excludeClasses() {
      if (annotation instanceof AutoProtoSchemaBuilder a) {
         return DangerousActions.getTypeMirrors(a, AutoProtoSchemaBuilder::excludeClasses);
      } else {
         return DangerousActions.getTypeMirrors((ProtoSchema) annotation, ProtoSchema::excludeClasses);
      }
   }

   public List<? extends TypeMirror> includeClasses() {
      if (annotation instanceof AutoProtoSchemaBuilder a) {
         return DangerousActions.getTypeMirrors(a, AutoProtoSchemaBuilder::includeClasses);
      } else {
         return DangerousActions.getTypeMirrors((ProtoSchema) annotation, ProtoSchema::includeClasses);
      }
   }

   public boolean marshallersOnly() {
      return marshallersOnly;
   }

   public String schemaFileName() {
      return schemaFileName;
   }

   public String schemaFilePath() {
      return schemaFilePath;
   }

   public String schemaPackageName() {
      return schemaPackageName;
   }

   public boolean service() {
      return service;
   }

   public ProtoSyntax syntax() {
      return syntax;
   }

   public String[] value() {
      return value;
   }

   public String toString() {
      return annotation.toString();
   }

   public String getAnnotationName() {
      return annotationName;
   }
}
