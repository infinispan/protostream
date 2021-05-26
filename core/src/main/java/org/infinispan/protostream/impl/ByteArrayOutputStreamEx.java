package org.infinispan.protostream.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import net.jcip.annotations.NotThreadSafe;

/**
 * Alternative to {@link java.io.ByteArrayOutputStream}.
 *
 * <ul>
 *     <li>Provides direct access to the internal buffer without making a copy.</ul>
 *     <li>Doesn't try to be thread-safe</li>
 *     <li>Allocates the buffer lazily</li>
 * </ul>
 *
 * @author anistor@redhat.com
 * @author Dan Berindei
 * @since 4.0
 */
@NotThreadSafe
public final class ByteArrayOutputStreamEx extends OutputStream {
   private static final int DEFAULT_SIZE = 32;
   // Insight from ByteArrayOutputStream
   private static final int MAX_SIZE = Integer.MAX_VALUE - 8;
   private static final byte[] EMPTY_ARRAY = new byte[0];

   byte[] buf;
   int pos;

   public ByteArrayOutputStreamEx() {
      // Do not allocate anything here
   }

   public ByteArrayOutputStreamEx(int size) {
      if (size < 0)
         throw new IllegalArgumentException();

      buf = new byte[size];
   }

   @Override
   public void write(int b) throws IOException {
      ensureCapacity(1);
      buf[pos++] = (byte) b;
   }

   @Override
   public void write(byte[] b, int off, int len) throws IOException {
      ensureCapacity(len - off);
      System.arraycopy(b, off, buf, pos, len);
      pos += len;
   }

   private void ensureCapacity(int requested) {
      int minCapacity = pos + requested;
      if (minCapacity < 0)
         throw new OutOfMemoryError();

      if (buf == null) {
         // This is the first write
         int initialCapacity = Math.max(requested, DEFAULT_SIZE);
         buf = new byte[initialCapacity];
         return;
      }

      int currentCapacity = buf.length;
      if (minCapacity - currentCapacity > 0) {
         int newCapacity;
         // grow by at least 50%
         long expCapacity = ((long) currentCapacity) * 3 / 2;
         if (expCapacity > MAX_SIZE) {
            newCapacity = MAX_SIZE;
         } else {
            newCapacity = Math.max(minCapacity, (int) expCapacity);
         }
         buf = Arrays.copyOf(buf, newCapacity);
      }
   }

   public ByteBuffer getByteBuffer() {
      return ByteBuffer.wrap(buf, 0, pos);
   }

   public int count() {
      return pos;
   }

   public byte[] buf() {
      if (buf == null)
         return EMPTY_ARRAY;

      return buf;
   }
}
