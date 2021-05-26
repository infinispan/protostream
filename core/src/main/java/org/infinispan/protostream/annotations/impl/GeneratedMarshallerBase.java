package org.infinispan.protostream.annotations.impl;

import java.io.IOException;

import org.infinispan.protostream.ProtobufTagMarshaller;
import org.infinispan.protostream.impl.BaseMarshallerDelegate;
import org.infinispan.protostream.impl.ByteArrayOutputStreamEx;
import org.infinispan.protostream.impl.TagWriterImpl;

/**
 * Base class for generated message marshallers. Provides some handy helper methods.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
@SuppressWarnings("unused")
public class GeneratedMarshallerBase {

   /**
    * Invoked by generated code.
    */
   protected final <T> T readMessage(BaseMarshallerDelegate<T> marshallerDelegate, ProtobufTagMarshaller.ReadContext ctx) throws IOException {
      return marshallerDelegate.unmarshall(ctx, null);
   }

   /**
    * Invoked by generated code.
    */
   protected final <T> void writeMessage(BaseMarshallerDelegate<T> marshallerDelegate, ProtobufTagMarshaller.WriteContext ctx, T message) throws IOException {
      if (message == null) {
         throw new IllegalArgumentException("Object to marshall cannot be null");
      }
      marshallerDelegate.marshall(ctx, null, message);
      ctx.getWriter().flush();
   }

   /**
    * Invoked by generated code.
    */
   protected final <T> void writeNestedMessage(BaseMarshallerDelegate<T> marshallerDelegate, ProtobufTagMarshaller.WriteContext ctx, int fieldNumber, T message) throws IOException {
      ctx.getWriter().flush();
      ByteArrayOutputStreamEx baos = new ByteArrayOutputStreamEx();
      TagWriterImpl nested = TagWriterImpl.newNestedInstance(ctx, baos);
      writeMessage(marshallerDelegate, nested, message);
      ctx.getWriter().writeBytes(fieldNumber, baos.getByteBuffer());
   }
}
