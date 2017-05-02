package org.infinispan.protostream;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
public interface TagReader extends RawProtoStreamReader {

   boolean isAtEnd() throws IOException;

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

   int readInt32() throws IOException;

   int readUInt32() throws IOException;

   int readSInt32() throws IOException;

   int readFixed32() throws IOException;

   int readSFixed32() throws IOException;

   int pushLimit(int limit) throws IOException;

   void popLimit(int oldLimit);
}
