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
    * Return the fulla name of the descriptor.
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
    * Get the documentation annotations in the form of a map of abstract syntax trees of {@link AnnotationElement} nodes.
    * Only the configured annotations are returned. The unconfigured ones are discarded immediately after parsing.
    * <p>
    * The parsing of annotations is performed lazily on first invocation.
    *
    * @return the map of annotations
    * @throws AnnotationParserException if parsing of annotations fails
    */
   Map<String, AnnotationElement.Annotation> getAnnotations() throws AnnotationParserException;

   /**
    * Get the annotation object created by the registered {@link org.infinispan.protostream.AnnotationMetadataCreator}
    * or {@code null} if the annotation is missing or no {@link org.infinispan.protostream.AnnotationMetadataCreator}
    * was registered.
    *
    * @param annotationName
    * @param <T>
    * @return
    * @throws AnnotationParserException if parsing of annotations fails
    */
   <T> T getProcessedAnnotation(String annotationName) throws AnnotationParserException;
}
