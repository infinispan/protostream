package org.infinispan.protostream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.impl.JsonUtils;
import org.infinispan.protostream.impl.SerializationContextImpl;
import org.infinispan.protostream.impl.TagReaderImpl;
import org.infinispan.protostream.impl.TagWriterImpl;
import org.infinispan.protostream.impl.parser.ProtostreamProtoParser;

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

   /**
    * Parses a set of .proto files but does not resolve type dependencies and does not detect semantic errors like
    * duplicate type definitions. If the {@link FileDescriptorSource} parameter does not include a progress callback
    * parsing will stop on first encountered error. If a callback exists all files will be processed; only one error per
    * file is reported and parsing will continue with the next file.
    *
    * @param configuration the {@link Configuration} to use
    * @param source the {@link FileDescriptorSource} containing the .proto files to parse
    * @return a map of successfully parsed {@link FileDescriptor} objects keyed by their names
    * @throws DescriptorParserException if parsing errors were encountered and no progress callback was specified in the
    *                                   {@link FileDescriptorSource}
    */
   public static Map<String, FileDescriptor> parseProtoFiles(Configuration configuration, FileDescriptorSource source) throws DescriptorParserException {
      return new ProtostreamProtoParser(configuration).parse(source);
   }

   public static <A> int computeMessageSize(ImmutableSerializationContext ctx, A t) throws IOException {
      TagWriter out = TagWriter.newInstance(ctx);
      write(ctx, out, t);
      return out.getWrittenBytes();
   }

   public static <A> int estimateSize(ImmutableSerializationContext ctx, A t) throws IOException {
      TagWriter out = TagWriter.newInstanceSizeEstimate(ctx);
      write(ctx, out, t);
      return out.getWrittenBytes();
   }

   public static <A> int computeWrappedMessageSize(ImmutableSerializationContext ctx, A t) throws IOException {
      TagWriter out = TagWriter.newInstance(ctx);
      WrappedMessage.write(ctx, out, t);
      return out.getWrittenBytes();
   }

   public static <A> int estimateMessageSize(ImmutableSerializationContext ctx, A t) throws IOException {
      TagWriter out = TagWriter.newInstanceSizeEstimate(ctx);
      WrappedMessage.write(ctx, out, t);
      return out.getWrittenBytes();
   }

   private static <A> void write(ImmutableSerializationContext ctx, TagWriter out, A t) throws IOException {
      Objects.requireNonNull(t, "Object to marshall cannot be null");
      BaseMarshallerDelegate<A> marshallerDelegate = ((SerializationContextImpl) ctx).getMarshallerDelegate(t);
      marshallerDelegate.marshall((ProtobufTagMarshaller.WriteContext) out, null, t);
      out.flush();
   }

   public static void writeTo(ImmutableSerializationContext ctx, RandomAccessOutputStream out, Object t) throws IOException {
      write(ctx, TagWriter.newInstance(ctx, out), t);
   }

   public static void writeTo(ImmutableSerializationContext ctx, OutputStream out, Object t) throws IOException {
      TagWriter writer = out instanceof RandomAccessOutputStream raos ? TagWriter.newInstance(ctx, raos) : TagWriter.newInstance(ctx, out);
      write(ctx, writer, t);
   }

   public static byte[] toByteArray(ImmutableSerializationContext ctx, Object t) throws IOException {
      RandomAccessOutputStream baos = RandomAccessOutputStream.withCapacity(DEFAULT_ARRAY_BUFFER_SIZE);
      writeTo(ctx, baos, t);
      return baos.toByteArray();
   }

   public static ByteBuffer toByteBuffer(ImmutableSerializationContext ctx, Object t) throws IOException {
      RandomAccessOutputStream baos = RandomAccessOutputStream.withCapacity(DEFAULT_ARRAY_BUFFER_SIZE);
      writeTo(ctx, baos, t);
      return baos.getByteBuffer();
   }

   private static <A> A readFrom(TagReader in, Class<A> clazz) throws IOException {
      if (clazz.isEnum()) {
         throw new IllegalArgumentException("The Class argument must not be an Enum");
      }
      TagReaderImpl inImpl = (TagReaderImpl) in;
      BaseMarshallerDelegate<A> marshallerDelegate = inImpl.getSerializationContext().getMarshallerDelegate(clazz);
      return marshallerDelegate.unmarshall(inImpl, null);
   }

   /**
    * @deprecated Please use {@link #readFrom(ImmutableSerializationContext, InputStream, int, Class)} instead
    * as we need to know how many bytes in the stream to read from
    */
   @Deprecated
   public static <A> A readFrom(ImmutableSerializationContext ctx, InputStream in, Class<A> clazz) throws IOException {
      return readFrom(TagReaderImpl.newInstance(ctx, in), clazz);
   }

   public static <A> A readFrom(ImmutableSerializationContext ctx, InputStream in, int length, Class<A> clazz) throws IOException {
      return readFrom(TagReader.newInstance(ctx, in, length), clazz);
   }

   public static <A> A fromByteArray(ImmutableSerializationContext ctx, byte[] bytes, Class<A> clazz) throws IOException {
      return readFrom(TagReader.newInstance(ctx, bytes), clazz);
   }

   //todo [anistor] what happens with remaining unconsumed trailing bytes after offset+length, here and in general? signal an error, a warning, or ignore?
   public static <A> A fromByteArray(ImmutableSerializationContext ctx, byte[] bytes, int offset, int length, Class<A> clazz) throws IOException {
      return readFrom(TagReader.newInstance(ctx, bytes, offset, length), clazz);
   }

   public static <A> A fromByteBuffer(ImmutableSerializationContext ctx, ByteBuffer byteBuffer, Class<A> clazz) throws IOException {
      return readFrom(TagReader.newInstance(ctx, byteBuffer), clazz);
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
      return WrappedMessage.read(ctx, TagReader.newInstance(ctx, bytes, offset, length));
   }

   public static <A> A fromWrappedByteBuffer(ImmutableSerializationContext ctx, ByteBuffer byteBuffer) throws IOException {
      return WrappedMessage.read(ctx, TagReader.newInstance(ctx, byteBuffer));
   }

   /**
    * @deprecated Please use {@link #fromWrappedStream(ImmutableSerializationContext, InputStream, int)} instead
    * as we need to know how many bytes in the stream to read from
    */
   @Deprecated
   public static <A> A fromWrappedStream(ImmutableSerializationContext ctx, InputStream in) throws IOException {
      return WrappedMessage.read(ctx, TagReaderImpl.newInstance(ctx, in));
   }

   public static <A> A fromWrappedStream(ImmutableSerializationContext ctx, InputStream in, int exactSize) throws IOException {
      return WrappedMessage.read(ctx, TagReader.newInstance(ctx, in, exactSize));
   }


   //todo [anistor] should make it possible to plug in a custom wrapping strategy instead of the default one
   public static byte[] toWrappedByteArray(ImmutableSerializationContext ctx, Object t) throws IOException {
      return toWrappedByteArray(ctx, t, DEFAULT_ARRAY_BUFFER_SIZE);
   }

   public static byte[] toWrappedByteArray(ImmutableSerializationContext ctx, Object t, int bufferSize) throws IOException {
      RandomAccessOutputStream baos = RandomAccessOutputStream.withCapacity(bufferSize);
      WrappedMessage.write(ctx, TagWriter.newInstance(ctx, baos), t);
      return baos.toByteArray();
   }

   public static ByteBuffer toWrappedByteBuffer(ImmutableSerializationContext ctx, Object t) throws IOException {
      RandomAccessOutputStream baos = RandomAccessOutputStream.withCapacity(DEFAULT_ARRAY_BUFFER_SIZE);
      WrappedMessage.write(ctx, TagWriter.newInstance(ctx, baos), t);
      return baos.getByteBuffer();
   }

   public static void toWrappedStream(ImmutableSerializationContext ctx, RandomAccessOutputStream out, Object t) throws IOException {
      WrappedMessage.write(ctx, TagWriter.newInstance(ctx, out), t);
   }

   public static void toWrappedStream(ImmutableSerializationContext ctx, OutputStream out, Object t) throws IOException {
      TagWriter writer = out instanceof RandomAccessOutputStream raos ? TagWriter.newInstance(ctx, raos) : TagWriter.newInstance(ctx, out);
      WrappedMessage.write(ctx, writer, t);
   }

   /**
    * The parts of a message wrapped according to the {@link WrappedMessage} proto definition.
    *
    * @param typeName the fully qualified type name ({@code WrappedMessage.WRAPPED_TYPE_NAME}), or {@code null}
    * @param typeId   the numeric type id ({@code WrappedMessage.WRAPPED_TYPE_ID}), or {@code null}
    * @param message  the raw (unwrapped) message bytes ({@code WrappedMessage.WRAPPED_MESSAGE}), or {@code null}
    * @since 6.0.14
    */
   public record WrappedMessageParts(String typeName, Integer typeId, byte[] message) {
   }

   /**
    * Splits a wrapped message into its parts, without decoding the inner message.
    *
    * @param wrapped the wrapped message bytes
    * @return the type name, type id and inner message bytes (each possibly {@code null})
    * @throws IOException if the wrapped bytes are malformed
    * @since 6.0.14
    */
   public static WrappedMessageParts splitWrappedMessage(byte[] wrapped) throws IOException {
      Objects.requireNonNull(wrapped, "wrapped cannot be null");
      String typeName = null;
      Integer typeId = null;
      byte[] message = null;
      int pos = 0;
      int end = wrapped.length;
      while (pos < end) {
         long tag = readEnvelopeVarint(wrapped, pos, end);
         pos += envelopeVarintSize(wrapped, pos);
         int fieldNumber = (int) (tag >>> 3);
         int wireType = (int) (tag & 7);
         switch (wireType) {
            case 0 -> {
               long value = readEnvelopeVarint(wrapped, pos, end);
               pos += envelopeVarintSize(wrapped, pos);
               if (fieldNumber == WrappedMessage.WRAPPED_TYPE_ID) {
                  typeId = (int) value;
               }
            }
            case 2 -> {
               long length = readEnvelopeVarint(wrapped, pos, end);
               pos += envelopeVarintSize(wrapped, pos);
               if (length < 0 || length > end - pos) {
                  throw new MalformedProtobufException("Truncated wrapped message");
               }
               if (fieldNumber == WrappedMessage.WRAPPED_TYPE_NAME) {
                  typeName = new String(wrapped, pos, (int) length, StandardCharsets.UTF_8);
               } else if (fieldNumber == WrappedMessage.WRAPPED_MESSAGE) {
                  message = Arrays.copyOfRange(wrapped, pos, pos + (int) length);
               }
               pos += (int) length;
            }
            case 1 -> {
               if (end - pos < 8) {
                  throw new MalformedProtobufException("Truncated wrapped message");
               }
               pos += 8;
            }
            case 5 -> {
               if (end - pos < 4) {
                  throw new MalformedProtobufException("Truncated wrapped message");
               }
               pos += 4;
            }
            case 3 -> {
               int depth = 1;
               int[] openGroups = new int[8];
               openGroups[0] = fieldNumber;
               while (depth > 0) {
                  if (pos >= end) {
                     throw new MalformedProtobufException("Unterminated group in wrapped message");
                  }
                  long gtag = readEnvelopeVarint(wrapped, pos, end);
                  pos += envelopeVarintSize(wrapped, pos);
                  int wt = (int) (gtag & 7);
                  if (wt == 3) {
                     if (depth == openGroups.length) {
                        openGroups = Arrays.copyOf(openGroups, openGroups.length * 2);
                     }
                     openGroups[depth] = (int) (gtag >>> 3);
                     depth++;
                  } else if (wt == 4) {
                     depth--;
                     if ((int) (gtag >>> 3) != openGroups[depth]) {
                        throw new MalformedProtobufException("Mismatched END_GROUP tag in wrapped message");
                     }
                  } else {
                     switch (wt) {
                        case 0 -> {
                           long v = readEnvelopeVarint(wrapped, pos, end);
                           pos += envelopeVarintSize(wrapped, pos);
                        }
                        case 1 -> {
                           if (end - pos < 8) {
                              throw new MalformedProtobufException("Truncated wrapped message");
                           }
                           pos += 8;
                        }
                        case 5 -> {
                           if (end - pos < 4) {
                              throw new MalformedProtobufException("Truncated wrapped message");
                           }
                           pos += 4;
                        }
                        case 2 -> {
                           long l = readEnvelopeVarint(wrapped, pos, end);
                           pos += envelopeVarintSize(wrapped, pos);
                           if (l < 0 || l > end - pos) {
                              throw new MalformedProtobufException("Truncated wrapped message");
                           }
                           pos += (int) l;
                        }
                        default -> throw new MalformedProtobufException("Invalid wire type in wrapped message");
                     }
                  }
               }
            }
            default -> throw new MalformedProtobufException("Invalid wire type in wrapped message");
         }
      }
      return new WrappedMessageParts(typeName, typeId, message);
   }

   /**
    * Joins a wrapped message from its parts, mirroring the {@link WrappedMessage} envelope layout.
    * The type name and type id are mutually exclusive, exactly as in {@link WrappedMessage#write}:
    * supplying both is rejected.
    *
    * @param typeName the fully qualified type name, or {@code null}
    * @param typeId   the numeric type id, or {@code null}
    * @param message  the raw (unwrapped) message bytes, or {@code null}
    * @return the wrapped message bytes
    * @throws IllegalArgumentException if both {@code typeName} and {@code typeId} are supplied
    * @throws IOException if the join fails
    * @since 6.0.14
    */
   public static byte[] joinWrappedMessage(String typeName, Integer typeId, byte[] message) throws IOException {
      if (typeName != null && typeId != null) {
         throw new IllegalArgumentException("typeName and typeId are mutually exclusive");
      }
      RandomAccessOutputStream baos = RandomAccessOutputStream.withCapacity((message == null ? 0 : message.length) + 20);
      TagWriter writer = TagWriter.newInstance(null, baos);
      if (typeId != null) {
         writer.writeUInt32(WrappedMessage.WRAPPED_TYPE_ID, typeId);
      } else if (typeName != null) {
         writer.writeString(WrappedMessage.WRAPPED_TYPE_NAME, typeName);
      }
      if (message != null) {
         writer.writeBytes(WrappedMessage.WRAPPED_MESSAGE, message);
      }
      writer.flush();
      return baos.toByteArray();
   }

   private static long readEnvelopeVarint(byte[] buf, int pos, int end) throws IOException {
      long value = 0;
      for (int i = 0; i < 64; i += 7) {
         if (pos >= end) {
            throw new MalformedProtobufException("Truncated wrapped message");
         }
         byte b = buf[pos++];
         value |= (long) (b & 0x7F) << i;
         if (b >= 0) {
            return value;
         }
      }
      throw new MalformedProtobufException("Malformed varint in wrapped message");
   }

   /**
    * Returns the encoded size of the varint located at the given position (recoverable from its
    * continuation bits).
    */
   private static int envelopeVarintSize(byte[] buf, int pos) {
      int size = 0;
      while ((buf[pos + size] & 0x80) != 0) {
         size++;
      }
      return size + 1;
   }

   /**
    * @deprecated since 5.0.10 Please use {@link #toWrappedStream(ImmutableSerializationContext, OutputStream, Object)} with a {@link java.io.BufferedOutputStream} instead
    */
   @Deprecated
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
