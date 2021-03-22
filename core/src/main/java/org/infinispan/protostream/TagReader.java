package org.infinispan.protostream;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
public interface TagReader extends RawProtoStreamReader {

   /**
    * Returns true if we have reached the end of input or the limit set with pushLimit.
    */
   boolean isAtEnd() throws IOException;

   /**
    * Reads a tag and returns it or returns 0 in case the input data is finished.
    */
   int readTag() throws IOException;

   /**
    * Checks that the previously read tag is the last tag of a message or group. The expected tag should be either 0 or
    * an end group tag.
    */
   void checkLastTagWas(int tag) throws IOException;

   /**
    * Skips a tag+value pair and returns true for normal tags but if the tag is an end group tag it returns false.
    */
   boolean skipField(int tag) throws IOException;

   boolean readBool() throws IOException;

   int readEnum() throws IOException;

   /**
    * Reads a {@code string} value.
    */
   String readString() throws IOException;

   /**
    * Reads a {@code bytes} value as a byte array.
    */
   byte[] readByteArray() throws IOException;

   /**
    * Reads a {@code bytes} value as a ByteBuffer.
    */
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

   /**
    * Sets a limit (based on the length of the length delimited value) when entering an embedded message.
    *
    * @return the previous limit.
    */
   int pushLimit(int limit) throws IOException;

   /**
    * Returns back to a previous limit returned by pushLimit.
    */
   void popLimit(int oldLimit);
}
