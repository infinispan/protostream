package org.infinispan.protostream;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public interface RawProtoStreamWriter {

   void flush() throws IOException;

   void writeTag(int number, int wireType) throws IOException;

   void writeRawVarint32(int value) throws IOException;

   void writeRawVarint64(long value) throws IOException;

   void writeString(int number, String value) throws IOException;

   void writeInt32(int number, int value) throws IOException;

   void writeFixed32(int number, int value) throws IOException;

   void writeUInt32(int number, int value) throws IOException;

   void writeSFixed32(int number, int value) throws IOException;

   void writeSInt32(int number, int value) throws IOException;

   void writeInt64(int number, long value) throws IOException;

   void writeUInt64(int number, long value) throws IOException;

   void writeFixed64(int number, long value) throws IOException;

   void writeSFixed64(int number, long value) throws IOException;

   void writeSInt64(int number, long value) throws IOException;

   void writeEnum(int number, int value) throws IOException;

   void writeBool(int number, boolean value) throws IOException;

   void writeDouble(int number, double value) throws IOException;

   void writeFloat(int number, float value) throws IOException;

   void writeBytes(int number, ByteBuffer value) throws IOException;

   void writeBytes(int number, byte[] value) throws IOException;

   void writeBytes(int number, byte[] value, int offset, int length) throws IOException;
}
