package org.infinispan.protostream.impl;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * Extends {@link java.io.ByteArrayOutputStream} and provides direct access to the internal buffer without making a copy.
 *
 * @author anistor@redhat.com
 * @since 4.0
 */
public final class ByteArrayOutputStreamEx extends ByteArrayOutputStream {

   public ByteArrayOutputStreamEx() {
   }

   public ByteArrayOutputStreamEx(int size) {
      super(size);
   }

   public synchronized ByteBuffer getByteBuffer() {
      return ByteBuffer.wrap(buf, 0, count);
   }

   public int skipFixedVarint() {
      int prev = count;
      count += 5;
      return prev;
   }

   public void writePositiveFixedVarint(int pos) {
      TagWriterImpl.writePositiveFixedVarint(buf, pos, count - pos - 5);
   }
}