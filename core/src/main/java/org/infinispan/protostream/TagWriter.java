package org.infinispan.protostream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.infinispan.protostream.descriptors.WireType;
import org.infinispan.protostream.impl.SerializationContextImpl;
import org.infinispan.protostream.impl.TagWriterImpl;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
public interface TagWriter extends AutoCloseable {
   // start low level ops

   /**
    * Flushes any buffered data to the underlying output destination.
    */
   void flush() throws IOException;

   /**
    * Flushes and closes this writer, releasing any associated resources.
    */
   @Override
   default void close() throws IOException {
      flush();
   }

   /**
    * Returns the number of bytes written so far.
    */
   int getWrittenBytes();

   /**
    * Writes a tag for the given field number and wire type.
    *
    * @param number   the field number
    * @param wireType the wire type encoded as an integer
    */
   void writeTag(int number, int wireType) throws IOException;

   /**
    * Writes a tag for the given field number and wire type.
    *
    * @param number   the field number
    * @param wireType the wire type
    */
   void writeTag(int number, WireType wireType) throws IOException;

   /**
    * Writes a 32-bit value encoded as a varint.
    *
    * @param value the value to write
    */
   void writeVarint32(int value) throws IOException;

   /**
    * Writes a 64-bit value encoded as a varint.
    *
    * @param value the value to write
    */
   void writeVarint64(long value) throws IOException;

   /**
    * Writes raw bytes without a tag or length prefix.
    *
    * @param value  the byte array containing the data
    * @param offset the index of the first byte to write
    * @param length the number of bytes to write
    */
   void writeRawBytes(byte[] value, int offset, int length) throws IOException;
   // end low level ops

   // start high level ops

   /**
    * Writes a {@code string} field.
    *
    * @param number the field number
    * @param value  the value to write
    */
   void writeString(int number, String value) throws IOException;

   /**
    * Writes an {@code int32} field.
    *
    * @param number the field number
    * @param value  the value to write
    */
   void writeInt32(int number, int value) throws IOException;

   /**
    * Writes a {@code uint32} field.
    *
    * @param number the field number
    * @param value  the value to write
    */
   void writeUInt32(int number, int value) throws IOException;

   /**
    * Writes a {@code sint32} field.
    *
    * @param number the field number
    * @param value  the value to write
    */
   void writeSInt32(int number, int value) throws IOException;

   /**
    * Writes a {@code fixed32} field.
    *
    * @param number the field number
    * @param value  the value to write
    */
   void writeFixed32(int number, int value) throws IOException;

   /**
    * Writes a {@code sfixed32} field.
    *
    * @param number the field number
    * @param value  the value to write
    */
   void writeSFixed32(int number, int value) throws IOException;

   /**
    * Writes an {@code int64} field.
    *
    * @param number the field number
    * @param value  the value to write
    */
   void writeInt64(int number, long value) throws IOException;

   /**
    * Writes a {@code uint64} field.
    *
    * @param number the field number
    * @param value  the value to write
    */
   void writeUInt64(int number, long value) throws IOException;

   /**
    * Writes a {@code sint64} field.
    *
    * @param number the field number
    * @param value  the value to write
    */
   void writeSInt64(int number, long value) throws IOException;

   /**
    * Writes a {@code fixed64} field.
    *
    * @param number the field number
    * @param value  the value to write
    */
   void writeFixed64(int number, long value) throws IOException;

   /**
    * Writes a {@code sfixed64} field.
    *
    * @param number the field number
    * @param value  the value to write
    */
   void writeSFixed64(int number, long value) throws IOException;

   /**
    * Writes an {@code enum} field.
    *
    * @param number the field number
    * @param value  the value to write
    */
   void writeEnum(int number, int value) throws IOException;

   /**
    * Writes a {@code bool} field.
    *
    * @param number the field number
    * @param value  the value to write
    */
   void writeBool(int number, boolean value) throws IOException;

   /**
    * Writes a {@code double} field.
    *
    * @param number the field number
    * @param value  the value to write
    */
   void writeDouble(int number, double value) throws IOException;

   /**
    * Writes a {@code float} field.
    *
    * @param number the field number
    * @param value  the value to write
    */
   void writeFloat(int number, float value) throws IOException;

   /**
    * Writes a {@code bytes} field from the supplied {@link ByteBuffer}.
    *
    * @param number the field number
    * @param value  the {@link ByteBuffer} containing the value to write
    */
   void writeBytes(int number, ByteBuffer value) throws IOException;

   /**
    * Writes a {@code bytes} field from the supplied byte array.
    *
    * @param number the field number
    * @param value  the byte array containing the value to write
    */
   void writeBytes(int number, byte[] value) throws IOException;

   /**
    * Writes a {@code bytes} field from a section of the supplied byte array.
    *
    * @param number the field number
    * @param value  the byte array containing the value to write
    * @param offset the index of the first byte to write
    * @param length the number of bytes to write
    */
   void writeBytes(int number, byte[] value, int offset, int length) throws IOException;
   // end high level ops

   /**
    * Used to write a sub message that can be optimized by implementation. When the sub writer is complete, flush
    * should also be invoked to ensure all data is pushed to the parent writer as necessary.
    *
    * @param number the field type number
    * @param nested if the subWriter is nested in another message
    * @return a writer that will handle the nested message writing
    * @throws IOException if there is an exception while writing data
    */
   TagWriter subWriter(int number, boolean nested) throws IOException;

   /**
    * Creates a new {@link TagWriter} which uses the supplied {@link ImmutableSerializationContext}
    *
    * @param serCtx the {@link ImmutableSerializationContext} to use
    * @return a new {@link TagWriter} instance
    */
   static TagWriter newInstance(ImmutableSerializationContext serCtx) {
      return TagWriterImpl.newInstance(serCtx);
   }

   /**
    * Creates a new {@link TagWriter} which uses the supplied {@link ImmutableSerializationContext} and {@link OutputStream}
    *
    * @param serCtx the {@link ImmutableSerializationContext} to use
    * @param os     the {@link OutputStream}
    * @return a new {@link TagWriter} instance
    */
   static TagWriter newInstance(ImmutableSerializationContext serCtx, OutputStream os) {
      return TagWriterImpl.newInstance(serCtx, os);
   }

   /**
    * Creates a new {@link TagWriter} which uses the supplied {@link ImmutableSerializationContext} and {@link RandomAccessOutputStream}
    *
    * @param serCtx the {@link ImmutableSerializationContext} to use
    * @param output     the {@link RandomAccessOutputStream}
    * @return a new {@link TagWriter} instance
    */
   static TagWriter newInstance(ImmutableSerializationContext serCtx, RandomAccessOutputStream output) {
      return TagWriterImpl.newInstance(serCtx, output);
   }

   /**
    * Creates a new {@link TagWriter} which estimates the size of the message as it writes, allowing the buffer to be
    * resized dynamically.
    *
    * @param serCtx the {@link ImmutableSerializationContext} to use
    * @return a new {@link TagWriter} instance
    */
   static TagWriter newInstanceSizeEstimate(ImmutableSerializationContext serCtx) {
      return TagWriterImpl.newInstanceSizeEstimate(serCtx);
   }

   /**
    * Creates a new {@link TagWriter} which uses the supplied {@link ImmutableSerializationContext} and {@link OutputStream}
    *
    * @param serCtx     the {@link ImmutableSerializationContext} to use
    * @param out        the {@link OutputStream}
    * @param bufferSize the initial buffer size
    * @return a new {@link TagWriter} instance
    * @deprecated since 5.0.10 Please use {@link #newInstance(ImmutableSerializationContext, OutputStream)} with a
    * {@link java.io.BufferedOutputStream} instead
    */
   @Deprecated
   static TagWriter newInstance(ImmutableSerializationContext serCtx, OutputStream out, int bufferSize) {
      return TagWriterImpl.newInstance(serCtx, out, bufferSize);
   }

}
