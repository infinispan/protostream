package org.infinispan.protostream.impl;

import java.io.IOException;

import org.infinispan.protostream.BaseMarshallerDelegate;
import org.infinispan.protostream.ProtobufTagMarshaller;
import org.infinispan.protostream.descriptors.FieldDescriptor;

/**
 * @author anistor@redhat.com
 * @since 1.0
 */
final class ProtobufTagMarshallerDelegate<T> extends BaseMarshallerDelegate<T> {

   private final ProtobufTagMarshaller<T> marshaller;

   ProtobufTagMarshallerDelegate(ProtobufTagMarshaller<T> marshaller) {
      this.marshaller = marshaller;
   }

   @Override
   public ProtobufTagMarshaller<T> getMarshaller() {
      return marshaller;
   }

   @Override
   public void marshall(ProtobufTagMarshaller.WriteContext ctx, FieldDescriptor fieldDescriptor, T value) throws IOException {
      marshaller.write(ctx, value);
   }

   @Override
   public T unmarshall(ProtobufTagMarshaller.ReadContext ctx, FieldDescriptor fieldDescriptor) throws IOException {
      return marshaller.read(ctx);
   }
}
