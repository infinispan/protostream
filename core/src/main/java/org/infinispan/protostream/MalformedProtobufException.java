package org.infinispan.protostream;

import java.io.IOException;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
public final class MalformedProtobufException extends IOException {

   public MalformedProtobufException(String message, Throwable cause) {
      super(message, cause);
   }

   public MalformedProtobufException(String message) {
      super(message);
   }
}
