package org.infinispan.protostream.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufTagMarshaller;
import org.infinispan.protostream.TagReader;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public final class TagReaderImpl implements TagReader, ProtobufTagMarshaller.ReadContext {

   // all read are delegated to a protocol decoder
   private final Decoder decoder;

   private final SerializationContextImpl serCtx;

   // lazily initialized
   private Map<Object, Object> params = null;

   // lazily initialized
   private ProtoStreamReaderImpl reader = null;

   private TagReaderImpl(SerializationContextImpl serCtx, Decoder decoder) {
      this.serCtx = serCtx;
      this.decoder = decoder;
   }

   public static TagReaderImpl newNestedInstance(ProtobufTagMarshaller.ReadContext parentCtx, InputStream input) {
      TagReaderImpl parent = (TagReaderImpl) parentCtx;
      TagReaderImpl nestedCtx = new TagReaderImpl(parent.getSerializationContext(), Decoder.newInstance(input, Decoder.DEFAULT_BUFFER_SIZE));
      nestedCtx.params = parent.params;
      nestedCtx.reader = parent.reader;
      return nestedCtx;
   }

   public static TagReaderImpl newNestedInstance(ProtobufTagMarshaller.ReadContext parentCtx, byte[] buf) {
      TagReaderImpl parent = (TagReaderImpl) parentCtx;
      TagReaderImpl nestedCtx = new TagReaderImpl(parent.getSerializationContext(), Decoder.newInstance(buf, 0, buf.length));
      nestedCtx.params = parent.params;
      nestedCtx.reader = parent.reader;
      return nestedCtx;
   }

   public static TagReaderImpl newInstance(ImmutableSerializationContext serCtx, InputStream input) {
      return new TagReaderImpl((SerializationContextImpl) serCtx, Decoder.newInstance(input, Decoder.DEFAULT_BUFFER_SIZE));
   }

   public static TagReaderImpl newInstance(ImmutableSerializationContext serCtx, ByteBuffer buf) {
      return new TagReaderImpl((SerializationContextImpl) serCtx, Decoder.newInstance(buf));
   }

   public static TagReaderImpl newInstance(ImmutableSerializationContext serCtx, byte[] buf) {
      return new TagReaderImpl((SerializationContextImpl) serCtx, Decoder.newInstance(buf, 0, buf.length));
   }

   public static TagReaderImpl newInstance(ImmutableSerializationContext serCtx, byte[] buf, int offset, int length) {
      return new TagReaderImpl((SerializationContextImpl) serCtx, Decoder.newInstance(buf, offset, length));
   }

   @Override
   public boolean isAtEnd() throws IOException {
      return decoder.isAtEnd();
   }

   @Override
   public int readTag() throws IOException {
      return decoder.readTag();
   }

   @Override
   public void checkLastTagWas(int tag) throws IOException {
      decoder.checkLastTagWas(tag);
   }

   @Override
   public boolean skipField(int tag) throws IOException {
      return decoder.skipField(tag);
   }

   @Override
   public long readUInt64() throws IOException {
      return decoder.readVarint64();
   }

   @Override
   public long readInt64() throws IOException {
      return decoder.readVarint64();
   }

   @Override
   public int readInt32() throws IOException {
      return decoder.readVarint32();
   }

   @Override
   public long readFixed64() throws IOException {
      return decoder.readFixed64();
   }

   @Override
   public int readFixed32() throws IOException {
      return decoder.readFixed32();
   }

   @Override
   public double readDouble() throws IOException {
      return decoder.readDouble();
   }

   @Override
   public float readFloat() throws IOException {
      return decoder.readFloat();
   }

   @Override
   public boolean readBool() throws IOException {
      return decoder.readBool();
   }

   @Override
   public String readString() throws IOException {
      return decoder.readString();
   }

   @Override
   public byte[] readByteArray() throws IOException {
      return decoder.readByteArray();
   }

   @Override
   public ByteBuffer readByteBuffer() throws IOException {
      return decoder.readByteBuffer();
   }

   @Override
   public int readUInt32() throws IOException {
      return decoder.readVarint32();
   }

   @Override
   public int readEnum() throws IOException {
      return decoder.readVarint32();
   }

   @Override
   public int readSFixed32() throws IOException {
      return decoder.readFixed32();
   }

   @Override
   public long readSFixed64() throws IOException {
      return decoder.readFixed64();
   }

   @Override
   public int readSInt32() throws IOException {
      return decoder.readSInt32();
   }

   @Override
   public long readSInt64() throws IOException {
      return decoder.readSInt64();
   }

   @Override
   public int pushLimit(int limit) throws IOException {
      return decoder.pushLimit(limit);
   }

   @Override
   public void popLimit(int oldLimit) {
      decoder.popLimit(oldLimit);
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
   public TagReader getReader() {
      return this;
   }

   /**
    * @deprecated this will be removed in 5.0 together with {@link org.infinispan.protostream.MessageMarshaller}
    */
   @Deprecated
   public ProtoStreamReaderImpl getProtoStreamReader() {
      if (reader == null) {
         reader = new ProtoStreamReaderImpl(this, serCtx);
      }
      return reader;
   }
}
