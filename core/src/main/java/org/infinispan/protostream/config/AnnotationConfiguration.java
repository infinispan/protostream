package org.infinispan.protostream.config;

import java.util.Map;

import org.infinispan.protostream.AnnotationMetadataCreator;
import org.infinispan.protostream.descriptors.AnnotatedDescriptor;
import org.infinispan.protostream.descriptors.AnnotationElement;

/**
 * @author anistor@redhat.com
 * @since 4.0
 */
public interface AnnotationConfiguration {

   /**
    * The name of the annotation.
    */
   String name();

   String packageName();

   /**
    * Applicable targets.
    */
   AnnotationElement.AnnotationTarget[] target();

   Map<String, AnnotationAttributeConfiguration> attributes();

   AnnotationMetadataCreator<?, ? extends AnnotatedDescriptor> metadataCreator();

   /**
    * The name of the containing annotation if this annotation is repeatable or null otherwise.
    */
   String repeatable();

   interface Builder {

      /**
       * Add a new attribute with the given name to the current annotation and return the builder to continue to
       * configure this attribute.
       */
      AnnotationAttributeConfiguration.Builder attribute(String name);

      /**
       * Attach a metadata creator for this annotation and return the same builder.
       */
      Builder metadataCreator(AnnotationMetadataCreator<?, ? extends AnnotatedDescriptor> annotationMetadataCreator);

      Builder repeatable(String containerAnnotationName);

      Builder packageName(String packageName);

      /**
       * Create a new annotation with the given name and return its builder to continue configuring it.
       */
      Builder annotation(String annotationName, AnnotationElement.AnnotationTarget... target);

      Configuration build();
   }
}
