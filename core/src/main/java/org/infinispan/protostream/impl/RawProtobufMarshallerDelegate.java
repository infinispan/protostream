package org.infinispan.protostream.impl;

import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.RawProtobufMarshaller;
import org.infinispan.protostream.descriptors.FieldDescriptor;

import java.io.IOException;

/**
 * @author anistor@redhat.com
 * @since 1.0
 */
final class RawProtobufMarshallerDelegate<T> implements BaseMarshallerDelegate<T> {

   private final RawProtobufMarshaller<T> marshaller;

   private final SerializationContextImpl ctx;

   public RawProtobufMarshallerDelegate(SerializationContextImpl ctx, RawProtobufMarshaller<T> marshaller) {
      this.ctx = ctx;
      this.marshaller = marshaller;
   }

   @Override
   public RawProtobufMarshaller<T> getMarshaller() {
      return marshaller;
   }

   @Override
   public void marshall(String fieldName, FieldDescriptor fieldDescriptor, T value, ProtoStreamWriterImpl writer, RawProtoStreamWriter out) throws IOException {
      marshaller.writeTo(ctx, out, value);
   }

   @Override
   public T unmarshall(String fieldName, FieldDescriptor fieldDescriptor, ProtoStreamReaderImpl reader, RawProtoStreamReader in) throws IOException {
      return marshaller.readFrom(ctx, in);
   }
}
