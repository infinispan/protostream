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
    * @return a trimmed <code>byte[]</code> instance based upon the current {@link #getPosition()} of the array
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
    * Moves bytes forward in the internal data stream.
    * @param startPos the starting position of the existing data
    * @param length how many bytes to be moved
    * @param newPos the new starting position of the data
    */
   void move(int startPos, int length, int newPos);

   /**
    * Write all bytes in this stream to the provided {@link DataOutput} stream
    * @param output the stream to write this stream's bytes to
    */
   void copyTo(DataOutput output) throws IOException;

   /**
    * Write a 32-bit value, starting into {@code position} using little endian byte order.
    * <p>
    * The implementation does not require updating the internal position as the caller should invoke
    * {@link #setPosition(int)} at some point after this method invocation.
    *
    * @param position The position to start writing.
    * @param value    The 32-bit value to write.
    * @throws IOException if an IO error occurs.
    */
   default void writeFixed32Direct(int position, int value) throws IOException {
      write(position, (byte) (value));
      write(position + 1, (byte) (value >> 8));
      write(position + 2, (byte) (value >> 16));
      write(position + 3, (byte) (value >> 24));
   }

   /**
    * Write a 64-bit value, starting into {@code position} using little endian byte order.
    * <p>
    * The implementation does not require updating the internal position as the caller should invoke
    * {@link #setPosition(int)} at some point after this method invocation.
    *
    * @param position The position to start writing.
    * @param value    The 64-bit value to write.
    * @throws IOException if an IO error occurs.
    */
   default void writeFixed64Direct(int position, long value) throws IOException {
      write(position, (byte) (value));
      write(position + 1, (byte) (value >> 8));
      write(position + 2, (byte) (value >> 16));
      write(position + 3, (byte) (value >> 24));
      write(position + 4, (byte) (value >> 32));
      write(position + 5, (byte) (value >> 40));
      write(position + 6, (byte) (value >> 48));
      write(position + 7, (byte) (value >> 56));
   }
}
