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

   private final Object defaultValue;

   private final AttributeType type;

   private final Set<String> allowedValues;

   private AnnotationAttributeConfig(String name, boolean isMultiple, Object defaultValue, AttributeType type, Set<String> allowedValues) {
      this.name = name;
      this.isMultiple = isMultiple;
      this.defaultValue = defaultValue;
      this.type = type;
      this.allowedValues = allowedValues;
   }

   public String name() {
      return name;
   }

   public boolean multiple() {
      return isMultiple;
   }

   public Object defaultValue() {
      return defaultValue;
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

      private Object defaultValue;

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

      public Builder<DescriptorType> defaultValue(Object defaultValue) {
         if (defaultValue == null) {
            throw new IllegalArgumentException("Default value cannot be null");
         }
         this.defaultValue = defaultValue;
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
         Set<String> allowedValuesSet = null;

         if (allowedValues != null && allowedValues.length != 0) {
            switch (type) {
               case ANNOTATION:
               case IDENTIFIER:
               case STRING:
                  allowedValuesSet = new HashSet<String>(allowedValues.length);
                  for (String v : allowedValues) {
                     allowedValuesSet.add(v);
                  }
                  break;

               default:
                  throw new IllegalArgumentException("The type of attribute '" + name + "' does not support a set of allowed values");
            }
         }

         if (defaultValue != null) {
            switch (type) {
               case ANNOTATION:
                  throw new IllegalArgumentException("The type of attribute '" + name + "' does not allow a default value.");
               case STRING:
               case IDENTIFIER:
                  if (!(defaultValue instanceof String)) {
                     throw new IllegalArgumentException("Illegal default value type for attribute '" + name + "'. String expected.");
                  }
                  break;
               case CHARACTER:
                  if (!(defaultValue instanceof Character)) {
                     throw new IllegalArgumentException("Illegal default value type for attribute '" + name + "'. Character expected.");
                  }
                  break;
               case BOOLEAN:
                  if (!(defaultValue instanceof Boolean)) {
                     throw new IllegalArgumentException("Illegal default value type for attribute '" + name + "'. Boolean expected.");
                  }
                  break;
               case INT:
                  if (!(defaultValue instanceof Integer)) {
                     throw new IllegalArgumentException("Illegal default value type for attribute '" + name + "'. Integer expected.");
                  }
                  break;
               case LONG:
                  if (!(defaultValue instanceof Long)) {
                     throw new IllegalArgumentException("Illegal default value type for attribute '" + name + "'. Long expected.");
                  }
                  break;
               case FLOAT:
                  if (!(defaultValue instanceof Float)) {
                     throw new IllegalArgumentException("Illegal default value type for attribute '" + name + "'. Float expected.");
                  }
                  break;
               case DOUBLE:
                  if (!(defaultValue instanceof Double)) {
                     throw new IllegalArgumentException("Illegal default value type for attribute '" + name + "'. Double expected.");
                  }
                  break;
            }
         }

         return new AnnotationAttributeConfig(name, isMultiple, defaultValue, type, allowedValuesSet);
      }

      public Configuration build() {
         return parentBuilder.build();
      }
   }
}
