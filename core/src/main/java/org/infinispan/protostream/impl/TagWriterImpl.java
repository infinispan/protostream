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

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufTagMarshaller;
import org.infinispan.protostream.RandomAccessOutputStream;
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

   public static TagWriterImpl newNestedInstance(ProtobufTagMarshaller.WriteContext parent, RandomAccessOutputStream output) {
      return new TagWriterImpl((TagWriterImpl) parent, new OutputStreamRandomAccessEncoder(output));
   }

   public static TagWriterImpl newNestedInstance(ProtobufTagMarshaller.WriteContext parent, OutputStream output) {
      return new TagWriterImpl((TagWriterImpl) parent, new OutputStreamNoBufferEncoder(output));
   }

   public static TagWriterImpl newNestedInstance(ProtobufTagMarshaller.WriteContext parent, byte[] buf) {
      return new TagWriterImpl((TagWriterImpl) parent, new ByteArrayEncoder(buf, 0, buf.length));
   }

   public static TagWriterImpl newInstance(ImmutableSerializationContext serCtx, OutputStream output) {
      return new TagWriterImpl((SerializationContextImpl) serCtx, new OutputStreamNoBufferEncoder(output));
   }

   public static TagWriterImpl newInstance(ImmutableSerializationContext serCtx, RandomAccessOutputStream output) {
      return new TagWriterImpl((SerializationContextImpl) serCtx, new OutputStreamRandomAccessEncoder(output));
   }

   /**
    * @deprecated since 5.0.10 Please use {@link #newInstance(ImmutableSerializationContext, OutputStream)} with a {@link java.io.BufferedOutputStream} instead
    */
   @Deprecated
   public static TagWriterImpl newInstance(ImmutableSerializationContext serCtx, OutputStream output, int bufferSize) {
      return new TagWriterImpl((SerializationContextImpl) serCtx, new OutputStreamEncoder(output, bufferSize));
   }

   public static TagWriterImpl newInstance(ImmutableSerializationContext serCtx, byte[] buf) {
      return new TagWriterImpl((SerializationContextImpl) serCtx, new ByteArrayEncoder(buf, 0, buf.length));
   }

   public static TagWriterImpl newInstance(ImmutableSerializationContext serCtx, byte[] buf, int offset, int length) {
      return new TagWriterImpl((SerializationContextImpl) serCtx, new ByteArrayEncoder(buf, offset, length));
   }

   public static TagWriterImpl newInstance(ImmutableSerializationContext serCtx, ByteBuffer byteBuffer) {
      Encoder encoder = byteBuffer.hasArray() ? new HeapByteBufferEncoder(byteBuffer) : new ByteBufferEncoder(byteBuffer);
      return new TagWriterImpl((SerializationContextImpl) serCtx, encoder);
   }

   public static TagWriterImpl newInstance(ImmutableSerializationContext serCtx) {
      return new TagWriterImpl((SerializationContextImpl) serCtx, new NoOpEncoder());
   }

   /**
    * @deprecated since 5.0.10 Please use {@link #newInstance(ImmutableSerializationContext, OutputStream)}
    */
   @Deprecated
   public static TagWriterImpl newInstanceNoBuffer(ImmutableSerializationContext ctx, OutputStream out) {
      return new TagWriterImpl((SerializationContextImpl) ctx, new OutputStreamNoBufferEncoder(out));
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
   public void writeTag(int number, int wireType) throws IOException {
      encoder.writeVarint32(WireType.makeTag(number, wireType));
   }

   @Override
   public void writeTag(int number, WireType wireType) throws IOException {
      encoder.writeVarint32(WireType.makeTag(number, wireType));
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
      encoder.writeUTF8Field(number, value);
   }

   @Override
   public void writeInt32(int number, int value) throws IOException {
      if (value >= 0) {
         encoder.writeUInt32Field(number, value);
      } else {
         encoder.writeUInt64Field(number, value);
      }
   }

   @Override
   public void writeUInt32(int number, int value) throws IOException {
      encoder.writeUInt32Field(number, value);
   }

   @Override
   public void writeSInt32(int number, int value) throws IOException {
      // Roll the bits in order to move the sign bit from position 31 to position 0, to reduce the wire length of negative numbers.
      encoder.writeUInt32Field(number, (value << 1) ^ (value >> 31));
   }

   @Override
   public void writeFixed32(int number, int value) throws IOException {
      encoder.writeFixed32Field(number, value);
   }

   @Override
   public void writeSFixed32(int number, int value) throws IOException {
      writeFixed32(number, value);
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
   public void writeSInt64(int number, long value) throws IOException {
      // Roll the bits in order to move the sign bit from position 63 to position 0, to reduce the wire length of negative numbers.
      encoder.writeUInt64Field(number, (value << 1) ^ (value >> 63));
   }

   @Override
   public void writeFixed64(int number, long value) throws IOException {
      encoder.writeFixed64Field(number, value);
   }

   @Override
   public void writeSFixed64(int number, long value) throws IOException {
      writeFixed64(number, value);
   }

   @Override
   public void writeEnum(int number, int value) throws IOException {
      writeInt32(number, value);
   }

   @Override
   public void writeBool(int number, boolean value) throws IOException {
      encoder.writeBoolField(number, value);
   }

   @Override
   public void writeDouble(int number, double value) throws IOException {
      encoder.writeFixed64Field(number, Double.doubleToRawLongBits(value));
   }

   @Override
   public void writeFloat(int number, float value) throws IOException {
      encoder.writeFixed32Field(number, Float.floatToRawIntBits(value));
   }

   @Override
   public void writeBytes(int number, ByteBuffer value) throws IOException {
      encoder.writeBytes(number, value);
   }

   @Override
   public void writeBytes(int number, byte[] value) throws IOException {
      writeBytes(number, value, 0, value.length);
   }

   @Override
   public void writeBytes(int number, byte[] value, int offset, int length) throws IOException {
      encoder.writeBytes(number, value, offset, length);
   }

   @Override
   public void writeRawBytes(byte[] value, int offset, int length) throws IOException {
      encoder.writeBytes(value, offset, length);
   }

   @Override
   public TagWriter subWriter(int number, boolean nested) throws IOException {
      Encoder subEncoder = encoder.subEncoder(number);
      if (subEncoder.canReuseWriter()) {
         return this;
      }
      return nested ? new TagWriterImpl(this, subEncoder) :
            new TagWriterImpl(serCtx, subEncoder);
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

   public ProtoStreamWriterImpl getProtoStreamWriter() {
      if (parent != null) {
         return parent.getProtoStreamWriter();
      }
      if (writer == null) {
         writer = new ProtoStreamWriterImpl(serCtx);
      }
      return writer;
   }

   //todo [anistor] need to provide a safety mechanism to limit message size in bytes and message nesting depth on write ops
   private abstract static class Encoder {
      /**
       * Commits the witten bytes after several write operations were performed. Updates counters, positions, whatever.
       */
      void flush() throws IOException {
      }

      boolean canReuseWriter() {
         return false;
      }

      /**
       * Method to be invoked when the encoder is no longer used. Data should be flushed and any resources are freed
       * @throws IOException
       */
      void close() throws IOException {
         flush();
      }

      // high level ops, writing fields

      void writeUInt32Field(int fieldNumber, int value) throws IOException {
         writeVarint32(WireType.makeTag(fieldNumber, WireType.WIRETYPE_VARINT));
         writeVarint32(value);
      }

      void writeUInt64Field(int fieldNumber, long value) throws IOException {
         writeVarint32(WireType.makeTag(fieldNumber, WireType.WIRETYPE_VARINT));
         writeVarint64(value);
      }

      void writeFixed32Field(int fieldNumber, int value) throws IOException {
         writeVarint32(WireType.makeTag(fieldNumber, WireType.WIRETYPE_FIXED32));
         writeFixed32(value);
      }

      void writeFixed64Field(int fieldNumber, long value) throws IOException {
         writeVarint32(WireType.makeTag(fieldNumber, WireType.WIRETYPE_FIXED64));
         writeFixed64(value);
      }

      void writeBoolField(int fieldNumber, boolean value) throws IOException {
         writeVarint32(WireType.makeTag(fieldNumber, WireType.WIRETYPE_VARINT));
         writeByte((byte) (value ? 1 : 0));
      }

      void writeLengthDelimitedField(int fieldNumber, int length) throws IOException {
         writeVarint32(WireType.makeTag(fieldNumber, WireType.WIRETYPE_LENGTH_DELIMITED));
         writeVarint32(length);
      }

      void writeBytes(int fieldNumber, ByteBuffer value) throws IOException {
         writeLengthDelimitedField(fieldNumber, value.remaining());
         writeBytes(value);
      }

      void writeBytes(int fieldNumber, byte[] value, int offset, int length) throws IOException {
         writeLengthDelimitedField(fieldNumber, length);
         writeBytes(value, offset, length);
      }

      void writeUTF8Field(int fieldNumber, String value) throws IOException {
         byte[] utf8buffer = value.getBytes(StandardCharsets.UTF_8);
         writeLengthDelimitedField(fieldNumber, utf8buffer.length);
         writeBytes(utf8buffer, 0, utf8buffer.length);
      }

      // low level ops, writing values without tag

      abstract void writeVarint32(int value) throws IOException;

      abstract void writeVarint64(long value) throws IOException;

      abstract void writeFixed32(int value) throws IOException;

      abstract void writeFixed64(long value) throws IOException;

      abstract void writeByte(byte value) throws IOException;

      abstract void writeBytes(byte[] value, int offset, int length) throws IOException;

      abstract void writeBytes(ByteBuffer value) throws IOException;

      Encoder subEncoder(int number) throws IOException {
         RandomAccessOutputStream raos = new RandomAccessOutputStreamImpl();
         return new WrappedEncoder(new OutputStreamRandomAccessEncoder(raos)) {
            @Override
            void close() throws IOException {
               Encoder.this.writeBytes(number, raos.getByteBuffer());
               super.close();
            }
         };
      }
   }

   private static class WrappedEncoder extends Encoder {
      private final Encoder innerEncoder;

      private WrappedEncoder(Encoder parentEncoder) {
         this.innerEncoder = parentEncoder;
      }

      @Override
      void writeVarint32(int value) throws IOException {
         innerEncoder.writeVarint32(value);
      }

      @Override
      void writeVarint64(long value) throws IOException {
         innerEncoder.writeVarint64(value);
      }

      @Override
      void writeFixed32(int value) throws IOException {
         innerEncoder.writeFixed32(value);
      }

      @Override
      void writeFixed64(long value) throws IOException {
         innerEncoder.writeFixed64(value);
      }

      @Override
      void writeByte(byte value) throws IOException {
         innerEncoder.writeByte(value);
      }

      @Override
      void writeBytes(byte[] value, int offset, int length) throws IOException {
         innerEncoder.writeBytes(value, offset, length);
      }

      @Override
      void writeBytes(ByteBuffer value) throws IOException {
         innerEncoder.writeBytes(value);
      }

      @Override
      void writeUInt32Field(int fieldNumber, int value) throws IOException {
         innerEncoder.writeUInt32Field(fieldNumber, value);
      }

      @Override
      void writeUInt64Field(int fieldNumber, long value) throws IOException {
         innerEncoder.writeUInt64Field(fieldNumber, value);
      }

      @Override
      void writeFixed32Field(int fieldNumber, int value) throws IOException {
         innerEncoder.writeFixed32Field(fieldNumber, value);
      }

      @Override
      void writeFixed64Field(int fieldNumber, long value) throws IOException {
         innerEncoder.writeFixed64Field(fieldNumber, value);
      }

      @Override
      void writeBoolField(int fieldNumber, boolean value) throws IOException {
         innerEncoder.writeBoolField(fieldNumber, value);
      }

      @Override
      void writeLengthDelimitedField(int fieldNumber, int length) throws IOException {
         innerEncoder.writeLengthDelimitedField(fieldNumber, length);
      }

      @Override
      void writeBytes(int fieldNumber, ByteBuffer value) throws IOException {
         innerEncoder.writeBytes(fieldNumber, value);
      }

      @Override
      void writeBytes(int fieldNumber, byte[] value, int offset, int length) throws IOException {
         innerEncoder.writeBytes(fieldNumber, value, offset, length);
      }

      @Override
      void writeUTF8Field(int fieldNumber, String value) throws IOException {
         innerEncoder.writeUTF8Field(fieldNumber, value);
      }

      @Override
      void flush() throws IOException {
         innerEncoder.flush();
      }

      @Override
      void close() throws IOException {
         innerEncoder.close();
      }
   }

   /**
    * An encoder that just counts the bytes and does not write anything and does not allocate buffers.
    * Useful for computing message size.
    */
   static class NoOpEncoder extends Encoder {

      protected int count = 0;
      protected int[] nestedPositions;
      // This will always be one position higher than any encoder size position
      protected int head;

      int getWrittenBytes() {
         return count;
      }

      /**
       * Resets the written bytes counter. Needed if we intend to reuse this to count the size of another message.
       */
      void reset() {
         count = 0;
         head = 0;
      }

      @Override
      void writeByte(byte value) {
         count++;
      }

      @Override
      void writeBytes(byte[] value, int offset, int length) {
         count += length;
      }

      @Override
      void writeBytes(ByteBuffer value) {
         count += value.remaining();
      }

      @Override
      void writeVarint32(int value) {
         while (true) {
            count++;
            if ((value & 0xFFFFFF80) == 0) {
               break;
            }
            value >>>= 7;
         }
      }

      @Override
      void writeVarint64(long value) {
         while (true) {
            count++;
            if ((value & 0xFFFFFFFFFFFFFF80L) == 0) {
               break;
            }
            value >>>= 7;
         }
      }

      @Override
      void writeFixed32(int value) {
         count += FIXED_32_SIZE;
      }

      @Override
      void writeFixed64(long value) {
         count += FIXED_64_SIZE;
      }

      @Override
      void writeUTF8Field(int fieldNumber, String s) {
         writeVarint32(WireType.makeTag(fieldNumber, WireType.WIRETYPE_VARINT));
         char c;
         int i;
         int count = s.length();
         for (i = 0; i < count; i++) {
            c = s.charAt(i);
            if (c > 127) {
               // TODO: do this without allocating the byte[]
               count = s.getBytes(StandardCharsets.UTF_8).length;
               break;
            }
         }
         writeVarint32(count);
         this.count += count;
      }

      @Override
      Encoder subEncoder(int number) {
         writeVarint32(WireType.makeTag(number, WireType.WIRETYPE_LENGTH_DELIMITED));
         resize();
         encoderStartPos(count);
         return this;
      }

      void encoderStartPos(int size) {
         nestedPositions[head++] = size;
      }

      void resize() {
         if (nestedPositions == null) {
            // We are guessing most objects won't have larger than 4 sub elements
            nestedPositions = new int[10];
         } else {
            if (head == nestedPositions.length) {
               nestedPositions = Arrays.copyOf(nestedPositions, nestedPositions.length + 10);
            }
         }
      }

      @Override
      public void close() {
         if (nestedPositions != null) {
            if (head > 0) {
               int lastSize = nestedPositions[--head];
               writeVarint32(count - lastSize);
            }
         }
      }

      @Override
      boolean canReuseWriter() {
         return true;
      }
   }

   /**
    * Writes to a user provided byte array.
    */
   private static class ByteArrayEncoder extends Encoder {

      private final byte[] array;

      protected final int offset;

      protected final int limit;

      protected int pos;

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

      protected final int remainingSpace() {
         return limit - pos;
      }

      /**
       * Make room for the required space by flushing the entire buffer to stream if the available space is not enough.
       */
      protected final void flushToStream(OutputStream out, int requiredSpace) throws IOException {
         if (requiredSpace > limit - pos) {
            out.write(array, 0, pos);
            pos = 0;
         }
      }

      /**
       * Flush the entire buffer to an output stream.
       */
      protected final void flushToStream(OutputStream out) throws IOException {
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
      final void writeByte(byte value) throws IOException {
         try {
            array[pos++] = value;
         } catch (IndexOutOfBoundsException e) {
            throw log.outOfWriteBufferSpace(e);
         }
      }

      @Override
      final void writeBytes(byte[] value, int offset, int length) throws IOException {
         try {
            System.arraycopy(value, offset, array, pos, length);
            pos += length;
         } catch (IndexOutOfBoundsException e) {
            throw log.outOfWriteBufferSpace(e);
         }
      }

      @Override
      final void writeBytes(ByteBuffer value) throws IOException {
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
      final void writeVarint32(int value) throws IOException {
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
      final void writeVarint64(long value) throws IOException {
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
      final void writeFixed32(int value) throws IOException {
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
      final void writeFixed64(long value) throws IOException {
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
      void flush() {
         buffer.position(startPos + pos - offset);
      }
   }

   /**
    * Writes to a {@link ByteBuffer} using put() operations. Only used for off-heap buffers.
    */
   private static final class ByteBufferEncoder extends Encoder {

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
      void writeByte(byte value) throws IOException {
         try {
            buffer.put(value);
         } catch (BufferOverflowException e) {
            throw log.outOfWriteBufferSpace(e);
         }
      }

      @Override
      void writeBytes(byte[] value, int offset, int length) throws IOException {
         try {
            buffer.put(value, offset, length);
         } catch (IndexOutOfBoundsException | BufferOverflowException e) {
            throw log.outOfWriteBufferSpace(e);
         }
      }

      @Override
      void writeBytes(ByteBuffer value) throws IOException {
         try {
            buffer.put(value);
         } catch (BufferOverflowException e) {
            throw log.outOfWriteBufferSpace(e);
         }
      }

      @Override
      void writeVarint32(int value) throws IOException {
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
      void writeVarint64(long value) throws IOException {
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
      void writeFixed32(int value) throws IOException {
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
      void writeFixed64(long value) throws IOException {
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

   private static class OutputStreamNoBufferEncoder extends Encoder {

      private final OutputStream out;

      private OutputStreamNoBufferEncoder(OutputStream out) {
         this.out = out;
      }

      @Override
      void writeVarint32(int value) throws IOException {
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
      void writeVarint64(long value) throws IOException {
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
      void writeFixed32(int value) throws IOException {
         out.write((byte) (value & 0xFF));
         out.write((byte) ((value >> 8) & 0xFF));
         out.write((byte) ((value >> 16) & 0xFF));
         out.write((byte) ((value >> 24) & 0xFF));
      }

      @Override
      void writeFixed64(long value) throws IOException {
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
      void writeByte(byte value) throws IOException {
         out.write(value);
      }

      @Override
      void writeBytes(byte[] value, int offset, int length) throws IOException {
         out.write(value, offset, length);
      }

      @Override
      void writeBytes(ByteBuffer value) throws IOException {
         if (value.hasArray()) {
            out.write(value.array(), value.arrayOffset(), value.remaining());
         } else {
            byte[] buffer = new byte[value.remaining()];
            value.get(buffer, value.position(), value.remaining());
            out.write(buffer);
         }
      }

      @Override
      void flush() throws IOException {
         super.flush();
         out.flush();
      }
   }

   /**
    * Writes to an {@link OutputStream} and performs internal buffering to minimize the number of stream writes.
    */
   private static final class OutputStreamEncoder extends Encoder {

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
      void writeUInt32Field(int fieldNumber, int value) throws IOException {
         buffer.flushToStream(out, MAX_VARINT_SIZE * 2);
         buffer.writeUInt32Field(fieldNumber, value);
      }

      @Override
      void writeUInt64Field(int fieldNumber, long value) throws IOException {
         buffer.flushToStream(out, MAX_VARINT_SIZE * 2);
         buffer.writeUInt64Field(fieldNumber, value);
      }

      @Override
      void writeFixed32Field(int fieldNumber, int value) throws IOException {
         buffer.flushToStream(out, MAX_VARINT_SIZE + FIXED_32_SIZE);
         buffer.writeFixed32Field(fieldNumber, value);
      }

      @Override
      void writeFixed64Field(int fieldNumber, long value) throws IOException {
         buffer.flushToStream(out, MAX_VARINT_SIZE + FIXED_64_SIZE);
         buffer.writeFixed64Field(fieldNumber, value);
      }

      @Override
      void writeBoolField(int fieldNumber, boolean value) throws IOException {
         buffer.flushToStream(out, MAX_VARINT_SIZE + 1);
         buffer.writeBoolField(fieldNumber, value);
      }

      @Override
      void writeLengthDelimitedField(int fieldNumber, int length) throws IOException {
         buffer.flushToStream(out, MAX_VARINT_SIZE * 2);
         buffer.writeLengthDelimitedField(fieldNumber, length);
      }

      @Override
      void writeVarint32(int value) throws IOException {
         buffer.flushToStream(out, MAX_VARINT_SIZE);
         buffer.writeVarint32(value);
      }

      @Override
      void writeVarint64(long value) throws IOException {
         buffer.flushToStream(out, MAX_VARINT_SIZE);
         buffer.writeVarint64(value);
      }

      @Override
      void writeFixed32(int value) throws IOException {
         buffer.flushToStream(out, FIXED_32_SIZE);
         buffer.writeFixed32(value);
      }

      @Override
      void writeFixed64(long value) throws IOException {
         buffer.flushToStream(out, FIXED_64_SIZE);
         buffer.writeFixed64(value);
      }

      @Override
      void writeByte(byte value) throws IOException {
         buffer.flushToStream(out, 1);
         buffer.writeByte(value);
      }

      @Override
      void writeBytes(byte[] value, int offset, int length) throws IOException {
         if (buffer.remainingSpace() >= length) {
            buffer.writeBytes(value, offset, length);
         } else {
            buffer.flushToStream(out);
            out.write(value, offset, length);
         }
      }

      @Override
      void writeBytes(ByteBuffer value) throws IOException {
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
      void flush() throws IOException {
         buffer.flushToStream(out);
      }
   }

   /**
    * Writes bytes positionally in order to reduce allocations as much as possible.
    */
   private static class OutputStreamRandomAccessEncoder extends Encoder {

      final RandomAccessOutputStream out;
      int fieldNumber = -1;
      OutputStreamRandomAccessEncoder childEncoder = null;

      public OutputStreamRandomAccessEncoder(RandomAccessOutputStream out) {
         this.out = out;
      }

      @Override
      void writeUInt32Field(int fieldNumber, int value) throws IOException {
         int pos = out.getPosition();
         int tag = WireType.makeTag(fieldNumber, WireType.WIRETYPE_VARINT);
         int tagLen = varIntBytes(tag);
         int valueLen = varIntBytes(value);
         out.ensureCapacity(pos + tagLen + valueLen);

         pos = writeVarInt32Direct(pos, tag);
         pos = writeVarInt32Direct(pos, value);
         out.setPosition(pos);
      }

      @Override
      void writeUInt64Field(int fieldNumber, long value) throws IOException {
         int pos = out.getPosition();
         int tag = WireType.makeTag(fieldNumber, WireType.WIRETYPE_VARINT);
         int tagLen = varIntBytes(tag);
         int valueLen = varIntBytes(value);
         out.ensureCapacity(pos + tagLen + valueLen);

         pos = writeVarInt32Direct(pos, tag);
         pos = writeVarInt64Direct(pos, value);
         out.setPosition(pos);
      }

      @Override
      void writeFixed32Field(int fieldNumber, int value) throws IOException {
         int pos = out.getPosition();
         int tag = WireType.makeTag(fieldNumber, WireType.WIRETYPE_FIXED32);
         int tagLen = varIntBytes(tag);
         out.ensureCapacity(pos + tagLen + FIXED_32_SIZE);

         pos = writeVarInt32Direct(pos, tag);
         pos = writeFixed32Direct(pos, value);
         out.setPosition(pos);
      }

      @Override
      void writeFixed64Field(int fieldNumber, long value) throws IOException {
         int pos = out.getPosition();
         int tag = WireType.makeTag(fieldNumber, WireType.WIRETYPE_FIXED64);
         int tagLen = varIntBytes(tag);
         out.ensureCapacity(pos + tagLen + FIXED_64_SIZE);

         pos = writeVarInt32Direct(pos, tag);
         pos = writeFixed64Direct(pos, value);
         out.setPosition(pos);
      }

      @Override
      void writeBoolField(int fieldNumber, boolean value) throws IOException {
         int pos = out.getPosition();
         int tag = WireType.makeTag(fieldNumber, WireType.WIRETYPE_VARINT);
         int tagLen = varIntBytes(tag);
         out.ensureCapacity(pos + tagLen + 1);

         pos = writeVarInt32Direct(pos, tag);
         out.write(pos++, (byte) (value ? 1 : 0));
         out.setPosition(pos);
      }

      @Override
      void writeLengthDelimitedField(int fieldNumber, int length) throws IOException {
         int pos = out.getPosition();
         int tag = WireType.makeTag(fieldNumber, WireType.WIRETYPE_LENGTH_DELIMITED);
         int tagLen = varIntBytes(tag);
         int valueLen = varIntBytes(length);
         out.ensureCapacity(pos + tagLen + valueLen);

         pos = writeVarInt32Direct(pos, tag);
         pos = writeVarInt32Direct(pos, length);
         out.setPosition(pos);
      }

      @Override
      void writeBytes(int fieldNumber, ByteBuffer value) throws IOException {
         int pos = out.getPosition();
         if (value.hasArray()) {
            int length = value.remaining();
            writeBytes(fieldNumber, value.array(), value.arrayOffset() + value.position(), length);
            value.position(value.position() + length);
         } else {
            int bbPos = value.position();
            int bbLen = bbPos + value.remaining();

            int tag = WireType.makeTag(fieldNumber, WireType.WIRETYPE_LENGTH_DELIMITED);
            int tagLen = varIntBytes(tag);
            int valueLen = varIntBytes(bbLen);
            out.ensureCapacity(pos + tagLen + valueLen + bbLen);

            pos = writeVarInt32Direct(pos, tag);
            pos = writeVarInt32Direct(pos, bbLen);
            for (int i = bbPos; i < bbLen; i++)
               out.write(pos++, value.get(i));
            out.setPosition(bbPos);
         }
      }

      @Override
      void writeBytes(int fieldNumber, byte[] value, int offset, int length) throws IOException {
         int pos = out.getPosition();
         int tag = WireType.makeTag(fieldNumber, WireType.WIRETYPE_LENGTH_DELIMITED);
         int tagLen = varIntBytes(tag);
         int valueLen = varIntBytes(length);

         out.ensureCapacity(pos + tagLen + valueLen + length);
         pos = writeVarInt32Direct(pos, tag);
         pos = writeVarInt32Direct(pos, length);
         out.write(pos, value, offset, length);
         out.setPosition(pos + length);
      }

      @Override
      void writeVarint32(int value) throws IOException {
         int pos = out.getPosition();
         out.ensureCapacity(pos + varIntBytes(value));
         pos = writeVarInt32Direct(pos, value);
         out.setPosition(pos);
      }

      @Override
      void writeVarint64(long value) throws IOException {
         int pos = out.getPosition();
         out.ensureCapacity(pos + varIntBytes(value));
         pos = writeVarInt64Direct(pos, value);
         out.setPosition(pos);
      }

      @Override
      void writeFixed32(int value) throws IOException {
         int pos = out.getPosition();
         out.ensureCapacity(pos + FIXED_32_SIZE);
         pos = writeFixed32Direct(pos, value);
         out.setPosition(pos);
      }

      @Override
      void writeFixed64(long value) throws IOException {
         int pos = out.getPosition();
         out.ensureCapacity(pos + FIXED_64_SIZE);
         pos = writeFixed64Direct(pos, value);
         out.setPosition(pos);
      }

      @Override
      void writeByte(byte value) throws IOException {
         out.write(value);
      }

      @Override
      void writeBytes(byte[] value, int offset, int length) throws IOException {
         int pos = out.getPosition();
         out.ensureCapacity(pos + length);
         out.write(pos, value, offset, length);
         out.setPosition(pos + length);
      }

      @Override
      void writeBytes(ByteBuffer value) {
         throw new IllegalStateException("Method not supported");
      }

      @Override
      void writeUTF8Field(int number, String s) throws IOException {
         int strlen = s.length();
         int tag = WireType.makeTag(number, WireType.WIRETYPE_LENGTH_DELIMITED);
         int varIntTagLen = varIntBytes(tag);
         int varIntBytesLen = varIntBytes(strlen);
         int startPos = out.getPosition();

         // First optimize for 1 - 127 case
         out.ensureCapacity(startPos + varIntTagLen + varIntBytesLen + strlen);
         writeVarInt32Direct(startPos, tag);
         // Note this will be overwritten if not all 1 - 127 characters below
         writeVarInt32Direct(startPos + varIntTagLen, strlen);

         int localPos = startPos + varIntTagLen + varIntBytesLen;

         int c;
         int i;
         for (i = 0; i < strlen; i++) {
            c = s.charAt(i);
            if (c > 127) break;

            out.write(localPos++, (byte) c);
         }

         out.setPosition(localPos);
         // All single byte characters
         if (i == strlen)
            return;

         byte[] utf8buffer = s.getBytes(StandardCharsets.UTF_8);
         varIntBytesLen = varIntBytes(utf8buffer.length);
         out.ensureCapacity(startPos + varIntTagLen + varIntBytesLen + utf8buffer.length);

         writeVarInt32Direct(startPos + varIntTagLen, utf8buffer.length);
         out.write(startPos + varIntTagLen + varIntBytesLen, utf8buffer);
         out.setPosition(startPos + varIntTagLen + varIntBytesLen + utf8buffer.length);
      }

      private int writeFixed32Direct(int i, int value) throws IOException {
         out.write(i++, (byte) (value & 0xFF));
         out.write(i++, (byte) ((value >> 8) & 0xFF));
         out.write(i++, (byte) ((value >> 16) & 0xFF));
         out.write(i++, (byte) ((value >> 24) & 0xFF));
         return i;
      }

      private int writeFixed64Direct(int i, long value) throws IOException {
         out.write(i++, (byte) (value & 0xFF));
         out.write(i++, (byte) ((value >> 8) & 0xFF));
         out.write(i++, (byte) ((value >> 16) & 0xFF));
         out.write(i++, (byte) ((value >> 24) & 0xFF));
         out.write(i++, (byte) ((int) (value >> 32) & 0xFF));
         out.write(i++, (byte) ((int) (value >> 40) & 0xFF));
         out.write(i++, (byte) ((int) (value >> 48) & 0xFF));
         out.write(i++, (byte) ((int) (value >> 56) & 0xFF));
         return i;
      }

      private int writeVarInt32Direct(int i, int value) throws IOException {
         while (true) {
            if ((value & 0xFFFFFF80) == 0) {
               out.write(i++, (byte) value);
               break;
            } else {
               out.write(i++, (byte) (value & 0x7F | 0x80));
               value >>>= 7;
            }
         }
         return i;
      }

      private int writeVarInt64Direct(int i, long value) throws IOException {
         while (true) {
            if ((value & 0xFFFFFFFFFFFFFF80L) == 0) {
               out.write(i++, (byte) value);
               break;
            } else {
               out.write(i++, (byte) ((int) value & 0x7F | 0x80));
               value >>>= 7;
            }
         }
         return i;
      }

      private static int varIntBytes(long value) {
         int i = 1;
         while ((value & 0xFFFFFFFFFFFFFF80L) != 0) {
            ++i;
            value >>>= 7;
         }
         return i;
      }

      @Override
      Encoder subEncoder(int number) {
         // This code handles caching a child encoder for each encoder in the chain. For example if you had an object
         // with repeated fields the child encoder will be "initialized" each time but reuses the same encoder
         // underneath for the child objects. Note these encoders are also chained together which allows for caching
         // as many layers down as we support context depth
         if (childEncoder == null) {
            childEncoder = new OutputStreamRandomAccessEncoder() {
               @Override
               void close() throws IOException {
                  OutputStreamRandomAccessEncoder.this.writeLengthDelimitedField(fieldNumber, out.getPosition());
                  if (out.getPosition() > 0) {
                     // TODO: remove this array copy somehow
                     OutputStreamRandomAccessEncoder.this.writeBytes(out.toByteArray(), 0, out.getPosition());
                  }
                  fieldNumber = -1;
                  out.reset();
               }
            };
         }
         // This just asserts every childEncoder is properly closed
         assert childEncoder.fieldNumber == -1;
         childEncoder.fieldNumber = number;
         return childEncoder;
      }

      @Override
      void close() throws IOException {
         // This method should only be invoked for outer encoders only!
         assert fieldNumber == -1;
         super.close();
         out.reset();
      }
   }
}
