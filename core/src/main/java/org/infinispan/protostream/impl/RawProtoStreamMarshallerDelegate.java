package org.infinispan.protostream.impl;

import java.io.IOException;

import org.infinispan.protostream.ProtoStreamMarshaller;
import org.infinispan.protostream.descriptors.FieldDescriptor;

/**
 * @author anistor@redhat.com
 * @since 1.0
 */
final class RawProtoStreamMarshallerDelegate<T> extends BaseMarshallerDelegate<T> {

   private final ProtoStreamMarshaller<T> marshaller;

   RawProtoStreamMarshallerDelegate(ProtoStreamMarshaller<T> marshaller) {
      this.marshaller = marshaller;
   }

   @Override
   public ProtoStreamMarshaller<T> getMarshaller() {
      return marshaller;
   }

   @Override
   public void marshall(ProtoStreamMarshaller.WriteContext ctx, FieldDescriptor fieldDescriptor, T value) throws IOException {
      marshaller.write(ctx, value);
   }

   @Override
   public T unmarshall(ProtoStreamMarshaller.ReadContext ctx, FieldDescriptor fieldDescriptor) throws IOException {
      return marshaller.read(ctx);
   }
}
