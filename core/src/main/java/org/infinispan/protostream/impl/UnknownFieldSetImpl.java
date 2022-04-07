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

import org.infinispan.protostream.TagReader;
import org.infinispan.protostream.TagWriter;
import org.infinispan.protostream.UnknownFieldSet;
import org.infinispan.protostream.descriptors.WireType;

/**
 * {@link UnknownFieldSet} implementation. This is not thread-safe. This class should never be directly instantiated by
 * users even though it is marked {@code public}.
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
    * Get a Deque of values for the given tag. A new Deque is created and added if it does not exist already.
    */
   private Deque<Object> getField(int tag) {
      if (tag == 0) {
         throw new IllegalArgumentException("0 is not a valid tag number");
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
   public void readAllFields(TagReader input) throws IOException {
      while (true) {
         int tag = input.readTag();
         if (tag == 0 || !readSingleField(tag, input)) {
            break;
         }
      }
   }

   @Override
   public boolean readSingleField(int tag, TagReader input) throws IOException {
      WireType wireType = WireType.fromTag(tag);
      switch (wireType) {
         case VARINT:
            getField(tag).addLast(input.readInt64());
            return true;

         case FIXED64:
            getField(tag).addLast(input.readFixed64());
            return true;

         case LENGTH_DELIMITED:
            getField(tag).addLast(input.readByteArray());
            return true;

         case START_GROUP:
            UnknownFieldSetImpl unknownFieldSet = new UnknownFieldSetImpl();
            unknownFieldSet.readAllFields(input);
            input.checkLastTagWas(WireType.makeTag(WireType.getTagFieldNumber(tag), WireType.WIRETYPE_END_GROUP));
            getField(tag).addLast(unknownFieldSet);
            return true;

         case END_GROUP:
            return false;

         case FIXED32:
            getField(tag).addLast(input.readFixed32());
            return true;

         default:
            throw new IOException("Protocol message tag " + tag + " has invalid wire type " + wireType);
      }
   }

   @Override
   public void putVarintField(int tag, int value) {
      if (tag == 0) {
         throw new IllegalArgumentException("0 is not a valid tag");
      }
      if (WireType.getTagWireType(tag) != WireType.WIRETYPE_VARINT) {
         throw new IllegalArgumentException("The tag is not a VARINT: " + tag);
      }
      getField(tag).addLast(value);
   }

   @Override
   public void writeTo(TagWriter output) throws IOException {
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
   private void writeField(int tag, Deque<?> values, TagWriter output) throws IOException {
      final WireType wireType = WireType.fromTag(tag);
      final int fieldNumber = WireType.getTagFieldNumber(tag);
      switch (wireType) {
         case VARINT:
            for (long value : (Deque<Long>) values) {
               output.writeUInt64(fieldNumber, value);
            }
            break;
         case FIXED32:
            for (int value : (Deque<Integer>) values) {
               output.writeFixed32(fieldNumber, value);
            }
            break;
         case FIXED64:
            for (long value : (Deque<Long>) values) {
               output.writeFixed64(fieldNumber, value);
            }
            break;
         case LENGTH_DELIMITED:
            for (byte[] value : (Deque<byte[]>) values) {
               output.writeBytes(fieldNumber, value);
            }
            break;
         case START_GROUP:
            for (UnknownFieldSetImpl value : (Deque<UnknownFieldSetImpl>) values) {
               output.writeVarint32(tag);
               value.writeTo(output);
               output.writeTag(fieldNumber, WireType.WIRETYPE_END_GROUP);
            }
            break;
         default:
            throw new IllegalArgumentException("Tag " + tag + " has invalid wire type " + wireType);
      }
   }

   @Override
   public <A> A consumeTag(int tag) {
      if (tag == 0) {
         throw new IllegalArgumentException("0 is not a valid tag number");
      }
      if (WireType.getTagWireType(tag) == WireType.WIRETYPE_END_GROUP) {
         throw new IllegalArgumentException("Tag " + tag + " is an end group tag");
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
         throw new IllegalArgumentException("0 is not a valid tag number");
      }
      return fields != null && fields.containsKey(tag);
   }

   @Override
   public void writeExternal(ObjectOutput out) throws IOException {
      ByteArrayOutputStreamEx baos = new ByteArrayOutputStreamEx();
      TagWriter output = TagWriterImpl.newInstanceNoBuffer(null, baos);
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
      readAllFields(TagReaderImpl.newInstance(null, bytes));
   }

   @Override
   public String toString() {
      return "UnknownFieldSetImpl{fields=" + fields + '}';
   }
}
