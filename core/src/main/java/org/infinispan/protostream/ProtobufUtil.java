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
import org.infinispan.protostream.impl.RawProtoStreamReaderImpl;
import org.infinispan.protostream.impl.RawProtoStreamWriterImpl;
import org.infinispan.protostream.impl.SerializationContextImpl;

import com.google.protobuf.CodedOutputStream;

/**
 * This is the entry point to the ProtoStream library. This class provides methods to write and read Java objects
 * to/from a Protobuf encoded data stream. Also provides conversion to and from canonical JSON.
 *
 * @author anistor@redhat.com
 * @since 1.0
 */
public final class ProtobufUtil {

   private static final int BUFFER_SIZE = 512;

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

      serializationContext.registerMarshaller(new WrappedMessage.Marshaller());

      return serializationContext;
   }

   private static <A> void writeTo(ImmutableSerializationContext ctx, RawProtoStreamWriter out, A t) throws IOException {
      if (t == null) {
         throw new IllegalArgumentException("Object to marshall cannot be null");
      }
      BaseMarshallerDelegate marshallerDelegate = ((SerializationContextImpl) ctx).getMarshallerDelegate(t);
      marshallerDelegate.marshall(null, t, null, out);
      out.flush();
   }

   public static void writeTo(ImmutableSerializationContext ctx, OutputStream out, Object t) throws IOException {
      writeTo(ctx, RawProtoStreamWriterImpl.newInstance(out), t);
   }

   public static byte[] toByteArray(ImmutableSerializationContext ctx, Object t) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE);
      writeTo(ctx, baos, t);
      return baos.toByteArray();
   }

   public static ByteBuffer toByteBuffer(ImmutableSerializationContext ctx, Object t) throws IOException {
      ByteArrayOutputStreamEx baos = new ByteArrayOutputStreamEx(BUFFER_SIZE);
      writeTo(ctx, baos, t);
      return baos.getByteBuffer();
   }

   private static <A> A readFrom(ImmutableSerializationContext ctx, RawProtoStreamReader in, Class<A> clazz) throws IOException {
      if (clazz.isEnum()) {
         throw new IllegalArgumentException("The Class argument must not be an Enum");
      }
      BaseMarshallerDelegate<A> marshallerDelegate = ((SerializationContextImpl) ctx).getMarshallerDelegate(clazz);
      return marshallerDelegate.unmarshall(null, null, in);
   }

   public static <A> A readFrom(ImmutableSerializationContext ctx, InputStream in, Class<A> clazz) throws IOException {
      return readFrom(ctx, RawProtoStreamReaderImpl.newInstance(in), clazz);
   }

   public static <A> A fromByteArray(ImmutableSerializationContext ctx, byte[] bytes, Class<A> clazz) throws IOException {
      return readFrom(ctx, RawProtoStreamReaderImpl.newInstance(bytes), clazz);
   }

   //todo [anistor] what happens with remaining unconsumed trailing bytes after offset+length, here and in general? signal an error, a warning, or ignore?
   public static <A> A fromByteArray(ImmutableSerializationContext ctx, byte[] bytes, int offset, int length, Class<A> clazz) throws IOException {
      return readFrom(ctx, RawProtoStreamReaderImpl.newInstance(bytes, offset, length), clazz);
   }

   public static <A> A fromByteBuffer(ImmutableSerializationContext ctx, ByteBuffer byteBuffer, Class<A> clazz) throws IOException {
      return readFrom(ctx, RawProtoStreamReaderImpl.newInstance(byteBuffer), clazz);
   }

   /**
    * Parses a top-level message that was wrapped according to the org.infinispan.protostream.WrappedMessage proto
    * definition.
    *
    * @param ctx the serialization context
    * @param bytes the array of bytes to parse
    * @return the unwrapped object
    * @throws IOException in case parsing fails
    */
   public static <A> A fromWrappedByteArray(ImmutableSerializationContext ctx, byte[] bytes) throws IOException {
      return fromWrappedByteArray(ctx, bytes, 0, bytes.length);
   }

   public static <A> A fromWrappedByteArray(ImmutableSerializationContext ctx, byte[] bytes, int offset, int length) throws IOException {
      return WrappedMessage.readMessage(ctx, RawProtoStreamReaderImpl.newInstance(bytes, offset, length));
   }

   public static <A> A fromWrappedByteBuffer(ImmutableSerializationContext ctx, ByteBuffer byteBuffer) throws IOException {
      return WrappedMessage.readMessage(ctx, RawProtoStreamReaderImpl.newInstance(byteBuffer));
   }

   public static <A> A fromWrappedStream(ImmutableSerializationContext ctx, InputStream in) throws IOException {
      return WrappedMessage.readMessage(ctx, RawProtoStreamReaderImpl.newInstance(in));
   }

   //todo [anistor] should make it possible to plug in a custom wrapping strategy instead of the default one
   public static byte[] toWrappedByteArray(ImmutableSerializationContext ctx, Object t) throws IOException {
      return toWrappedByteArray(ctx, t, BUFFER_SIZE);
   }

   public static byte[] toWrappedByteArray(ImmutableSerializationContext ctx, Object t, int bufferSize) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(bufferSize);
      WrappedMessage.writeMessage(ctx, RawProtoStreamWriterImpl.newInstance(baos), t);
      return baos.toByteArray();
   }

   public static ByteBuffer toWrappedByteBuffer(ImmutableSerializationContext ctx, Object t) throws IOException {
      ByteArrayOutputStreamEx baos = new ByteArrayOutputStreamEx(BUFFER_SIZE);
      WrappedMessage.writeMessage(ctx, RawProtoStreamWriterImpl.newInstance(baos), t);
      return baos.getByteBuffer();
   }

   public static void toWrappedStream(ImmutableSerializationContext ctx, OutputStream out, Object t) throws IOException {
      toWrappedStream(ctx, out, t, CodedOutputStream.DEFAULT_BUFFER_SIZE);
   }

   public static void toWrappedStream(ImmutableSerializationContext ctx, OutputStream out, Object t, int bufferSize) throws IOException {
      WrappedMessage.writeMessage(ctx, RawProtoStreamWriterImpl.newInstance(out, bufferSize), t);
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
      return toCanonicalJSON(ctx, bytes, true);
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
