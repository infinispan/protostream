package org.infinispan.protostream.annotations.impl.processor;

import javax.lang.model.element.Element;

/**
 * An exception thrown to stop processing of annotations abruptly whenever the conditions do not allow to continue (ie.
 * annotation values are not invalid) and a compilation error is to be issued immediately.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
final class AnnotationProcessingException extends RuntimeException {

   private final Element location;

   private final String message;

   private final Object[] msgParams;

   AnnotationProcessingException(Element location, String message, Object... msgParams) {
      this.location = location;
      this.message = message;
      this.msgParams = msgParams;
   }

   AnnotationProcessingException(Throwable cause, Element location, String message, Object... msgParams) {
      super(cause);
      this.location = location;
      this.message = message;
      this.msgParams = msgParams;
   }

   public Element getLocation() {
      return location;
   }

   public String getFormattedMessage() {
      return String.format(message, msgParams);
   }
}
