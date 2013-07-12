package org.infinispan.protostream.impl;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import org.infinispan.protostream.UnknownFieldSet;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.TreeMap;

//todo study DynamicMessage , FieldSet

/**
 * {@code UnknownFieldSet} is used to keep track of fields which were seen when parsing a protocol message but whose
 * field numbers or types are unrecognized. This most frequently occurs when new fields are added to a message type and
 * then messages containing those fields are read by old software that was compiled before the new types were added.
 */
final class UnknownFieldSetImpl implements UnknownFieldSet {

   private Map<Integer, UnknownField> fields = new TreeMap<Integer, UnknownField>();

   /**
    * Get a UnknownField for the given field number. A new one is created and added if it does not exist already.
    */
   private UnknownField addField(int tag) {
      if (tag == 0) {
         throw new IllegalArgumentException("Zero is not a valid tag number");
      }
      UnknownField field = fields.get(tag);
      if (field == null) {
         field = new UnknownField();
         fields.put(tag, field);
      }
      return field;
   }

   @Override
   public boolean isEmpty() {
      return fields.isEmpty();
   }

   /**
    * Parse an entire message from {@code input} and merge its fields into this set.
    */
   public void mergeFrom(CodedInputStream input) throws IOException {
      while (true) {
         int tag = input.readTag();
         if (tag == 0 || !mergeFieldFrom(tag, input)) {
            break;
         }
      }
   }

   /**
    * Parse a single field from {@code input} and merge it into this set.
    *
    * @param tag The field's tag number, which was already parsed.
    * @return {@code false} if the tag is an end group tag.
    */
   public boolean mergeFieldFrom(int tag, CodedInputStream input) throws IOException {
      int wireType = WireFormat.getTagWireType(tag);
      switch (wireType) {
         case WireFormat.WIRETYPE_VARINT:
            addField(tag).addVarint(input.readInt64());
            return true;

         case WireFormat.WIRETYPE_FIXED64:
            addField(tag).addFixed64(input.readFixed64());
            return true;

         case WireFormat.WIRETYPE_LENGTH_DELIMITED:
            addField(tag).addLengthDelimited(input.readBytes());
            return true;

         case WireFormat.WIRETYPE_START_GROUP:
            UnknownFieldSetImpl unknownFieldSet = new UnknownFieldSetImpl();
            unknownFieldSet.mergeFrom(input);
            input.checkLastTagWas(WireFormat.makeTag(WireFormat.getTagFieldNumber(tag), WireFormat.WIRETYPE_END_GROUP));
            addField(tag).addGroup(unknownFieldSet);
            return true;

         case WireFormat.WIRETYPE_END_GROUP:
            return false;

         case WireFormat.WIRETYPE_FIXED32:
            addField(tag).addFixed32(input.readFixed32());
            return true;

         default:
            throw new InvalidProtocolBufferException("Protocol message tag had invalid wire type " + wireType);
      }
   }

   /**
    * Convenience method for merging a new field containing a single varint value. This is used in particular when an
    * unknown enum value is encountered.
    */
   public void mergeVarintField(int tag, int value) {
      if (tag == 0) {
         throw new IllegalArgumentException("Zero is not a valid tag");
      }
      addField(tag).addVarint(value);
   }

   /**
    * Serializes the set and writes it to {@code output}.
    */
   public void writeTo(CodedOutputStream output) throws IOException {
      for (Map.Entry<Integer, UnknownField> entry : fields.entrySet()) {
         entry.getValue().writeTo(entry.getKey(), output);
      }
      output.flush();
   }

   /**
    * Get the number of bytes required to encode this set.
    */
   public int getSerializedSize() {
      int result = 0;
      for (Map.Entry<Integer, UnknownField> entry : fields.entrySet()) {
         result += entry.getValue().getSerializedSize(entry.getKey());
      }
      return result;
   }

   public <A> A consumeTag(int tag) {
      if (tag == 0) {
         throw new IllegalArgumentException("Zero is not a valid tag number");
      }
      UnknownField unknownField = fields.get(tag);
      if (unknownField == null) {
         return null;
      }
      int wireType = WireFormat.getTagWireType(tag);
      Deque<A> values;
      switch (wireType) {              //todo can a field have multiple wire types?
         case WireFormat.WIRETYPE_VARINT:
            values = (Deque<A>) unknownField.varint;
            break;
         case WireFormat.WIRETYPE_FIXED64:
            values = (Deque<A>) unknownField.fixed64;
            break;
         case WireFormat.WIRETYPE_LENGTH_DELIMITED:
            values = (Deque<A>) unknownField.lengthDelimited;
            break;
         case WireFormat.WIRETYPE_START_GROUP:
            values = (Deque<A>) unknownField.group;
            break;
         case WireFormat.WIRETYPE_FIXED32:
            values = (Deque<A>) unknownField.fixed32;
         default:
            throw new IllegalArgumentException("Invalid wire type " + wireType);
      }
      A value = values.poll();
      if (values.isEmpty()) {
         fields.remove(tag);
      }
      return value;
   }

   /**
    * Represents a single field in an {@code UnknownFieldSet}. <p>A {@code UnknownField} consists of five lists of
    * values.  The lists correspond to the five "wire types" used in the protocol buffer binary format. The wire type of
    * each field can be determined from the encoded form alone, without knowing the field's declared type.  So, we are
    * able to parse unknown values at least this far and separate them. Normally, only one of the five lists will
    * contain any values, since it is impossible to define a valid message type that declares two different types for
    * the same field number.  However, the code is designed to allow for the case where the same unknown field number is
    * encountered using multiple different wire types.
    * <p/>
    *
    * @see UnknownFieldSetImpl
    */
   public static final class UnknownField {

      //todo if a field cannot have multiple wire types then we could split this in separate classes for each wire type
      private Deque<Long> varint;
      private Deque<Integer> fixed32;
      private Deque<Long> fixed64;
      private Deque<ByteString> lengthDelimited;
      private Deque<UnknownFieldSetImpl> group;

      /**
       * Add a varint value.
       */
      public void addVarint(long value) {
         if (varint == null) {
            varint = new ArrayDeque<Long>();
         }
         varint.add(value);
      }

      /**
       * Add a fixed32 value.
       */
      public void addFixed32(int value) {
         if (fixed32 == null) {
            fixed32 = new ArrayDeque<Integer>();
         }
         fixed32.add(value);
      }

      /**
       * Add a fixed64 value.
       */
      public void addFixed64(long value) {
         if (fixed64 == null) {
            fixed64 = new ArrayDeque<Long>();
         }
         fixed64.add(value);
      }

      /**
       * Add a length-delimited value.
       */
      public void addLengthDelimited(ByteString value) {
         if (lengthDelimited == null) {
            lengthDelimited = new ArrayDeque<ByteString>();
         }
         lengthDelimited.add(value);
      }

      /**
       * Add an embedded group.
       */
      public void addGroup(UnknownFieldSetImpl value) {
         if (group == null) {
            group = new ArrayDeque<UnknownFieldSetImpl>();
         }
         group.add(value);
      }

      /**
       * Serializes the field, including field number, and writes it to {@code output}.
       */
      public void writeTo(int tag, CodedOutputStream output) throws IOException {
         if (varint != null) {
            for (long value : varint) {
               output.writeRawVarint32(tag);
               output.writeUInt64NoTag(value);
            }
         }
         if (fixed32 != null) {
            for (int value : fixed32) {
               output.writeRawVarint32(tag);
               output.writeFixed32NoTag(value);
            }
         }
         if (fixed64 != null) {
            for (long value : fixed64) {
               output.writeRawVarint32(tag);
               output.writeFixed64NoTag(value);
            }
         }
         if (lengthDelimited != null) {
            for (ByteString value : lengthDelimited) {
               output.writeRawVarint32(tag);
               output.writeBytesNoTag(value);
            }
         }
         if (group != null) {
            for (UnknownFieldSet value : group) {
               output.writeRawVarint32(tag);
               value.writeTo(output);
               output.writeTag(WireFormat.getTagFieldNumber(tag), WireFormat.WIRETYPE_END_GROUP);
            }
         }
      }

      /**
       * Get the number of bytes required to encode this field, including field number.
       */
      public int getSerializedSize(int fieldNumber) {
         int result = 0;
         if (varint != null) {
            for (long value : varint) {
               result += CodedOutputStream.computeUInt64Size(fieldNumber, value);
            }
         }
         if (fixed32 != null) {
            for (int value : fixed32) {
               result += CodedOutputStream.computeFixed32Size(fieldNumber, value);
            }
         }
         if (fixed64 != null) {
            for (long value : fixed64) {
               result += CodedOutputStream.computeFixed64Size(fieldNumber, value);
            }
         }
         if (lengthDelimited != null) {
            for (ByteString value : lengthDelimited) {
               result += CodedOutputStream.computeBytesSize(fieldNumber, value);
            }
         }
         if (group != null) {
            for (UnknownFieldSetImpl value : group) {
               result += (CodedOutputStream.computeTagSize(fieldNumber) << 1 + value.getSerializedSize());
            }
         }
         return result;
      }
   }
}
