package org.infinispan.protostream;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Interface for direct read access to the protobuf data stream.
 *
 * @author anistor@redhat.com
 * @since 3.0
 * @deprecated replaced by {@link TagReader}. To be removed in 5.0.
 */
@Deprecated
public interface RawProtoStreamReader {

   int readTag() throws IOException;

   void checkLastTagWas(int tag) throws IOException;

   boolean skipField(int tag) throws IOException;

   /**
    * Read a {@code bool} value from the stream.
    */
   boolean readBool() throws IOException;

   /**
    * Reads an enum value from the stream as an integer value.
    */
   int readEnum() throws IOException;

   /**
    * Read a {@code string} value from the stream.
    */
   String readString() throws IOException;

   /**
    * Read a {@code bytes} value from the stream.
    */
   byte[] readByteArray() throws IOException;

   /**
    * Read a {@code bytes} value from the stream.
    */
   ByteBuffer readByteBuffer() throws IOException;

   /**
    * Read a {@code double} value from the stream.
    */
   double readDouble() throws IOException;

   /**
    * Read a {@code float} value from the stream.
    */
   float readFloat() throws IOException;

   /**
    * Read a {@code int64} value from the stream.
    */
   long readInt64() throws IOException;

   /**
    * Read a {@code uint64} value from the stream.
    */
   long readUInt64() throws IOException;

   /**
    * Read a {@code sint64} value from the stream.
    */
   long readSInt64() throws IOException;

   /**
    * Read a {@code fixed64} value from the stream.
    */
   long readFixed64() throws IOException;

   /**
    * Read a {@code sfixed64} value from the stream.
    */
   long readSFixed64() throws IOException;

   /**
    * Read a {@code int32} value from the stream.
    */
   int readInt32() throws IOException;

   /**
    * Read a {@code uint32} value from the stream.
    */
   int readUInt32() throws IOException;

   /**
    * Read a {@code sint32} value from the stream.
    */
   int readSInt32() throws IOException;

   /**
    * Read a {@code fixed32} value from the stream.
    */
   int readFixed32() throws IOException;

   /**
    * Read a {@code sfixed32} value from the stream.
    */
   int readSFixed32() throws IOException;

   default long readRawVarint64() throws IOException {
      return readUInt64();
   }

   default int readRawVarint32() throws IOException {
      return readUInt32();
   }

   int pushLimit(int byteLimit) throws IOException;

   void popLimit(int oldLimit);
}
