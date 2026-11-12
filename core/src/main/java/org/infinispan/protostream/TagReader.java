package org.infinispan.protostream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.infinispan.protostream.impl.TagReaderImpl;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
public interface TagReader {

   /**
    * Returns true if we have reached the end of input or the limit set with pushLimit.
    */
   boolean isAtEnd() throws IOException;

   /**
    * Reads a tag and returns it or returns 0 in case the input data is finished.
    */
   int readTag() throws IOException;

   /**
    * Reads a tag and returns it or returns 0 in case the input data is finished. Only to be used when
    * a reader knows the tag is always one byte. Note that if the tag is greater than one byte a negative number
    * is returned to signify that.
    */
   byte readByteTag() throws IOException;

   /**
    * Checks that the previously read tag is the last tag of a message or group. The expected tag should be either 0 or
    * an end group tag.
    */
   void checkLastTagWas(int tag) throws IOException;

   /**
    * Skips a tag+value pair and returns true for normal tags but if the tag is an end group tag it returns false.
    */
   boolean skipField(int tag) throws IOException;

   /**
    * Reads a {@code bool} value.
    */
   boolean readBool() throws IOException;

   /**
    * Reads an {@code enum} value.
    */
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

   /**
    * Reads a {@code double} value.
    */
   double readDouble() throws IOException;

   /**
    * Reads a {@code float} value.
    */
   float readFloat() throws IOException;

   /**
    * Reads an {@code int64} value.
    */
   long readInt64() throws IOException;

   /**
    * Reads a {@code uint64} value.
    */
   long readUInt64() throws IOException;

   /**
    * Reads a {@code sint64} value.
    */
   long readSInt64() throws IOException;

   /**
    * Reads a {@code fixed64} value.
    */
   long readFixed64() throws IOException;

   /**
    * Reads a {@code sfixed64} value.
    */
   long readSFixed64() throws IOException;

   /**
    * Reads an {@code int32} value.
    */
   int readInt32() throws IOException;

   /**
    * Reads a {@code uint32} value.
    */
   int readUInt32() throws IOException;

   /**
    * Reads a {@code sint32} value.
    */
   int readSInt32() throws IOException;

   /**
    * Reads a {@code fixed32} value.
    */
   int readFixed32() throws IOException;

   /**
    * Reads a {@code sfixed32} value.
    */
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

   /**
    * Creates a new {@link TagReader} which reads from the supplied {@link InputStream}.
    *
    * @param serCtx the {@link ImmutableSerializationContext} to use
    * @param input  the {@link InputStream} to read from
    * @param length the length of the message to read from the stream
    * @return a new {@link TagReader} instance
    * @throws IOException if there is an error reading from the input stream
    */
   static TagReader newInstance(ImmutableSerializationContext serCtx, InputStream input, int length) throws IOException {
      return TagReaderImpl.newInstance(serCtx, input, length);
   }

   /**
    * Creates a new {@link TagReader} which reads from the supplied {@link InputStream}.
    *
    * @param serCtx the {@link ImmutableSerializationContext} to use
    * @param input  the {@link InputStream} to read from
    * @return a new {@link TagReader} instance
    * @throws IOException if there is an error reading from the input stream
    * @deprecated since 6.0. Please use {@link #newInstance(ImmutableSerializationContext, InputStream, int)} instead
    * as the size of the message should be known for proper processing.
    */
   @Deprecated
   static TagReader newInstance(ImmutableSerializationContext serCtx, InputStream input) throws IOException {
      return TagReaderImpl.newInstance(serCtx, input);
   }

   /**
    * Creates a new {@link TagReader} which reads from the supplied {@link ByteBuffer}.
    *
    * @param serCtx the {@link ImmutableSerializationContext} to use
    * @param buf    the {@link ByteBuffer} to read from
    * @return a new {@link TagReader} instance
    */
   static TagReader newInstance(ImmutableSerializationContext serCtx, ByteBuffer buf) {
      return TagReaderImpl.newInstance(serCtx, buf);
   }

   /**
    * Creates a new {@link TagReader} which reads from the supplied byte array.
    *
    * @param serCtx the {@link ImmutableSerializationContext} to use
    * @param buf    the byte array to read from
    * @return a new {@link TagReader} instance
    */
   static TagReader newInstance(ImmutableSerializationContext serCtx, byte[] buf) {
      return TagReaderImpl.newInstance(serCtx, buf);
   }

   /**
    * Creates a new {@link TagReader} which reads from a section of the supplied byte array.
    *
    * @param serCtx the {@link ImmutableSerializationContext} to use
    * @param buf    the byte array to read from
    * @param offset the index of the first byte to read
    * @param length the number of bytes to read
    * @return a new {@link TagReader} instance
    */
   static TagReader newInstance(ImmutableSerializationContext serCtx, byte[] buf, int offset, int length) {
      return TagReaderImpl.newInstance(serCtx, buf, offset, length);
   }
}
