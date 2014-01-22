package org.infinispan.protostream;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import java.io.IOException;

/**
 * A marshaller for messages or enums that has direct access to the low level protobuf streams to read and write tags in
 * an unchecked manner. The access is not verified against a protobuf definition as it would normally happen in case of
 * {@code MessageMarshaller} and {@code EnumMarshaller}. This is usually used to provide more flexible or generic
 * marshallers, not tied to a specific schema.
 *
 * @author anistor@redhat.com
 * @since 1.0
 */
public interface RawProtobufMarshaller<T> extends BaseMarshaller<T> {

   T readFrom(SerializationContext ctx, CodedInputStream in) throws IOException;

   void writeTo(SerializationContext ctx, CodedOutputStream out, T t) throws IOException;
}
