package org.infinispan.protostream.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.infinispan.protostream.RawProtoStreamReader;

import com.google.protobuf.CodedInputStream;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public final class RawProtoStreamReaderImpl implements RawProtoStreamReader {

   private final CodedInputStream delegate;

   private RawProtoStreamReaderImpl(CodedInputStream delegate) {
      this.delegate = delegate;
   }

   public static RawProtoStreamReader newInstance(InputStream input) {
      return new RawProtoStreamReaderImpl(CodedInputStream.newInstance(input));
   }

   public static RawProtoStreamReader newInstance(byte[] buf) {
      return new RawProtoStreamReaderImpl(CodedInputStream.newInstance(buf));
   }

   public static RawProtoStreamReader newInstance(byte[] buf, int off, int len) {
      return new RawProtoStreamReaderImpl(CodedInputStream.newInstance(buf, off, len));
   }

   public static RawProtoStreamReader newInstance(ByteBuffer buf) {
      return new RawProtoStreamReaderImpl(CodedInputStream.newInstance(buf));
   }

   public CodedInputStream getDelegate() {
      return delegate;
   }

   @Override
   public int readTag() throws IOException {
      return delegate.readTag();
   }

   @Override
   public void checkLastTagWas(int tag) throws IOException {
      delegate.checkLastTagWas(tag);
   }

   @Override
   public boolean skipField(int tag) throws IOException {
      return delegate.skipField(tag);
   }

   @Override
   public double readDouble() throws IOException {
      return delegate.readDouble();
   }

   @Override
   public float readFloat() throws IOException {
      return delegate.readFloat();
   }

   @Override
   public long readUInt64() throws IOException {
      return delegate.readUInt64();
   }

   @Override
   public long readInt64() throws IOException {
      return delegate.readInt64();
   }

   @Override
   public int readInt32() throws IOException {
      return delegate.readInt32();
   }

   @Override
   public long readFixed64() throws IOException {
      return delegate.readFixed64();
   }

   @Override
   public int readFixed32() throws IOException {
      return delegate.readFixed32();
   }

   @Override
   public boolean readBool() throws IOException {
      return delegate.readBool();
   }

   @Override
   public String readString() throws IOException {
      return delegate.readString();
   }

   @Override
   public byte[] readByteArray() throws IOException {
      return delegate.readByteArray();
   }

   @Override
   public ByteBuffer readByteBuffer() throws IOException {
      return delegate.readByteBuffer();
   }

   @Override
   public int readUInt32() throws IOException {
      return delegate.readUInt32();
   }

   @Override
   public int readEnum() throws IOException {
      return delegate.readEnum();
   }

   @Override
   public int readSFixed32() throws IOException {
      return delegate.readSFixed32();
   }

   @Override
   public long readSFixed64() throws IOException {
      return delegate.readSFixed64();
   }

   @Override
   public int readSInt32() throws IOException {
      return delegate.readSInt32();
   }

   @Override
   public long readSInt64() throws IOException {
      return delegate.readSInt64();
   }

   @Override
   public int readRawVarint32() throws IOException {
      return delegate.readRawVarint32();
   }

   @Override
   public long readRawVarint64() throws IOException {
      return delegate.readRawVarint64();
   }

   @Override
   public int pushLimit(int byteLimit) throws IOException {
      return delegate.pushLimit(byteLimit);
   }

   @Override
   public void popLimit(int oldLimit) {
      delegate.popLimit(oldLimit);
   }
}
