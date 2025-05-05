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
    *
    * @return the name
    */
   String name();

   /**
    * The optional package name
    *
    * @return the package name or null if none is set
    */
   String packageName();

   /**
    * The type. Defaults to String if not explicitly set.
    *
    * @return the type
    */
   AnnotationElement.AttributeType type();

   /**
    * Is it a single value or an array of values?
    *
    * @return true if it is an array of values, false otherwise
    */
   boolean multiple();

   /**
    * The default value (optional)
    *
    * @return the default value or null if none is set
    */
   Object defaultValue();

   /**
    * The set of allowed values. This is only used with STRING, IDENTIFIER, or ANNOTATION type.
    * ANNOTATION type must have a single allowed value
    *
    * @return the set of allowed values or an empty set if none is set.
    */
   Set<String> allowedValues();

   /**
    * Is the given value allowed?
    *
    * @param value the value to check
    * @return true if the value is allowed, false otherwise
    */
   boolean isAllowed(AnnotationElement.Value value);

   interface Builder {

      /**
       * The type. Defaults to String if not explicitly set.
       *
       * @param type the type
       * @return the builder
       */
      Builder type(AnnotationElement.AttributeType type);

      /**
       * Is it a single value or an array of values?
       *
       * @param isMultiple true if it is an array of values, false otherwise
       * @return the builder
       */
      Builder multiple(boolean isMultiple);

      /**
       * The default value (optional)
       *
       * @param defaultValue the default value or null if none is set.
       * @return the builder
       */
      Builder defaultValue(Object defaultValue);

      /**
       * The set of allowed values. This is only used with STRING, IDENTIFIER, or ANNOTATION type.
       * ANNOTATION type must have a single allowed value
       *
       * @param allowedValues the set of allowed values or an empty set if none is set.
       * @return the builder
       */
      Builder allowedValues(String... allowedValues);

      /**
       * Attach a metadata creator for this attribute and return the same builder.
       *
       * @param annotationMetadataCreator the metadata creator
       * @return the builder
       */
      Builder metadataCreator(AnnotationMetadataCreator<?, ? extends AnnotatedDescriptor> annotationMetadataCreator);

      /**
       * Set the name of the containing annotation if this attribute is repeatable or null otherwise.
       *
       * @param containingAnnotationName the name of the containing annotation
       * @return the builder
       */
      Builder repeatable(String containingAnnotationName);

      /**
       * Create a <b>new</b> attribute with the given name and return the builder to configure it.
       *
       * @param name the name of the attribute
       * @return the builder to configure the new attribute.
       */
      Builder attribute(String name);

      /**
       * Sets the optional package name
       *
       * @param packageName the package name or null if none is set
       * @return the builder to configure the new attribute.
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
       *
       * @param annotationName the name of the annotation
       * @param target the targets of the annotation
       */
      AnnotationConfiguration.Builder annotation(String annotationName, AnnotationElement.AnnotationTarget... target);

      /// Create a new annotation attribute configuration.
      /// @return the new annotation attribute configuration.
      Configuration build();
   }
}
