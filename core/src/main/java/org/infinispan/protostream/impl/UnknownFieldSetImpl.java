package org.infinispan.protostream.impl;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.UnknownFieldSet;

/**
 * {@code UnknownFieldSet} implementation. This class should never be directly instantiated by users even though it is
 * marked {@code public}.
 *
 * @author anistor@redhat.com
 * @since 1.0
 */
public final class UnknownFieldSetImpl implements UnknownFieldSet, Externalizable {

   // elements of the Deque can be one of : varint, fixed32, fixed64, byte[] or UnknownFieldSetImpl
   // this is created lazily
   private Map<Integer, Deque<Object>> fields;

   public UnknownFieldSetImpl() {
      // needs to be public to be Serializable/Externalizable
   }

   /**
    * Get an Deque of values for the given field number. A new one is created and added if it does not exist already.
    */
   private Deque<Object> getField(int tag) {
      if (tag == 0) {
         throw new IllegalArgumentException("Zero is not a valid tag number");
      }
      Deque<Object> field = null;
      if (fields == null) {
         fields = new HashMap<>();
      } else {
         field = fields.get(tag);
      }
      if (field == null) {
         field = new ArrayDeque<>();
         fields.put(tag, field);
      }
      return field;
   }

   @Override
   public boolean isEmpty() {
      return fields == null || fields.isEmpty();
   }

   @Override
   public void readAllFields(RawProtoStreamReader input) throws IOException {
      while (true) {
         int tag = input.readTag();
         if (tag == 0 || !readSingleField(tag, input)) {
            break;
         }
      }
   }

   @Override
   public boolean readSingleField(int tag, RawProtoStreamReader input) throws IOException {
      int wireType = WireFormat.getTagWireType(tag);
      switch (wireType) {
         case WireFormat.WIRETYPE_VARINT:
            getField(tag).addLast(input.readInt64());
            return true;

         case WireFormat.WIRETYPE_FIXED64:
            getField(tag).addLast(input.readFixed64());
            return true;

         case WireFormat.WIRETYPE_LENGTH_DELIMITED:
            getField(tag).addLast(input.readByteArray());
            return true;

         case WireFormat.WIRETYPE_START_GROUP:
            UnknownFieldSetImpl unknownFieldSet = new UnknownFieldSetImpl();
            unknownFieldSet.readAllFields(input);
            input.checkLastTagWas(WireFormat.makeTag(WireFormat.getTagFieldNumber(tag), WireFormat.WIRETYPE_END_GROUP));
            getField(tag).addLast(unknownFieldSet);
            return true;

         case WireFormat.WIRETYPE_END_GROUP:
            return false;

         case WireFormat.WIRETYPE_FIXED32:
            getField(tag).addLast(input.readFixed32());
            return true;

         default:
            throw new IOException("Protocol message tag had invalid wire type " + wireType);
      }
   }

   @Override
   public void putVarintField(int tag, int value) {
      if (tag == 0) {
         throw new IllegalArgumentException("Zero is not a valid tag");
      }
      if (WireFormat.getTagWireType(tag) != WireFormat.WIRETYPE_VARINT) {
         throw new IllegalArgumentException("The tag is not a VARINT");
      }
      getField(tag).addLast(value);
   }

   @Override
   public void writeTo(RawProtoStreamWriter output) throws IOException {
      if (fields != null) {
         // we sort by tag to ensure we always have a predictable output order
         SortedMap<Integer, Deque> sorted = new TreeMap<>(fields);
         for (Map.Entry<Integer, Deque> entry : sorted.entrySet()) {
            writeField(entry.getKey(), entry.getValue(), output);
         }
         output.flush();
      }
   }

   /**
    * Serializes a field, including field number, and writes it to {@code output}.
    */
   private void writeField(int tag, Deque<?> values, RawProtoStreamWriter output) throws IOException {
      final int wireType = WireFormat.getTagWireType(tag);
      final int fieldNumber = WireFormat.getTagFieldNumber(tag);
      switch (wireType) {
         case WireFormat.WIRETYPE_VARINT:
            for (long value : (Deque<Long>) values) {
               output.writeUInt64(fieldNumber, value);
            }
            break;
         case WireFormat.WIRETYPE_FIXED32:
            for (int value : (Deque<Integer>) values) {
               output.writeFixed32(fieldNumber, value);
            }
            break;
         case WireFormat.WIRETYPE_FIXED64:
            for (long value : (Deque<Long>) values) {
               output.writeFixed64(fieldNumber, value);
            }
            break;
         case WireFormat.WIRETYPE_LENGTH_DELIMITED:
            for (byte[] value : (Deque<byte[]>) values) {
               output.writeBytes(fieldNumber, value);
            }
            break;
         case WireFormat.WIRETYPE_START_GROUP:
            for (UnknownFieldSetImpl value : (Deque<UnknownFieldSetImpl>) values) {
               output.writeUInt32NoTag(tag);
               value.writeTo(output);
               output.writeTag(fieldNumber, WireFormat.WIRETYPE_END_GROUP);
            }
            break;
         default:
            throw new IllegalArgumentException("Invalid wire type " + wireType);
      }
   }

   @Override
   public <A> A consumeTag(int tag) {
      if (tag == 0) {
         throw new IllegalArgumentException("Zero is not a valid tag number");
      }
      int wireType = WireFormat.getTagWireType(tag);
      switch (wireType) {
         case WireFormat.WIRETYPE_VARINT:
         case WireFormat.WIRETYPE_FIXED32:
         case WireFormat.WIRETYPE_FIXED64:
         case WireFormat.WIRETYPE_LENGTH_DELIMITED:
         case WireFormat.WIRETYPE_START_GROUP:
            break;
         default:
            throw new IllegalArgumentException("Invalid wire type " + wireType);
      }
      if (fields == null) {
         return null;
      }
      Deque<A> values = (Deque<A>) fields.get(tag);
      if (values == null) {
         return null;
      }
      A value = values.pollFirst();
      if (values.isEmpty()) {
         fields.remove(tag);
      }
      return value;
   }

   @Override
   public boolean hasTag(int tag) {
      if (tag == 0) {
         throw new IllegalArgumentException("Zero is not a valid tag number");
      }
      return fields != null && fields.containsKey(tag);
   }

   @Override
   public void writeExternal(ObjectOutput out) throws IOException {
      ByteArrayOutputStreamEx baos = new ByteArrayOutputStreamEx();
      RawProtoStreamWriter output = RawProtoStreamWriterImpl.newInstance(baos);
      writeTo(output);
      output.flush();
      ByteBuffer buffer = baos.getByteBuffer();
      int off = buffer.arrayOffset();
      int len = buffer.limit() - off;
      out.writeInt(len);
      out.write(buffer.array(), off, len);
   }

   @Override
   public void readExternal(ObjectInput in) throws IOException {
      int len = in.readInt();
      byte[] bytes = new byte[len];
      in.readFully(bytes);
      readAllFields(RawProtoStreamReaderImpl.newInstance(bytes));
   }

   @Override
   public String toString() {
      return "UnknownFieldSetImpl{" +
            "fields=" + fields +
            '}';
   }
}
