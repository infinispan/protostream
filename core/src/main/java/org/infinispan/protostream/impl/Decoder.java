package org.infinispan.protostream.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.infinispan.protostream.MalformedProtobufException;
import org.infinispan.protostream.descriptors.WireType;

import static org.infinispan.protostream.descriptors.WireType.FIXED_32_SIZE;
import static org.infinispan.protostream.descriptors.WireType.FIXED_64_SIZE;
import static org.infinispan.protostream.descriptors.WireType.MAX_VARINT_SIZE;

abstract class Decoder {

   public static final int DEFAULT_BUFFER_SIZE = 4096;

   private static final Charset UTF8 = StandardCharsets.UTF_8;

   private static final byte[] EMPTY = new byte[0];
   private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.wrap(EMPTY);

   private static final String MESSAGE_TRUNCATED = "Input data ended unexpectedly in the middle of a field. The message is corrupt.";
   private static final String NEGATIVE_LENGTH = "Encountered a length delimited field with negative length.";
   private static final String MALFORMED_VARINT = "Encountered a malformed varint.";
   private static final String GLOBAL_LIMIT_EXCEEDED = "Protobuf message appears to be larger than the configured limit. The message is possibly corrupt.";

   protected int globalLimit = Integer.MAX_VALUE;

   protected int lastTag;

   static Decoder newInstance(InputStream input, int bufferSize) {
      return new InputStreamDecoder(input, bufferSize);
   }

   static Decoder newInstance(byte[] buf, int offset, int length) {
      return new ByteArrayDecoder(buf, offset, length);
   }

   static Decoder newInstance(ByteBuffer buf) {
      if (buf.hasArray()) {
         return newInstance(buf.array(), buf.arrayOffset() + buf.position(), buf.remaining());
      }

      // todo [anistor] copy to heap because I was too lazy to create a subclass that works with ByteBuffer.get instead of arrays
      byte[] buffer = new byte[buf.remaining()];
      buf.duplicate().get(buffer);
      return newInstance(buffer, 0, buffer.length);
   }

   private Decoder() {
   }

   /**
    * Returns true if we have reached the end of input or the limit set with pushLimit.
    */
   public abstract boolean isAtEnd() throws IOException;

   /**
    * Reads a tag and returns it or returns 0 in case the input data is finished.
    */
   public final int readTag() throws IOException {
      if (isAtEnd()) {
         lastTag = 0;
         return 0;
      }
      long tag = readVarint64();
      lastTag = (int) tag;
      if (lastTag != tag) {
         throw new MalformedProtobufException("Found a protobuf tag (" + tag + ") greater than the largest allowed value");
      }
      if (WireType.getTagFieldNumber(lastTag) >= 1) {
         return lastTag;
      }
      throw new MalformedProtobufException("Found an invalid protobuf tag (" + lastTag + ") having a field number smaller than 1");
   }

   /**
    * Checks that the previously read tag is the last tag of a message or group. The expected tag should be either 0 or
    * an end group tag.
    */
   public final void checkLastTagWas(int expectedTag) throws IOException {
      if (lastTag == expectedTag || expectedTag == 0 && isAtEnd()) {
         return;
      }
      if (expectedTag == 0) {
         throw new MalformedProtobufException("Expected ond of message but found tag " + lastTag);
      }
      throw new MalformedProtobufException("Protobuf message end group tag expected but found " + lastTag);
   }

   /**
    * Skips a tag+value pair and returns true for normal tags but if the tag is an end group tag it returns false.
    */
   public final boolean skipField(int tag) throws IOException {
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

   protected abstract void skipVarint() throws IOException;

   protected abstract void skipRawBytes(int length) throws IOException;

   /**
    * Reads a {@code string} value.
    */
   public abstract String readString() throws IOException;

   /**
    * Reads a {@code bytes} value as a byte array.
    */
   public final byte[] readByteArray() throws IOException {
      int length = readVarint32();
      return readRawBytes(length);
   }

   /**
    * Reads a {@code bytes} value as a ByteBuffer.
    */
   public abstract ByteBuffer readByteBuffer() throws IOException;

   public abstract byte readRawByte() throws IOException;

   public abstract byte[] readRawBytes(int length) throws IOException;

   /**
    * Reads a Varint (possibly 64 bits wide) and silently discards the upper bits if larger than 32 bits.
    */
   public final int readVarint32() throws IOException {
      return (int) readVarint64();
   }

   public abstract long readVarint64() throws IOException;

   public abstract int readFixed32() throws IOException;

   public abstract long readFixed64() throws IOException;

   public final double readDouble() throws IOException {
      return Double.longBitsToDouble(readFixed64());
   }

   public final float readFloat() throws IOException {
      return Float.intBitsToFloat(readFixed32());
   }

   public final boolean readBool() throws IOException {
      return readVarint64() != 0L;
   }

   public final int readSInt32() throws IOException {
      int value = readVarint32();
      // Unroll the bits in order to move the sign bit from position 0 back to position 31.
      return (value >>> 1) ^ -(value & 1);
   }

   public final long readSInt64() throws IOException {
      // Unroll the bits in order to move the sign bit from position 0 back to position 63.
      long value = readVarint64();
      return (value >>> 1) ^ -(value & 1);
   }

   /**
    * Sets a limit (based on the length of the length delimited value) when entering an embedded message.
    *
    * @return the previous limit.
    */
   public abstract int pushLimit(int newLimit) throws MalformedProtobufException;

   /**
    * Returns back to a previous limit returned by pushLimit.
    */
   public abstract void popLimit(int oldLimit);

   /**
    * Sets a hard limit on how many bytes we can continue to read while parsing a message from current position. This is
    * useful to prevent corrupted or malicious messages with wrong length values to abuse memory allocation. Initially
    * this limit is set to {@code Integer.MAX_INT}, which means the protection mechanism is disabled by default.
    * The limit is only useful when processing streams. Setting a limit for a decoder backed by a byte array is useless
    * because the memory allocation already happened.
    */
   public abstract int setGlobalLimit(int globalLimit);

   private static final class ByteArrayDecoder extends Decoder {

      private final byte[] buf;

      // all positions are absolute
      private final int start;
      private final int stop;
      private int pos;
      private int end; // limit adjusted

      // number of bytes we are allowed to read starting from start position
      private int limit;

      private ByteArrayDecoder(byte[] buf, int offset, int length) {
         if (offset < 0) {
            throw new IllegalArgumentException("Offset must be positive");
         }
         if (length < 0) {
            throw new IllegalArgumentException("Length must be positive");
         }
         this.buf = buf;
         this.start = offset;
         this.pos = offset;
         this.stop = this.end = offset + length;
         this.limit = length;
         adjustEnd();
      }

      @Override
      public int pushLimit(int limit) throws MalformedProtobufException {
         if (limit < 0) {
            throw new MalformedProtobufException(NEGATIVE_LENGTH);
         }
         limit += pos - start;
         int oldLimit = this.limit;
         if (limit > oldLimit) {
            // the end of a nested message cannot go beyond the end of the outer message
            throw new MalformedProtobufException(MESSAGE_TRUNCATED);
         }
         this.limit = limit;
         adjustEnd();
         return oldLimit;
      }

      @Override
      public void popLimit(int oldLimit) {
         limit = oldLimit;
         adjustEnd();
      }

      private void adjustEnd() {
         end = stop - start > limit ? start + limit : stop;
      }

      @Override
      public boolean isAtEnd() {
         return pos == end;
      }

      @Override
      public String readString() throws IOException {
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
            throw new MalformedProtobufException(NEGATIVE_LENGTH);
         }
         throw new MalformedProtobufException(MESSAGE_TRUNCATED);
      }

      @Override
      public ByteBuffer readByteBuffer() throws IOException {
         int length = readVarint32();
         if (length > 0 && length <= end - pos) {
            int from = pos;
            pos += length;
            return ByteBuffer.wrap(buf, from, length).slice();
         }
         if (length == 0) {
            return EMPTY_BUFFER;
         }
         if (length < 0) {
            throw new MalformedProtobufException(NEGATIVE_LENGTH);
         }
         throw new MalformedProtobufException(MESSAGE_TRUNCATED);
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
         throw new MalformedProtobufException(MALFORMED_VARINT);
      }

      @Override
      public long readVarint64() throws IOException {
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
         throw new MalformedProtobufException(MALFORMED_VARINT);
      }

      @Override
      public int readFixed32() throws IOException {
         try {
            int value = (buf[pos] & 0xFF)
                  | ((buf[pos + 1] & 0xFF) << 8)
                  | ((buf[pos + 2] & 0xFF) << 16)
                  | ((buf[pos + 3] & 0xFF) << 24);
            pos += FIXED_32_SIZE;
            return value;
         } catch (ArrayIndexOutOfBoundsException e) {
            throw new MalformedProtobufException(MESSAGE_TRUNCATED, e);
         }
      }

      @Override
      public long readFixed64() throws IOException {
         try {
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
         } catch (ArrayIndexOutOfBoundsException e) {
            throw new MalformedProtobufException(MESSAGE_TRUNCATED, e);
         }
      }

      @Override
      public byte readRawByte() throws IOException {
         try {
            return buf[pos++];
         } catch (ArrayIndexOutOfBoundsException e) {
            throw new MalformedProtobufException(MESSAGE_TRUNCATED, e);
         }
      }

      @Override
      public byte[] readRawBytes(int length) throws IOException {
         if (length > 0 && length <= end - pos) {
            int from = pos;
            pos += length;
            return Arrays.copyOfRange(buf, from, pos);
         }
         if (length == 0) {
            return EMPTY;
         }
         if (length < 0) {
            throw new MalformedProtobufException(NEGATIVE_LENGTH);
         }
         throw new MalformedProtobufException(MESSAGE_TRUNCATED);
      }

      @Override
      protected void skipRawBytes(int length) throws IOException {
         if (length < 0) {
            throw new MalformedProtobufException(NEGATIVE_LENGTH);
         }
         if (length <= end - pos) {
            pos += length;
            return;
         }
         throw new MalformedProtobufException(MESSAGE_TRUNCATED);
      }

      @Override
      public int setGlobalLimit(int globalLimit) {
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
       * Number of bytes read before the current buffer.
       */
      private int bytesBeforeStart = 0;

      /**
       * Number of bytes after the limit.
       */
      private int bytesAfterLimit = 0;

      /**
       * Absolute position (from start of input data) of the last byte we are allowed to read.
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
      public String readString() throws IOException {
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
            throw new MalformedProtobufException(NEGATIVE_LENGTH);
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
      public ByteBuffer readByteBuffer() throws IOException {
         int length = readVarint32();
         if (length <= end - pos && length > 0) {
            int from = pos;
            pos += length;
            return ByteBuffer.wrap(Arrays.copyOfRange(buf, from, pos));
         }
         if (length == 0) {
            return EMPTY_BUFFER;
         }
         if (length < 0) {
            throw new MalformedProtobufException(NEGATIVE_LENGTH);
         }
         if (length <= buf.length) {
            fillBuffer(length);
            int from = pos;
            pos += length;
            return ByteBuffer.wrap(Arrays.copyOfRange(buf, from, pos));
         }
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
         throw new MalformedProtobufException(MALFORMED_VARINT);
      }

      @Override
      public long readVarint64() throws IOException {
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
         throw new MalformedProtobufException(MALFORMED_VARINT);
      }

      @Override
      public int readFixed32() throws IOException {
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
      public long readFixed64() throws IOException {
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
      public int setGlobalLimit(int globalLimit) {
         if (globalLimit < 0) {
            throw new IllegalArgumentException("Global limit cannot be negative: " + globalLimit);
         }
         int oldGlobalLimit = this.globalLimit;
         this.globalLimit = globalLimit;
         return oldGlobalLimit;
      }

      @Override
      public int pushLimit(int limit) throws MalformedProtobufException {
         if (limit < 0) {
            throw new MalformedProtobufException(NEGATIVE_LENGTH);
         }
         limit = bytesBeforeStart + pos + limit;
         int oldLimit = this.limit;
         if (limit > oldLimit) {
            // the end of a nested message cannot go beyond the end of the outer message
            throw new MalformedProtobufException(MESSAGE_TRUNCATED);
         }
         this.limit = limit;
         adjustEnd();
         return oldLimit;
      }

      @Override
      public void popLimit(int oldLimit) {
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
      public boolean isAtEnd() throws IOException {
         return pos == end && !tryFillBuffer(1);
      }

      /**
       * Ensure that at least the requested number of bytes, or more, but no more than the buffer capacity are
       * available in the buffer.
       */
      private void fillBuffer(int requestedBytes) throws IOException {
         if (!tryFillBuffer(requestedBytes)) {
            throw new MalformedProtobufException(MESSAGE_TRUNCATED);
         }
      }

      /**
       * Tries to fill the buffer with at least the requested number of bytes, or more, but no more than the buffer
       * capacity and indicates if the operation succeeded or failed either due to lack of available data in stream or
       * by reaching the limit.
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
            return false;
         }

         end += read;
         if (requestedBytes + bytesBeforeStart - globalLimit > 0) {
            throw new MalformedProtobufException(GLOBAL_LIMIT_EXCEEDED);
         }

         adjustEnd();
         return end >= requestedBytes || end == buf.length || tryFillBuffer(requestedBytes);
      }

      @Override
      public byte readRawByte() throws IOException {
         if (pos == end) {
            fillBuffer(1);
         }
         return buf[pos++];
      }

      @Override
      public byte[] readRawBytes(int length) throws IOException {
         if (length > 0 && length <= end - pos) {
            int from = pos;
            pos += length;
            return Arrays.copyOfRange(buf, from, pos);
         }
         if (length == 0) {
            return EMPTY;
         }
         if (length < 0) {
            throw new MalformedProtobufException(NEGATIVE_LENGTH);
         }
         if (length <= buf.length) {
            fillBuffer(length);
            int from = pos;
            pos += length;
            return Arrays.copyOfRange(buf, from, pos);
         }
         return readRawBytesLarge(length);
      }

      private byte[] readRawBytesLarge(int length) throws IOException {
         if (length < 0) {
            throw new IllegalArgumentException("Length must not be negative");
         }

         int total = bytesBeforeStart + pos + length;
         if (total - globalLimit > 0) {
            throw new MalformedProtobufException(GLOBAL_LIMIT_EXCEEDED);
         }
         if (total > limit) {
            // limit exceeded, skip up to limit and fail
            skipRawBytes(limit - bytesBeforeStart - pos);
            throw new MalformedProtobufException(MESSAGE_TRUNCATED);
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

         if (needed < DEFAULT_BUFFER_SIZE || needed <= in.available()) {
            byte[] bytes = new byte[length];
            System.arraycopy(buf, oldPos, bytes, 0, buffered);
            while (buffered < bytes.length) {
               int read = in.read(bytes, buffered, length - buffered);
               if (read <= 0) {
                  throw new MalformedProtobufException(MESSAGE_TRUNCATED);
               }
               bytesBeforeStart += read;
               buffered += read;
            }
            return bytes;
         }

         // read in segments to avoid allocating full length at once
         List<byte[]> segments = new ArrayList<>();
         while (needed > 0) {
            byte[] segment = new byte[Math.min(needed, DEFAULT_BUFFER_SIZE)];
            int segPos = 0;
            while (segPos < segment.length) {
               int read = in.read(segment, segPos, segment.length - segPos);
               if (read <= 0) {
                  throw new MalformedProtobufException(MESSAGE_TRUNCATED);
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
         for (byte[] segment : segments) {
            System.arraycopy(segment, 0, bytes, segPos, segment.length);
            segPos += segment.length;
         }
         return bytes;
      }

      @Override
      protected void skipRawBytes(int length) throws IOException {
         if (length <= end - pos && length >= 0) {
            pos += length;
         } else {
            if (length < 0) {
               throw new MalformedProtobufException(NEGATIVE_LENGTH);
            }

            if (bytesBeforeStart + pos + length > limit) {
               // limit exceeded, skip up to limit and fail
               skipRawBytes(limit - bytesBeforeStart - pos);
               throw new MalformedProtobufException(MESSAGE_TRUNCATED);
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
