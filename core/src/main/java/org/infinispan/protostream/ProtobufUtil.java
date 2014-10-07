package org.infinispan.protostream;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.impl.ProtoStreamReaderImpl;
import org.infinispan.protostream.impl.ProtoStreamWriterImpl;
import org.infinispan.protostream.impl.SerializationContextImpl;
import org.infinispan.protostream.impl.WrappedMessageMarshaller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author anistor@redhat.com
 * @since 1.0
 */
public final class ProtobufUtil {

   private static final String WRAPPING_DEFINITIONS_RES = "/org/infinispan/protostream/message-wrapping.proto";

   public static SerializationContext newSerializationContext(Configuration configuration) {
      SerializationContextImpl serializationContext = new SerializationContextImpl(configuration);

      try {
         serializationContext.registerProtoFiles(FileDescriptorSource.fromResources(WRAPPING_DEFINITIONS_RES));
      } catch (IOException | DescriptorParserException e) {
         throw new RuntimeException("Failed to initialize serialization context", e);
      }

      serializationContext.registerMarshaller(new WrappedMessageMarshaller());

      return serializationContext;
   }

   public static <A> void writeTo(SerializationContext ctx, CodedOutputStream out, A t) throws IOException {
      if (t == null) {
         throw new IllegalArgumentException("Object to marshall cannot be null");
      }
      ProtoStreamWriterImpl writer = new ProtoStreamWriterImpl((SerializationContextImpl) ctx);
      writer.write(out, t);
   }

   public static void writeTo(SerializationContext ctx, OutputStream out, Object t) throws IOException {
      writeTo(ctx, CodedOutputStream.newInstance(out), t);
   }

   public static byte[] toByteArray(SerializationContext ctx, Object t) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      writeTo(ctx, baos, t);
      return baos.toByteArray();
   }

   public static <A> A readFrom(SerializationContext ctx, CodedInputStream in, Class<A> clazz) throws IOException {
      ProtoStreamReaderImpl reader = new ProtoStreamReaderImpl((SerializationContextImpl) ctx);
      return reader.read(in, clazz);
   }

   public static <A> A readFrom(SerializationContext ctx, InputStream in, Class<A> clazz) throws IOException {
      return readFrom(ctx, CodedInputStream.newInstance(in), clazz);
   }

   public static <A> A fromByteArray(SerializationContext ctx, byte[] bytes, Class<A> clazz) throws IOException {
      return readFrom(ctx, CodedInputStream.newInstance(bytes), clazz);
   }

   public static <A> A fromByteArray(SerializationContext ctx, byte[] bytes, int offset, int length, Class<A> clazz) throws IOException {
      return readFrom(ctx, CodedInputStream.newInstance(bytes, offset, length), clazz);
   }

   /**
    * Parses a top-level message that was wrapped according to the org.infinispan.protostream.WrappedMessage proto
    * definition.
    *
    * @param ctx
    * @param bytes
    * @return
    * @throws IOException
    */
   public static Object fromWrappedByteArray(SerializationContext ctx, byte[] bytes) throws IOException {
      return fromWrappedByteArray(ctx, bytes, 0, bytes.length);
   }

   public static Object fromWrappedByteArray(SerializationContext ctx, byte[] bytes, int offset, int length) throws IOException {
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes, offset, length);
      return WrappedMessageMarshaller.readWrappedMessage(ctx, CodedInputStream.newInstance(bais));
   }

   public static byte[] toWrappedByteArray(SerializationContext ctx, Object t) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      WrappedMessageMarshaller.writeWrappedMessage(ctx, CodedOutputStream.newInstance(baos), t);
      return baos.toByteArray();
   }
}
