package org.infinispan.protostream;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author anistor@redhat.com
 * @since 3.0
 * @deprecated replaced by {@link TagReader}. To be removed in 5.0.
 */
@Deprecated
public interface RawProtoStreamReader {

   int readTag() throws IOException;

   void checkLastTagWas(int tag) throws IOException;

   boolean skipField(int tag) throws IOException;

   boolean readBool() throws IOException;

   int readEnum() throws IOException;

   String readString() throws IOException;

   byte[] readByteArray() throws IOException;

   ByteBuffer readByteBuffer() throws IOException;

   double readDouble() throws IOException;

   float readFloat() throws IOException;

   long readInt64() throws IOException;

   long readUInt64() throws IOException;

   long readSInt64() throws IOException;

   long readFixed64() throws IOException;

   long readSFixed64() throws IOException;

   long readRawVarint64() throws IOException;

   int readInt32() throws IOException;

   int readUInt32() throws IOException;

   int readSInt32() throws IOException;

   int readFixed32() throws IOException;

   int readSFixed32() throws IOException;

   int readRawVarint32() throws IOException;

   int pushLimit(int byteLimit) throws IOException;

   void popLimit(int oldLimit);
}
