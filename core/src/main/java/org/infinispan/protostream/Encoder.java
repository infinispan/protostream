package org.infinispan.protostream;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface Encoder {
   void flush() throws IOException;

   void close() throws IOException;

   int remainingSpace();

   void writeUInt32Field(int fieldNumber, int value) throws IOException;

   void writeUInt64Field(int fieldNumber, long value) throws IOException;

   void writeFixed32Field(int fieldNumber, int value) throws IOException;

   void writeFixed64Field(int fieldNumber, long value) throws IOException;

   void writeBoolField(int fieldNumber, boolean value) throws IOException;

   void writeLengthDelimitedField(int fieldNumber, int length) throws IOException;

   void writeVarint32(int value) throws IOException;

   void writeVarint64(long value) throws IOException;

   void writeFixed32(int value) throws IOException;

   void writeFixed64(long value) throws IOException;

   void writeByte(byte value) throws IOException;

   void writeBytes(byte[] value, int offset, int length) throws IOException;

   void writeBytes(ByteBuffer value) throws IOException;

   default int skipFixedVarint() {
      throw new UnsupportedOperationException();
   }

   default void writePositiveFixedVarint(int pos) {
      throw new UnsupportedOperationException();
   }

   default boolean supportsFixedVarint() {
      return false;
   }
}
