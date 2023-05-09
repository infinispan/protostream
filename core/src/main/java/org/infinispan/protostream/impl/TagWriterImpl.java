package org.infinispan.protostream.impl;

import static org.infinispan.protostream.descriptors.WireType.FIXED_32_SIZE;
import static org.infinispan.protostream.descriptors.WireType.FIXED_64_SIZE;
import static org.infinispan.protostream.descriptors.WireType.MAX_VARINT_SIZE;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.infinispan.protostream.Encoder;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufTagMarshaller;
import org.infinispan.protostream.TagWriter;
import org.infinispan.protostream.descriptors.WireType;


/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public final class TagWriterImpl implements TagWriter, ProtobufTagMarshaller.WriteContext {

   private static final Log log = Log.LogFactory.getLog(TagWriterImpl.class);

   private final SerializationContextImpl serCtx;

   // all writes are delegated to a lower level protocol encoder
   private final Encoder encoder;

   private final TagWriterImpl parent;

   private final int depth;

   // lazily initialized
   private Map<Object, Object> params = null;

   // lazily initialized
   @Deprecated
   private ProtoStreamWriterImpl writer = null;

   private TagWriterImpl(TagWriterImpl parent, Encoder encoder) {
      this.parent = parent;
      this.depth = parent.depth + 1;
      this.serCtx = parent.serCtx;
      this.encoder = encoder;
   }

   private TagWriterImpl(SerializationContextImpl serCtx, Encoder encoder) {
      this.parent = null;
      this.depth = 0;
      this.serCtx = serCtx;
      this.encoder = encoder;
   }

   public static TagWriterImpl newNestedInstance(ProtobufTagMarshaller.WriteContext parent, OutputStream output) {
      return new TagWriterImpl((TagWriterImpl) parent, new OutputStreamNoBufferEncoder(output));
   }

   public static TagWriterImpl newNestedInstance(ProtobufTagMarshaller.WriteContext parent, byte[] buf) {
      return new TagWriterImpl((TagWriterImpl) parent, new ByteArrayEncoder(buf, 0, buf.length));
   }

   public static TagWriterImpl newInstance(ImmutableSerializationContext serCtx, OutputStream output) {
      return newInstance(serCtx, new OutputStreamNoBufferEncoder(output));
   }

   /**
    * @deprecated since 4.6.3 Please use {@link #newInstance(ImmutableSerializationContext, OutputStream)} with a {@link java.io.BufferedOutputStream instead}
    */
   @Deprecated
   public static TagWriterImpl newInstance(ImmutableSerializationContext serCtx, OutputStream output, int bufferSize) {
      return newInstance(serCtx, new OutputStreamEncoder(output, bufferSize));
   }

   public static TagWriterImpl newInstance(ImmutableSerializationContext serCtx, byte[] buf) {
      return newInstance(serCtx, new ByteArrayEncoder(buf, 0, buf.length));
   }

   public static TagWriterImpl newInstance(ImmutableSerializationContext serCtx, byte[] buf, int offset, int length) {
      return newInstance(serCtx, new ByteArrayEncoder(buf, offset, length));
   }

   public static TagWriterImpl newInstance(ImmutableSerializationContext serCtx, ByteBuffer byteBuffer) {
      Encoder encoder = byteBuffer.hasArray() ? new HeapByteBufferEncoder(byteBuffer) : new ByteBufferEncoder(byteBuffer);
      return newInstance(serCtx, encoder);
   }

   public static TagWriterImpl newInstance(ImmutableSerializationContext serCtx) {
      return newInstance(serCtx, new NoOpEncoder());
   }

   public static TagWriterImpl newInstanceNoBuffer(ImmutableSerializationContext ctx, OutputStream out) {
      return newInstance(ctx, new OutputStreamNoBufferEncoder(out));
   }

   public static TagWriterImpl newInstance(ImmutableSerializationContext serCtx, Encoder encoder) {
      return new TagWriterImpl((SerializationContextImpl) serCtx, encoder);
   }

   public int getWrittenBytes() {
      // the CCE here will signal misuse; let it happen
      return ((NoOpEncoder) encoder).getWrittenBytes();
   }

   @Override
   public void flush() throws IOException {
      encoder.flush();
   }

   @Override
   public void close() throws IOException {
      encoder.close();
   }

   @Override
   public void writeVarint32(int value) throws IOException {
      encoder.writeVarint32(value);
   }

   @Override
   public void writeVarint64(long value) throws IOException {
      encoder.writeVarint64(value);
   }

   @Override
   public void writeString(int number, String value) throws IOException {
      // TODO [anistor] This is expensive! What can we do to make it more efficient?
      // Also, when just count bytes for message size we do a useless first conversion, and another one will follow later.

      // Charset.encode is not able to encode directly into our own buffers!
      byte[] utf8buffer = value.getBytes(StandardCharsets.UTF_8);

      encoder.writeLengthDelimitedField(number, utf8buffer.length);
      encoder.writeBytes(utf8buffer, 0, utf8buffer.length);
   }

   @Override
   public void writeUInt32(int number, int value) throws IOException {
      encoder.writeUInt32Field(number, value);
   }

   @Override
   public void writeFixed32(int number, int value) throws IOException {
      encoder.writeFixed32Field(number, value);
   }

   @Override
   public void writeInt64(int number, long value) throws IOException {
      encoder.writeUInt64Field(number, value);
   }

   @Override
   public void writeUInt64(int number, long value) throws IOException {
      encoder.writeUInt64Field(number, value);
   }

   @Override
   public void writeFixed64(int number, long value) throws IOException {
      encoder.writeFixed64Field(number, value);
   }

   @Override
   public void writeBool(int number, boolean value) throws IOException {
      encoder.writeBoolField(number, value);
   }

   @Override
   public void writeBytes(int number, ByteBuffer value) throws IOException {
      encoder.writeLengthDelimitedField(number, value.remaining());
      encoder.writeBytes(value);
   }

   @Override
   public void writeBytes(int number, byte[] value, int offset, int length) throws IOException {
      encoder.writeLengthDelimitedField(number, length);
      encoder.writeBytes(value, offset, length);
   }

   @Override
   public ProtobufTagMarshaller.WriteContext subWriter(int number, boolean nested) throws IOException {
      if (encoder.supportsFixedVarint()) {
         writeVarint32(WireType.makeTag(number, WireType.WIRETYPE_LENGTH_DELIMITED));
         return nested ? new TagWriterImpl(this, new FixedVarintWrappedEncoder(encoder)) :
               new TagWriterImpl(serCtx, new FixedVarintWrappedEncoder(encoder));
      }
      // This ensures we aren't allocating a byte[] larger than we actually need
      int space = bytesAvailableForVariableEncoding(encoder.remainingSpace());
      return nested ? new TagWriterImpl(this, new ArrayBasedWrappedEncoder(space, encoder, number)) :
            new TagWriterImpl(serCtx, new ArrayBasedWrappedEncoder(space, encoder, number));
   }

   // Returns how many bytes are usable for data from a given range of bytes when inserting a variable int before
   // the actual data
   private static int bytesAvailableForVariableEncoding(int spaceAllowed) {
      if (spaceAllowed < 128) {
         return spaceAllowed - 1;
      } else if (spaceAllowed < 16384) {
         return spaceAllowed - 2;
      } else if (spaceAllowed < 2097151) {
         return spaceAllowed - 3;
      }
      return spaceAllowed - (spaceAllowed < 268435455 ? 4 : 5);
   }

   @Override
   public void writeRawByte(byte value) throws IOException {
      encoder.writeByte(value);
   }

   @Override
   public void writeRawBytes(byte[] value, int offset, int length) throws IOException {
      encoder.writeBytes(value, offset, length);
   }

   @Override
   public void writeRawBytes(ByteBuffer value) throws IOException {
      encoder.writeBytes(value);
   }

   @Override
   public SerializationContextImpl getSerializationContext() {
      return serCtx;
   }

   @Override
   public Object getParam(Object key) {
      if (parent != null) {
         return parent.getParam(key);
      } else {
         if (params == null) {
            return null;
         }
         return params.get(key);
      }
   }

   @Override
   public void setParam(Object key, Object value) {
      if (parent != null) {
         parent.setParam(key, value);
      } else {
         if (params == null) {
            params = new HashMap<>();
         }
         params.put(key, value);
      }
   }

   @Override
   public TagWriter getWriter() {
      return this;
   }

   @Override
   public int depth() {
      return depth;
   }

   /**
    * @deprecated this will be removed in 5.0 together with {@link org.infinispan.protostream.MessageMarshaller}
    */
   @Deprecated
   public ProtoStreamWriterImpl getProtoStreamWriter() {
      if (parent != null) {
         return parent.getProtoStreamWriter();
      }
      if (writer == null) {
         writer = new ProtoStreamWriterImpl(this, serCtx);
      }
      return writer;
   }

   //todo [anistor] need to provide a safety mechanism to limit message size in bytes and message nesting depth on write ops
   public abstract static class EncoderImpl implements Encoder {

      /**
       * Commits the witten bytes after several write operations were performed. Updates counters, positions, whatever.
       */
      @Override
      public void flush() throws IOException {
      }

      @Override
      public void close() throws IOException {
         flush();
      }

      @Override
      public int remainingSpace() {
         return Integer.MAX_VALUE;
      }

      // high level ops, writing fields

      @Override
      public void writeUInt32Field(int fieldNumber, int value) throws IOException {
         writeVarint32(WireType.makeTag(fieldNumber, WireType.WIRETYPE_VARINT));
         writeVarint32(value);
      }

      @Override
      public void writeUInt64Field(int fieldNumber, long value) throws IOException {
         writeVarint32(WireType.makeTag(fieldNumber, WireType.WIRETYPE_VARINT));
         writeVarint64(value);
      }

      @Override
      public void writeFixed32Field(int fieldNumber, int value) throws IOException {
         writeVarint32(WireType.makeTag(fieldNumber, WireType.WIRETYPE_FIXED32));
         writeFixed32(value);
      }

      @Override
      public void writeFixed64Field(int fieldNumber, long value) throws IOException {
         writeVarint32(WireType.makeTag(fieldNumber, WireType.WIRETYPE_FIXED64));
         writeFixed64(value);
      }

      @Override
      public void writeBoolField(int fieldNumber, boolean value) throws IOException {
         writeVarint32(WireType.makeTag(fieldNumber, WireType.WIRETYPE_VARINT));
         writeByte((byte) (value ? 1 : 0));
      }

      @Override
      public void writeLengthDelimitedField(int fieldNumber, int length) throws IOException {
         writeVarint32(WireType.makeTag(fieldNumber, WireType.WIRETYPE_LENGTH_DELIMITED));
         writeVarint32(length);
      }

      // low level ops, writing values without tag

      @Override
      public boolean supportsFixedVarint() {
         return false;
      }
   }

   /**
    * An encoder that just counts the bytes and does not write anything and does not allocate buffers.
    * Useful for computing message size.
    */
   private static final class NoOpEncoder extends EncoderImpl {

      private int count = 0;

      int getWrittenBytes() {
         return count;
      }

      /**
       * Resets the written bytes counter. Needed if we intend to reuse this to count the size of another message.
       */
      void reset() {
         count = 0;
      }

      @Override
      public void writeByte(byte value) {
         count++;
      }

      @Override
      public void writeBytes(byte[] value, int offset, int length) {
         count += length;
      }

      @Override
      public void writeBytes(ByteBuffer value) {
         count += value.remaining();
      }

      @Override
      public void writeVarint32(int value) {
         while (true) {
            count++;
            if ((value & 0xFFFFFF80) == 0) {
               break;
            }
            value >>>= 7;
         }
      }

      @Override
      public void writeVarint64(long value) {
         while (true) {
            count++;
            if ((value & 0xFFFFFFFFFFFFFF80L) == 0) {
               break;
            }
            value >>>= 7;
         }
      }

      @Override
      public void writeFixed32(int value) {
         count += FIXED_32_SIZE;
      }

      @Override
      public void writeFixed64(long value) {
         count += FIXED_64_SIZE;
      }

      @Override
      public int skipFixedVarint() {
         count += 5;
         return -1;
      }

      @Override
      public void writePositiveFixedVarint(int pos) {
         // Do nothing
      }

      @Override
      public boolean supportsFixedVarint() {
         return true;
      }
   }

   /**
    * Writes to a user provided byte array.
    */
   private static class ByteArrayEncoder extends EncoderImpl {

      private final byte[] array;

      protected final int offset;

      protected final int limit;

      public int pos;

      private ByteArrayEncoder(byte[] array, int offset, int length) {
         if (array == null) {
            throw new IllegalArgumentException("array cannot be null");
         }
         if (offset < 0) {
            throw new IllegalArgumentException("offset cannot be negative");
         }
         if (length < 0) {
            throw new IllegalArgumentException("length cannot be negative");
         }
         if (offset >= array.length) {
            throw new IllegalArgumentException("start position is outside array bounds");
         }
         if (offset + length > array.length) {
            throw new IllegalArgumentException("end position is outside array bounds");
         }
         this.array = array;
         this.offset = offset;
         this.limit = offset + length;
         this.pos = offset;
      }

      @Override
      public final int remainingSpace() {
         return limit - pos;
      }

      /**
       * Make room for the required space by flushing the entire buffer to stream if the available space is not enough.
       */
      public final void flushToStream(OutputStream out, int requiredSpace) throws IOException {
         if (requiredSpace > limit - pos) {
            out.write(array, 0, pos);
            pos = 0;
         }
      }

      /**
       * Flush the entire buffer to an output stream.
       */
      public final void flushToStream(OutputStream out) throws IOException {
         if (pos > 0) {
            out.write(array, 0, pos);
            pos = 0;
         }
      }

      /**
       * Copies from {@code byteBuffer} buffer to our internal buffer the maximum bytes allowed by the available space
       * and the remaining bytes. Multiple invocations preceded by a {@link #flushToStream(OutputStream)} invocation are
       * needed if the {@code byteBuffer} buffer still has remaining bytes after this call.
       */
      protected final void fillFromBuffer(ByteBuffer byteBuffer) {
         int length = byteBuffer.remaining();
         int free = array.length - pos;
         if (free >= length) {
            byteBuffer.get(array, pos, length);
            pos += length;
         } else {
            byteBuffer.get(array, pos, free);
            pos = array.length;
         }
      }

      @Override
      public final void writeByte(byte value) throws IOException {
         try {
            array[pos++] = value;
         } catch (IndexOutOfBoundsException e) {
            throw log.outOfWriteBufferSpace(e);
         }
      }

      @Override
      public final void writeBytes(byte[] value, int offset, int length) throws IOException {
         try {
            System.arraycopy(value, offset, array, pos, length);
            pos += length;
         } catch (IndexOutOfBoundsException e) {
            throw log.outOfWriteBufferSpace(e);
         }
      }

      @Override
      public final void writeBytes(ByteBuffer value) throws IOException {
         int length = value.remaining();
         if (value.hasArray()) {
            writeBytes(value.array(), value.arrayOffset() + value.position(), length);
            value.position(value.position() + length);
         } else {
            try {
               value.get(array, pos, length);
               pos += length;
            } catch (IndexOutOfBoundsException e) {
               throw log.outOfWriteBufferSpace(e);
            }
         }
      }

      @Override
      public final void writeVarint32(int value) throws IOException {
         try {
            while (true) {
               if ((value & 0xFFFFFF80) == 0) {
                  array[pos++] = (byte) value;
                  break;
               } else {
                  array[pos++] = (byte) (value & 0x7F | 0x80);
                  value >>>= 7;
               }
            }
         } catch (IndexOutOfBoundsException e) {
            throw log.outOfWriteBufferSpace(e);
         }
      }

      @Override
      public final void writeVarint64(long value) throws IOException {
         try {
            while (true) {
               if ((value & 0xFFFFFFFFFFFFFF80L) == 0) {
                  array[pos++] = (byte) value;
                  break;
               } else {
                  array[pos++] = (byte) ((int) value & 0x7F | 0x80);
                  value >>>= 7;
               }
            }
         } catch (IndexOutOfBoundsException e) {
            throw log.outOfWriteBufferSpace(e);
         }
      }

      @Override
      public final void writeFixed32(int value) throws IOException {
         try {
            array[pos++] = (byte) (value & 0xFF);
            array[pos++] = (byte) ((value >> 8) & 0xFF);
            array[pos++] = (byte) ((value >> 16) & 0xFF);
            array[pos++] = (byte) ((value >> 24) & 0xFF);
         } catch (IndexOutOfBoundsException e) {
            throw log.outOfWriteBufferSpace(e);
         }
      }

      @Override
      public final void writeFixed64(long value) throws IOException {
         try {
            array[pos++] = (byte) (value & 0xFF);
            array[pos++] = (byte) ((value >> 8) & 0xFF);
            array[pos++] = (byte) ((value >> 16) & 0xFF);
            array[pos++] = (byte) ((value >> 24) & 0xFF);
            array[pos++] = (byte) ((int) (value >> 32) & 0xFF);
            array[pos++] = (byte) ((int) (value >> 40) & 0xFF);
            array[pos++] = (byte) ((int) (value >> 48) & 0xFF);
            array[pos++] = (byte) ((int) (value >> 56) & 0xFF);
         } catch (IndexOutOfBoundsException e) {
            throw log.outOfWriteBufferSpace(e);
         }
      }

      @Override
      public int skipFixedVarint() {
         int prev = pos;
         pos += 5;
         return prev;
      }

      @Override
      public void writePositiveFixedVarint(int pos) {
         TagWriterImpl.writePositiveFixedVarint(array, pos, this.pos - pos - 5);
      }

      @Override
      public boolean supportsFixedVarint() {
         return true;
      }
   }

   public static void writePositiveFixedVarint(byte[] array, int pos, int length) {
      array[pos++] = (byte) (length & 0x7F | 0x80);
      array[pos++] = (byte) ((length >>> 7) & 0x7F | 0x80);
      array[pos++] = (byte) ((length >>> 14) & 0x7F | 0x80);
      array[pos++] = (byte) ((length >>> 21) & 0x7F | 0x80);
      array[pos] = (byte) ((length >>> 28) & 0x7F);
   }

   /**
    * Writes directly to the underlying array of a heap {@link ByteBuffer} because is faster than the put() operation.
    * Buffer position is not updated after every write, just on flush.
    */
   private static final class HeapByteBufferEncoder extends ByteArrayEncoder {

      private final ByteBuffer buffer;

      private final int startPos;

      private HeapByteBufferEncoder(ByteBuffer buffer) {
         super(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
         this.buffer = buffer;
         this.startPos = buffer.position();
      }

      @Override
      public void flush() {
         buffer.position(startPos + pos - offset);
      }
   }

   /**
    * Writes to a {@link ByteBuffer} using put() operations. Only used for off-heap buffers.
    */
   private static final class ByteBufferEncoder extends EncoderImpl {

      private final ByteBuffer buffer;

      /**
       * If buffer byte order is not LITTLE_ENDIAN as expected by Protobuf binary format then we need to reverse bytes
       * whenever we write a fixed32 or fixed64 value.
       */
      private final boolean reverse;

      private ByteBufferEncoder(ByteBuffer buffer) {
         this.buffer = buffer;
         this.reverse = buffer.order() == ByteOrder.BIG_ENDIAN;
      }

      @Override
      public void writeByte(byte value) throws IOException {
         try {
            buffer.put(value);
         } catch (BufferOverflowException e) {
            throw log.outOfWriteBufferSpace(e);
         }
      }

      @Override
      public void writeBytes(byte[] value, int offset, int length) throws IOException {
         try {
            buffer.put(value, offset, length);
         } catch (IndexOutOfBoundsException | BufferOverflowException e) {
            throw log.outOfWriteBufferSpace(e);
         }
      }

      @Override
      public void writeBytes(ByteBuffer value) throws IOException {
         try {
            buffer.put(value);
         } catch (BufferOverflowException e) {
            throw log.outOfWriteBufferSpace(e);
         }
      }

      @Override
      public void writeVarint32(int value) throws IOException {
         try {
            while (true) {
               if ((value & 0xFFFFFF80) == 0) {
                  buffer.put((byte) value);
                  break;
               } else {
                  buffer.put((byte) (value & 0x7F | 0x80));
                  value >>>= 7;
               }
            }
         } catch (BufferOverflowException e) {
            throw log.outOfWriteBufferSpace(e);
         }
      }

      @Override
      public void writeVarint64(long value) throws IOException {
         try {
            while (true) {
               if ((value & 0xFFFFFFFFFFFFFF80L) == 0) {
                  buffer.put((byte) value);
                  break;
               } else {
                  buffer.put((byte) ((int) value & 0x7F | 0x80));
                  value >>>= 7;
               }
            }
         } catch (BufferOverflowException e) {
            throw log.outOfWriteBufferSpace(e);
         }
      }

      @Override
      public void writeFixed32(int value) throws IOException {
         if (reverse) {
            value = Integer.reverseBytes(value);
         }
         try {
            buffer.putInt(value);
         } catch (BufferOverflowException e) {
            throw log.outOfWriteBufferSpace(e);
         }
      }

      @Override
      public void writeFixed64(long value) throws IOException {
         if (reverse) {
            value = Long.reverseBytes(value);
         }
         try {
            buffer.putLong(value);
         } catch (BufferOverflowException e) {
            throw log.outOfWriteBufferSpace(e);
         }
      }
   }

   private static class OutputStreamNoBufferEncoder extends EncoderImpl {

      private final OutputStream out;

      private OutputStreamNoBufferEncoder(OutputStream out) {
         this.out = out;
      }

      @Override
      public void writeVarint32(int value) throws IOException {
         while (true) {
            if ((value & 0xFFFFFF80) == 0) {
               out.write((byte) value);
               break;
            } else {
               out.write((byte) (value & 0x7F | 0x80));
               value >>>= 7;
            }
         }
      }

      @Override
      public void writeVarint64(long value) throws IOException {
         try {
            while (true) {
               if ((value & 0xFFFFFFFFFFFFFF80L) == 0) {
                  out.write((byte) value);
                  break;
               } else {
                  out.write((byte) ((int) value & 0x7F | 0x80));
                  value >>>= 7;
               }
            }
         } catch (IndexOutOfBoundsException e) {
            throw log.outOfWriteBufferSpace(e);
         }
      }

      @Override
      public void writeFixed32(int value) throws IOException {
         out.write((byte) (value & 0xFF));
         out.write((byte) ((value >> 8) & 0xFF));
         out.write((byte) ((value >> 16) & 0xFF));
         out.write((byte) ((value >> 24) & 0xFF));
      }

      @Override
      public void writeFixed64(long value) throws IOException {
         out.write((byte) (value & 0xFF));
         out.write((byte) ((value >> 8) & 0xFF));
         out.write((byte) ((value >> 16) & 0xFF));
         out.write((byte) ((value >> 24) & 0xFF));
         out.write((byte) ((int) (value >> 32) & 0xFF));
         out.write((byte) ((int) (value >> 40) & 0xFF));
         out.write((byte) ((int) (value >> 48) & 0xFF));
         out.write((byte) ((int) (value >> 56) & 0xFF));
      }

      @Override
      public void writeByte(byte value) throws IOException {
         out.write(value);
      }

      @Override
      public void writeBytes(byte[] value, int offset, int length) throws IOException {
         out.write(value, offset, length);
      }

      @Override
      public void writeBytes(ByteBuffer value) throws IOException {
         if (value.hasArray()) {
            out.write(value.array(), value.arrayOffset(), value.remaining());
         } else {
            byte[] buffer = new byte[value.remaining()];
            value.get(buffer, value.position(), value.remaining());
            out.write(buffer);
         }
      }

      @Override
      public void flush() throws IOException {
         super.flush();
         out.flush();
      }

      @Override
      public int skipFixedVarint() {
         return ((ByteArrayOutputStreamEx) out).skipFixedVarint();
      }

      @Override
      public void writePositiveFixedVarint(int pos) {
         ((ByteArrayOutputStreamEx) out).writePositiveFixedVarint(pos);
      }

      @Override
      public boolean supportsFixedVarint() {
         return out instanceof ByteArrayOutputStreamEx;
      }
   }

   /**
    * Writes to an {@link OutputStream} and performs internal buffering to minimize the number of stream writes.
    * @Deprecated this is to be removed in next major
    */
   @Deprecated
   private static final class OutputStreamEncoder extends EncoderImpl {

      private final ByteArrayEncoder buffer;

      private final OutputStream out;

      OutputStreamEncoder(OutputStream out, int bufferSize) {
         // Must fit at least 2 varints without having to flush, so we can write the biggest possible tag and also
         // the biggest possible field value, except for length delimited fields which can be arbitrarily big, but at
         // least their length varint should fit.
         bufferSize = Math.max(bufferSize, MAX_VARINT_SIZE * 2);
         buffer = new ByteArrayEncoder(new byte[bufferSize], 0, bufferSize);
         this.out = out;
      }

      @Override
      public void writeUInt32Field(int fieldNumber, int value) throws IOException {
         buffer.flushToStream(out, MAX_VARINT_SIZE * 2);
         buffer.writeUInt32Field(fieldNumber, value);
      }

      @Override
      public void writeUInt64Field(int fieldNumber, long value) throws IOException {
         buffer.flushToStream(out, MAX_VARINT_SIZE * 2);
         buffer.writeUInt64Field(fieldNumber, value);
      }

      @Override
      public void writeFixed32Field(int fieldNumber, int value) throws IOException {
         buffer.flushToStream(out, MAX_VARINT_SIZE + FIXED_32_SIZE);
         buffer.writeFixed32Field(fieldNumber, value);
      }

      @Override
      public void writeFixed64Field(int fieldNumber, long value) throws IOException {
         buffer.flushToStream(out, MAX_VARINT_SIZE + FIXED_64_SIZE);
         buffer.writeFixed64Field(fieldNumber, value);
      }

      @Override
      public void writeBoolField(int fieldNumber, boolean value) throws IOException {
         buffer.flushToStream(out, MAX_VARINT_SIZE + 1);
         buffer.writeBoolField(fieldNumber, value);
      }

      @Override
      public void writeLengthDelimitedField(int fieldNumber, int length) throws IOException {
         buffer.flushToStream(out, MAX_VARINT_SIZE * 2);
         buffer.writeLengthDelimitedField(fieldNumber, length);
      }

      @Override
      public void writeVarint32(int value) throws IOException {
         buffer.flushToStream(out, MAX_VARINT_SIZE);
         buffer.writeVarint32(value);
      }

      @Override
      public void writeVarint64(long value) throws IOException {
         buffer.flushToStream(out, MAX_VARINT_SIZE);
         buffer.writeVarint64(value);
      }

      @Override
      public void writeFixed32(int value) throws IOException {
         buffer.flushToStream(out, FIXED_32_SIZE);
         buffer.writeFixed32(value);
      }

      @Override
      public void writeFixed64(long value) throws IOException {
         buffer.flushToStream(out, FIXED_64_SIZE);
         buffer.writeFixed64(value);
      }

      @Override
      public void writeByte(byte value) throws IOException {
         buffer.flushToStream(out, 1);
         buffer.writeByte(value);
      }

      @Override
      public void writeBytes(byte[] value, int offset, int length) throws IOException {
         if (buffer.remainingSpace() >= length) {
            buffer.writeBytes(value, offset, length);
         } else {
            buffer.flushToStream(out);
            out.write(value, offset, length);
         }
      }

      @Override
      public void writeBytes(ByteBuffer value) throws IOException {
         if (value.hasArray()) {
            buffer.flushToStream(out);
            out.write(value.array(), value.arrayOffset(), value.remaining());
            return;
         }
         while (value.hasRemaining()) {
            if (buffer.remainingSpace() == 0) {
               buffer.flushToStream(out);
            }
            buffer.fillFromBuffer(value);
         }
      }

      @Override
      public void flush() throws IOException {
         buffer.flushToStream(out);
      }
   }

   private static class FixedVarintWrappedEncoder extends EncoderImpl {
      private final Encoder parentEncoder;
      private final int originalPos;
      private boolean closed;

      private FixedVarintWrappedEncoder(Encoder parentEncoder) {
         this.parentEncoder = parentEncoder;
         this.originalPos = parentEncoder.skipFixedVarint();
      }

      @Override
      public void writeVarint32(int value) throws IOException {
         parentEncoder.writeVarint32(value);
      }

      @Override
      public void writeVarint64(long value) throws IOException {
         parentEncoder.writeVarint64(value);
      }

      @Override
      public void writeFixed32(int value) throws IOException {
         parentEncoder.writeFixed32(value);
      }

      @Override
      public void writeFixed64(long value) throws IOException {
         parentEncoder.writeFixed64(value);
      }

      @Override
      public void writeByte(byte value) throws IOException {
         parentEncoder.writeByte(value);
      }

      @Override
      public void writeBytes(byte[] value, int offset, int length) throws IOException {
         parentEncoder.writeBytes(value, offset, length);
      }

      @Override
      public void writeBytes(ByteBuffer value) throws IOException {
         parentEncoder.writeBytes(value);
      }

      @Override
      public void close() throws IOException {
         if (!closed) {
            closed = true;
            parentEncoder.writePositiveFixedVarint(originalPos);
         }
      }
   }

   private static class ArrayBasedWrappedEncoder extends EncoderImpl {
      private final int maxSize;
      private final Encoder parentEncoder;
      private final int number;
      private int pos = 0;
      private byte[] bytes;
      private boolean closed;

      public ArrayBasedWrappedEncoder(int maxSize, Encoder parentEncoder, int number) {
         this.maxSize = maxSize;
         this.parentEncoder = parentEncoder;
         this.number = number;
         bytes = new byte[Math.min(maxSize, 32)];
      }

      @Override
      public void writeVarint32(int value) throws IOException {
         ensureSize(5);
         try {
            while (true) {
               if ((value & 0xFFFFFF80) == 0) {
                  bytes[pos++] = (byte) value;
                  break;
               } else {
                  bytes[pos++] = (byte) (value & 0x7F | 0x80);
                  value >>>= 7;
               }
            }
         } catch (IndexOutOfBoundsException e) {
            throw log.outOfWriteBufferSpace(e);
         }
      }

      @Override
      public void writeVarint64(long value) throws IOException {
         ensureSize(10);
         try {
            while (true) {
               if ((value & 0xFFFFFFFFFFFFFF80L) == 0) {
                  bytes[pos++] = (byte) value;
                  break;
               } else {
                  bytes[pos++] = (byte) ((int) value & 0x7F | 0x80);
                  value >>>= 7;
               }
            }
         } catch (IndexOutOfBoundsException e) {
            throw log.outOfWriteBufferSpace(e);
         }
      }

      @Override
      public void writeFixed32(int value) throws IOException {
         ensureSize(4);
         try {
            bytes[pos++] = (byte) (value & 0xFF);
            bytes[pos++] = (byte) ((value >> 8) & 0xFF);
            bytes[pos++] = (byte) ((value >> 16) & 0xFF);
            bytes[pos++] = (byte) ((value >> 24) & 0xFF);
         } catch (IndexOutOfBoundsException e) {
            throw log.outOfWriteBufferSpace(e);
         }
      }

      @Override
      public void writeFixed64(long value) throws IOException {
         ensureSize(8);
         try {
            bytes[pos++] = (byte) (value & 0xFF);
            bytes[pos++] = (byte) ((value >> 8) & 0xFF);
            bytes[pos++] = (byte) ((value >> 16) & 0xFF);
            bytes[pos++] = (byte) ((value >> 24) & 0xFF);
            bytes[pos++] = (byte) ((int) (value >> 32) & 0xFF);
            bytes[pos++] = (byte) ((int) (value >> 40) & 0xFF);
            bytes[pos++] = (byte) ((int) (value >> 48) & 0xFF);
            bytes[pos++] = (byte) ((int) (value >> 56) & 0xFF);
         } catch (IndexOutOfBoundsException e) {
            throw log.outOfWriteBufferSpace(e);
         }
      }

      @Override
      public void writeByte(byte value) throws IOException {
         ensureSize(1);
         try {
            bytes[pos++] = value;
         } catch (IndexOutOfBoundsException e) {
            throw log.outOfWriteBufferSpace(e);
         }
      }

      @Override
      public void writeBytes(byte[] value, int offset, int length) throws IOException {
         ensureSize(length);
         try {
            System.arraycopy(value, offset, bytes, pos, length);
            pos += length;
         } catch (IndexOutOfBoundsException e) {
            throw log.outOfWriteBufferSpace(e);
         }
      }

      @Override
      public void writeBytes(ByteBuffer value) throws IOException {
         int length = value.remaining();
         ensureSize(length);
         if (value.hasArray()) {
            writeBytes(value.array(), value.arrayOffset() + value.position(), length);
            value.position(value.position() + length);
         } else {
            try {
               value.get(bytes, pos, length);
               pos += length;
            } catch (IndexOutOfBoundsException e) {
               throw log.outOfWriteBufferSpace(e);
            }
         }
      }

      @Override
      public void close() throws IOException {
         if (!closed) {
            closed = true;
            parentEncoder.writeLengthDelimitedField(number, pos);
            parentEncoder.writeBytes(bytes, 0, pos);
         }
      }

      @Override
      public int skipFixedVarint() {
         int prev = pos;
         pos += 5;
         return prev;
      }

      @Override
      public void writePositiveFixedVarint(int pos) {
         TagWriterImpl.writePositiveFixedVarint(bytes, pos, this.pos - pos - 5);
      }

      @Override
      public boolean supportsFixedVarint() {
         return true;
      }

      private void ensureSize(int possibleLength) {
         int targetSize = pos + possibleLength;
         int currentSize = bytes.length;
         while (targetSize > currentSize) {
            if (currentSize > maxSize) {
               currentSize = maxSize;
               break;
            }
            currentSize <<= 1;
         }
         if (currentSize != bytes.length) {
            bytes = Arrays.copyOf(bytes, currentSize);
         }
      }
   }
}
