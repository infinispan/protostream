package org.infinispan.protostream;

import java.io.Closeable;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A byte output stream that exposes positional arguments in order to allow existing bytes in the stream to be overwritten.
 *
 * @author Ryan Emerson
 * @since 6.0
 */
public interface RandomAccessOutputStream extends Closeable {

   /**
    * @return the position in the stream where the next {@link #write(int)} will be written.
    */
   int getPosition();

   /**
    * Set the position in the stream where the next {@link #write(int)} will be written.
    */
   void setPosition(int position);

   /**
    * Ensure that the stream has sufficient capacity to hold at least `capacity` bytes.
    *
    * @param capacity the minimum number of bytes that should be writeable
    */
   void ensureCapacity(int capacity);

   /**
    * Reset the position of the stream to zero, so that all currently accumulated output in the stream is discarded.
    * The output stream can then be used again, reusing the already allocated buffer space.
    */
   default void reset() {
      setPosition(0);
   }

   /**
    * @return a {@link ByteBuffer} representation of the stream
    */
   ByteBuffer getByteBuffer();

   /**
    * @return a trimmed {@link byte[]} instance based upon the current {@link #getPosition()} of the array
    */
   byte[] toByteArray();

   /**
    * @param position the position in the stream to read the byte from
    * @return the byte associated with the specified position
    */
   byte get(int position);

   /**
    * Write a single byte to the head of the stream.
    *
    * @param b the byte to be written
    */
   default void write(int b) throws IOException {
      write(getPosition(), b);
   }

   /**
    * Write all bytes to the head of the stream.
    *
    * @param b the array of bytes to be written
    */
   default void write(byte[] b) throws IOException {
      write(b, 0, b.length);
   }

   /**
    * Write all bytes to the head of the stream.
    *
    * @param b the array of bytes to be written
    * @param off the offset within the array to be written
    * @param len the number of bytes from the array to be written
    */
   default void write(byte[] b, int off, int len) throws IOException {
      write(getPosition(), b, off, len);
   }

   /**
    * Write a single byte to the specified position in the stream.
    *
    * @param position the position in the stream to write the byte to
    * @param b the byte to be written
    */
   void write(int position, int b) throws IOException;

   /**
    * Write all bytes to the specified position in the stream.
    *
    * @param position the position in the stream to write the bytes to
    * @param b the array of bytes to be written
    */
   default void write(int position, byte[] b) throws IOException {
      write(position, b, 0, b.length);
   }

   /**
    * Write all bytes to the specified position in the stream.
    *
    * @param position the position in the stream to write the bytes to
    * @param b the array of bytes to be written
    * @param off the offset within the array to be written
    * @param len the number of bytes from the array to be written
    */
   void write(int position, byte[] b, int off, int len) throws IOException;

   /**
    * Write all bytes in this stream to the provided {@link DataOutput} stream
    * @param output the stream to write this stream's bytes to
    */
   void copyTo(DataOutput output) throws IOException;
}
