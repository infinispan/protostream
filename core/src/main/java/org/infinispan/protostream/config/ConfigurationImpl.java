package org.infinispan.protostream.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.infinispan.protostream.descriptors.AnnotationElement;

/**
 * @author anistor@redhat.com
 * @since 2.0
 */
final class ConfigurationImpl implements Configuration {

   private final boolean logOutOfSequenceReads;

   private final boolean logOutOfSequenceWrites;

   private final AnnotationsConfigImpl annotationsConfig;

   private ConfigurationImpl(boolean logOutOfSequenceReads,
                             boolean logOutOfSequenceWrites,
                             Map<String, AnnotationConfigurationImpl> annotations) {
      this.logOutOfSequenceReads = logOutOfSequenceReads;
      this.logOutOfSequenceWrites = logOutOfSequenceWrites;
      this.annotationsConfig = new AnnotationsConfigImpl(annotations);
   }

   @Override
   public boolean logOutOfSequenceReads() {
      return logOutOfSequenceReads;
   }

   @Override
   public boolean logOutOfSequenceWrites() {
      return logOutOfSequenceWrites;
   }

   @Override
   public AnnotationsConfig annotationsConfig() {
      return annotationsConfig;
   }

   @Override
   public String toString() {
      return "Configuration{" +
            "logOutOfSequenceReads=" + logOutOfSequenceReads +
            ", logOutOfSequenceWrites=" + logOutOfSequenceWrites +
            ", annotationsConfig=" + annotationsConfig +
            '}';
   }

   private static final class AnnotationsConfigImpl implements AnnotationsConfig {

      private final Map<String, AnnotationConfiguration> annotations;

      AnnotationsConfigImpl(Map<String, AnnotationConfigurationImpl> annotations) {
         this.annotations = Collections.unmodifiableMap(annotations);
      }

      @Override
      public Map<String, AnnotationConfiguration> annotations() {
         return annotations;
      }

      @Override
      public String toString() {
         return "AnnotationsConfig{annotations=" + annotations + '}';
      }
   }

   static final class BuilderImpl implements Builder {

      private boolean logOutOfSequenceReads = true;

      private boolean logOutOfSequenceWrites = true;

      private AnnotationsConfig.Builder annotationsConfigBuilder = null;

      private final class AnnotationsConfigBuilderImpl implements AnnotationsConfig.Builder {

         private final Map<String, AnnotationConfiguration.Builder> annotationBuilders = new HashMap<>();

         @Override
         public AnnotationConfiguration.Builder annotation(String annotationName, AnnotationElement.AnnotationTarget... target) {
            if (annotationBuilders.containsKey(annotationName)) {
               throw new IllegalArgumentException("Duplicate annotation name definition: " + annotationName);
            }
            if (target == null || target.length == 0) {
               throw new IllegalArgumentException("At least one target must be specified for annotation: " + annotationName);
            }
            AnnotationConfiguration.Builder builder = new AnnotationConfigurationImpl.BuilderImpl(this, annotationName, target);
            annotationBuilders.put(annotationName, builder);
            return builder;
         }

         @Override
         public Configuration build() {
            return BuilderImpl.this.build();
         }
      }

      BuilderImpl() {
      }

      @Override
      public Builder setLogOutOfSequenceReads(boolean logOutOfSequenceReads) {
         this.logOutOfSequenceReads = logOutOfSequenceReads;
         return this;
      }

      @Override
      public Builder setLogOutOfSequenceWrites(boolean logOutOfSequenceWrites) {
         this.logOutOfSequenceWrites = logOutOfSequenceWrites;
         return this;
      }

      @Override
      public AnnotationsConfig.Builder annotationsConfig() {
         if (annotationsConfigBuilder == null) {
            annotationsConfigBuilder = new AnnotationsConfigBuilderImpl();
         }
         return annotationsConfigBuilder;
      }

      @Override
      public Configuration build() {
         // define @TypeId annotation for message and enum types
         annotationsConfig()
               .annotation(TYPE_ID_ANNOTATION, AnnotationElement.AnnotationTarget.MESSAGE, AnnotationElement.AnnotationTarget.ENUM)
               .attribute(AnnotationElement.Annotation.VALUE_DEFAULT_ATTRIBUTE)
               .type(AnnotationElement.AttributeType.INT)
               .metadataCreator((annotatedDescriptor, annotation) -> annotation.getDefaultAttributeValue().getValue());

         AnnotationsConfigBuilderImpl annotationsConfig = (AnnotationsConfigBuilderImpl) annotationsConfig();
         Map<String, AnnotationConfigurationImpl> annotations = new HashMap<>(annotationsConfig.annotationBuilders.size());
         for (AnnotationConfiguration.Builder annotationBuilder : annotationsConfig.annotationBuilders.values()) {
            AnnotationConfigurationImpl annotationConfig = ((AnnotationConfigurationImpl.BuilderImpl) annotationBuilder).buildAnnotationConfiguration();
            annotations.put(annotationConfig.name(), annotationConfig);
         }

         // resolve containers for repeatable annotations
         for (AnnotationConfigurationImpl a : annotations.values()) {
            String repeatable = a.repeatable();
            if (repeatable != null) {
               a.container = annotations.get(repeatable);
            }
         }

         return new ConfigurationImpl(logOutOfSequenceReads, logOutOfSequenceWrites, annotations);
      }
   }
}
