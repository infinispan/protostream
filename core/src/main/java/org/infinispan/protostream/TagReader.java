package org.infinispan.protostream;

import java.io.IOException;
import java.io.InputStream;
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

   default int readEnum() throws IOException {
      return readInt32();
   }

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

   /**
    * Similar to {@link #readByteArray()} except that the reader impl may optimize creation of a sub TagReader from
    * itself, possibly avoiding byte[] allocations
    * @return a new TagReader
    */
   TagReader subReaderFromArray() throws IOException;

   default double readDouble() throws IOException {
      return Double.longBitsToDouble(readFixed64());
   }

   default float readFloat() throws IOException {
      return Float.intBitsToFloat(readFixed32());
   }

   long readInt64() throws IOException;

   default long readUInt64() throws IOException {
      return readInt64();
   }

   long readSInt64() throws IOException;

   long readFixed64() throws IOException;

   default long readSFixed64() throws IOException {
      return readFixed64();
   }

   int readInt32() throws IOException;

   default int readUInt32() throws IOException {
      return readInt32();
   }

   int readSInt32() throws IOException;

   int readFixed32() throws IOException;

   default int readSFixed32() throws IOException {
      return readFixed32();
   }

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

   /** 
    * Returns the full buffer array allowing an alternative protobuf parser to be used in marshallers,
    * and at the root message level. This should not be mixed with the other tag reader read***() 
    * methods, or else an IllegalStateException will be thrown. Therefore you cannot mix protostream
    * annotated models with other parsers reading the raw payload array. 
    */
   byte[] fullBufferArray() throws IOException;

   /** 
    * Returns the input stream allowing an alternative protobuf parser to be used in marshallers,
    * and at the root message level. This should not be mixed with the other tag reader read***() 
    * methods, or else an IllegalStateException will be thrown. Therefore you cannot mix protostream
    * annotated models with other parsers reading the raw payload input stream.  
    */
   InputStream fullBufferInputStream() throws IOException;

   /**
    * @return Returns true if the original payload is InputStream based.
    */
   boolean isInputStream(); 
}
