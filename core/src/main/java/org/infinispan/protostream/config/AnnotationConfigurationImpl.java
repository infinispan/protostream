package org.infinispan.protostream.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.infinispan.protostream.AnnotationMetadataCreator;
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

   private final AnnotationElement.AnnotationTarget[] target;

   private final Map<String, AnnotationAttributeConfiguration> attributes;

   private final AnnotationMetadataCreator<?, ? extends AnnotatedDescriptor> annotationMetadataCreator;

   private final String repeatable;

   protected AnnotationConfigurationImpl container;

   private AnnotationConfigurationImpl(String name,
                                       AnnotationElement.AnnotationTarget[] target,
                                       Map<String, AnnotationAttributeConfiguration> attributes,
                                       AnnotationMetadataCreator<?, ? extends AnnotatedDescriptor> annotationMetadataCreator,
                                       String repeatable) {
      this.name = name;
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

      private final Configuration.AnnotationsConfig.Builder parentBuilder;

      /**
       * The annotation name.
       */
      private final String name;

      private final AnnotationElement.AnnotationTarget[] target;

      private final Map<String, AnnotationAttributeConfiguration.Builder> attributeBuilders = new HashMap<>();

      private AnnotationMetadataCreator<?, ? extends AnnotatedDescriptor> annotationMetadataCreator;

      /**
       * The name of the containing annotation, if this annotation is repeatable.
       */
      private String repeatable;

      BuilderImpl(Configuration.AnnotationsConfig.Builder parentBuilder, String name, AnnotationElement.AnnotationTarget[] target) {
         if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("annotation name must not be null or empty");
         }
         this.name = name;
         this.target = target;
         this.parentBuilder = parentBuilder;
      }

      @Override
      public AnnotationAttributeConfiguration.Builder attribute(String name) {
         if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("attribute name must not be null or empty");
         }
         if (attributeBuilders.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate attribute name definition: " + name);
         }
         AnnotationAttributeConfiguration.Builder builder = new AnnotationAttributeConfigurationImpl.BuilderImpl(this, name);
         attributeBuilders.put(name, builder);
         return builder;
      }

      @Override
      public Builder metadataCreator(AnnotationMetadataCreator<?, ? extends AnnotatedDescriptor> annotationMetadataCreator) {
         this.annotationMetadataCreator = annotationMetadataCreator;
         return this;
      }

      @Override
      public Builder repeatable(String containerAnnotationName) {
         if (containerAnnotationName == null) {
            throw new IllegalArgumentException("containerAnnotationName cannot be null");
         }
         if (name.equals(containerAnnotationName)) {
            throw new IllegalArgumentException("The name of the container annotation ('"
                  + containerAnnotationName + "') cannot be identical to the name of this annotation: '" + name + "'");
         }
         this.repeatable = containerAnnotationName;

         // auto-define the container annotation
         parentBuilder.annotation(containerAnnotationName, target)
               .attribute(AnnotationElement.Annotation.VALUE_DEFAULT_ATTRIBUTE)
               .type(AnnotationElement.AttributeType.ANNOTATION)
               .allowedValues(name)
               .multiple(true);

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
         return new AnnotationConfigurationImpl(name, target, attributes, annotationMetadataCreator, repeatable);
      }

      @Override
      public Configuration build() {
         return parentBuilder.build();
      }
   }
}
