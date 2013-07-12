package org.infinispan.protostream.impl;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Descriptors;
import com.google.protobuf.MessageLite;
import com.google.protobuf.ProtocolMessageEnum;
import org.infinispan.protostream.EnumEncoder;
import org.infinispan.protostream.Message;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.UnknownFieldSet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author anistor@redhat.com
 */
public final class ProtobufWriterImpl implements MessageMarshaller.ProtobufWriter {

   private final SerializationContext ctx;

   private WriteMessageContext messageContext;

   public ProtobufWriterImpl(SerializationContext ctx) {
      this.ctx = ctx;
   }

   public void write(CodedOutputStream out, Object t) throws IOException {
      resetContext();
      if (t instanceof MessageLite) {
         ((MessageLite) t).writeTo(out);
      } else {
         MessageMarshaller marshaller = ctx.getMarshaller(t.getClass());
         Descriptors.Descriptor messageDescriptor = ctx.getMessageDescriptor(marshaller.getFullName());
         enterContext(messageDescriptor, out);
         marshall(t, marshaller);
         exitContext();
      }
      out.flush();
   }

   private void marshall(Object value, MessageMarshaller marshaller) throws IOException {
      marshaller.writeTo(this, value);
      if (value instanceof Message) {
         UnknownFieldSet unknownFieldSet = ((Message) value).getUnknownFieldSet();
         if (unknownFieldSet != null) {
            // TODO check that the unknown field set does not contains fields that have already been written or fields that are actually known already
            unknownFieldSet.writeTo(messageContext.out);
         }
      }
   }

   private void resetContext() {
      messageContext = null;
   }

   private void enterContext(Descriptors.Descriptor messageDescriptor, CodedOutputStream out) {
      messageContext = new WriteMessageContext(messageContext, messageDescriptor, out);
   }

   private void exitContext() {
      // validate that all the required fields were written
      for (Descriptors.FieldDescriptor fd : messageContext.messageDescriptor.getFields()) {
         if (fd.isRequired() && !messageContext.writtenFields.contains(fd)) {
            throw new IllegalStateException("Required field \"" + fd.getName() + "\" should have been written by a calling one of " + MessageMarshaller.ProtobufWriter.class.getName() + " writeXYZ(..) methods");
         }
      }
      messageContext = messageContext.parentContext;
   }

   @Override
   public void writeInt(String fieldName, Integer value) throws IOException {
      Descriptors.FieldDescriptor fd = messageContext.getFieldId(fieldName);

      if (value == null) {
         if (fd.isRequired()) {
            throw new IllegalArgumentException("A required field cannot be null : " + fieldName);
         }
         return;
      }

      writeInt(fieldName, value.intValue());
   }

   @Override
   public void writeInt(String fieldName, int value) throws IOException {
      Descriptors.FieldDescriptor fd = messageContext.getFieldId(fieldName);

      checkFieldWrite(fd, false);

      //need to know which exact flavor of write to use depending on wire type
      switch (fd.getType()) {
         case INT32:
            messageContext.out.writeInt32(fd.getNumber(), value);
            break;
         case FIXED32:
            messageContext.out.writeFixed32(fd.getNumber(), value);
            break;
         case UINT32:
            messageContext.out.writeUInt32(fd.getNumber(), value);
            break;
         case SFIXED32:
            messageContext.out.writeSFixed32(fd.getNumber(), value);
            break;
         case SINT32:
            messageContext.out.writeSInt32(fd.getNumber(), value);
            break;
         default:
            throw new IllegalArgumentException("The declared field type is not compatible with the written type : " + fieldName);
      }
   }

   @Override
   public void writeLong(String fieldName, long value) throws IOException {
      Descriptors.FieldDescriptor fd = messageContext.getFieldId(fieldName);

      checkFieldWrite(fd, false);

      //need to know which exact flavor of write to use depending on wire type
      switch (fd.getType()) {
         case INT64:
            messageContext.out.writeInt64(fd.getNumber(), value);
            break;
         case UINT64:
            messageContext.out.writeUInt64(fd.getNumber(), value);
            break;
         case FIXED64:
            messageContext.out.writeFixed64(fd.getNumber(), value);
            break;
         case SFIXED64:
            messageContext.out.writeSFixed64(fd.getNumber(), value);
            break;
         case SINT64:
            messageContext.out.writeSInt64(fd.getNumber(), value);
            break;
         default:
            throw new IllegalArgumentException("The declared field type is not compatible with the written type : " + fieldName);
      }
   }

   @Override
   public void writeLong(String fieldName, Long value) throws IOException {
      Descriptors.FieldDescriptor fd = messageContext.getFieldId(fieldName);

      if (value == null) {
         if (fd.isRequired()) {
            throw new IllegalArgumentException("A required field cannot be null : " + fieldName);
         }
         return;
      }

      writeLong(fieldName, value.longValue());
   }

   @Override
   public void writeDouble(String fieldName, double value) throws IOException {
      Descriptors.FieldDescriptor fd = messageContext.getFieldId(fieldName);

      checkFieldWrite(fd, false);

      switch (fd.getType()) {
         case DOUBLE:
            messageContext.out.writeDouble(fd.getNumber(), value);
            break;
         default:
            throw new IllegalArgumentException("The declared field type is not compatible with the written type : " + fieldName);
      }
   }

   @Override
   public void writeDouble(String fieldName, Double value) throws IOException {
      Descriptors.FieldDescriptor fd = messageContext.getFieldId(fieldName);

      if (value == null) {
         if (fd.isRequired()) {
            throw new IllegalArgumentException("A required field cannot be null : " + fieldName);
         }
         return;
      }

      writeDouble(fieldName, value.doubleValue());
   }

   @Override
   public void writeFloat(String fieldName, float value) throws IOException {
      Descriptors.FieldDescriptor fd = messageContext.getFieldId(fieldName);

      checkFieldWrite(fd, false);

      switch (fd.getType()) {
         case FLOAT:
            messageContext.out.writeFloat(fd.getNumber(), value);
            break;
         default:
            throw new IllegalArgumentException("The declared field type is not compatible with the written type : " + fieldName);
      }
   }

   @Override
   public void writeFloat(String fieldName, Float value) throws IOException {
      Descriptors.FieldDescriptor fd = messageContext.getFieldId(fieldName);

      if (value == null) {
         if (fd.isRequired()) {
            throw new IllegalArgumentException("A required field cannot be null : " + fieldName);
         }
         return;
      }

      writeFloat(fieldName, value.floatValue());
   }

   @Override
   public void writeBoolean(String fieldName, boolean value) throws IOException {
      Descriptors.FieldDescriptor fd = messageContext.getFieldId(fieldName);

      checkFieldWrite(fd, false);

      switch (fd.getType()) {
         case FLOAT:
            messageContext.out.writeBool(fd.getNumber(), value);
            break;
         default:
            throw new IllegalArgumentException("The declared field type is not compatible with the written type : " + fieldName);
      }
   }

   @Override
   public void writeBoolean(String fieldName, Boolean value) throws IOException {
      Descriptors.FieldDescriptor fd = messageContext.getFieldId(fieldName);

      if (value == null) {
         if (fd.isRequired()) {
            throw new IllegalArgumentException("A required field cannot be null : " + fieldName);
         }
         return;
      }

      writeBoolean(fieldName, value.booleanValue());
   }

   private void writePrimitiveCollection(Descriptors.FieldDescriptor fd, Collection<?> collection, Class clazz) throws IOException {
      switch (fd.getType()) {
         case DOUBLE:
            for (Object value : collection) {  //todo check (value != null && value.getClass() == clazz)
               messageContext.out.writeDouble(fd.getNumber(), (Double) value);
            }
            break;
         case FLOAT:
            for (Object value : collection) {
               messageContext.out.writeDouble(fd.getNumber(), (Double) value);
            }
            break;
         case BOOL:
            for (Object value : collection) {
               messageContext.out.writeBool(fd.getNumber(), (Boolean) value);
            }
            break;
         case STRING:
            for (Object value : collection) {
               messageContext.out.writeString(fd.getNumber(), (String) value);
            }
            break;
         case BYTES:
            for (Object value : collection) {
               messageContext.out.writeBytes(fd.getNumber(), (ByteString) value);
            }
            break;
         case INT64:
            for (Object value : collection) {
               messageContext.out.writeInt64(fd.getNumber(), (Long) value);
            }
            break;
         case UINT64:
            for (Object value : collection) {
               messageContext.out.writeUInt64(fd.getNumber(), (Long) value);
            }
            break;
         case FIXED64:
            for (Object value : collection) {
               messageContext.out.writeFixed64(fd.getNumber(), (Long) value);
            }
            break;
         case SFIXED64:
            for (Object value : collection) {
               messageContext.out.writeSFixed64(fd.getNumber(), (Long) value);
            }
            break;
         case SINT64:
            for (Object value : collection) {
               messageContext.out.writeSInt64(fd.getNumber(), (Long) value);
            }
            break;
         case INT32:
            for (Object value : collection) {
               messageContext.out.writeInt32(fd.getNumber(), (Integer) value);
            }
            break;
         case FIXED32:
            for (Object value : collection) {
               messageContext.out.writeFixed32(fd.getNumber(), (Integer) value);
            }
            break;
         case UINT32:
            for (Object value : collection) {
               messageContext.out.writeUInt32(fd.getNumber(), (Integer) value);
            }
            break;
         case SFIXED32:
            for (Object value : collection) {
               messageContext.out.writeSFixed32(fd.getNumber(), (Integer) value);
            }
            break;
         case SINT32:
            for (Object value : collection) {
               messageContext.out.writeSInt32(fd.getNumber(), (Integer) value);
            }
            break;
         default:
            throw new IllegalStateException("Unexpected field type : " + fd.getType());
      }
   }

   @Override
   public void writeString(String fieldName, String value) throws IOException {
      Descriptors.FieldDescriptor fd = messageContext.getFieldId(fieldName);

      if (value == null) {
         if (fd.isRequired()) {
            throw new IllegalArgumentException("A required field cannot be null : " + fieldName);
         }
         return;
      }

      checkFieldWrite(fd, false);

      if (fd.getType() != Descriptors.FieldDescriptor.Type.STRING) {
         throw new IllegalArgumentException("Declared field type is not of type String : " + fieldName);
      }

      messageContext.out.writeString(fd.getNumber(), value);
   }

   @Override
   public void writeBytes(String fieldName, byte[] value) throws IOException {
      Descriptors.FieldDescriptor fd = messageContext.getFieldId(fieldName);

      if (value == null) {
         if (fd.isRequired()) {
            throw new IllegalArgumentException("A required field cannot be null : " + fieldName);
         }
         return;
      }

      checkFieldWrite(fd, false);

      if (fd.getType() != Descriptors.FieldDescriptor.Type.BYTES) {
         throw new IllegalArgumentException("Declared field type is not of type byte[] : " + fieldName);
      }

      messageContext.out.writeTag(fd.getNumber(), WireFormat.WIRETYPE_LENGTH_DELIMITED);
      messageContext.out.writeRawVarint32(value.length);
      messageContext.out.writeRawBytes(value);
   }

   @Override
   public void writeObject(String fieldName, Object value, Class clazz) throws IOException {
      Descriptors.FieldDescriptor fd = messageContext.getFieldId(fieldName);

      if (value == null) {
         if (fd.isRequired()) {
            throw new IllegalArgumentException("A required field cannot be null : " + fieldName);
         }
         return;
      }

      checkFieldWrite(fd, false);

      if (fd.getType() == Descriptors.FieldDescriptor.Type.GROUP) {
         writeGroup(fd, value, clazz);
      } else if (fd.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
         writeMessage(fd, value, clazz);
      } else if (fd.getType() == Descriptors.FieldDescriptor.Type.ENUM) {
         writeEnum(fd, (Enum) value);
      } else {
         throw new IllegalArgumentException("Declared field type is not a message or an enum : " + fieldName);
      }
   }

   private void writeMessage(Descriptors.FieldDescriptor fd, Object value, Class clazz) throws IOException {
      if (MessageLite.class.isAssignableFrom(clazz)) {
         messageContext.out.writeMessage(fd.getNumber(), (MessageLite) value);
      } else {
         MessageMarshaller marshaller = ctx.getMarshaller(clazz);
         ByteArrayOutputStream baos = new ByteArrayOutputStream();      //todo here we should use a better buffer allocation strategy
         CodedOutputStream out = CodedOutputStream.newInstance(baos);
         enterContext(fd.getMessageType(), out);
         marshall(value, marshaller);
         out.flush();
         exitContext();
         messageContext.out.writeTag(fd.getNumber(), WireFormat.WIRETYPE_LENGTH_DELIMITED);
         messageContext.out.writeRawVarint32(baos.size());
         messageContext.out.writeRawBytes(baos.toByteArray());
      }
   }

   private void writeGroup(Descriptors.FieldDescriptor fd, Object value, Class clazz) throws IOException {
      messageContext.out.writeTag(fd.getNumber(), WireFormat.WIRETYPE_START_GROUP);
      if (MessageLite.class.isAssignableFrom(clazz)) {
         messageContext.out.writeGroup(fd.getNumber(), (MessageLite) value);
      } else {
         enterContext(fd.getMessageType(), messageContext.out);
         MessageMarshaller marshaller = ctx.getMarshaller(clazz);
         marshall(value, marshaller);
         exitContext();
      }
      messageContext.out.writeTag(fd.getNumber(), WireFormat.WIRETYPE_END_GROUP);
   }

   private <T extends Enum<T>> void writeEnum(Descriptors.FieldDescriptor fd, T value) throws IOException {
      int enumValue;
      if (value instanceof ProtocolMessageEnum) {
         enumValue = ((ProtocolMessageEnum) value).getNumber();
      } else {
         EnumEncoder<T> encoder = (EnumEncoder<T>) ctx.getEnumEncoder(value.getClass());
         enumValue = encoder.encode(value);
      }

      boolean valid = false;
      for (Descriptors.EnumValueDescriptor evd : fd.getEnumType().getValues()) {
         if (evd.getIndex() == enumValue) {
            valid = true;
            break;
         }
      }

      if (!valid) {
         throw new IllegalArgumentException("Undefined enum value : " + value);
      }

      messageContext.out.writeEnum(fd.getNumber(), enumValue);
   }

   @Override
   public <T> void writeCollection(String fieldName, Collection<T> collection, Class<T> clazz) throws IOException {
      // a repeated field is never marked as required
      Descriptors.FieldDescriptor fd = messageContext.getFieldId(fieldName);

      if (collection == null) {
         if (fd.isRequired()) {
            throw new IllegalArgumentException("A required field cannot be null : " + fieldName);
         }
         return;
      }

      checkFieldWrite(fd, true);

      if (fd.getType() == Descriptors.FieldDescriptor.Type.GROUP) {
         for (Object t : collection) {
            writeGroup(fd, t, clazz);
         }
      } else if (fd.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
         for (Object t : collection) {
            writeMessage(fd, t, clazz);
         }
      } else if (fd.getType() == Descriptors.FieldDescriptor.Type.ENUM) {
         for (Object t : collection) {
            writeEnum(fd, (Enum) t);
         }
      } else {
         writePrimitiveCollection(fd, collection, clazz);
      }
   }

   @Override
   public <T> void writeArray(String fieldName, T[] array, Class<T> clazz) throws IOException {
      // a repeated field is never marked as required
      Descriptors.FieldDescriptor fd = messageContext.getFieldId(fieldName);

      if (array == null) {
         if (fd.isRequired()) {
            throw new IllegalArgumentException("A required field cannot be null : " + fieldName);
         }
         return;
      }

      checkFieldWrite(fd, true);

      if (fd.getType() == Descriptors.FieldDescriptor.Type.GROUP) {
         for (Object t : array) {
            writeGroup(fd, t, clazz);
         }
      } else if (fd.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
         for (Object t : array) {
            writeMessage(fd, t, clazz);
         }
      } else if (fd.getType() == Descriptors.FieldDescriptor.Type.ENUM) {
         for (Object t : array) {
            writeEnum(fd, (Enum) t);
         }
      } else {
         writePrimitiveCollection(fd, Arrays.asList(array), clazz);   //todo optimize !
      }
   }

   private void checkFieldWrite(Descriptors.FieldDescriptor fd, boolean expectRepeated) {
      if (fd.isRepeated()) {
         if (!expectRepeated) {
            throw new IllegalArgumentException("A repeated field should be written with one of the methods intended for collections or arrays: " + fd.getName());
         }
      } else {
         if (expectRepeated) {
            throw new IllegalArgumentException("This field is not repeated and cannot be written with one of the methods intended for collections or arrays: " + fd.getName());
         }
      }
      if (!messageContext.writtenFields.add(fd)) {
         throw new IllegalArgumentException("Cannot write a field twice : " + fd.getName());
      }
   }
}
