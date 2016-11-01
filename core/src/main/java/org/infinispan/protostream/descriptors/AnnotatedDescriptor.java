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

   String getName();

   String getFullName();

   FileDescriptor getFileDescriptor();

   String getDocumentation();

   /**
    * Get the documentation annotations in the form of a map of abstract syntax trees of AnnotationElement nodes.
    *
    * @return the map of annotations
    * @throws AnnotationParserException
    */
   Map<String, AnnotationElement.Annotation> getAnnotations() throws AnnotationParserException;

   /**
    * Get the annotation object created by the registered AnnotationMetadataCreator or {@code null} if the annotation is
    * missing or no {@link org.infinispan.protostream.AnnotationMetadataCreator} was registered.
    *
    * @param annotationName
    * @param <T>
    * @return
    * @throws AnnotationParserException
    */
   <T> T getProcessedAnnotation(String annotationName) throws AnnotationParserException;
}
