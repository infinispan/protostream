package org.infinispan.protostream.config;

import java.util.Set;

import org.infinispan.protostream.AnnotationMetadataCreator;
import org.infinispan.protostream.descriptors.AnnotatedDescriptor;
import org.infinispan.protostream.descriptors.AnnotationElement;

/**
 * @author anistor@redhat.com
 * @since 4.0
 */
public interface AnnotationAttributeConfiguration {

   /**
    * The name of the annotation element (required).
    */
   String name();

   /**
    * The optional package name
    */
   String packageName();

   /**
    * The type. Defaults to String if not explicitly set.
    */
   AnnotationElement.AttributeType type();

   /**
    * Is it a single value or an array of values?
    */
   boolean multiple();

   /**
    * The default value (optional)
    */
   Object defaultValue();

   /**
    * The set of allowed values. This is only used with STRING, IDENTIFIER, or ANNOTATION type.
    * ANNOTATION type must have a single allowed value.
    */
   Set<String> allowedValues();

   boolean isAllowed(AnnotationElement.Value value);

   interface Builder {

      /**
       * The type. Defaults to String if not explicitly set.
       */
      Builder type(AnnotationElement.AttributeType type);

      Builder multiple(boolean isMultiple);

      Builder defaultValue(Object defaultValue);

      Builder allowedValues(String... allowedValues);

      Builder metadataCreator(AnnotationMetadataCreator<?, ? extends AnnotatedDescriptor> annotationMetadataCreator);

      Builder repeatable(String containingAnnotationName);

      /**
       * Create a <b>new</b> attribute with the given name and return the builder to configure it.
       */
      Builder attribute(String name);

      /**
       * Sets the optional package name
       */
      Builder packageName(String packageName);

      /**
       * @return the parent builder in order to allow defining more annotations
       * @deprecated just call {@link #annotation(String, AnnotationElement.AnnotationTarget...)} directly
       */
      @Deprecated
      AnnotationConfiguration.Builder parentBuilder();

      /**
       * Starts the creation of a new annotation with the given name and return its builder to continue define it.
       */
      AnnotationConfiguration.Builder annotation(String annotationName, AnnotationElement.AnnotationTarget... target);

      Configuration build();
   }
}
