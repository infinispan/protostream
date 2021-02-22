package org.infinispan.protostream.annotations.impl;

import java.io.IOException;

import org.infinispan.protostream.ProtoStreamMarshaller;
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
   protected final <T> T readMessage(BaseMarshallerDelegate<T> marshallerDelegate, ProtoStreamMarshaller.ReadContext ctx) throws IOException {
      return marshallerDelegate.unmarshall(ctx, null);
   }

   /**
    * Invoked by generated code.
    */
   protected final <T> void writeMessage(BaseMarshallerDelegate<T> marshallerDelegate, ProtoStreamMarshaller.WriteContext ctx, T message) throws IOException {
      if (message == null) {
         throw new IllegalArgumentException("Object to marshall cannot be null");
      }
      marshallerDelegate.marshall(ctx, null, message);
      ctx.getOut().flush();
   }

   /**
    * Invoked by generated code.
    */
   protected final <T> void writeNestedMessage(BaseMarshallerDelegate<T> marshallerDelegate, ProtoStreamMarshaller.WriteContext ctx, int fieldNumber, T message) throws IOException {
      ByteArrayOutputStreamEx baos = new ByteArrayOutputStreamEx();
      TagWriterImpl nested = TagWriterImpl.newNestedInstance(ctx, baos);
      writeMessage(marshallerDelegate, nested, message);
      ctx.getOut().writeBytes(fieldNumber, baos.getByteBuffer());
   }
}
