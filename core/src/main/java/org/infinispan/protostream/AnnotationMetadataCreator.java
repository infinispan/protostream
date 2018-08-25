package org.infinispan.protostream;

import org.infinispan.protostream.descriptors.AnnotatedDescriptor;
import org.infinispan.protostream.descriptors.AnnotationElement;

/**
 * Creates an application specific alternative representation of an {@link org.infinispan.protostream.descriptors.AnnotationElement.Annotation}
 * value.
 *
 * @author anistor@redhat.com
 * @since 2.0
 */
@FunctionalInterface
public interface AnnotationMetadataCreator<MetadataOutType, AnnotatedDescriptorType extends AnnotatedDescriptor> {

   MetadataOutType create(AnnotatedDescriptorType annotatedDescriptor, AnnotationElement.Annotation annotation);
}
