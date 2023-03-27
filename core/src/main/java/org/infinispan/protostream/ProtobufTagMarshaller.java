package org.infinispan.protostream;

import java.io.IOException;

/**
 * A marshaller for message types that has direct access to the low level Protobuf streams (TagReader/TagWriter) to
 * freely read and write tags. Cannot be used for enums because enums are not top level objects. The read/write access
 * to the tag stream is not verified against a Protobuf schema definition as it would normally happen in case of
 * {@link MessageMarshaller} so the implementer must take care to follow the schema. This can also be used to provide
 * more flexible or generic marshallers that are able to handle multiple types.
 *
 * @author anistor@redhat.com
 * @since 4.4
 */
public interface ProtobufTagMarshaller<T> extends BaseMarshaller<T> {

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

      Object getParam(Object key);

      void setParam(Object key, Object value);
   }

   /**
    * Operation context of unmarshalling operations.
    */
   interface ReadContext extends OperationContext {
      TagReader getReader();
   }

   /**
    * Operation context of marshalling operations.
    */
   interface WriteContext extends OperationContext {
      TagWriter getWriter();

      int depth();
   }
}
