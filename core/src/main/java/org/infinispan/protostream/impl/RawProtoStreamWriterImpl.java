package org.infinispan.protostream.impl;

import com.google.protobuf.CodedOutputStream;
import org.infinispan.protostream.RawProtoStreamWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public final class RawProtoStreamWriterImpl implements RawProtoStreamWriter {

   private final CodedOutputStream delegate;

   private RawProtoStreamWriterImpl(CodedOutputStream delegate) {
      this.delegate = delegate;
   }

   public static RawProtoStreamWriter newInstance(OutputStream output) {
      return new RawProtoStreamWriterImpl(CodedOutputStream.newInstance(output));
   }

   public static RawProtoStreamWriter newInstance(OutputStream output, int bufferSize) {
      return new RawProtoStreamWriterImpl(CodedOutputStream.newInstance(output, bufferSize));
   }

   public static RawProtoStreamWriter newInstance(byte[] flatArray) {
      return new RawProtoStreamWriterImpl(CodedOutputStream.newInstance(flatArray));
   }

   public static RawProtoStreamWriter newInstance(byte[] flatArray, int offset, int length) {
      return new RawProtoStreamWriterImpl(CodedOutputStream.newInstance(flatArray, offset, length));
   }

   public static RawProtoStreamWriter newInstance(ByteBuffer byteBuffer) {
      return new RawProtoStreamWriterImpl(CodedOutputStream.newInstance(byteBuffer));
   }

   public static RawProtoStreamWriter newInstance(ByteBuffer byteBuffer, int bufferSize) {
      return new RawProtoStreamWriterImpl(CodedOutputStream.newInstance(byteBuffer, bufferSize));
   }

   public CodedOutputStream getDelegate() {
      return delegate;
   }

   @Override
   public void writeTag(int number, int wireType) throws IOException {
      delegate.writeTag(number, wireType);
   }

   @Override
   public void writeRawVarint32(int value) throws IOException {
      delegate.writeRawVarint32(value);
   }

   @Override
   public void writeRawVarint64(long value) throws IOException {
      delegate.writeRawVarint64(value);
   }

   @Override
   public void writeString(int number, String value) throws IOException {
      delegate.writeString(number, value);
   }

   @Override
   public void writeInt32(int number, int value) throws IOException {
      delegate.writeInt32(number, value);
   }

   @Override
   public void writeInt64(int number, long value) throws IOException {
      delegate.writeInt64(number, value);
   }

   @Override
   public void writeFixed32(int number, int value) throws IOException {
      delegate.writeFixed32(number, value);
   }

   @Override
   public void writeUInt32(int number, int value) throws IOException {
      delegate.writeUInt32(number, value);
   }

   @Override
   public void writeSFixed32(int number, int value) throws IOException {
      delegate.writeSFixed32(number, value);
   }

   @Override
   public void writeSInt32(int number, int value) throws IOException {
      delegate.writeSInt32(number, value);
   }

   @Override
   public void writeEnum(int number, int value) throws IOException {
      delegate.writeEnum(number, value);
   }

   @Override
   public void flush() throws IOException {
      delegate.flush();
   }

   @Override
   public void writeBool(int number, boolean value) throws IOException {
      delegate.writeBool(number, value);
   }

   @Override
   public void writeDouble(int number, double value) throws IOException {
      delegate.writeDouble(number, value);
   }

   @Override
   public void writeFloat(int number, float value) throws IOException {
      delegate.writeFloat(number, value);
   }

   @Override
   public void writeBytes(int number, ByteBuffer value) throws IOException {
      delegate.writeByteBuffer(number, value);
   }

   @Override
   public void writeBytes(int number, byte[] value) throws IOException {
      delegate.writeByteArray(number, value);
   }

   @Override
   public void writeBytes(int number, byte[] value, int offset, int length) throws IOException {
      delegate.writeByteArray(number, value, offset, length);
   }

   @Override
   public void writeUInt64(int number, long value) throws IOException {
      delegate.writeUInt64(number, value);
   }

   @Override
   public void writeFixed64(int number, long value) throws IOException {
      delegate.writeFixed64(number, value);
   }

   @Override
   public void writeSFixed64(int number, long value) throws IOException {
      delegate.writeSFixed64(number, value);
   }

   @Override
   public void writeSInt64(int number, long value) throws IOException {
      delegate.writeSInt64(number, value);
   }
}
