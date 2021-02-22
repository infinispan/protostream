package org.infinispan.protostream;

import java.io.IOException;

/**
 * A marshaller for message types that has direct access to the low level Protobuf streams to read and write tags in an
 * unchecked manner. Cannot be used for enums. The access is not verified against a Protobuf definition as it would
 * normally happen in case of {@link MessageMarshaller}. This is usually used to provide more flexible or generic
 * marshallers, not tied to a specific schema.
 *
 * @author anistor@redhat.com
 * @since 1.0
 * @deprecated replaced by {@link ProtoStreamMarshaller}. To be removed in version 5.
 */
@Deprecated
public interface RawProtobufMarshaller<T> extends ProtoStreamMarshaller<T> {

   T readFrom(ImmutableSerializationContext ctx, RawProtoStreamReader in) throws IOException;

   void writeTo(ImmutableSerializationContext ctx, RawProtoStreamWriter out, T t) throws IOException;

   @Override
   default T read(ReadContext ctx) throws IOException {
      return readFrom(ctx.getSerializationContext(), ctx.getIn());
   }

   @Override
   default void write(WriteContext ctx, T t) throws IOException {
      writeTo(ctx.getSerializationContext(), ctx.getOut(), t);
   }
}
