package org.infinispan.protostream.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtoStreamMarshaller;
import org.infinispan.protostream.TagReader;

import com.google.protobuf.CodedInputStream;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public final class TagReaderImpl implements TagReader, ProtoStreamMarshaller.ReadContext {

   private final CodedInputStream delegate;

   private final SerializationContextImpl serCtx;

   // lazily initialized
   private Map<Object, Object> params = null;

   // lazily initialized
   private ProtoStreamReaderImpl reader = null;

   private TagReaderImpl(SerializationContextImpl serCtx, CodedInputStream delegate) {
      this.serCtx = serCtx;
      this.delegate = delegate;
   }

   public static TagReaderImpl newNestedInstance(ProtoStreamMarshaller.ReadContext parentCtx, InputStream input) {
      TagReaderImpl parent = (TagReaderImpl) parentCtx;
      TagReaderImpl nestedCtx = new TagReaderImpl(parent.getSerializationContext(), CodedInputStream.newInstance(input));
      nestedCtx.params = parent.params;
      nestedCtx.reader = parent.reader;
      return nestedCtx;
   }

   public static TagReaderImpl newNestedInstance(ProtoStreamMarshaller.ReadContext parentCtx, byte[] buf) {
      TagReaderImpl parent = (TagReaderImpl) parentCtx;
      TagReaderImpl nestedCtx = new TagReaderImpl(parent.getSerializationContext(), CodedInputStream.newInstance(buf));
      nestedCtx.params = parent.params;
      nestedCtx.reader = parent.reader;
      return nestedCtx;
   }

   public static TagReaderImpl newInstance(ImmutableSerializationContext serCtx, InputStream input) {
      return new TagReaderImpl((SerializationContextImpl) serCtx, CodedInputStream.newInstance(input));
   }

   public static TagReaderImpl newInstance(ImmutableSerializationContext serCtx, byte[] buf) {
      return new TagReaderImpl((SerializationContextImpl) serCtx, CodedInputStream.newInstance(buf));
   }

   public static TagReaderImpl newInstance(ImmutableSerializationContext serCtx, byte[] buf, int off, int len) {
      return new TagReaderImpl((SerializationContextImpl) serCtx, CodedInputStream.newInstance(buf, off, len));
   }

   public static TagReaderImpl newInstance(ImmutableSerializationContext serCtx, ByteBuffer buf) {
      return new TagReaderImpl((SerializationContextImpl) serCtx, CodedInputStream.newInstance(buf));
   }

   @Override
   public boolean isAtEnd() throws IOException {
      return delegate.isAtEnd();
   }

   @Override
   public int readTag() throws IOException {
      return delegate.readTag();
   }

   @Override
   public void checkLastTagWas(int tag) throws IOException {
      if (tag == 0 && delegate.isAtEnd()) {
         // this extra condition patches the broken behaviour of Protobuf lib regarding invalid end tag
         return;
      }
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

   @Override
   public SerializationContextImpl getSerializationContext() {
      return serCtx;
   }

   @Override
   public Object getParamValue(Object key) {
      if (params == null) {
         return null;
      }
      return params.get(key);
   }

   @Override
   public void setParamValue(Object key, Object value) {
      if (params == null) {
         params = new HashMap<>();
      }
      params.put(key, value);
   }

   @Override
   public TagReader getIn() {
      return this;
   }

   /**
    * @deprecated this will be removed in 5.0 together with {@link org.infinispan.protostream.MessageMarshaller}
    */
   @Deprecated
   public ProtoStreamReaderImpl getReader() {
      if (reader == null) {
         reader = new ProtoStreamReaderImpl(this, serCtx);
      }
      return reader;
   }
}
