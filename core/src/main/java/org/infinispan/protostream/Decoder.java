package org.infinispan.protostream;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface Decoder {
   int getEnd();

   int getPos();

   byte[] getBufferArray() throws IOException;

   boolean isAtEnd() throws IOException;

   int readTag() throws IOException;

   void checkLastTagWas(int expectedTag) throws IOException;

   boolean skipField(int tag) throws IOException;

   void skipVarint() throws IOException;

   void skipRawBytes(int length) throws IOException;

   String readString() throws IOException;

   byte readRawByte() throws IOException;

   byte[] readRawByteArray(int length) throws IOException;

   ByteBuffer readRawByteBuffer(int length) throws IOException;

   int readVarint32() throws IOException;

   long readVarint64() throws IOException;

   int readFixed32() throws IOException;

   long readFixed64() throws IOException;

   int pushLimit(int newLimit) throws IOException;

   void popLimit(int oldLimit);

   Decoder decoderFromLength(int length) throws IOException;

   /**
    * Sets a hard limit on how many bytes we can continue to read while parsing a message from current position. This is
    * useful to prevent corrupted or malicious messages with wrong length values to abuse memory allocation. Initially
    * this limit is set to {@code Integer.MAX_INT}, which means the protection mechanism is disabled by default.
    * The limit is only useful when processing streams. Setting a limit for a decoder backed by a byte array is useless
    * because the memory allocation already happened.
    */
   int setGlobalLimit(int globalLimit);
}
