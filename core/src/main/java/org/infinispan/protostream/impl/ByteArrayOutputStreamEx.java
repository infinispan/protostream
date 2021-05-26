package org.infinispan.protostream.impl;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import net.jcip.annotations.NotThreadSafe;

/**
 * Extends {@link java.io.ByteArrayOutputStream} and provides direct access to the internal buffer without making a copy.
 *
 * @author anistor@redhat.com
 * @since 4.0
 */
@NotThreadSafe
public final class ByteArrayOutputStreamEx extends ByteArrayOutputStream {

   public ByteArrayOutputStreamEx() {
   }

   public ByteArrayOutputStreamEx(int size) {
      super(size);
   }

   public ByteBuffer getByteBuffer() {
      return ByteBuffer.wrap(buf, 0, count);
   }

   public int count() {
      return count;
   }

   public byte[] buf() {
      return buf;
   }
}
