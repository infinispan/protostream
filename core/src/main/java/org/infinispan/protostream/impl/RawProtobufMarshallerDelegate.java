package org.infinispan.protostream.impl;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Descriptors.FieldDescriptor;
import org.infinispan.protostream.RawProtobufMarshaller;

import java.io.IOException;

/**
 * @author anistor@redhat.com
 * @since 1.0
 */
public final class RawProtobufMarshallerDelegate<T> implements BaseMarshallerDelegate<T> {

   private final RawProtobufMarshaller<T> marshaller;

   private final SerializationContextImpl ctx;

   public RawProtobufMarshallerDelegate(RawProtobufMarshaller<T> marshaller, SerializationContextImpl ctx) {
      this.marshaller = marshaller;
      this.ctx = ctx;
   }

   @Override
   public RawProtobufMarshaller<T> getMarshaller() {
      return marshaller;
   }

   @Override
   public void marshall(String fieldName, FieldDescriptor fieldDescriptor, T value, ProtoStreamWriterImpl writer, CodedOutputStream out) throws IOException {
      marshaller.writeTo(ctx, out, value);
   }

   @Override
   public T unmarshall(String fieldName, FieldDescriptor fieldDescriptor, ProtoStreamReaderImpl reader, CodedInputStream in) throws IOException {
      return marshaller.readFrom(ctx, in);
   }
}
