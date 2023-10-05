package org.infinispan.protostream.config.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.infinispan.protostream.AnnotationMetadataCreator;
import org.infinispan.protostream.config.AnnotationAttributeConfiguration;
import org.infinispan.protostream.config.AnnotationConfiguration;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.AnnotatedDescriptor;
import org.infinispan.protostream.descriptors.AnnotationElement;

/**
 * @author anistor@redhat.com
 * @since 2.0
 */
final class AnnotationConfigurationImpl implements AnnotationConfiguration {

   /**
    * The name of the annotation.
    */
   private final String name;

   private final String packageName;

   private final AnnotationElement.AnnotationTarget[] target;

   private final Map<String, AnnotationAttributeConfiguration> attributes;

   private final AnnotationMetadataCreator<?, ? extends AnnotatedDescriptor> annotationMetadataCreator;

   private final String repeatable;

   AnnotationConfigurationImpl container;

   private AnnotationConfigurationImpl(String name, String packageName,
                                       AnnotationElement.AnnotationTarget[] target,
                                       Map<String, AnnotationAttributeConfiguration> attributes,
                                       AnnotationMetadataCreator<?, ? extends AnnotatedDescriptor> annotationMetadataCreator,
                                       String repeatable) {
      this.name = name;
      this.packageName = packageName;
      this.target = target;
      this.attributes = Collections.unmodifiableMap(attributes);
      this.annotationMetadataCreator = annotationMetadataCreator;
      this.repeatable = repeatable;
   }

   @Override
   public String name() {
      return name;
   }

   @Override
   public String packageName() {
      return packageName;
   }

   @Override
   public AnnotationElement.AnnotationTarget[] target() {
      return target;
   }

   @Override
   public Map<String, AnnotationAttributeConfiguration> attributes() {
      return attributes;
   }

   @Override
   public AnnotationMetadataCreator<?, ? extends AnnotatedDescriptor> metadataCreator() {
      return annotationMetadataCreator;
   }

   @Override
   public String repeatable() {
      return repeatable;
   }

   static final class BuilderImpl implements Builder {

      private final ConfigurationImpl.BuilderImpl.AnnotationsConfigBuilderImpl parentBuilder;

      /**
       * The annotation name.
       */
      private final String name;

      private String packageName;

      private final AnnotationElement.AnnotationTarget[] target;

      private final Map<String, AnnotationAttributeConfigurationImpl.BuilderImpl> attributeBuilders = new HashMap<>();

      private AnnotationMetadataCreator<?, ? extends AnnotatedDescriptor> annotationMetadataCreator;

      /**
       * The name of the containing annotation, if this annotation is repeatable.
       */
      private String repeatable;

      BuilderImpl(ConfigurationImpl.BuilderImpl.AnnotationsConfigBuilderImpl parentBuilder, String name, AnnotationElement.AnnotationTarget[] target) {
         checkValidIdentifier(name, "annotation name");
         this.name = name;
         this.target = target;
         this.parentBuilder = parentBuilder;
      }

      private static void checkValidIdentifier(String str, String what) {
         if (str == null) {
            throw new IllegalArgumentException(what + " must not be null");
         }
         if (str.isEmpty() || !Character.isJavaIdentifierStart(str.charAt(0))) {
            throw new IllegalArgumentException("'" + str + "' is not a valid " + what);
         }
         for (int i = 1; i < str.length(); i++) {
            if (!Character.isJavaIdentifierPart(str.charAt(i))) {
               throw new IllegalArgumentException(str + " is not a valid " + what);
            }
         }
      }

      @Override
      public Builder packageName(String packageName) {
         this.packageName = packageName;
         return this;
      }

      @Override
      public AnnotationAttributeConfiguration.Builder attribute(String name) {
         checkValidIdentifier(name, "annotation element name");
         if (attributeBuilders.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate annotation element name definition: " + name);
         }
         AnnotationAttributeConfigurationImpl.BuilderImpl builder = new AnnotationAttributeConfigurationImpl.BuilderImpl(this, name);
         attributeBuilders.put(name, builder);
         return builder;
      }

      @Override
      public Builder metadataCreator(AnnotationMetadataCreator<?, ? extends AnnotatedDescriptor> annotationMetadataCreator) {
         this.annotationMetadataCreator = annotationMetadataCreator;
         return this;
      }

      @Override
      public Builder repeatable(String containingAnnotationName) {
         if (containingAnnotationName == null) {
            throw new IllegalArgumentException("containingAnnotationName cannot be null");
         }
         if (name.equals(containingAnnotationName)) {
            throw new IllegalArgumentException("The name of the containing annotation ('"
                  + containingAnnotationName + "') cannot be identical to the name of the repeatable annotation");
         }
         this.repeatable = containingAnnotationName;

         AnnotationConfigurationImpl.BuilderImpl containingAnnotationBuilder = parentBuilder.annotationBuilders.get(containingAnnotationName);
         if (containingAnnotationBuilder != null) {
            if (!new HashSet<>(Arrays.asList(containingAnnotationBuilder.target)).containsAll(Arrays.asList(target))) {
               throw new IllegalArgumentException("The containing annotation '" + containingAnnotationName
                     + "' has a target that does not include the target of the repeatable annotation '" + name + "'");
            }
            AnnotationAttributeConfigurationImpl.BuilderImpl valueAttrBuilder = containingAnnotationBuilder.attributeBuilders.get(AnnotationElement.Annotation.VALUE_DEFAULT_ATTRIBUTE);
            if (valueAttrBuilder == null
                  || !valueAttrBuilder.isMultiple
                  || valueAttrBuilder.type != AnnotationElement.AttributeType.ANNOTATION
                  || valueAttrBuilder.allowedValues == null
                  || !Arrays.asList(valueAttrBuilder.allowedValues).contains(name)) {
               throw new IllegalArgumentException("The containing annotation '" + containingAnnotationName
                     + "' of the repeatable annotation '" + name + "' does not have a '"
                     + AnnotationElement.Annotation.VALUE_DEFAULT_ATTRIBUTE + "' element of suitable type");
            }
            for (Map.Entry<String, AnnotationAttributeConfigurationImpl.BuilderImpl> entry : containingAnnotationBuilder.attributeBuilders.entrySet()) {
               String attrName = entry.getKey();
               if (!attrName.equals(AnnotationElement.Annotation.VALUE_DEFAULT_ATTRIBUTE)) {
                  AnnotationAttributeConfigurationImpl.BuilderImpl attrBuilder = entry.getValue();
                  if (attrBuilder.defaultValue == null) {
                     throw new IllegalArgumentException("The containing annotation '" + containingAnnotationName
                           + "' of the repeatable annotation '" + name
                           + "' does not have a default value for element '" + attrName + "'");
                  }
               }
            }

         } else {
            // The containing annotation does not exist so we auto-define it.
            parentBuilder.annotation(containingAnnotationName, target)
                  .attribute(AnnotationElement.Annotation.VALUE_DEFAULT_ATTRIBUTE)
                  .type(AnnotationElement.AttributeType.ANNOTATION)
                  .allowedValues(name)
                  .multiple(true);
         }
         return this;
      }

      @Override
      public Builder annotation(String annotationName, AnnotationElement.AnnotationTarget... target) {
         return parentBuilder.annotation(annotationName, target);
      }

      AnnotationConfigurationImpl buildAnnotationConfiguration() {
         Map<String, AnnotationAttributeConfiguration> attributes = new HashMap<>(attributeBuilders.size());
         for (AnnotationAttributeConfiguration.Builder attributeBuilder : attributeBuilders.values()) {
            AnnotationAttributeConfiguration annotationAttributeConfig = ((AnnotationAttributeConfigurationImpl.BuilderImpl) attributeBuilder).buildAnnotationAttributeConfiguration();
            attributes.put(annotationAttributeConfig.name(), annotationAttributeConfig);
         }
         return new AnnotationConfigurationImpl(name, packageName, target, attributes, annotationMetadataCreator, repeatable);
      }

      @Override
      public Configuration build() {
         return parentBuilder.build();
      }
   }
}
