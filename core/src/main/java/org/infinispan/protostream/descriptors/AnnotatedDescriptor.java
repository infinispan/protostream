package org.infinispan.protostream.descriptors;

import java.util.Map;

import org.infinispan.protostream.AnnotationParserException;

/**
 * Base class for all descriptors. Provides common methods for naming, accessing the attached documentation and
 * annotations.
 *
 * @author anistor@redhat.com
 * @since 2.0
 */
public interface AnnotatedDescriptor {

   /**
    * Return the name of the descriptor.
    *
    * @return the name of the descriptor (never {@code null})
    */
   String getName();

   /**
    * Return the full name of the descriptor.
    *
    * @return the name of the descriptor (never {@code null})
    */
   String getFullName();

   /**
    * Return the containing files's descriptor
    *
    * @return the containing files's descriptor (never {@code null})
    */
   FileDescriptor getFileDescriptor();

   /**
    * Return the documentation text associated with this descriptor.
    *
    * @return the documentation text or {@code null} if not present
    */
   String getDocumentation();

   /**
    * Return the annotations associated with this descriptor.
    * @return a map of annotation name to annotation object
    * @throws AnnotationParserException in case of parsing errors
    */
   Map<String, AnnotationElement.Annotation> getAnnotations() throws AnnotationParserException;

   /**
    * Get the 'processed' annotation object created by the registered {@link org.infinispan.protostream.AnnotationMetadataCreator}
    * or {@code null} if the annotation is missing or no {@link org.infinispan.protostream.AnnotationMetadataCreator}
    * was registered.
    *
    * @param annotationName the name of the annotation
    * @param <T>            the expected type of the object created by the {@link org.infinispan.protostream.AnnotationMetadataCreator}
    * @return the 'processed' annotation object or {@code null} if not found
    * @throws AnnotationParserException if parsing of annotations fails
    */
   <T> T getProcessedAnnotation(String annotationName) throws AnnotationParserException;
}
