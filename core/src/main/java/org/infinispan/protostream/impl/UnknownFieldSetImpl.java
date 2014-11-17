package org.infinispan.protostream.impl;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import org.infinispan.protostream.UnknownFieldSet;

import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * {@code UnknownFieldSet} implementation. This class should never be directly instantiated by users.
 *
 * @author anistor@redhat.com
 * @since 1.0
 */
final class UnknownFieldSetImpl implements UnknownFieldSet, Externalizable {

   // elements of the Deque can be one of : varint, fixed32, fixed64, ByteString, UnknownFieldSetImpl
   // this is created lazily
   private Map<Integer, Deque<Object>> fields;

   public UnknownFieldSetImpl() {
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
         fields = new HashMap<Integer, Deque<Object>>();
      } else {
         field = fields.get(tag);
      }
      if (field == null) {
         field = new ArrayDeque<Object>();
         fields.put(tag, field);
      }
      return field;
   }

   @Override
   public boolean isEmpty() {
      return fields == null || fields.isEmpty();
   }

   public void readAllFields(CodedInputStream input) throws IOException {
      while (true) {
         int tag = input.readTag();
         if (tag == 0 || !readSingleField(tag, input)) {
            break;
         }
      }
   }

   public boolean readSingleField(int tag, CodedInputStream input) throws IOException {
      int wireType = WireFormat.getTagWireType(tag);
      switch (wireType) {
         case WireFormat.WIRETYPE_VARINT:
            getField(tag).addLast(input.readInt64());
            return true;

         case WireFormat.WIRETYPE_FIXED64:
            getField(tag).addLast(input.readFixed64());
            return true;

         case WireFormat.WIRETYPE_LENGTH_DELIMITED:
            getField(tag).addLast(input.readBytes());
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
            throw new InvalidProtocolBufferException("Protocol message tag had invalid wire type " + wireType);
      }
   }

   public void putVarintField(int tag, int value) {
      if (tag == 0) {
         throw new IllegalArgumentException("Zero is not a valid tag");
      }
      if (WireFormat.getTagWireType(tag) != WireFormat.WIRETYPE_VARINT) {
         throw new IllegalArgumentException("The tag is not a VARINT");
      }
      getField(tag).addLast(value);
   }

   public void writeTo(CodedOutputStream output) throws IOException {
      if (fields != null) {
         // we sort by tag to ensure we always have a predictable output order
         SortedMap<Integer, Deque> sorted = new TreeMap<Integer, Deque>(fields);
         for (Map.Entry<Integer, Deque> entry : sorted.entrySet()) {
            writeField(entry.getKey(), entry.getValue(), output);
         }
         output.flush();
      }
   }

   /**
    * Serializes a field, including field number, and writes it to {@code output}.
    */
   private void writeField(int tag, Deque<?> values, CodedOutputStream output) throws IOException {
      int wireType = WireFormat.getTagWireType(tag);
      switch (wireType) {
         case WireFormat.WIRETYPE_VARINT:
            for (long value : (Deque<Long>) values) {
               output.writeRawVarint32(tag);
               output.writeUInt64NoTag(value);
            }
            break;
         case WireFormat.WIRETYPE_FIXED32:
            for (int value : (Deque<Integer>) values) {
               output.writeRawVarint32(tag);
               output.writeFixed32NoTag(value);
            }
            break;
         case WireFormat.WIRETYPE_FIXED64:
            for (long value : (Deque<Long>) values) {
               output.writeRawVarint32(tag);
               output.writeFixed64NoTag(value);
            }
            break;
         case WireFormat.WIRETYPE_LENGTH_DELIMITED:
            for (ByteString value : (Deque<ByteString>) values) {
               output.writeRawVarint32(tag);
               output.writeBytesNoTag(value);
            }
            break;
         case WireFormat.WIRETYPE_START_GROUP:
            for (UnknownFieldSetImpl value : (Deque<UnknownFieldSetImpl>) values) {
               output.writeRawVarint32(tag);
               value.writeTo(output);
               output.writeTag(WireFormat.getTagFieldNumber(tag), WireFormat.WIRETYPE_END_GROUP);
            }
            break;
         default:
            throw new IllegalArgumentException("Invalid wire type " + wireType);
      }
   }

   /**
    * Compute the number of bytes required to encode this set.
    */
   public int getSerializedSize() {
      int result = 0;
      if (fields != null) {
         for (Map.Entry<Integer, Deque<Object>> entry : fields.entrySet()) {
            result += computeSerializedFieldSize(entry.getKey(), entry.getValue());
         }
      }
      return result;
   }

   /**
    * Get the number of bytes required to encode a field, including field number.
    */
   private static int computeSerializedFieldSize(int tag, Deque<?> values) {
      //todo extract constants outside the loop
      int result = 0;
      int wireType = WireFormat.getTagWireType(tag);
      switch (wireType) {
         case WireFormat.WIRETYPE_VARINT:
            for (long value : (Deque<Long>) values) {
               result += CodedOutputStream.computeUInt64Size(tag, value);
            }
            break;
         case WireFormat.WIRETYPE_FIXED32:
            for (int value : (Deque<Integer>) values) {
               result += CodedOutputStream.computeFixed32Size(tag, value);
            }
            break;
         case WireFormat.WIRETYPE_FIXED64:
            for (long value : (Deque<Long>) values) {
               result += CodedOutputStream.computeFixed64Size(tag, value);
            }
            break;
         case WireFormat.WIRETYPE_LENGTH_DELIMITED:
            for (ByteString value : (Deque<ByteString>) values) {
               result += CodedOutputStream.computeBytesSize(tag, value);
            }
            break;
         case WireFormat.WIRETYPE_START_GROUP:
            for (UnknownFieldSetImpl value : (Deque<UnknownFieldSetImpl>) values) {
               result += (CodedOutputStream.computeTagSize(tag) << 1 + value.getSerializedSize());
            }
            break;
         default:
            throw new IllegalArgumentException("Invalid wire type " + wireType);
      }
      return result;
   }

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
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      CodedOutputStream output = CodedOutputStream.newInstance(baos);
      writeTo(output);
      output.flush();
      byte[] bytes = baos.toByteArray();
      out.writeInt(bytes.length);
      out.write(bytes);
   }

   @Override
   public void readExternal(ObjectInput in) throws IOException {
      int len = in.readInt();
      byte[] bytes = new byte[len];
      in.readFully(bytes);
      readAllFields(CodedInputStream.newInstance(bytes));
   }
}
