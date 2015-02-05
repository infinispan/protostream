package org.infinispan.protostream.config;

import org.infinispan.protostream.AnnotationMetadataCreator;
import org.infinispan.protostream.descriptors.AnnotationElement;
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
public final class Configuration {

   public static final String TYPE_ID_ANNOTATION = "TypeId";

   private final boolean logOutOfSequenceReads;

   private final boolean logOutOfSequenceWrites;

   private final Map<String, AnnotationConfig<Descriptor>> messageAnnotations;
   private final Map<String, AnnotationConfig<FieldDescriptor>> fieldAnnotations;
   private final Map<String, AnnotationConfig<EnumDescriptor>> enumAnnotations;

   private Configuration(boolean logOutOfSequenceReads, boolean logOutOfSequenceWrites,
                         Map<String, AnnotationConfig<Descriptor>> messageAnnotations,
                         Map<String, AnnotationConfig<FieldDescriptor>> fieldAnnotations,
                         Map<String, AnnotationConfig<EnumDescriptor>> enumAnnotations) {
      this.logOutOfSequenceReads = logOutOfSequenceReads;
      this.logOutOfSequenceWrites = logOutOfSequenceWrites;
      this.messageAnnotations = Collections.unmodifiableMap(messageAnnotations);
      this.fieldAnnotations = Collections.unmodifiableMap(fieldAnnotations);
      this.enumAnnotations = Collections.unmodifiableMap(enumAnnotations);
   }

   public boolean logOutOfSequenceReads() {
      return logOutOfSequenceReads;
   }

   public boolean logOutOfSequenceWrites() {
      return logOutOfSequenceWrites;
   }

   public Map<String, AnnotationConfig<Descriptor>> messageAnnotations() {
      return messageAnnotations;
   }

   public Map<String, AnnotationConfig<FieldDescriptor>> fieldAnnotations() {
      return fieldAnnotations;
   }

   public Map<String, AnnotationConfig<EnumDescriptor>> enumAnnotations() {
      return enumAnnotations;
   }

   @Override
   public String toString() {
      return "Configuration{" +
            "logOutOfSequenceReads=" + logOutOfSequenceReads +
            ", logOutOfSequenceWrites=" + logOutOfSequenceWrites +
            ", messageAnnotations=" + messageAnnotations +
            ", fieldAnnotations=" + fieldAnnotations +
            ", enumAnnotations=" + enumAnnotations +
            '}';
   }

   public static final class Builder {

      private boolean logOutOfSequenceReads = true;

      private boolean logOutOfSequenceWrites = true;

      private final Map<String, AnnotationConfig.Builder<Descriptor>> messageAnnotationBuilders = new HashMap<String, AnnotationConfig.Builder<Descriptor>>();
      private final Map<String, AnnotationConfig.Builder<FieldDescriptor>> fieldAnnotationBuilders = new HashMap<String, AnnotationConfig.Builder<FieldDescriptor>>();
      private final Map<String, AnnotationConfig.Builder<EnumDescriptor>> enumAnnotationBuilders = new HashMap<String, AnnotationConfig.Builder<EnumDescriptor>>();

      public Builder() {
      }

      public boolean isLogOutOfSequenceReads() {
         return logOutOfSequenceReads;
      }

      public Builder setLogOutOfSequenceReads(boolean logOutOfSequenceReads) {
         this.logOutOfSequenceReads = logOutOfSequenceReads;
         return this;
      }

      public boolean isLogOutOfSequenceWrites() {
         return logOutOfSequenceWrites;
      }

      public Builder setLogOutOfSequenceWrites(boolean logOutOfSequenceWrites) {
         this.logOutOfSequenceWrites = logOutOfSequenceWrites;
         return this;
      }

      public AnnotationConfig.Builder<Descriptor> messageAnnotation(String annotationName) {
         AnnotationConfig.Builder<Descriptor> builder = new AnnotationConfig.Builder<Descriptor>(this, annotationName);
         messageAnnotationBuilders.put(annotationName, builder);
         return builder;
      }

      public AnnotationConfig.Builder<EnumDescriptor> enumAnnotation(String annotationName) {
         AnnotationConfig.Builder<EnumDescriptor> builder = new AnnotationConfig.Builder<EnumDescriptor>(this, annotationName);
         enumAnnotationBuilders.put(annotationName, builder);
         return builder;
      }

      public AnnotationConfig.Builder<FieldDescriptor> fieldAnnotation(String annotationName) {
         AnnotationConfig.Builder<FieldDescriptor> builder = new AnnotationConfig.Builder<FieldDescriptor>(this, annotationName);
         fieldAnnotationBuilders.put(annotationName, builder);
         return builder;
      }

      public Configuration build() {
         messageAnnotation(TYPE_ID_ANNOTATION)
               .attribute(AnnotationElement.Annotation.DEFAULT_ATTRIBUTE)
               .intType()
               .annotationMetadataCreator(new AnnotationMetadataCreator<Integer, Descriptor>() {
                  @Override
                  public Integer create(Descriptor annotatedDescriptor, AnnotationElement.Annotation annotation) {
                     return (Integer) annotation.getDefaultAttributeValue().getValue();
                  }
               });
         enumAnnotation(TYPE_ID_ANNOTATION)
               .attribute(AnnotationElement.Annotation.DEFAULT_ATTRIBUTE)
               .intType()
               .annotationMetadataCreator(new AnnotationMetadataCreator<Integer, EnumDescriptor>() {
                  @Override
                  public Integer create(EnumDescriptor annotatedDescriptor, AnnotationElement.Annotation annotation) {
                     return (Integer) annotation.getDefaultAttributeValue().getValue();
                  }
               });

         Map<String, AnnotationConfig<Descriptor>> messageAnnotations = new HashMap<String, AnnotationConfig<Descriptor>>(messageAnnotationBuilders.size());
         for (AnnotationConfig.Builder<Descriptor> annotationBuilder : messageAnnotationBuilders.values()) {
            AnnotationConfig<Descriptor> annotationConfig = annotationBuilder.buildAnnotationConfig();
            messageAnnotations.put(annotationConfig.name(), annotationConfig);
         }

         Map<String, AnnotationConfig<FieldDescriptor>> fieldAnnotations = new HashMap<String, AnnotationConfig<FieldDescriptor>>(fieldAnnotationBuilders.size());
         for (AnnotationConfig.Builder<FieldDescriptor> annotationBuilder : fieldAnnotationBuilders.values()) {
            AnnotationConfig<FieldDescriptor> annotationConfig = annotationBuilder.buildAnnotationConfig();
            fieldAnnotations.put(annotationConfig.name(), annotationConfig);
         }

         Map<String, AnnotationConfig<EnumDescriptor>> enumAnnotations = new HashMap<String, AnnotationConfig<EnumDescriptor>>(enumAnnotationBuilders.size());
         for (AnnotationConfig.Builder<EnumDescriptor> annotationBuilder : enumAnnotationBuilders.values()) {
            AnnotationConfig<EnumDescriptor> annotationConfig = annotationBuilder.buildAnnotationConfig();
            enumAnnotations.put(annotationConfig.name(), annotationConfig);
         }

         return new Configuration(logOutOfSequenceReads, logOutOfSequenceWrites, messageAnnotations, fieldAnnotations, enumAnnotations);
      }
   }
}
