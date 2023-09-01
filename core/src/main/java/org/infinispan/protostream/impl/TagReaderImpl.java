package org.infinispan.protostream.impl;

import static org.infinispan.protostream.descriptors.WireType.FIXED_32_SIZE;
import static org.infinispan.protostream.descriptors.WireType.FIXED_64_SIZE;
import static org.infinispan.protostream.descriptors.WireType.MAX_VARINT_SIZE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.MalformedProtobufException;
import org.infinispan.protostream.ProtobufTagMarshaller;
import org.infinispan.protostream.TagReader;
import org.infinispan.protostream.descriptors.WireType;

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
      return new TagReaderImpl((TagReaderImpl) parent, new InputStreamDecoder(input));
   }

   public static TagReaderImpl newNestedInstance(ProtobufTagMarshaller.ReadContext parent, byte[] buf) {
      return new TagReaderImpl((TagReaderImpl) parent, new ByteArrayDecoder(buf, 0, buf.length));
   }

   public static TagReaderImpl newInstance(ImmutableSerializationContext serCtx, InputStream input) {
      return new TagReaderImpl((SerializationContextImpl) serCtx, new InputStreamDecoder(input));
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
   public boolean readBool() throws IOException {
      return decoder.readRawByte() != 0L;
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
   public TagReader subReaderFromArray() throws IOException {
      int length = decoder.readVarint32();
      return new TagReaderImpl(serCtx, decoder.decoderFromLength(length));
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


   @Override
   public byte[] fullBufferArray() throws IOException {
      checkBufferUnused("fullBufferArray");
      return decoder.getBufferArray();
   }

   @Override
   public InputStream fullBufferInputStream() throws IOException {
      checkBufferUnused("fullBufferInputStream");
      
      if (isInputStream()) {
         return ((InputStreamDecoder)decoder).getInputStream();
      } else {
         return new ByteArrayInputStream(decoder.getBufferArray());
      }
   }

   private void checkBufferUnused(String methodName) {
      if (decoder.getPos() > 0) {
         throw new IllegalStateException(methodName + " in marshaller can only be used on an unprocessed buffer");
      }
   }

   @Override
   public boolean isInputStream() {
      return (decoder instanceof InputStreamDecoder);
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

      abstract int getEnd();

      abstract int getPos();

      abstract byte[] getBufferArray() throws IOException;

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

      abstract Decoder decoderFromLength(int length) throws IOException;

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
      private int pos;
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
         this.limit = offset + length;
      }

      @Override
      int pushLimit(int limit) throws IOException {
         if (limit < 0) {
            throw log.negativeLength();
         }
         limit += pos;
         int oldLimit = this.limit;
         if (limit > oldLimit) {
            // the end of a nested message cannot go beyond the end of the outer message
            throw log.messageTruncated();
         }
         this.limit = limit;
         return oldLimit;
      }

      @Override
      void popLimit(int oldLimit) {
         limit = oldLimit;
      }

      @Override
      int getEnd() {
         return limit;
      }

      @Override
      int getPos() {
         return pos - start;
      }

      @Override
      byte[] getBufferArray() throws IOException {
         if (pos == 0 && limit == array.length) {
            return array;
         } else {
            return Arrays.copyOfRange(array, pos, limit);
         }
      }

      @Override
      boolean isAtEnd() {
         return pos == limit;
      }

      @Override
      String readString() throws IOException {
         int length = readVarint32();
         if (length > 0 && length <= limit - pos) {
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
         if (length > 0 && length <= limit - pos) {
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
         if (limit - pos >= MAX_VARINT_SIZE) {
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
         if (limit - pos >= MAX_VARINT_SIZE) {
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
         if (length > 0 && length <= limit - pos) {
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
         if (length <= limit - pos) {
            pos += length;
            return;
         }
         throw log.messageTruncated();
      }

      @Override
      Decoder decoderFromLength(int length) throws IOException {
         int currentPos = pos;
         if (length + currentPos > limit) {
            throw log.messageTruncated();
         }
         pos += length;
         return new ByteArrayDecoder(array, currentPos, length);
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
      int getEnd() {
         return end;
      }

      @Override
      int getPos() {
         return buf.position() - start;
      }

      @Override
      byte[] getBufferArray() throws IOException {
         if (end < limit) {
            throw log.messageTruncated();
         }
         int pos = buf.position();
         int remaining = buf.remaining();
         if (pos == 0 && end == remaining && buf.hasArray()) {
            return buf.array();
         } else {
            byte[] bytes = new byte[remaining];
            buf.get(bytes, 0, remaining);
            return bytes;
         }
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
      Decoder decoderFromLength(int length) throws IOException {
         ByteBuffer buffer = buf.slice().limit(length);
         buf.position(buffer.position() + length);
         return new ByteBufferDecoder(buffer);
      }

      @Override
      int setGlobalLimit(int globalLimit) {
         return Integer.MAX_VALUE;
      }
   }

   private static final class InputStreamDecoder extends Decoder {

      private final InputStream in;

      /**
       * Current position.
       */
      private int pos;

      /**
       * Absolute position (from start of input data) of the last byte we are allowed to read by last pushLimit.
       */
      private int limit = Integer.MAX_VALUE;

      private InputStreamDecoder(InputStream in) {
         if (in == null) {
            throw new IllegalArgumentException("input stream cannot be null");
         }
         if (in.markSupported()) {
            this.in = in;
         } else {
            this.in = new PushbackInputStream(in);
         }
      }

      @Override
      String readString() throws IOException {
         int length = readVarint32();
         if (length > 0 && length <= limit - pos) {
            byte[] bytes = readRawByteArray(length);
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
         if (length == 0) {
            return EMPTY_BUFFER;
         }
         byte[] bytes = readRawByteArray(length);
         return ByteBuffer.wrap(bytes);
      }

      @Override
      protected void skipVarint() throws IOException {
         for (int i = 0; i < MAX_VARINT_SIZE; i++) {
            if (readRawByte() >= 0) {
               return;
            }
         }
         throw log.malformedVarint();
      }

      @Override
      long readVarint64() throws IOException {
         long value = 0;
         for (int i = 0; i < 64; i += 7) {
            byte b = readRawByte();
            value |= (long) (b & 0x7F) << i;
            if (b >= 0) {
               return value;
            }
         }
         throw log.malformedVarint();
      }

      @Override
      int readFixed32() throws IOException {
         if (limit - pos < FIXED_32_SIZE) {
            throw log.messageTruncated();
         }
         return (readRawByte() & 0xFF)
               | ((readRawByte() & 0xFF) << 8)
               | ((readRawByte() & 0xFF) << 16)
               | ((readRawByte() & 0xFF) << 24);
      }

      @Override
      long readFixed64() throws IOException {
         if (limit - pos < FIXED_64_SIZE) {
            throw log.messageTruncated();
         }
         return (readRawByte() & 0xFFL)
               | ((readRawByte() & 0xFFL) << 8)
               | ((readRawByte() & 0xFFL) << 16)
               | ((readRawByte() & 0xFFL) << 24)
               | ((readRawByte() & 0xFFL) << 32)
               | ((readRawByte() & 0xFFL) << 40)
               | ((readRawByte() & 0xFFL) << 48)
               | ((readRawByte() & 0xFFL) << 56);
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
         limit = pos + limit;
         int oldLimit = this.limit;
         if (limit > oldLimit) {
            // the end of a nested message cannot go beyond the end of the outer message
            throw log.messageTruncated();
         }
         this.limit = limit;
         return oldLimit;
      }

      @Override
      void popLimit(int oldLimit) {
         limit = oldLimit;
      }

      @Override
      int getEnd() {
         return limit;
      }

      @Override
      int getPos() {
         return pos;
      }

      @Override
      byte[] getBufferArray() throws IOException {
         int readLimit = Math.min(limit, globalLimit);
         if (readLimit == Integer.MAX_VALUE) {
            pos = Integer.MAX_VALUE;
            return in.readAllBytes();
         } else {
            int length = readLimit - pos;
            return readRawByteArray(length);
         }
      }

      InputStream getInputStream() {
         return in;
      }

      @Override
      boolean isAtEnd() throws IOException {
         if (pos == limit) {
            return true;
         }
         if (in.available() > 0) {
            return false;
         }
         if (in instanceof PushbackInputStream) {
            return isPushbackDone();
         }
         return isMarkDone();
      }

      private boolean isPushbackDone() throws IOException {
         int intVal = in.read();
         if (intVal < 0) {
            return true;
         }
         ((PushbackInputStream) in).unread(intVal);
         return false;
      }

      private boolean isMarkDone() throws IOException {
         in.mark(1);
         if (in.read() < 0) {
            return true;
         }
         in.reset();
         return false;
      }


      @Override
      byte readRawByte() throws IOException {
         if (pos == limit) {
            throw log.messageTruncated();
         }
         int byteValue = in.read();
         if (byteValue < 0) {
            throw log.messageTruncated();
         }
         pos++;
         return (byte) byteValue;
      }

      @Override
      byte[] readRawByteArray(int length) throws IOException {
         if (length > 0 && length <= limit - pos) {
            pos += length;
            if (pos > globalLimit) {
               throw log.globalLimitExceeded();
            }
            int readTotal = 0;
            int readAmount;
            byte[] array = new byte[length];
            while ((readAmount = in.read(array, readTotal, length - readTotal)) != -1) {
               readTotal += readAmount;
               if (readTotal == length) {
                  break;
               }
            }
            if (readTotal != length) {
               throw log.messageTruncated();
            }
            return array;
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
         if (length <= limit - pos && length >= 0) {
            pos += length;
            skipNBytes(length);
         } else {
            if (length < 0) {
               throw log.negativeLength();
            }
            throw log.messageTruncated();
         }
      }

      // Copied from InputStream, we can't use Java 12 or newer just yet, can be removed when on a newer version.
      private void skipNBytes(long n) throws IOException {
         while (n > 0) {
            long ns = in.skip(n);
            if (ns > 0 && ns <= n) {
               // adjust number to skip
               n -= ns;
            } else if (ns == 0) { // no bytes skipped
               // read one byte to check for EOS
               if (in.read() == -1) {
                  throw log.messageTruncated();
               }
               // one byte read so decrement number to skip
               n--;
            } else { // skipped negative or too many bytes
               throw new IOException("Unable to skip exactly");
            }
         }
      }

      @Override
      Decoder decoderFromLength(int length) throws IOException {
         byte[] bytes = readRawByteArray(length);
         return new ByteArrayDecoder(bytes, 0, length);
      }
   }

}
