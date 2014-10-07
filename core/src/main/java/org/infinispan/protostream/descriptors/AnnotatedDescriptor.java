package org.infinispan.protostream.descriptors;

import org.infinispan.protostream.AnnotationParserException;

import java.util.Map;

/**
 * @author anistor@redhat.com
 * @since 2.0
 */
public interface AnnotatedDescriptor {

   String getName();

   String getFullName();

   FileDescriptor getFileDescriptor();

   String getDocumentation();

   Map<String, AnnotationElement.Annotation> getAnnotations() throws AnnotationParserException;

   <T> T getParsedAnnotation(String annotationName) throws AnnotationParserException;
}
