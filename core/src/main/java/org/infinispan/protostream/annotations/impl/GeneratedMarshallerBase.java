package org.infinispan.protostream.annotations.impl;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.RawProtobufMarshaller;
import org.infinispan.protostream.SerializationContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Base class for generated marshallers. Provides some handy helper methods.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
public class GeneratedMarshallerBase {

   protected final <T> T readMessage(SerializationContext ctx, CodedInputStream in, Class<T> clazz) throws IOException {
      BaseMarshaller<T> m = ctx.getMarshaller(clazz);
      if (m instanceof RawProtobufMarshaller) {
         return ((RawProtobufMarshaller<T>) m).readFrom(ctx, in);
      } else {
         return ProtobufUtil.readFrom(ctx, in, clazz);
      }
   }

   protected final <T> void writeMessage(SerializationContext ctx, CodedOutputStream out, Class<T> clazz, T message) throws IOException {
      BaseMarshaller<T> m = ctx.getMarshaller(clazz);
      if (m instanceof RawProtobufMarshaller) {
         ((RawProtobufMarshaller<T>) m).writeTo(ctx, out, message);
      } else {
         ProtobufUtil.writeTo(ctx, out, message);
      }
   }

   protected final <T> void writeNestedMessage(SerializationContext ctx, CodedOutputStream out, Class<T> clazz, T message) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      CodedOutputStream nested = CodedOutputStream.newInstance(baos);
      writeMessage(ctx, nested, clazz, message);
      nested.flush();
      out.writeRawVarint32(baos.size());
      out.writeRawBytes(baos.toByteArray());
   }
}
