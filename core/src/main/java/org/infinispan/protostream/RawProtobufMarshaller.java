package org.infinispan.protostream;

import java.io.IOException;

/**
 * A marshaller for messages that has direct access to the low level protobuf streams to read and write tags in an
 * unchecked manner. The access is not verified against a protobuf definition as it would normally happen in case of
 * {@code MessageMarshaller}. This is usually used to provide more flexible or generic marshallers, not tied to a
 * specific schema.
 *
 * @author anistor@redhat.com
 * @since 1.0
 */
public interface RawProtobufMarshaller<T> extends BaseMarshaller<T> {

   T readFrom(ImmutableSerializationContext ctx, RawProtoStreamReader in) throws IOException;

   void writeTo(ImmutableSerializationContext ctx, RawProtoStreamWriter out, T t) throws IOException;
}
