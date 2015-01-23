package org.infinispan.protostream.annotations.impl;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.RawProtobufMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.impl.ByteArrayOutputStreamEx;
import org.infinispan.protostream.impl.ProtoStreamReaderImpl;
import org.infinispan.protostream.impl.ProtoStreamWriterImpl;
import org.infinispan.protostream.impl.RawProtoStreamWriterImpl;
import org.infinispan.protostream.impl.SerializationContextImpl;

import java.io.IOException;

/**
 * Base class for generated marshallers. Provides some handy helper methods.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
public class GeneratedMarshallerBase {

   protected final <T> T readMessage(SerializationContext ctx, RawProtoStreamReader in, Class<T> clazz) throws IOException {
      BaseMarshaller<T> m = ctx.getMarshaller(clazz);
      if (m instanceof RawProtobufMarshaller) {
         return ((RawProtobufMarshaller<T>) m).readFrom(ctx, in);
      } else {
         ProtoStreamReaderImpl reader = new ProtoStreamReaderImpl((SerializationContextImpl) ctx);
         return reader.read(in, clazz);
      }
   }

   protected final <T> void writeMessage(SerializationContext ctx, RawProtoStreamWriter out, Class<T> clazz, T message) throws IOException {
      BaseMarshaller<T> m = ctx.getMarshaller(clazz);
      if (m instanceof RawProtobufMarshaller) {
         ((RawProtobufMarshaller<T>) m).writeTo(ctx, out, message);
      } else {
         ProtoStreamWriterImpl writer = new ProtoStreamWriterImpl((SerializationContextImpl) ctx);
         writer.write(out, message);
      }
   }

   protected final <T> void writeNestedMessage(SerializationContext ctx, RawProtoStreamWriter out, Class<T> clazz, int fieldNumber, T message) throws IOException {
      ByteArrayOutputStreamEx baos = new ByteArrayOutputStreamEx();
      RawProtoStreamWriter nested = RawProtoStreamWriterImpl.newInstance(baos);
      writeMessage(ctx, nested, clazz, message);
      nested.flush();
      out.writeBytes(fieldNumber, baos.getByteBuffer());
   }
}
