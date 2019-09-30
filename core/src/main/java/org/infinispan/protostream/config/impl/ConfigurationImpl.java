package org.infinispan.protostream.config.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.infinispan.protostream.WrappedMessageTypeMapper;
import org.infinispan.protostream.config.AnnotationConfiguration;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.AnnotationElement;

/**
 * @author anistor@redhat.com
 * @since 2.0
 */
public final class ConfigurationImpl implements Configuration {

   private final boolean logOutOfSequenceReads;

   private final boolean logOutOfSequenceWrites;

   private final WrappingConfigImpl wrappingConfig;

   private final AnnotationsConfigImpl annotationsConfig;

   private ConfigurationImpl(boolean logOutOfSequenceReads, boolean logOutOfSequenceWrites,
                             WrappedMessageTypeMapper wrappedMessageTypeMapper,
                             Map<String, AnnotationConfigurationImpl> annotations, boolean logUndefinedAnnotations) {
      this.logOutOfSequenceReads = logOutOfSequenceReads;
      this.logOutOfSequenceWrites = logOutOfSequenceWrites;
      this.wrappingConfig = new WrappingConfigImpl(wrappedMessageTypeMapper);
      this.annotationsConfig = new AnnotationsConfigImpl(annotations, logUndefinedAnnotations);
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
   public WrappingConfig wrappingConfig() {
      return wrappingConfig;
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
            ", wrappingConfig=" + wrappingConfig +
            ", annotationsConfig=" + annotationsConfig +
            '}';
   }

   private static final class WrappingConfigImpl implements WrappingConfig {

      private final WrappedMessageTypeMapper wrappedMessageTypeMapper;

      private WrappingConfigImpl(WrappedMessageTypeMapper wrappedMessageTypeMapper) {
         this.wrappedMessageTypeMapper = wrappedMessageTypeMapper;
      }

      @Override
      public WrappedMessageTypeMapper wrappedMessageTypeMapper() {
         return wrappedMessageTypeMapper;
      }

      @Override
      public String toString() {
         return "WrappingConfigImpl{wrappedMessageTypeMapper=" + wrappedMessageTypeMapper + '}';
      }
   }

   private static final class AnnotationsConfigImpl implements AnnotationsConfig {

      private final Map<String, AnnotationConfiguration> annotations;

      private final boolean logUndefinedAnnotations;

      AnnotationsConfigImpl(Map<String, AnnotationConfigurationImpl> annotations, boolean logUndefinedAnnotations) {
         this.annotations = Collections.unmodifiableMap(annotations);
         this.logUndefinedAnnotations = logUndefinedAnnotations;
      }

      @Override
      public boolean logUndefinedAnnotations() {
         return logUndefinedAnnotations;
      }

      @Override
      public Map<String, AnnotationConfiguration> annotations() {
         return annotations;
      }

      @Override
      public String toString() {
         return "AnnotationsConfig{annotations=" + annotations + ", logUndefinedAnnotations=" + logUndefinedAnnotations + '}';
      }
   }

   public static final class BuilderImpl implements Builder {

      private boolean logOutOfSequenceReads = true;

      private boolean logOutOfSequenceWrites = true;

      private WrappingConfigBuilderImpl wrappingConfigBuilder = null;

      private AnnotationsConfigBuilderImpl annotationsConfigBuilder = null;

      final class WrappingConfigBuilderImpl implements WrappingConfig.Builder {

         private WrappedMessageTypeMapper wrappedMessageTypeMapper;

         @Override
         public WrappingConfig.Builder wrappedMessageTypeMapper(WrappedMessageTypeMapper wrappedMessageTypeMapper) {
            this.wrappedMessageTypeMapper = wrappedMessageTypeMapper;
            return this;
         }

         @Override
         public Configuration build() {
            return BuilderImpl.this.build();
         }
      }

      final class AnnotationsConfigBuilderImpl implements AnnotationsConfig.Builder {

         private Boolean logUndefinedAnnotations = null;

         final Map<String, AnnotationConfigurationImpl.BuilderImpl> annotationBuilders = new HashMap<>();

         @Override
         public AnnotationsConfig.Builder setLogUndefinedAnnotations(boolean logUndefinedAnnotations) {
            this.logUndefinedAnnotations = logUndefinedAnnotations;
            return this;
         }

         @Override
         public AnnotationConfiguration.Builder annotation(String annotationName, AnnotationElement.AnnotationTarget... target) {
            if (annotationBuilders.containsKey(annotationName)) {
               throw new IllegalArgumentException("Duplicate annotation name definition: " + annotationName);
            }
            if (target == null || target.length == 0) {
               throw new IllegalArgumentException("At least one target must be specified for annotation: " + annotationName);
            }
            AnnotationConfigurationImpl.BuilderImpl builder = new AnnotationConfigurationImpl.BuilderImpl(this, annotationName, target);
            annotationBuilders.put(annotationName, builder);
            return builder;
         }

         @Override
         public Configuration build() {
            return BuilderImpl.this.build();
         }
      }

      public BuilderImpl() {
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
      public WrappingConfigBuilderImpl wrappingConfig() {
         if (wrappingConfigBuilder == null) {
            wrappingConfigBuilder = new WrappingConfigBuilderImpl();
         }
         return wrappingConfigBuilder;
      }

      @Override
      public AnnotationsConfigBuilderImpl annotationsConfig() {
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

         AnnotationsConfigBuilderImpl annotationsConfig = annotationsConfig();
         Map<String, AnnotationConfigurationImpl> annotations = new HashMap<>(annotationsConfig.annotationBuilders.size());
         for (AnnotationConfiguration.Builder annotationBuilder : annotationsConfig.annotationBuilders.values()) {
            AnnotationConfigurationImpl annotationConfig = ((AnnotationConfigurationImpl.BuilderImpl) annotationBuilder).buildAnnotationConfiguration();
            annotations.put(annotationConfig.name(), annotationConfig);
         }

         // resolve containers for repeatable annotations
         for (AnnotationConfigurationImpl a : annotations.values()) {
            String repeatable = a.repeatable();
            if (repeatable != null) {
               AnnotationConfigurationImpl container = annotations.get(repeatable);
               if (container == null) {
                  // unlikely, because we auto-create it
                  throw new IllegalStateException("Containing annotation '" + repeatable +
                        "' of repeatable annotation '" + a.name() + "' was not found in configuration.");
               }
               a.container = container;
            }
         }

         boolean logUndefinedAnnotations = annotationsConfig().logUndefinedAnnotations == null ? annotations.size() > 1 : annotationsConfig().logUndefinedAnnotations;
         return new ConfigurationImpl(logOutOfSequenceReads, logOutOfSequenceWrites,
               wrappingConfig().wrappedMessageTypeMapper,
               annotations, logUndefinedAnnotations);
      }
   }
}
