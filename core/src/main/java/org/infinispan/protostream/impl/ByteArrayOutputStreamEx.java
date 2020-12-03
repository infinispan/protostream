package org.infinispan.protostream.impl;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * Extends java.io.ByteArrayOutputStream and provides direct access to the internal buffer without copy.
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

   public ByteBuffer getByteBuffer() {
      return ByteBuffer.wrap(buf, 0, count);
   }
}