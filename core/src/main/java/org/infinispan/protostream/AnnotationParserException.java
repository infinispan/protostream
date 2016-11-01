package org.infinispan.protostream;

/**
 * Exception indicating a syntax or semantic error encountered during parsing or validation of annotations.
 *
 * @author anistor@redhat.com
 * @since 2.0
 */
public class AnnotationParserException extends RuntimeException {

   public AnnotationParserException(String message) {
      super(message);
   }
}
