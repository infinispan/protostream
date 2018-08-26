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
    * The name of the attribute.
    */
   String name();

   AnnotationElement.AttributeType type();

   boolean multiple();

   Object defaultValue();

   Set<String> allowedValues();

   interface Builder {

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
