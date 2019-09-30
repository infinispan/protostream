package org.infinispan.protostream.config.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.infinispan.protostream.AnnotationMetadataCreator;
import org.infinispan.protostream.config.AnnotationAttributeConfiguration;
import org.infinispan.protostream.config.AnnotationConfiguration;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.AnnotatedDescriptor;
import org.infinispan.protostream.descriptors.AnnotationElement;
import org.infinispan.protostream.impl.parser.AnnotationParser;


/**
 * @author anistor@redhat.com
 * @since 2.0
 */
final class AnnotationAttributeConfigurationImpl implements AnnotationAttributeConfiguration {

   /**
    * The name of the annotation element.
    */
   private final String name;

   private final boolean isMultiple;

   private final Object defaultValue;

   private final AnnotationElement.AttributeType type;

   private final Set<String> allowedValues;

   private AnnotationAttributeConfigurationImpl(String name, boolean isMultiple, Object defaultValue, AnnotationElement.AttributeType type, Set<String> allowedValues) {
      this.name = name;
      this.isMultiple = isMultiple;
      this.defaultValue = defaultValue;
      this.type = type;
      this.allowedValues = allowedValues;
   }

   @Override
   public String name() {
      return name;
   }

   @Override
   public boolean multiple() {
      return isMultiple;
   }

   @Override
   public Object defaultValue() {
      return defaultValue;
   }

   @Override
   public AnnotationElement.AttributeType type() {
      return type;
   }

   @Override
   public Set<String> allowedValues() {
      return allowedValues;
   }

   static final class BuilderImpl implements Builder {

      private final AnnotationConfiguration.Builder parentBuilder;

      /**
       * The attribute name.
       */
      private final String name;

      /**
       * The type. Defaults to String if not explicitly set.
       */
      AnnotationElement.AttributeType type = AnnotationElement.AttributeType.STRING;

      boolean isMultiple;

      Object defaultValue;

      /**
       * The set of allowed values. This is only used with STRING, IDENTIFIER, or ANNOTATION type.
       */
      String[] allowedValues;

      BuilderImpl(AnnotationConfiguration.Builder parentBuilder, String name) {
         this.parentBuilder = parentBuilder;
         this.name = name;
      }

      @Override
      public Builder type(AnnotationElement.AttributeType type) {
         if (type == null) {
            throw new IllegalArgumentException("Annotation element type must not be null");
         }
         this.type = type;
         return this;
      }

      @Override
      public Builder multiple(boolean isMultiple) {
         this.isMultiple = isMultiple;
         return this;
      }

      @Override
      public Builder defaultValue(Object defaultValue) {
         if (defaultValue == null) {
            throw new IllegalArgumentException("Default value cannot be null");
         }
         this.defaultValue = defaultValue;
         return this;
      }

      @Override
      public Builder allowedValues(String... allowedValues) {
         this.allowedValues = allowedValues;
         return this;
      }

      @Override
      public Builder attribute(String name) {
         return parentBuilder.attribute(name);
      }

      @Override
      public Builder metadataCreator(AnnotationMetadataCreator<?, ? extends AnnotatedDescriptor> annotationMetadataCreator) {
         parentBuilder.metadataCreator(annotationMetadataCreator);
         return this;
      }

      @Override
      public Builder repeatable(String containingAnnotationName) {
         parentBuilder.repeatable(containingAnnotationName);
         return this;
      }

      AnnotationAttributeConfiguration buildAnnotationAttributeConfiguration() {
         Set<String> allowedValuesSet = null;

         if (allowedValues != null && allowedValues.length != 0) {
            switch (type) {
               case ANNOTATION:
               case IDENTIFIER:
               case STRING:
                  allowedValuesSet = new HashSet<>(Arrays.asList(allowedValues));
                  break;

               default:
                  throw new IllegalArgumentException("The type ('" + type + "') of annotation element '" + name + "' does not support a set of allowed values");
            }
         }

         if (type == AnnotationElement.AttributeType.ANNOTATION && (allowedValuesSet == null || allowedValuesSet.size() != 1)) {
            throw new IllegalArgumentException("The type ('" + type + "') of annotation element '" + name + "' requires exactly one allowed value");
         }

         if (defaultValue != null) {
            switch (type) {
               case ANNOTATION:
                  if (!(defaultValue instanceof String)) {
                     throw new IllegalArgumentException("Illegal default value type for annotation element '" + name + "'. Annotation expected.");
                  }
                  AnnotationParser parser = new AnnotationParser((String) defaultValue, false);
                  List<AnnotationElement.Annotation> _annotations = parser.parse();
                  if (_annotations.size() != 1) {
                     throw new IllegalArgumentException("Default value for annotation element '" + name + "' must contain a single annotation value");
                  }
                  AnnotationElement.Annotation annotationValue = _annotations.iterator().next();
                  if (!allowedValuesSet.contains(annotationValue.getName())) {
                     throw new IllegalArgumentException("Default value for annotation element '" + name + "' must be an annotation of type " + allowedValuesSet.iterator().next());
                  }
                  defaultValue = annotationValue;
                  break;

               case STRING:
               case IDENTIFIER:
                  if (!(defaultValue instanceof String)) {
                     throw new IllegalArgumentException("Illegal default value type for annotation element '" + name + "'. String expected.");
                  }
                  break;
               case CHARACTER:
                  if (!(defaultValue instanceof Character)) {
                     throw new IllegalArgumentException("Illegal default value type for annotation element '" + name + "'. Character expected.");
                  }
                  break;
               case BOOLEAN:
                  if (!(defaultValue instanceof Boolean)) {
                     throw new IllegalArgumentException("Illegal default value type for annotation element '" + name + "'. Boolean expected.");
                  }
                  break;
               case INT:
                  if (!(defaultValue instanceof Integer)) {
                     throw new IllegalArgumentException("Illegal default value type for annotation element '" + name + "'. Integer expected.");
                  }
                  break;
               case LONG:
                  if (!(defaultValue instanceof Long)) {
                     throw new IllegalArgumentException("Illegal default value type for annotation element '" + name + "'. Long expected.");
                  }
                  break;
               case FLOAT:
                  if (!(defaultValue instanceof Float)) {
                     throw new IllegalArgumentException("Illegal default value type for annotation element '" + name + "'. Float expected.");
                  }
                  break;
               case DOUBLE:
                  if (!(defaultValue instanceof Double)) {
                     throw new IllegalArgumentException("Illegal default value type for annotation element '" + name + "'. Double expected.");
                  }
                  break;
            }
         }

         return new AnnotationAttributeConfigurationImpl(name, isMultiple, defaultValue, type, allowedValuesSet);
      }

      @Override
      public AnnotationConfiguration.Builder parentBuilder() {
         return parentBuilder;
      }

      @Override
      public AnnotationConfiguration.Builder annotation(String annotationName, AnnotationElement.AnnotationTarget... target) {
         return parentBuilder.annotation(annotationName, target);
      }

      @Override
      public Configuration build() {
         return parentBuilder.build();
      }
   }
}
