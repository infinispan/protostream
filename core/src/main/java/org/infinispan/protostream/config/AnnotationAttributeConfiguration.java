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

      AnnotationConfiguration.Builder parentBuilder();

      Configuration build();
   }
}
