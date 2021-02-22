package org.infinispan.protostream;

import java.io.IOException;

/**
 * A marshaller for message types that has direct access to the low level Protobuf streams to read and write tags in an
 * unchecked manner. Cannot be used for enums. The access is not verified against a Protobuf definition as it would
 * normally happen in case of {@link MessageMarshaller}. This is usually used to provide more flexible or generic
 * marshallers, not tied to a specific schema.
 *
 * @author anistor@redhat.com
 * @since 4.4
 */
public interface ProtoStreamMarshaller<T> extends BaseMarshaller<T> {

   T read(ReadContext ctx) throws IOException;

   void write(WriteContext ctx, T t) throws IOException;

   /**
    * Base interface for marshalling operation contexts.
    */
   interface OperationContext {

      /**
       * Provides access to the {@link ImmutableSerializationContext}.
       */
      ImmutableSerializationContext getSerializationContext();

      Object getParamValue(Object key);

      void setParamValue(Object key, Object value);
   }

   /**
    * Operation context of unmarshalling operations.
    */
   interface ReadContext extends OperationContext {

      TagReader getIn();

      @Deprecated
      MessageMarshaller.ProtoStreamReader getReader();
   }

   /**
    * Operation context of marshalling operations.
    */
   interface WriteContext extends OperationContext {

      TagWriter getOut();

      @Deprecated
      MessageMarshaller.ProtoStreamWriter getWriter();
   }
}
