package org.infinispan.protostream.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.MalformedProtobufException;
import org.infinispan.protostream.ProtobufTagMarshaller;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.TagReader;
import org.infinispan.protostream.descriptors.WireType;

import static org.infinispan.protostream.descriptors.WireType.FIXED_32_SIZE;
import static org.infinispan.protostream.descriptors.WireType.FIXED_64_SIZE;
import static org.infinispan.protostream.descriptors.WireType.MAX_VARINT_SIZE;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public final class TagReaderImpl implements TagReader, ProtobufTagMarshaller.ReadContext {

   private static final Log log = Log.LogFactory.getLog(TagReaderImpl.class);

   private static final Charset UTF8 = StandardCharsets.UTF_8;
   private static final byte[] EMPTY = new byte[0];
   private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.wrap(EMPTY);

   private final SerializationContextImpl serCtx;

   // all reads are delegated to a lower level protocol decoder
   private final Decoder decoder;

   private final TagReaderImpl parent;

   // lazily initialized
   private Map<Object, Object> params = null;

   // lazily initialized
   @Deprecated
   private ProtoStreamReaderImpl reader = null;

   private TagReaderImpl(TagReaderImpl parent, Decoder decoder) {
      this.parent = parent;
      this.serCtx = parent.serCtx;
      this.decoder = decoder;
   }

   private TagReaderImpl(SerializationContextImpl serCtx, Decoder decoder) {
      this.parent = null;
      this.serCtx = serCtx;
      this.decoder = decoder;
   }

   public static TagReaderImpl newNestedInstance(ProtobufTagMarshaller.ReadContext parent, InputStream input) {
      return new TagReaderImpl((TagReaderImpl) parent, new InputStreamDecoder(input, ProtobufUtil.DEFAULT_STREAM_BUFFER_SIZE));
   }

   public static TagReaderImpl newNestedInstance(ProtobufTagMarshaller.ReadContext parent, byte[] buf) {
      return new TagReaderImpl((TagReaderImpl) parent, new ByteArrayDecoder(buf, 0, buf.length));
   }

   public static TagReaderImpl newInstance(ImmutableSerializationContext serCtx, InputStream input) {
      return new TagReaderImpl((SerializationContextImpl) serCtx, new InputStreamDecoder(input, ProtobufUtil.DEFAULT_STREAM_BUFFER_SIZE));
   }

   public static TagReaderImpl newInstance(ImmutableSerializationContext serCtx, ByteBuffer buf) {
      Decoder decoder = buf.hasArray()
            ? new ByteArrayDecoder(buf.array(), buf.arrayOffset() + buf.position(), buf.remaining())
            : new ByteBufferDecoder(buf);
      return new TagReaderImpl((SerializationContextImpl) serCtx, decoder);
   }

   public static TagReaderImpl newInstance(ImmutableSerializationContext serCtx, byte[] buf) {
      return new TagReaderImpl((SerializationContextImpl) serCtx, new ByteArrayDecoder(buf, 0, buf.length));
   }

   public static TagReaderImpl newInstance(ImmutableSerializationContext serCtx, byte[] buf, int offset, int length) {
      return new TagReaderImpl((SerializationContextImpl) serCtx, new ByteArrayDecoder(buf, offset, length));
   }

   @Override
   public boolean isAtEnd() throws IOException {
      return decoder.isAtEnd();
   }

   @Override
   public int readTag() throws IOException {
      return decoder.readTag();
   }

   @Override
   public void checkLastTagWas(int tag) throws IOException {
      decoder.checkLastTagWas(tag);
   }

   @Override
   public boolean skipField(int tag) throws IOException {
      return decoder.skipField(tag);
   }

   @Override
   public long readUInt64() throws IOException {
      return decoder.readVarint64();
   }

   @Override
   public long readInt64() throws IOException {
      return decoder.readVarint64();
   }

   @Override
   public int readInt32() throws IOException {
      return decoder.readVarint32();
   }

   @Override
   public long readFixed64() throws IOException {
      return decoder.readFixed64();
   }

   @Override
   public int readFixed32() throws IOException {
      return decoder.readFixed32();
   }

   @Override
   public double readDouble() throws IOException {
      return Double.longBitsToDouble(decoder.readFixed64());
   }

   @Override
   public float readFloat() throws IOException {
      return Float.intBitsToFloat(decoder.readFixed32());
   }

   @Override
   public boolean readBool() throws IOException {
      return decoder.readVarint64() != 0L;
   }

   @Override
   public String readString() throws IOException {
      return decoder.readString();
   }

   @Override
   public byte[] readByteArray() throws IOException {
      int length = decoder.readVarint32();
      return decoder.readRawByteArray(length);
   }

   @Override
   public ByteBuffer readByteBuffer() throws IOException {
      int length = decoder.readVarint32();
      return decoder.readRawByteBuffer(length);
   }

   @Override
   public int readUInt32() throws IOException {
      return decoder.readVarint32();
   }

   @Override
   public int readEnum() throws IOException {
      return decoder.readVarint32();
   }

   @Override
   public int readSFixed32() throws IOException {
      return decoder.readFixed32();
   }

   @Override
   public long readSFixed64() throws IOException {
      return decoder.readFixed64();
   }

   @Override
   public int readSInt32() throws IOException {
      int value = decoder.readVarint32();
      // Unroll the bits in order to move the sign bit from position 0 back to position 31.
      return (value >>> 1) ^ -(value & 1);
   }

   @Override
   public long readSInt64() throws IOException {
      // Unroll the bits in order to move the sign bit from position 0 back to position 63.
      long value = decoder.readVarint64();
      return (value >>> 1) ^ -(value & 1);
   }

   @Override
   public int pushLimit(int limit) throws IOException {
      return decoder.pushLimit(limit);
   }

   @Override
   public void popLimit(int oldLimit) {
      decoder.popLimit(oldLimit);
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
   public TagReader getReader() {
      return this;
   }

   /**
    * @deprecated this will be removed in 5.0 together with {@link org.infinispan.protostream.MessageMarshaller}
    */
   @Deprecated
   public ProtoStreamReaderImpl getProtoStreamReader() {
      if (parent != null) {
         return parent.getProtoStreamReader();
      }
      if (reader == null) {
         reader = new ProtoStreamReaderImpl(this, serCtx);
      }
      return reader;
   }

   //todo [anistor] need to provide a safety mechanism to limit message size in bytes and message nesting depth on read ops
   private static abstract class Decoder {

      protected int globalLimit = Integer.MAX_VALUE;

      protected int lastTag;

      abstract boolean isAtEnd() throws IOException;

      final int readTag() throws IOException {
         if (isAtEnd()) {
            lastTag = 0;
            return 0;
         }
         long tag = readVarint64();
         lastTag = (int) tag;
         if (lastTag != tag) {
            throw new MalformedProtobufException("Found a protobuf tag (" + tag + ") greater than the largest allowed value");
         }

         // validate wire type
         WireType.fromTag(lastTag);

         if (WireType.getTagFieldNumber(lastTag) >= 1) {
            return lastTag;
         }
         throw new MalformedProtobufException("Found an invalid protobuf tag (" + lastTag + ") having a field number smaller than 1");
      }

      final void checkLastTagWas(int expectedTag) throws IOException {
         if (lastTag == expectedTag || expectedTag == 0 && isAtEnd()) {
            return;
         }
         if (expectedTag == 0) {
            throw new MalformedProtobufException("Expected ond of message but found tag " + lastTag);
         }
         throw new MalformedProtobufException("Protobuf message end group tag expected but found " + lastTag);
      }

      final boolean skipField(int tag) throws IOException {
         switch (WireType.getTagWireType(tag)) {
            case WireType.WIRETYPE_VARINT:
               skipVarint();
               return true;

            case WireType.WIRETYPE_FIXED32:
               skipRawBytes(FIXED_32_SIZE);
               return true;

            case WireType.WIRETYPE_FIXED64:
               skipRawBytes(FIXED_64_SIZE);
               return true;

            case WireType.WIRETYPE_LENGTH_DELIMITED:
               skipRawBytes(readVarint32());
               return true;

            case WireType.WIRETYPE_START_GROUP: {
               while (true) {
                  int t = readTag();
                  if (t == 0 || !skipField(t)) {
                     break;
                  }
               }
               checkLastTagWas(WireType.makeTag(WireType.getTagFieldNumber(tag), WireType.WIRETYPE_END_GROUP));
               return true;
            }

            case WireType.WIRETYPE_END_GROUP:
               return false;

            default:
               throw new MalformedProtobufException("Found a protobuf tag with invalid wire type : " + tag);
         }
      }

      abstract void skipVarint() throws IOException;

      abstract void skipRawBytes(int length) throws IOException;

      abstract String readString() throws IOException;

      abstract byte readRawByte() throws IOException;

      abstract byte[] readRawByteArray(int length) throws IOException;

      abstract ByteBuffer readRawByteBuffer(int length) throws IOException;

      /**
       * Reads a Varint (possibly 64 bits wide) and silently discards the upper bits if larger than 32 bits.
       */
      final int readVarint32() throws IOException {
         return (int) readVarint64();
      }

      abstract long readVarint64() throws IOException;

      abstract int readFixed32() throws IOException;

      abstract long readFixed64() throws IOException;

      abstract int pushLimit(int newLimit) throws IOException;

      abstract void popLimit(int oldLimit);

      /**
       * Sets a hard limit on how many bytes we can continue to read while parsing a message from current position. This is
       * useful to prevent corrupted or malicious messages with wrong length values to abuse memory allocation. Initially
       * this limit is set to {@code Integer.MAX_INT}, which means the protection mechanism is disabled by default.
       * The limit is only useful when processing streams. Setting a limit for a decoder backed by a byte array is useless
       * because the memory allocation already happened.
       */
      abstract int setGlobalLimit(int globalLimit);
   }

   private static final class ByteArrayDecoder extends Decoder {

      private final byte[] array;

      // all positions are absolute
      private final int start;
      private final int stop;
      private int pos;
      private int end; // limit adjusted

      // number of bytes we are allowed to read starting from start position
      private int limit;

      private ByteArrayDecoder(byte[] array, int offset, int length) {
         if (array == null) {
            throw new IllegalArgumentException("array cannot be null");
         }
         if (offset < 0) {
            throw new IllegalArgumentException("offset cannot be negative");
         }
         if (length < 0) {
            throw new IllegalArgumentException("length cannot be negative");
         }
         if (offset > array.length) {
            throw new IllegalArgumentException("start position is outside array bounds");
         }
         if (offset + length > array.length) {
            throw new IllegalArgumentException("end position is outside array bounds");
         }
         this.array = array;
         this.start = this.pos = offset;
         this.limit = length;
         this.stop = this.end = offset + length;
         adjustEnd();
      }

      @Override
      int pushLimit(int limit) throws IOException {
         if (limit < 0) {
            throw log.negativeLength();
         }
         limit += pos - start;
         int oldLimit = this.limit;
         if (limit > oldLimit) {
            // the end of a nested message cannot go beyond the end of the outer message
            throw log.messageTruncated();
         }
         this.limit = limit;
         adjustEnd();
         return oldLimit;
      }

      @Override
      void popLimit(int oldLimit) {
         limit = oldLimit;
         adjustEnd();
      }

      private void adjustEnd() {
         end = stop - start > limit ? start + limit : stop;
      }

      @Override
      boolean isAtEnd() {
         return pos == end;
      }

      @Override
      String readString() throws IOException {
         int length = readVarint32();
         if (length > 0 && length <= end - pos) {
            String value = new String(array, pos, length, UTF8);
            pos += length;
            return value;
         }
         if (length == 0) {
            return "";
         }
         if (length < 0) {
            throw log.negativeLength();
         }
         throw log.messageTruncated();
      }

      @Override
      ByteBuffer readRawByteBuffer(int length) throws IOException {
         if (length > 0 && length <= end - pos) {
            int from = pos;
            pos += length;
            return ByteBuffer.wrap(array, from, length).slice();
         }
         if (length == 0) {
            return EMPTY_BUFFER;
         }
         if (length < 0) {
            throw log.negativeLength();
         }
         throw log.messageTruncated();
      }

      @Override
      protected void skipVarint() throws IOException {
         if (end - pos >= MAX_VARINT_SIZE) {
            for (int i = 0; i < MAX_VARINT_SIZE; i++) {
               if (array[pos++] >= 0) {
                  return;
               }
            }
         } else {
            for (int i = 0; i < MAX_VARINT_SIZE; i++) {
               if (readRawByte() >= 0) {
                  return;
               }
            }
         }
         throw log.malformedVarint();
      }

      @Override
      long readVarint64() throws IOException {
         long value = 0;
         if (end - pos >= MAX_VARINT_SIZE) {
            for (int i = 0; i < 64; i += 7) {
               byte b = array[pos++];
               value |= (long) (b & 0x7F) << i;
               if (b >= 0) {
                  return value;
               }
            }
         } else {
            for (int i = 0; i < 64; i += 7) {
               byte b = readRawByte();
               value |= (long) (b & 0x7F) << i;
               if (b >= 0) {
                  return value;
               }
            }
         }
         throw log.malformedVarint();
      }

      @Override
      int readFixed32() throws IOException {
         try {
            int value = (array[pos] & 0xFF)
                  | ((array[pos + 1] & 0xFF) << 8)
                  | ((array[pos + 2] & 0xFF) << 16)
                  | ((array[pos + 3] & 0xFF) << 24);
            pos += FIXED_32_SIZE;
            return value;
         } catch (ArrayIndexOutOfBoundsException e) {
            throw log.messageTruncated(e);
         }
      }

      @Override
      long readFixed64() throws IOException {
         try {
            long value = (array[pos] & 0xFFL)
                  | ((array[pos + 1] & 0xFFL) << 8)
                  | ((array[pos + 2] & 0xFFL) << 16)
                  | ((array[pos + 3] & 0xFFL) << 24)
                  | ((array[pos + 4] & 0xFFL) << 32)
                  | ((array[pos + 5] & 0xFFL) << 40)
                  | ((array[pos + 6] & 0xFFL) << 48)
                  | ((array[pos + 7] & 0xFFL) << 56);
            pos += FIXED_64_SIZE;
            return value;
         } catch (ArrayIndexOutOfBoundsException e) {
            throw log.messageTruncated(e);
         }
      }

      @Override
      byte readRawByte() throws IOException {
         try {
            return array[pos++];
         } catch (ArrayIndexOutOfBoundsException e) {
            throw log.messageTruncated(e);
         }
      }

      @Override
      byte[] readRawByteArray(int length) throws IOException {
         if (length > 0 && length <= end - pos) {
            int from = pos;
            pos += length;
            return Arrays.copyOfRange(array, from, pos);
         }
         if (length == 0) {
            return EMPTY;
         }
         if (length < 0) {
            throw log.negativeLength();
         }
         throw log.messageTruncated();
      }

      @Override
      protected void skipRawBytes(int length) throws IOException {
         if (length < 0) {
            throw log.negativeLength();
         }
         if (length <= end - pos) {
            pos += length;
            return;
         }
         throw log.messageTruncated();
      }

      @Override
      int setGlobalLimit(int globalLimit) {
         return Integer.MAX_VALUE;
      }
   }

   private static final class ByteBufferDecoder extends Decoder {

      private final ByteBuffer buf;

      // all positions are absolute
      private final int start;
      private final int stop;
      private int end; // limit adjusted

      // number of bytes we are allowed to read starting from start position
      private int limit;

      private ByteBufferDecoder(ByteBuffer buf) {
         this.buf = buf;
         this.start = buf.position();
         this.limit = buf.remaining();
         this.stop = this.end = this.start + this.limit;
      }

      @Override
      int pushLimit(int limit) throws IOException {
         if (limit < 0) {
            throw log.negativeLength();
         }
         limit += buf.position() - start;
         int oldLimit = this.limit;
         if (limit > oldLimit) {
            // the end of a nested message cannot go beyond the end of the outer message
            throw log.messageTruncated();
         }
         this.limit = limit;
         adjustEnd();
         return oldLimit;
      }

      @Override
      void popLimit(int oldLimit) {
         limit = oldLimit;
         adjustEnd();
      }

      private void adjustEnd() {
         end = stop - start > limit ? start + limit : stop;
      }

      @Override
      boolean isAtEnd() {
         return buf.position() == end;
      }

      @Override
      String readString() throws IOException {
         int length = readVarint32();
         if (length > 0 && length <= end - buf.position()) {
            byte[] bytes = new byte[length];
            buf.get(bytes);
            return new String(bytes, 0, length, UTF8);
         }
         if (length == 0) {
            return "";
         }
         if (length < 0) {
            throw log.negativeLength();
         }
         throw log.messageTruncated();
      }

      @Override
      ByteBuffer readRawByteBuffer(int length) throws IOException {
         if (length > 0 && length <= end - buf.position()) {
            // apparently redundant cast, needed just for Java 8 binary compat, not needed for 9+
            ByteBuffer byteBuffer = (ByteBuffer) buf.slice().limit(length);
            buf.position(buf.position() + length);
            return byteBuffer;
         }
         if (length == 0) {
            return EMPTY_BUFFER;
         }
         if (length < 0) {
            throw log.negativeLength();
         }
         throw log.messageTruncated();
      }

      @Override
      protected void skipVarint() throws IOException {
         if (end - buf.position() >= MAX_VARINT_SIZE) {
            for (int i = 0; i < MAX_VARINT_SIZE; i++) {
               if (buf.get() >= 0) {
                  return;
               }
            }
         } else {
            for (int i = 0; i < MAX_VARINT_SIZE; i++) {
               if (readRawByte() >= 0) {
                  return;
               }
            }
         }
         throw log.malformedVarint();
      }

      @Override
      long readVarint64() throws IOException {
         long value = 0;
         if (end - buf.position() >= MAX_VARINT_SIZE) {
            for (int i = 0; i < 64; i += 7) {
               byte b = buf.get();
               value |= (long) (b & 0x7F) << i;
               if (b >= 0) {
                  return value;
               }
            }
         } else {
            for (int i = 0; i < 64; i += 7) {
               byte b = readRawByte();
               value |= (long) (b & 0x7F) << i;
               if (b >= 0) {
                  return value;
               }
            }
         }
         throw log.malformedVarint();
      }

      @Override
      int readFixed32() throws IOException {
         try {
            return buf.getInt();  //todo [anistor] should we worry about byte order?
         } catch (BufferUnderflowException e) {
            throw log.messageTruncated(e);
         }
      }

      @Override
      long readFixed64() throws IOException {
         try {
            return buf.getLong();  //todo [anistor] should we worry about byte order?
         } catch (BufferUnderflowException e) {
            throw log.messageTruncated(e);
         }
      }

      @Override
      byte readRawByte() throws IOException {
         try {
            return buf.get();
         } catch (BufferUnderflowException e) {
            throw log.messageTruncated(e);
         }
      }

      @Override
      byte[] readRawByteArray(int length) throws IOException {
         if (length > 0 && length <= end - buf.position()) {
            byte[] bytes = new byte[length];
            buf.get(bytes);
            return bytes;
         }
         if (length == 0) {
            return EMPTY;
         }
         if (length < 0) {
            throw log.negativeLength();
         }
         throw log.messageTruncated();
      }

      @Override
      protected void skipRawBytes(int length) throws IOException {
         if (length < 0) {
            throw log.negativeLength();
         }
         if (length <= end - buf.position()) {
            buf.position(buf.position() + length);
            return;
         }
         throw log.messageTruncated();
      }

      @Override
      int setGlobalLimit(int globalLimit) {
         return Integer.MAX_VALUE;
      }
   }

   private static final class InputStreamDecoder extends Decoder {

      private final InputStream in;

      private final byte[] buf;

      /**
       * The end position of buffered data. This is limit adjusted.
       */
      private int end;

      /**
       * Current position.
       */
      private int pos;

      /**
       * Number of bytes already read before the current buffer.
       */
      private int bytesBeforeStart = 0;

      /**
       * Number of bytes after the limit.
       */
      private int bytesAfterLimit = 0;

      /**
       * Absolute position (from start of input data) of the last byte we are allowed to read by last pushLimit.
       */
      private int limit = Integer.MAX_VALUE;

      private InputStreamDecoder(InputStream in, int bufferSize) {
         if (in == null) {
            throw new IllegalArgumentException("input stream cannot be null");
         }
         this.in = in;
         bufferSize = Math.max(bufferSize, MAX_VARINT_SIZE * 2);
         this.buf = new byte[bufferSize];
         this.end = 0;
         this.pos = 0;
      }

      @Override
      String readString() throws IOException {
         int length = readVarint32();
         if (length > 0 && length <= end - pos) {
            String value = new String(buf, pos, length, UTF8);
            pos += length;
            return value;
         }
         if (length == 0) {
            return "";
         }
         if (length < 0) {
            throw log.negativeLength();
         }
         if (length <= buf.length) {
            fillBuffer(length);
            String value = new String(buf, pos, length, UTF8);
            pos += length;
            return value;
         }
         return new String(readRawBytesLarge(length), UTF8);
      }

      @Override
      ByteBuffer readRawByteBuffer(int length) throws IOException {
         if (length <= end - pos && length > 0) {
            int from = pos;
            pos += length;
            return ByteBuffer.wrap(Arrays.copyOfRange(buf, from, pos));
         }
         if (length == 0) {
            return EMPTY_BUFFER;
         }
         if (length < 0) {
            throw log.negativeLength();
         }
         if (length <= buf.length) {
            fillBuffer(length);
            int from = pos;
            pos += length;
            return ByteBuffer.wrap(Arrays.copyOfRange(buf, from, pos));
         }
         // TODO [anistor] implement a readRawByteBufferLarge, using off-heap allocation
         return ByteBuffer.wrap(readRawBytesLarge(length));
      }

      @Override
      protected void skipVarint() throws IOException {
         if (end - pos >= MAX_VARINT_SIZE) {
            for (int i = 0; i < MAX_VARINT_SIZE; i++) {
               if (buf[pos++] >= 0) {
                  return;
               }
            }
         } else {
            for (int i = 0; i < MAX_VARINT_SIZE; i++) {
               if (readRawByte() >= 0) {
                  return;
               }
            }
         }
         throw log.malformedVarint();
      }

      @Override
      long readVarint64() throws IOException {
         long value = 0;
         if (end - pos >= MAX_VARINT_SIZE) {
            for (int i = 0; i < 64; i += 7) {
               byte b = buf[pos++];
               value |= (long) (b & 0x7F) << i;
               if (b >= 0) {
                  return value;
               }
            }
         } else {
            for (int i = 0; i < 64; i += 7) {
               byte b = readRawByte();
               value |= (long) (b & 0x7F) << i;
               if (b >= 0) {
                  return value;
               }
            }
         }
         throw log.malformedVarint();
      }

      @Override
      int readFixed32() throws IOException {
         if (end - pos < FIXED_32_SIZE) {
            fillBuffer(FIXED_32_SIZE);
         }
         int value = (buf[pos] & 0xFF)
               | ((buf[pos + 1] & 0xFF) << 8)
               | ((buf[pos + 2] & 0xFF) << 16)
               | ((buf[pos + 3] & 0xFF) << 24);
         pos += FIXED_32_SIZE;
         return value;
      }

      @Override
      long readFixed64() throws IOException {
         if (end - pos < FIXED_64_SIZE) {
            fillBuffer(FIXED_64_SIZE);
         }
         long value = (buf[pos] & 0xFFL)
               | ((buf[pos + 1] & 0xFFL) << 8)
               | ((buf[pos + 2] & 0xFFL) << 16)
               | ((buf[pos + 3] & 0xFFL) << 24)
               | ((buf[pos + 4] & 0xFFL) << 32)
               | ((buf[pos + 5] & 0xFFL) << 40)
               | ((buf[pos + 6] & 0xFFL) << 48)
               | ((buf[pos + 7] & 0xFFL) << 56);
         pos += FIXED_64_SIZE;
         return value;
      }

      @Override
      int setGlobalLimit(int globalLimit) {
         if (globalLimit < 0) {
            throw new IllegalArgumentException("Global limit cannot be negative: " + globalLimit);
         }
         int oldGlobalLimit = this.globalLimit;
         this.globalLimit = globalLimit;
         return oldGlobalLimit;
      }

      @Override
      int pushLimit(int limit) throws IOException {
         if (limit < 0) {
            throw log.negativeLength();
         }
         limit = bytesBeforeStart + pos + limit;
         int oldLimit = this.limit;
         if (limit > oldLimit) {
            // the end of a nested message cannot go beyond the end of the outer message
            throw log.messageTruncated();
         }
         this.limit = limit;
         adjustEnd();
         return oldLimit;
      }

      @Override
      void popLimit(int oldLimit) {
         limit = oldLimit;
         adjustEnd();
      }

      private void adjustEnd() {
         end += bytesAfterLimit;
         int absEnd = bytesBeforeStart + end;
         if (absEnd > limit) {
            bytesAfterLimit = absEnd - limit;
            end -= bytesAfterLimit;
         } else {
            bytesAfterLimit = 0;
         }
      }

      @Override
      boolean isAtEnd() throws IOException {
         return pos == end && !tryFillBuffer(1);
      }

      /**
       * Ensure that at least the requested number of bytes, or more, but no more than the buffer capacity are
       * available in the buffer.
       */
      private void fillBuffer(int requestedBytes) throws IOException {
         if (!tryFillBuffer(requestedBytes)) {
            throw log.messageTruncated();
         }
      }

      /**
       * Tries to fill the buffer with at least the requested number of bytes, or more, but no more than the buffer
       * capacity and indicates if the operation succeeded or failed either due to lack of available data in stream or
       * by reaching the limit set with pushLimit.
       */
      private boolean tryFillBuffer(int requestedBytes) throws IOException {
         if (requestedBytes + pos <= end) {
            // all requested bytes already available; nothing to do
            return true;
         }

         if (requestedBytes + bytesBeforeStart + pos > limit) {
            // oops, should not exceed the limit
            return false;
         }

         // slide existing data if some bytes are already consumed
         if (pos > 0) {
            if (end > pos) {
               System.arraycopy(buf, pos, buf, 0, end - pos);
            }
            bytesBeforeStart += pos;
            end -= pos;
            pos = 0;
         }

         // fill the space at the end with data from stream
         int read = in.read(buf, end, buf.length - end);
         if (read <= 0) {
            // EOF or maybe our buffer is full and we attempted to read 0 bytes
            return false;
         }
         end += read;

         if (requestedBytes + bytesBeforeStart - globalLimit > 0) {
            throw log.globalLimitExceeded();
         }

         adjustEnd();
         return end >= requestedBytes || end == buf.length || tryFillBuffer(requestedBytes);
      }

      @Override
      byte readRawByte() throws IOException {
         if (pos == end) {
            fillBuffer(1);
         }
         return buf[pos++];
      }

      @Override
      byte[] readRawByteArray(int length) throws IOException {
         if (length > 0 && length <= end - pos) {
            int from = pos;
            pos += length;
            return Arrays.copyOfRange(buf, from, pos);
         }
         if (length == 0) {
            return EMPTY;
         }
         if (length < 0) {
            throw log.negativeLength();
         }
         if (length <= buf.length) {
            fillBuffer(length);
            int from = pos;
            pos += length;
            return Arrays.copyOfRange(buf, from, pos);
         }
         return readRawBytesLarge(length);
      }

      // handle the unhappy case when the length does not fit in the internal buffer
      private byte[] readRawBytesLarge(int length) throws IOException {
         if (length < 0) {
            throw new IllegalArgumentException("Length must not be negative");
         }

         int total = bytesBeforeStart + pos + length;
         if (total - globalLimit > 0) {
            throw log.globalLimitExceeded();
         }
         if (total > limit) {
            // limit exceeded, skip up to limit and fail
            skipRawBytes(limit - bytesBeforeStart - pos);
            throw log.messageTruncated();
         }

         int buffered = end - pos;
         int needed = length - buffered;
         if (needed <= 0) {
            throw new IllegalStateException("The needed data already exists in buffer!");
         }
         int oldPos = pos;
         bytesBeforeStart += end;
         pos = 0;
         end = 0;

         if (needed < ProtobufUtil.DEFAULT_STREAM_BUFFER_SIZE || needed <= in.available()) {
            byte[] bytes = new byte[length];
            System.arraycopy(buf, oldPos, bytes, 0, buffered);
            while (buffered < bytes.length) {
               int read = in.read(bytes, buffered, length - buffered);
               if (read <= 0) {
                  throw log.messageTruncated();
               }
               bytesBeforeStart += read;
               buffered += read;
            }
            return bytes;
         }

         // read in segments to avoid allocating full length at once to prevent sudden death
         List<byte[]> segments = new ArrayList<>();
         while (needed > 0) {
            byte[] segment = new byte[Math.min(needed, ProtobufUtil.DEFAULT_STREAM_BUFFER_SIZE)];
            int segPos = 0;
            while (segPos < segment.length) {
               int read = in.read(segment, segPos, segment.length - segPos);
               if (read <= 0) {
                  throw log.messageTruncated();
               }
               segPos += read;
               bytesBeforeStart += read;
            }
            segments.add(segment);
            needed -= segment.length;
         }

         // stitch the segments and hope not to blow up
         byte[] bytes = new byte[length];
         System.arraycopy(buf, oldPos, bytes, 0, buffered);
         int segPos = buffered;
         for (int i = 0; i < segments.size(); i++) {
            byte[] segment = segments.get(i);
            System.arraycopy(segment, 0, bytes, segPos, segment.length);
            segPos += segment.length;
            segments.set(i, null);
         }
         return bytes;
      }

      @Override
      protected void skipRawBytes(int length) throws IOException {
         if (length <= end - pos && length >= 0) {
            pos += length;
         } else {
            if (length < 0) {
               throw log.negativeLength();
            }

            if (bytesBeforeStart + pos + length > limit) {
               // limit exceeded, skip up to limit and fail
               skipRawBytes(limit - bytesBeforeStart - pos);
               throw log.messageTruncated();
            }

            length -= end - pos;
            while (true) {
               pos = end;
               fillBuffer(1);
               if (length <= end) {
                  pos = length;
                  break;
               }
               length -= end;
            }
         }
      }
   }
}
