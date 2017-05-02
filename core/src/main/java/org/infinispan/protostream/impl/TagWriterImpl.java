package org.infinispan.protostream.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufTagMarshaller;
import org.infinispan.protostream.TagWriter;
import org.infinispan.protostream.descriptors.WireType;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public final class TagWriterImpl implements TagWriter, ProtobufTagMarshaller.WriteContext {

   private static final Charset UTF8 = StandardCharsets.UTF_8;

   private static final int DEFAULT_BUFFER_SIZE = 4096;

   // all writes are delegated to a protocol decoder
   private final Encoder encoder;

   private final SerializationContextImpl serCtx;

   // lazily initialized
   private Map<Object, Object> params = null;

   // lazily initialized
   private ProtoStreamWriterImpl writer = null;

   private TagWriterImpl(SerializationContextImpl serCtx, Encoder encoder) {
      this.serCtx = serCtx;
      this.encoder = encoder;
   }

   public static TagWriterImpl newNestedInstance(ProtobufTagMarshaller.WriteContext parentCtx, OutputStream output) {
      TagWriterImpl parent = (TagWriterImpl) parentCtx;
      TagWriterImpl nestedCtx = new TagWriterImpl(parent.serCtx, Encoder.newInstance(output, DEFAULT_BUFFER_SIZE));
      nestedCtx.params = parent.params;
      nestedCtx.writer = parent.writer;
      return nestedCtx;
   }

   public static TagWriterImpl newInstance(ImmutableSerializationContext serCtx, OutputStream output) {
      return new TagWriterImpl((SerializationContextImpl) serCtx, Encoder.newInstance(output, DEFAULT_BUFFER_SIZE));
   }

   public static TagWriterImpl newInstance(ImmutableSerializationContext serCtx, OutputStream output, int bufferSize) {
      return new TagWriterImpl((SerializationContextImpl) serCtx, Encoder.newInstance(output, bufferSize));
   }

   public static TagWriterImpl newInstance(ImmutableSerializationContext serCtx, byte[] buf) {
      return new TagWriterImpl((SerializationContextImpl) serCtx, Encoder.newInstance(buf, 0, buf.length));
   }

   public static TagWriterImpl newInstance(ImmutableSerializationContext serCtx, byte[] buf, int offset, int length) {
      return new TagWriterImpl((SerializationContextImpl) serCtx, Encoder.newInstance(buf, offset, length));
   }

   public static TagWriterImpl newInstance(ImmutableSerializationContext serCtx, ByteBuffer byteBuffer) {
      return new TagWriterImpl((SerializationContextImpl) serCtx, Encoder.newInstance(byteBuffer));
   }

   @Override
   public void flush() throws IOException {
      encoder.flush();
   }

   @Override
   public void writeTag(int number, int wireType) throws IOException {
      encoder.writeVarint32(WireType.makeTag(number, wireType));
   }

   @Override
   public void writeTag(int number, WireType wireType) throws IOException {
      encoder.writeVarint32(WireType.makeTag(number, wireType));
   }

   @Override
   public void writeVarint32(int value) throws IOException {
      encoder.writeVarint32(value);
   }

   @Override
   public void writeVarint64(long value) throws IOException {
      encoder.writeVarint64(value);
   }

   @Override
   public void writeString(int number, String value) throws IOException {
      // TODO [anistor] This is expensive! What can we do to make it more efficient?
      // Also, when just count bytes for message size we do a useless first conversion, and another one will follow later.

      // Charset.encode is not able to encode directly into our own buffers!
      ByteBuffer utf8buffer = UTF8.encode(value);

      encoder.writeLengthDelimitedField(number, utf8buffer.remaining());
      encoder.writeBytes(utf8buffer);
   }

   @Override
   public void writeInt32(int number, int value) throws IOException {
      encoder.writeInt32(number, value);
   }

   @Override
   public void writeUInt32(int number, int value) throws IOException {
      encoder.writeUInt32Field(number, value);
   }

   @Override
   public void writeSInt32(int number, int value) throws IOException {
      encoder.writeSInt32(number, value);
   }

   @Override
   public void writeFixed32(int number, int value) throws IOException {
      encoder.writeFixed32Field(number, value);
   }

   @Override
   public void writeSFixed32(int number, int value) throws IOException {
      writeFixed32(number, value);
   }

   @Override
   public void writeInt64(int number, long value) throws IOException {
      encoder.writeUInt64Field(number, value);
   }

   @Override
   public void writeUInt64(int number, long value) throws IOException {
      encoder.writeUInt64Field(number, value);
   }

   @Override
   public void writeSInt64(int number, long value) throws IOException {
      encoder.writeSInt64(number, value);
   }

   @Override
   public void writeFixed64(int number, long value) throws IOException {
      encoder.writeFixed64Field(number, value);
   }

   @Override
   public void writeSFixed64(int number, long value) throws IOException {
      writeFixed64(number, value);
   }

   @Override
   public void writeEnum(int number, int value) throws IOException {
      writeInt32(number, value);
   }

   @Override
   public void writeBool(int number, boolean value) throws IOException {
      encoder.writeBoolField(number, value);
   }

   @Override
   public void writeDouble(int number, double value) throws IOException {
      encoder.writeDouble(number, value);
   }

   @Override
   public void writeFloat(int number, float value) throws IOException {
      encoder.writeFloat(number, value);
   }

   @Override
   public void writeBytes(int number, ByteBuffer value) throws IOException {
      encoder.writeLengthDelimitedField(number, value.remaining());
      encoder.writeBytes(value);
   }

   @Override
   public void writeBytes(int number, byte[] value) throws IOException {
      writeBytes(number, value, 0, value.length);
   }

   @Override
   public void writeBytes(int number, byte[] value, int offset, int length) throws IOException {
      encoder.writeLengthDelimitedField(number, length);
      encoder.writeBytes(value, offset, length);
   }

   @Override
   public void writeRawByte(byte value) throws IOException {
      encoder.writeByte(value);
   }

   @Override
   public void writeRawBytes(byte[] value, int offset, int length) throws IOException {
      encoder.writeBytes(value, offset, length);
   }

   @Override
   public void writeRawBytes(ByteBuffer value) throws IOException {
      encoder.writeBytes(value);
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
