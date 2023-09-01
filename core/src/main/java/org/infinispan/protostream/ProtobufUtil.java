package org.infinispan.protostream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.ByteBuffer;

import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.impl.BaseMarshallerDelegate;
import org.infinispan.protostream.impl.ByteArrayOutputStreamEx;
import org.infinispan.protostream.impl.JsonUtils;
import org.infinispan.protostream.impl.SerializationContextImpl;
import org.infinispan.protostream.impl.TagReaderImpl;
import org.infinispan.protostream.impl.TagWriterImpl;

/**
 * This is the entry point to the ProtoStream library. This class provides methods to write and read Java objects
 * to/from a Protobuf encoded data stream. Also provides conversion to and from canonical JSON.
 *
 * @author anistor@redhat.com
 * @since 1.0
 */
public final class ProtobufUtil {

   public static final int DEFAULT_STREAM_BUFFER_SIZE = 4096;

   public static final int DEFAULT_ARRAY_BUFFER_SIZE = 512;

   private ProtobufUtil() {
   }

   public static SerializationContext newSerializationContext() {
      return newSerializationContext(Configuration.builder().build());
   }

   public static SerializationContext newSerializationContext(Configuration configuration) {
      SerializationContextImpl serializationContext = new SerializationContextImpl(configuration);

      try {
         // always register message-wrapping.proto
         serializationContext.registerProtoFiles(FileDescriptorSource.fromResources(WrappedMessage.PROTO_FILE));
      } catch (IOException | DescriptorParserException e) {
         throw new RuntimeException("Failed to initialize serialization context", e);
      }

      serializationContext.registerMarshaller(WrappedMessage.MARSHALLER);

      return serializationContext;
   }

   public static <A> int computeMessageSize(ImmutableSerializationContext ctx, A t) throws IOException {
      TagWriterImpl out = TagWriterImpl.newInstance(ctx);
      write(ctx, out, t);
      return out.getWrittenBytes();
   }

   public static <A> int computeWrappedMessageSize(ImmutableSerializationContext ctx, A t) throws IOException {
      TagWriterImpl out = TagWriterImpl.newInstance(ctx);
      WrappedMessage.write(ctx, out, t);
      return out.getWrittenBytes();
   }

   private static <A> void write(ImmutableSerializationContext ctx, TagWriterImpl out, A t) throws IOException {
      if (t == null) {
         throw new IllegalArgumentException("Object to marshall cannot be null");
      }
      BaseMarshallerDelegate marshallerDelegate = ((SerializationContextImpl) ctx).getMarshallerDelegate(t);
      marshallerDelegate.marshall(out, null, t);
      out.flush();
   }

   public static void writeTo(ImmutableSerializationContext ctx, OutputStream out, Object t) throws IOException {
      write(ctx, TagWriterImpl.newInstance(ctx, out), t);
   }

   public static byte[] toByteArray(ImmutableSerializationContext ctx, Object t) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(DEFAULT_ARRAY_BUFFER_SIZE);
      writeTo(ctx, baos, t);
      return baos.toByteArray();
   }

   public static ByteBuffer toByteBuffer(ImmutableSerializationContext ctx, Object t) throws IOException {
      ByteArrayOutputStreamEx baos = new ByteArrayOutputStreamEx(DEFAULT_ARRAY_BUFFER_SIZE);
      writeTo(ctx, baos, t);
      return baos.getByteBuffer();
   }

   private static <A> A readFrom(TagReaderImpl in, Class<A> clazz) throws IOException {
      if (clazz.isEnum()) {
         throw new IllegalArgumentException("The Class argument must not be an Enum");
      }
      BaseMarshallerDelegate<A> marshallerDelegate = in.getSerializationContext().getMarshallerDelegate(clazz);
      return marshallerDelegate.unmarshall(in, null);
   }

   public static <A> A readFrom(ImmutableSerializationContext ctx, InputStream in, Class<A> clazz) throws IOException {
      return readFrom(TagReaderImpl.newInstance(ctx, in), clazz);
   }

   public static <A> A fromByteArray(ImmutableSerializationContext ctx, byte[] bytes, Class<A> clazz) throws IOException {
      return readFrom(TagReaderImpl.newInstance(ctx, bytes), clazz);
   }

   //todo [anistor] what happens with remaining unconsumed trailing bytes after offset+length, here and in general? signal an error, a warning, or ignore?
   public static <A> A fromByteArray(ImmutableSerializationContext ctx, byte[] bytes, int offset, int length, Class<A> clazz) throws IOException {
      return readFrom(TagReaderImpl.newInstance(ctx, bytes, offset, length), clazz);
   }

   public static <A> A fromByteBuffer(ImmutableSerializationContext ctx, ByteBuffer byteBuffer, Class<A> clazz) throws IOException {
      return readFrom(TagReaderImpl.newInstance(ctx, byteBuffer), clazz);
   }

   /**
    * Parses a top-level message that was wrapped according to the org.infinispan.protostream.WrappedMessage proto
    * definition.
    *
    * @param ctx   the serialization context
    * @param bytes the array of bytes to parse
    * @return the unwrapped object
    * @throws IOException in case parsing fails
    */
   public static <A> A fromWrappedByteArray(ImmutableSerializationContext ctx, byte[] bytes) throws IOException {
      return fromWrappedByteArray(ctx, bytes, 0, bytes.length);
   }

   public static <A> A fromWrappedByteArray(ImmutableSerializationContext ctx, byte[] bytes, int offset, int length) throws IOException {
      return WrappedMessage.read(ctx, TagReaderImpl.newInstance(ctx, bytes, offset, length));
   }

   public static <A> A fromWrappedByteBuffer(ImmutableSerializationContext ctx, ByteBuffer byteBuffer) throws IOException {
      return WrappedMessage.read(ctx, TagReaderImpl.newInstance(ctx, byteBuffer));
   }

   public static <A> A fromWrappedStream(ImmutableSerializationContext ctx, InputStream in) throws IOException {
      return WrappedMessage.read(ctx, TagReaderImpl.newInstance(ctx, in));
   }

   //todo [anistor] should make it possible to plug in a custom wrapping strategy instead of the default one
   public static byte[] toWrappedByteArray(ImmutableSerializationContext ctx, Object t) throws IOException {
      return toWrappedByteArray(ctx, t, DEFAULT_ARRAY_BUFFER_SIZE);
   }

   public static byte[] toWrappedByteArray(ImmutableSerializationContext ctx, Object t, int bufferSize) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStreamEx(bufferSize);
      WrappedMessage.write(ctx, TagWriterImpl.newInstanceNoBuffer(ctx, baos), t);
      return baos.toByteArray();
   }

   public static ByteBuffer toWrappedByteBuffer(ImmutableSerializationContext ctx, Object t) throws IOException {
      ByteArrayOutputStreamEx baos = new ByteArrayOutputStreamEx(DEFAULT_ARRAY_BUFFER_SIZE);
      WrappedMessage.write(ctx, TagWriterImpl.newInstanceNoBuffer(ctx, baos), t);
      return baos.getByteBuffer();
   }

   public static void toWrappedStream(ImmutableSerializationContext ctx, OutputStream out, Object t) throws IOException {
      WrappedMessage.write(ctx, TagWriterImpl.newInstance(ctx, out), t);
   }

   public static void toWrappedStream(ImmutableSerializationContext ctx, OutputStream out, Object t, int bufferSize) throws IOException {
      WrappedMessage.write(ctx, TagWriterImpl.newInstance(ctx, out, bufferSize), t);
   }

   /**
    * Converts a Protobuf encoded message to its <a href="https://developers.google.com/protocol-buffers/docs/proto3#json">
    * canonical JSON representation</a>.
    *
    * @param ctx   the serialization context
    * @param bytes the Protobuf encoded message bytes to parse
    * @return the JSON string representation
    * @throws IOException if I/O operations fail
    */
   public static String toCanonicalJSON(ImmutableSerializationContext ctx, byte[] bytes) throws IOException {
      return toCanonicalJSON(ctx, bytes, false);
   }

   /**
    * Converts a Protobuf encoded message to its <a href="https://developers.google.com/protocol-buffers/docs/proto3#json">
    * canonical JSON representation</a>.
    *
    * @param ctx         the serialization context
    * @param bytes       the Protobuf encoded message bytes to parse
    * @param prettyPrint indicates if the JSON output should use a 'pretty' human-readable format or a compact format
    * @return the JSON string representation
    * @throws IOException if I/O operations fail
    */
   public static String toCanonicalJSON(ImmutableSerializationContext ctx, byte[] bytes, boolean prettyPrint) throws IOException {
      return JsonUtils.toCanonicalJSON(ctx, bytes, prettyPrint);
   }

   public static byte[] fromCanonicalJSON(ImmutableSerializationContext ctx, Reader reader) throws IOException {
      return JsonUtils.fromCanonicalJSON(ctx, reader);
   }
}
