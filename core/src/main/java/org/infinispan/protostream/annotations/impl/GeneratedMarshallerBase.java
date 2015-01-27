package org.infinispan.protostream.annotations.impl;

import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.impl.BaseMarshallerDelegate;
import org.infinispan.protostream.impl.ByteArrayOutputStreamEx;
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
      BaseMarshallerDelegate<T> marshallerDelegate = ((SerializationContextImpl) ctx).getMarshallerDelegate(clazz);
      return marshallerDelegate.unmarshall(null, null, null, in);
   }

   protected final <T> void writeMessage(SerializationContext ctx, RawProtoStreamWriter out, Class<T> clazz, T message) throws IOException {
      if (message == null) {
         throw new IllegalArgumentException("Object to marshall cannot be null");
      }
      BaseMarshallerDelegate<T> marshallerDelegate = ((SerializationContextImpl) ctx).getMarshallerDelegate(clazz);
      marshallerDelegate.marshall(null, null, message, null, out);
      out.flush();
   }

   protected final <T> void writeNestedMessage(SerializationContext ctx, RawProtoStreamWriter out, Class<T> clazz, int fieldNumber, T message) throws IOException {
      ByteArrayOutputStreamEx baos = new ByteArrayOutputStreamEx();
      RawProtoStreamWriter nested = RawProtoStreamWriterImpl.newInstance(baos);
      writeMessage(ctx, nested, clazz, message);
      nested.flush();
      out.writeBytes(fieldNumber, baos.getByteBuffer());
   }
}
