package org.infinispan.protostream.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufTagMarshaller;
import org.infinispan.protostream.TagWriter;
import org.infinispan.protostream.descriptors.WireType;

import com.google.protobuf.CodedOutputStream;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public final class TagWriterImpl implements TagWriter, ProtobufTagMarshaller.WriteContext {

   private final CodedOutputStream delegate;

   private final SerializationContextImpl serCtx;

   // lazily initialized
   private Map<Object, Object> params = null;

   // lazily initialized
   private ProtoStreamWriterImpl writer = null;

   private TagWriterImpl(SerializationContextImpl serCtx, CodedOutputStream delegate) {
      this.serCtx = serCtx;
      this.delegate = delegate;
   }

   public static TagWriterImpl newNestedInstance(ProtobufTagMarshaller.WriteContext parentCtx, OutputStream output) {
      TagWriterImpl parent = (TagWriterImpl) parentCtx;
      TagWriterImpl nestedCtx = new TagWriterImpl(parent.serCtx, CodedOutputStream.newInstance(output));
      nestedCtx.params = parent.params;
      nestedCtx.writer = parent.writer;
      return nestedCtx;
   }

   public static TagWriterImpl newInstance(ImmutableSerializationContext serCtx, OutputStream output) {
      return new TagWriterImpl((SerializationContextImpl) serCtx, CodedOutputStream.newInstance(output));
   }

   public static TagWriterImpl newInstance(ImmutableSerializationContext serCtx, OutputStream output, int bufferSize) {
      return new TagWriterImpl((SerializationContextImpl) serCtx, CodedOutputStream.newInstance(output, bufferSize));
   }

   public static TagWriterImpl newInstance(ImmutableSerializationContext serCtx, byte[] flatArray) {
      return new TagWriterImpl((SerializationContextImpl) serCtx, CodedOutputStream.newInstance(flatArray));
   }

   public static TagWriterImpl newInstance(ImmutableSerializationContext serCtx, byte[] flatArray, int offset, int length) {
      return new TagWriterImpl((SerializationContextImpl) serCtx, CodedOutputStream.newInstance(flatArray, offset, length));
   }

   public static TagWriterImpl newInstance(ImmutableSerializationContext serCtx, ByteBuffer byteBuffer) {
      return new TagWriterImpl((SerializationContextImpl) serCtx, CodedOutputStream.newInstance(byteBuffer));
   }

   @Override
   public void writeTag(int number, int wireType) throws IOException {
      writeTag(number, WireType.fromValue(wireType));
   }

   @Override
   public void writeTag(int number, WireType wireType) throws IOException {
      delegate.writeTag(number, wireType.value);
   }

   @Override
   public void writeUInt32NoTag(int value) throws IOException {
      delegate.writeUInt32NoTag(value);
   }

   @Override
   public void writeUInt64NoTag(long value) throws IOException {
      delegate.writeUInt64NoTag(value);
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
      final int off = value.arrayOffset();
      final int len = value.limit() - off;
      delegate.writeByteArray(number, value.array(), off, len);
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

   @Override
   public void writeRawBytes(byte[] value, int offset, int length) throws IOException {
      delegate.writeRawBytes(value, offset, length);
   }

   @Override
   public SerializationContextImpl getSerializationContext() {
      return serCtx;
   }

   @Override
   public Object getParam(Object key) {
      if (params == null) {
         return null;
      }
      return params.get(key);
   }

   @Override
   public void setParam(Object key, Object value) {
      if (params == null) {
         params = new HashMap<>();
      }
      params.put(key, value);
   }

   @Override
   public TagWriter getWriter() {
      return this;
   }

   /**
    * @deprecated this will be removed in 5.0 together with {@link org.infinispan.protostream.MessageMarshaller}
    */
   @Deprecated
   public ProtoStreamWriterImpl getProtoStreamWriter() {
      if (writer == null) {
         writer = new ProtoStreamWriterImpl(this, serCtx);
      }
      return writer;
   }
}
