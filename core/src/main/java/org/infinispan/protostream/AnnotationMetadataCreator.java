package org.infinispan.protostream;

import org.infinispan.protostream.descriptors.AnnotatedDescriptor;
import org.infinispan.protostream.descriptors.AnnotationElement;

/**
 * @author anistor@redhat.com
 * @since 2.0
 */
public interface AnnotationMetadataCreator<MetadataType, AnnotatedDescriptorType extends AnnotatedDescriptor> {

   MetadataType create(AnnotatedDescriptorType annotatedDescriptor, AnnotationElement.Annotation annotation);
}
