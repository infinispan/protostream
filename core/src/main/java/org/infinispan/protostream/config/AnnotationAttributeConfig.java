package org.infinispan.protostream.config;

import org.infinispan.protostream.AnnotationMetadataCreator;
import org.infinispan.protostream.descriptors.AnnotatedDescriptor;

import java.util.HashSet;
import java.util.Set;

/**
 * @author anistor@redhat.com
 * @since 2.0
 */
public final class AnnotationAttributeConfig {

   public enum AttributeType {
      IDENTIFIER, STRING, CHARACTER, BOOLEAN, INT, LONG, FLOAT, DOUBLE, ANNOTATION
   }

   private final String name;

   private final boolean isMultiple;

   private final AttributeType type;

   private final Set<String> allowedValues;

   private AnnotationAttributeConfig(String name, boolean isMultiple, AttributeType type, Set<String> allowedValues) {
      this.name = name;
      this.isMultiple = isMultiple;
      this.type = type;
      this.allowedValues = allowedValues;
   }

   public String name() {
      return name;
   }
   // todo [anistor] implement required attributes and default value

   public boolean multiple() {
      return isMultiple;
   }

   public AttributeType type() {
      return type;
   }

   public Set<String> allowedValues() {
      return allowedValues;
   }

   public static final class Builder<DescriptorType extends AnnotatedDescriptor> {

      private final AnnotationConfig.Builder<DescriptorType> parentBuilder;

      private final String name;

      private boolean isMultiple;

      private AttributeType type = AttributeType.STRING;

      private String[] allowedValues;

      Builder(AnnotationConfig.Builder<DescriptorType> parentBuilder, String name) {
         this.parentBuilder = parentBuilder;
         this.name = name;
      }

      public Builder<DescriptorType> multiple(boolean isMultiple) {
         this.isMultiple = isMultiple;
         return this;
      }

      public Builder<DescriptorType> annotationType(String... allowedAnnotations) {
         type = AttributeType.ANNOTATION;
         allowedValues = allowedAnnotations;
         return this;
      }

      public Builder<DescriptorType> identifierType(String... allowedValues) {
         type = AttributeType.IDENTIFIER;
         this.allowedValues = allowedValues;
         return this;
      }

      public Builder<DescriptorType> stringType(String... allowedValues) {
         type = AttributeType.STRING;
         this.allowedValues = allowedValues;
         return this;
      }

      public Builder<DescriptorType> characterType() {
         type = AttributeType.CHARACTER;
         allowedValues = null;
         return this;
      }

      public Builder<DescriptorType> booleanType() {
         type = AttributeType.BOOLEAN;
         allowedValues = null;
         return this;
      }

      public Builder<DescriptorType> intType() {
         type = AttributeType.INT;
         allowedValues = null;
         return this;
      }

      public Builder<DescriptorType> longType() {
         type = AttributeType.LONG;
         allowedValues = null;
         return this;
      }

      public Builder<DescriptorType> floatType() {
         type = AttributeType.FLOAT;
         allowedValues = null;
         return this;
      }

      public Builder<DescriptorType> doubleType() {
         type = AttributeType.DOUBLE;
         allowedValues = null;
         return this;
      }

      public Builder<DescriptorType> attribute(String name) {
         return parentBuilder.attribute(name);
      }

      public AnnotationConfig.Builder<DescriptorType> annotationMetadataCreator(AnnotationMetadataCreator<?, DescriptorType> annotationMetadataCreator) {
         return parentBuilder.annotationMetadataCreator(annotationMetadataCreator);
      }

      AnnotationAttributeConfig buildAnnotationAttributeConfig() {
         Set<String> _allowedValues = null;
         if (allowedValues != null && allowedValues.length != 0) {
            _allowedValues = new HashSet<>(allowedValues.length);
            for (String v : allowedValues) {
               _allowedValues.add(v);
            }
         }
         return new AnnotationAttributeConfig(name, isMultiple, type, _allowedValues);
      }

      public Configuration build() {
         return parentBuilder.build();
      }
   }
}
