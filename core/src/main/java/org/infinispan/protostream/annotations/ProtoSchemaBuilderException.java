package org.infinispan.protostream.annotations;

/**
 * A runtime exception that can be thrown during the generation of the Protocol Buffers schema and marshallers either
 * due to improper API usage or due to internal errors.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
public final class ProtoSchemaBuilderException extends RuntimeException {

   public ProtoSchemaBuilderException(String message) {

      super(message);
   }

   public ProtoSchemaBuilderException(Throwable cause) {
      super(cause);
   }

   public ProtoSchemaBuilderException(String message, Throwable cause) {
      super(message, cause);
   }
}
