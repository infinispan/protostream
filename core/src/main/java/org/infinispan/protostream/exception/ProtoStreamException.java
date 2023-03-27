package org.infinispan.protostream.exception;

public class ProtoStreamException extends RuntimeException {

   public ProtoStreamException() {
      super();
   }

   public ProtoStreamException(Throwable cause) {
      super(cause);
   }

   public ProtoStreamException(String msg) {
      super(msg);
   }

   public ProtoStreamException(String msg, Throwable cause) {
      super(msg, cause);
   }

   public ProtoStreamException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
      super(message, cause, enableSuppression, writableStackTrace);
   }
}
