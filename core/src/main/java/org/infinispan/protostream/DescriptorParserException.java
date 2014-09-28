package org.infinispan.protostream;

/**
 * Exception for parse error.
 *
 * @author gustavonalle
 * @since 2.0
 */
public class DescriptorParserException extends RuntimeException {

   public DescriptorParserException(String message) {
      super(message);
   }

   public DescriptorParserException(String message, Throwable cause) {
      super(message, cause);
   }

   public DescriptorParserException(Throwable cause) {
      super(cause);
   }
}
