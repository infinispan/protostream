package org.infinispan.protostream;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.infinispan.protostream.descriptors.WireType;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
public interface TagWriter extends RawProtoStreamWriter {

   // start low level ops
   void flush() throws IOException;

   /**
    * Invoke after done with writer, this implies a flush if necessary
    * It is necessary to invoke this on a writer returned from {@link #subWriter(int)} to actually push the data
    */
   void close() throws IOException;

   default void writeTag(int number, int wireType) throws IOException {
      writeVarint32(WireType.makeTag(number, wireType));
   }

   default void writeTag(int number, WireType wireType) throws IOException {
      writeVarint32(WireType.makeTag(number, wireType));
   }

   void writeVarint32(int value) throws IOException;

   void writeVarint64(long value) throws IOException;

   void writeRawBytes(byte[] value, int offset, int length) throws IOException;
   // end low level ops

   // start high level ops
   void writeString(int number, String value) throws IOException;

   default void writeInt32(int number, int value) throws IOException {
      if (value >= 0) {
         writeUInt32(number, value);
      } else {
         writeUInt64(number, value);
      }
   }

   void writeUInt32(int number, int value) throws IOException;

   default void writeSInt32(int number, int value) throws IOException {
      // Roll the bits in order to move the sign bit from position 31 to position 0, to reduce the wire length of negative numbers.
      writeUInt32(number, (value << 1) ^ (value >> 31));
   }

   void writeFixed32(int number, int value) throws IOException;

   default void writeSFixed32(int number, int value) throws IOException {
      writeFixed32(number, value);
   }

   void writeInt64(int number, long value) throws IOException;

   void writeUInt64(int number, long value) throws IOException;

   default void writeSInt64(int number, long value) throws IOException {
      // Roll the bits in order to move the sign bit from position 63 to position 0, to reduce the wire length of negative numbers.
      writeUInt64(number, (value << 1) ^ (value >> 63));
   }

   void writeFixed64(int number, long value) throws IOException;

   default void writeSFixed64(int number, long value) throws IOException {
      writeFixed64(number, value);
   }

   default void writeEnum(int number, int value) throws IOException {
      writeInt32(number, value);
   }

   void writeBool(int number, boolean value) throws IOException;

   default void writeDouble(int number, double value) throws IOException {
      writeFixed64(number, Double.doubleToRawLongBits(value));
   }

   default void writeFloat(int number, float value) throws IOException {
      writeFixed32(number, Float.floatToRawIntBits(value));
   }

   void writeBytes(int number, ByteBuffer value) throws IOException;

   default void writeBytes(int number, byte[] value) throws IOException {
      writeBytes(number, value, 0, value.length);
   }

   void writeBytes(int number, byte[] value, int offset, int length) throws IOException;
   // end high level ops

   /**
    * Used to write a sub message that can be optimized by implementation. When the sub writer is complete, flush
    * should be invoked to ensure
    * @return
    * @throws IOException
    */
   TagWriter subWriter(int number, boolean nested) throws IOException;
}
