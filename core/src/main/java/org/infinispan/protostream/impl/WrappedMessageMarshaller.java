package org.infinispan.protostream.impl;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.RawProtobufMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.WrappedMessage;

import java.io.IOException;

/**
 * @author anistor@redhat.com
 */
public class WrappedMessageMarshaller implements RawProtobufMarshaller<WrappedMessage> {

   @Override
   public Class<? extends WrappedMessage> getJavaClass() {
      return WrappedMessage.class;
   }

   @Override
   public String getTypeName() {
      return "org.infinispan.protostream.WrappedMessage";
   }

   @Override
   public WrappedMessage readFrom(SerializationContext ctx, CodedInputStream in) throws IOException {
      Object o = ProtobufUtil.fromWrappedByteArray(ctx, in);
      return new WrappedMessage(o);
   }

   @Override
   public void writeTo(SerializationContext ctx, CodedOutputStream out, WrappedMessage wrappedMessage) throws IOException {
      ProtobufUtil.toWrappedByteArray(ctx, out, wrappedMessage.getValue());
   }
}
