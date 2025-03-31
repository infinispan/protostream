package org.infinispan.protostream.impl;

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.infinispan.protostream.RandomAccessOutputStream;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class RandomAccessOutputStreamImpl extends OutputStream implements RandomAccessOutputStream {

   static final int MIN_SIZE = 32;

   static final int DEFAULT_DOUBLING_SIZE = 4 * 1024 * 1024; // 4MB

   byte[] buf;
   int pos = 0;

   public RandomAccessOutputStreamImpl() {
   }

   public RandomAccessOutputStreamImpl(int capacity) {
      if (capacity < 0)
         throw new IllegalArgumentException("Negative initial capacity: " + capacity);
      this.buf = new byte[capacity];
   }

   @Override
   public byte get(int position) {
      return buf[position];
   }

   @Override
   public void write(int b) {
      int newpos = pos + 1;
      ensureCapacity(newpos);
      buf[pos] = (byte) b;
      pos = newpos;
   }

   @Override
   public void write(byte[] b, int off, int len) {
      if (len == 0)
         return;

      final int newcount = pos + len;
      ensureCapacity(newcount);
      write(pos, b, off, len);
      pos = newcount;
   }

   @Override
   public void write(int position, int b) {
      buf[position] = (byte) b;
   }

   @Override
   public void write(int position, byte[] b, int off, int len) {
      System.arraycopy(b, off, buf, position, len);
   }

   @Override
   public void move(int startPos, int length, int newPos) {
      assert startPos < newPos;
      ensureCapacity(newPos + length);
      System.arraycopy(buf, startPos, buf, newPos, length);
   }

   @Override
   public void ensureCapacity(int capacity) {
      if (buf == null) {
         buf = new byte[Math.max(MIN_SIZE, capacity)];
      } else if (capacity > buf.length) {
         byte[] newbuf = new byte[getNewBufferSize(buf.length, capacity)];
         System.arraycopy(buf, 0, newbuf, 0, pos);
         buf = newbuf;
      }
   }

   private static int getNewBufferSize(int curSize, int minNewSize) {
      if (curSize <= DEFAULT_DOUBLING_SIZE)
         return Math.max(curSize << 1, minNewSize);
      else
         return Math.max(curSize + (curSize >> 2), minNewSize);
   }

   @Override
   public int getPosition() {
      return pos;
   }

   @Override
   public void setPosition(int position) {
      this.pos = position;
   }

   @Override
   public byte[] toByteArray() {
      return Arrays.copyOf(buf, pos);
   }

   @Override
   public ByteBuffer getByteBuffer() {
      return buf == null ?
            ByteBuffer.wrap(new byte[0]) :
            ByteBuffer.wrap(buf, 0, pos);
   }

   @Override
   public void copyTo(DataOutput output) throws IOException {
      output.write(buf, 0, pos);
   }

   @Override
   public void writeFixed32Direct(int position, int value) {
      VarHandlesUtil.INT.set(buf, position, value);
   }

   @Override
   public void writeFixed64Direct(int position, long value) {
      VarHandlesUtil.LONG.set(buf, position, value);
   }
}
