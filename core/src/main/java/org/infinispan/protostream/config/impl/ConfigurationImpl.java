package org.infinispan.protostream.config.impl;

import java.util.HashMap;
import java.util.Map;

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
   private final boolean lenient;
   private final AnnotationsConfigImpl annotationsConfig;
   private final int maxNestedMessageDepth;
   private final SchemaValidation schemaValidation;

   private ConfigurationImpl(BuilderImpl builder, Map<String, AnnotationConfigurationImpl> annotations) {
      this.logOutOfSequenceReads = builder.logOutOfSequenceReads;
      this.logOutOfSequenceWrites = builder.logOutOfSequenceWrites;
      this.lenient = builder.lenient;
      this.maxNestedMessageDepth = builder.maxNestedMessageDepth;
      this.schemaValidation = builder.schemaValidation;
      this.annotationsConfig = new AnnotationsConfigImpl(annotations, builder.logUndefinedAnnotations);
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
   public int maxNestedMessageDepth() {
      return maxNestedMessageDepth;
   }

   @Override
   public SchemaValidation schemaValidation() {
      return schemaValidation;
   }

   @Override
   public AnnotationsConfig annotationsConfig() {
      return annotationsConfig;
   }

   @Override
   public String toString() {
      return "ConfigurationImpl{" +
            "logOutOfSequenceReads=" + logOutOfSequenceReads +
            ", logOutOfSequenceWrites=" + logOutOfSequenceWrites +
            ", lenient=" + lenient +
            ", annotationsConfig=" + annotationsConfig +
            ", maxNestedMessageDepth=" + maxNestedMessageDepth +
            ", schemaValidation=" + schemaValidation +
            '}';
   }

   private static final class AnnotationsConfigImpl implements AnnotationsConfig {

      private final Map<String, AnnotationConfiguration> annotations;

      private final boolean logUndefinedAnnotations;

      AnnotationsConfigImpl(Map<String, AnnotationConfigurationImpl> annotations, boolean logUndefinedAnnotations) {
         this.annotations = Map.copyOf(annotations);
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
      private boolean lenient = true;
      private int maxNestedMessageDepth = Configuration.DEFAULT_MAX_NESTED_DEPTH;
      private AnnotationsConfigBuilderImpl annotationsConfigBuilder = null;
      private Boolean logUndefinedAnnotations;
      private SchemaValidation schemaValidation = SchemaValidation.DEFAULT;

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
      public Builder setLenient(boolean lenient) {
         this.lenient = lenient;
         return this;
      }

      @Override
      public Builder maxNestedMessageDepth(int maxNestedMessageDepth) {
         this.maxNestedMessageDepth = maxNestedMessageDepth;
         return this;
      }

      @Override
      public Builder schemaValidation(SchemaValidation schemaValidation) {
         this.schemaValidation = schemaValidation;
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
         for (AnnotationConfigurationImpl.BuilderImpl annotationBuilder : annotationsConfig.annotationBuilders.values()) {
            AnnotationConfigurationImpl annotationConfig = annotationBuilder.buildAnnotationConfiguration();
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

         // TypeId is the only predefined annotation. If there are more than one then we know we have at least one user defined.
         logUndefinedAnnotations = ((AnnotationsConfigBuilderImpl) annotationsConfig()).logUndefinedAnnotations;
         if (logUndefinedAnnotations == null) logUndefinedAnnotations = annotations.size() > 1;
         return new ConfigurationImpl(this, annotations);
      }
   }
}
