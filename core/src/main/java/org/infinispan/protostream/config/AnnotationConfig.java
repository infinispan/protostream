package org.infinispan.protostream.config;

import org.infinispan.protostream.AnnotationMetadataCreator;
import org.infinispan.protostream.descriptors.AnnotatedDescriptor;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author anistor@redhat.com
 * @since 2.0
 */
public final class AnnotationConfig<DescriptorType extends AnnotatedDescriptor> {

   private final String name;

   private final Map<String, AnnotationAttributeConfig> attributes;

   private final AnnotationMetadataCreator<?, DescriptorType> annotationMetadataCreator;

   private AnnotationConfig(String name, Map<String, AnnotationAttributeConfig> attributes, AnnotationMetadataCreator<?, DescriptorType> annotationMetadataCreator) {
      this.name = name;
      this.attributes = Collections.unmodifiableMap(attributes);
      this.annotationMetadataCreator = annotationMetadataCreator;
   }

   public String name() {
      return name;
   }

   public Map<String, AnnotationAttributeConfig> attributes() {
      return attributes;
   }

   public AnnotationMetadataCreator<?, DescriptorType> annotationMetadataCreator() {
      return annotationMetadataCreator;
   }

   public static final class Builder<DescriptorType extends AnnotatedDescriptor> {

      private final Configuration.Builder parentBuilder;

      private String name;

      private final Map<String, AnnotationAttributeConfig.Builder<DescriptorType>> attributeBuilders = new HashMap<String, AnnotationAttributeConfig.Builder<DescriptorType>>();

      private AnnotationMetadataCreator<?, DescriptorType> annotationMetadataCreator;

      Builder(Configuration.Builder parentBuilder, String name) {
         if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("annotation name must not be null or empty");
         }
         this.name = name;
         this.parentBuilder = parentBuilder;
      }

      public AnnotationAttributeConfig.Builder<DescriptorType> attribute(String name) {
         if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("attribute name must not be null or empty");
         }
         AnnotationAttributeConfig.Builder<DescriptorType> annotationAttributeConfigBuilder = new AnnotationAttributeConfig.Builder<DescriptorType>(this, name);
         attributeBuilders.put(name, annotationAttributeConfigBuilder);
         return annotationAttributeConfigBuilder;
      }

      public Builder<DescriptorType> annotationMetadataCreator(AnnotationMetadataCreator<?, DescriptorType> annotationMetadataCreator) {
         this.annotationMetadataCreator = annotationMetadataCreator;
         return this;
      }

      public AnnotationConfig.Builder<Descriptor> messageAnnotation(String annotationName) {
         return parentBuilder.messageAnnotation(annotationName);
      }

      public AnnotationConfig.Builder<EnumDescriptor> enumAnnotation(String annotationName) {
         return parentBuilder.enumAnnotation(annotationName);
      }

      public AnnotationConfig.Builder<FieldDescriptor> fieldAnnotation(String annotationName) {
         return parentBuilder.fieldAnnotation(annotationName);
      }

      AnnotationConfig<DescriptorType> buildAnnotationConfig() {
         Map<String, AnnotationAttributeConfig> attributes = new HashMap<String, AnnotationAttributeConfig>(attributeBuilders.size());
         for (AnnotationAttributeConfig.Builder<DescriptorType> attributeBuilder : attributeBuilders.values()) {
            AnnotationAttributeConfig annotationAttributeConfig = attributeBuilder.buildAnnotationAttributeConfig();
            attributes.put(annotationAttributeConfig.name(), annotationAttributeConfig);
         }
         return new AnnotationConfig<DescriptorType>(name, attributes, annotationMetadataCreator);
      }

      public Configuration build() {
         return parentBuilder.build();
      }
   }
}
