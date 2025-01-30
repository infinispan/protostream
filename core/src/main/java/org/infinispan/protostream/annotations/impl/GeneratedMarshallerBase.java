package org.infinispan.protostream.annotations.impl;

import java.io.IOException;

import org.infinispan.protostream.ProtobufTagMarshaller;
import org.infinispan.protostream.RandomAccessOutputStream;
import org.infinispan.protostream.impl.BaseMarshallerDelegate;
import org.infinispan.protostream.impl.Log;
import org.infinispan.protostream.impl.RandomAccessOutputStreamImpl;
import org.infinispan.protostream.impl.TagWriterImpl;

/**
 * Base class for generated message marshallers. Provides some handy helper methods.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
@SuppressWarnings("unused")
public class GeneratedMarshallerBase {

   private static final Log log = Log.LogFactory.getLog(GeneratedMarshallerBase.class);

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
      int maxNestedMessageDepth = ctx.getSerializationContext().getConfiguration().maxNestedMessageDepth();
      if (ctx.depth() >= maxNestedMessageDepth) {
         throw log.maxNestedMessageDepth(maxNestedMessageDepth, message.getClass());
      }
      try (NestedWriter nested = new NestedWriter(ctx, fieldNumber)) {
         writeMessage(marshallerDelegate, nested.writer, message);
      }
   }

   public static class NestedWriter implements AutoCloseable {
      final RandomAccessOutputStream baos;
      final TagWriterImpl writer;
      private final ProtobufTagMarshaller.WriteContext ctx;
      private final int fieldNumber;

      public NestedWriter(ProtobufTagMarshaller.WriteContext ctx, int fieldNumber) {
         this.ctx = ctx;
         this.fieldNumber = fieldNumber;
         this.baos = new RandomAccessOutputStreamImpl();
         this.writer = TagWriterImpl.newNestedInstance(ctx, baos);
      }

      public TagWriterImpl getWriter() {
         return writer;
      }

      public void close() throws IOException {
         ctx.getWriter().writeBytes(fieldNumber, baos.getByteBuffer());
      }
   }
}
