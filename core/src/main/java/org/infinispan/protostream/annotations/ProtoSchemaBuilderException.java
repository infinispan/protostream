package org.infinispan.protostream.annotations;

import org.infinispan.protostream.annotations.impl.types.XElement;

/**
 * A runtime exception that can be thrown during the generation of the Protocol Buffers schema and marshallers either
 * due to improper API usage or due to internal errors.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
public final class ProtoSchemaBuilderException extends RuntimeException {

   private final XElement element;

   public ProtoSchemaBuilderException(String message) {
      super(message);
      this.element = null;
   }

   public ProtoSchemaBuilderException(Throwable cause) {
      super(cause);
      this.element = null;
   }

   public ProtoSchemaBuilderException(String message, Throwable cause) {
      super(message, cause);
      this.element = null;
   }

   public ProtoSchemaBuilderException(XElement element, String message) {
      super(message);
      this.element = element;
   }

   public ProtoSchemaBuilderException(XElement element, Throwable cause) {
      super(cause);
      this.element = element;
   }

   public ProtoSchemaBuilderException(XElement element, String message, Throwable cause) {
      super(message, cause);
      this.element = element;
   }

   public XElement getElement() {
      return element;
   }
}
