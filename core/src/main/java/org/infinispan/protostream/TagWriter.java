package org.infinispan.protostream;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.infinispan.protostream.descriptors.WireType;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
public interface TagWriter extends AutoCloseable {

   // start low level ops
   void flush() throws IOException;

   @Override
   default void close() throws IOException {
      flush();
   }

   void writeTag(int number, int wireType) throws IOException;

   void writeTag(int number, WireType wireType) throws IOException;

   void writeVarint32(int value) throws IOException;

   void writeVarint64(long value) throws IOException;

   void writeRawBytes(byte[] value, int offset, int length) throws IOException;
   // end low level ops

   // start high level ops
   void writeString(int number, String value) throws IOException;

   void writeInt32(int number, int value) throws IOException;

   void writeUInt32(int number, int value) throws IOException;

   void writeSInt32(int number, int value) throws IOException;

   void writeFixed32(int number, int value) throws IOException;

   void writeSFixed32(int number, int value) throws IOException;

   void writeInt64(int number, long value) throws IOException;

   void writeUInt64(int number, long value) throws IOException;

   void writeSInt64(int number, long value) throws IOException;

   void writeFixed64(int number, long value) throws IOException;

   void writeSFixed64(int number, long value) throws IOException;

   void writeEnum(int number, int value) throws IOException;

   void writeBool(int number, boolean value) throws IOException;

   void writeDouble(int number, double value) throws IOException;

   void writeFloat(int number, float value) throws IOException;

   void writeBytes(int number, ByteBuffer value) throws IOException;

   void writeBytes(int number, byte[] value) throws IOException;

   void writeBytes(int number, byte[] value, int offset, int length) throws IOException;
   // end high level ops

   /**
    * Used to write a sub message that can be optimized by implementation. When the sub writer is complete, flush
    * should also be invoked to ensure all data is pushed to the parent writer as necessary.
    * @param number the field type number
    * @param nested if the subWriter is nested in another message
    * @return a writer that will handle the nested message writing
    * @throws IOException if there is an exception while writing data
    */
   TagWriter subWriter(int number, boolean nested) throws IOException;
}
