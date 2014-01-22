package org.infinispan.protostream.impl;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.Message;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.RawProtobufMarshaller;
import org.infinispan.protostream.UnknownFieldSet;
import org.jboss.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author anistor@redhat.com
 */
public final class ProtoStreamWriterImpl implements MessageMarshaller.ProtoStreamWriter {

   private static final Log log = Log.LogFactory.getLog(ProtoStreamWriterImpl.class);

   private final SerializationContextImpl ctx;

   private WriteMessageContext messageContext;

   public ProtoStreamWriterImpl(SerializationContextImpl ctx) {
      this.ctx = ctx;
   }

   public void write(CodedOutputStream out, Object t) throws IOException {
      resetContext();
      BaseMarshaller marshaller = ctx.getMarshaller(t.getClass());
      MessageDescriptor messageDescriptor = ctx.getInternalMessageDescriptor(marshaller.getTypeName());
      enterContext(null, messageDescriptor, out);
      marshall(t, marshaller, out);
      exitContext(t);
      out.flush();
   }

   private void marshall(Object value, BaseMarshaller marshaller, CodedOutputStream out) throws IOException {
      if (marshaller instanceof MessageMarshaller) {
         ((MessageMarshaller) marshaller).writeTo(this, value);
      } else {
         ((RawProtobufMarshaller) marshaller).writeTo(ctx, out, value);
      }
      if (value instanceof Message) {
         UnknownFieldSet unknownFieldSet = ((Message) value).getUnknownFieldSet();
         if (unknownFieldSet != null) {
            // check that none of the unknown fields are actually known
            for (Descriptors.FieldDescriptor fd : messageContext.getFieldDescriptors()) {
               if (unknownFieldSet.hasTag(WireFormat.makeTag(fd.getNumber(), fd.getLiteType().getWireType()))) {
                  throw new IOException("Field " + fd.getFullName() + " is a known field so it is illegal to be present in the unknown field set");
               }
            }
            unknownFieldSet.writeTo(messageContext.out);
         }
      }
   }

   private void resetContext() {
      messageContext = null;
   }

   private void enterContext(String fieldName, MessageDescriptor messageDescriptor, CodedOutputStream out) {
      messageContext = new WriteMessageContext(messageContext, fieldName, messageDescriptor, out);
   }

   private void exitContext(Object value) {
      UnknownFieldSet unknownFieldSet = value instanceof Message ? ((Message) value).getUnknownFieldSet() : null;

      // validate that all the required fields were written
      for (Descriptors.FieldDescriptor fd : messageContext.getFieldDescriptors()) {
         if (fd.isRequired()) {
            if (!messageContext.isFieldMarked(fd.getNumber())
                  && unknownFieldSet != null
                  && !unknownFieldSet.hasTag(WireFormat.makeTag(fd.getNumber(), fd.getLiteType().getWireType()))) {
               throw new IllegalStateException("Required field \"" + fd.getFullName()
                                                     + "\" should have been written by a calling a suitable method from "
                                                     + MessageMarshaller.ProtoStreamWriter.class.getName());
            }
         }
      }
      messageContext = messageContext.getParentContext();
   }

   @Override
   public void writeInt(String fieldName, Integer value) throws IOException {
      Descriptors.FieldDescriptor fd = messageContext.getFieldByName(fieldName);

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
      Descriptors.FieldDescriptor fd = messageContext.getFieldByName(fieldName);

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
      Descriptors.FieldDescriptor fd = messageContext.getFieldByName(fieldName);

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
      Descriptors.FieldDescriptor fd = messageContext.getFieldByName(fieldName);

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
      Descriptors.FieldDescriptor fd = messageContext.getFieldByName(fieldName);

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
      Descriptors.FieldDescriptor fd = messageContext.getFieldByName(fieldName);

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
      Descriptors.FieldDescriptor fd = messageContext.getFieldByName(fieldName);

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
      Descriptors.FieldDescriptor fd = messageContext.getFieldByName(fieldName);

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
      Descriptors.FieldDescriptor fd = messageContext.getFieldByName(fieldName);

      checkFieldWrite(fd, false);

      switch (fd.getType()) {
         case BOOL:
            messageContext.out.writeBool(fd.getNumber(), value);
            break;
         default:
            throw new IllegalArgumentException("The declared field type is not compatible with the written type : " + fieldName);
      }
   }

   @Override
   public void writeBoolean(String fieldName, Boolean value) throws IOException {
      Descriptors.FieldDescriptor fd = messageContext.getFieldByName(fieldName);

      if (value == null) {
         if (fd.isRequired()) {
            throw new IllegalArgumentException("A required field cannot be null : " + fieldName);
         }
         return;
      }

      writeBoolean(fieldName, value.booleanValue());
   }

   private void writePrimitiveCollection(Descriptors.FieldDescriptor fd, Collection<?> collection, Class elementClass) throws IOException {
      CodedOutputStream out = messageContext.out;
      int fieldNumber = fd.getNumber();
      Descriptors.FieldDescriptor.Type type = fd.getType();
      switch (type) {
         case DOUBLE:
            for (Object value : collection) {  //todo check (value != null && value.getClass() == elementClass)
               out.writeDouble(fieldNumber, (Double) value);
            }
            break;
         case FLOAT:
            for (Object value : collection) {
               out.writeFloat(fieldNumber, (Float) value);
            }
            break;
         case BOOL:
            for (Object value : collection) {
               out.writeBool(fieldNumber, (Boolean) value);
            }
            break;
         case STRING:
            for (Object value : collection) {
               out.writeString(fieldNumber, (String) value);
            }
            break;
         case BYTES:
            for (Object value : collection) {
               if (value instanceof byte[]) {
                  value = ByteString.copyFrom((byte[]) value);
               }
               out.writeBytes(fieldNumber, (ByteString) value);
            }
            break;
         case INT64:
            for (Object value : collection) {
               out.writeInt64(fieldNumber, (Long) value);
            }
            break;
         case UINT64:
            for (Object value : collection) {
               out.writeUInt64(fieldNumber, (Long) value);
            }
            break;
         case FIXED64:
            for (Object value : collection) {
               out.writeFixed64(fieldNumber, (Long) value);
            }
            break;
         case SFIXED64:
            for (Object value : collection) {
               out.writeSFixed64(fieldNumber, (Long) value);
            }
            break;
         case SINT64:
            for (Object value : collection) {
               out.writeSInt64(fieldNumber, (Long) value);
            }
            break;
         case INT32:
            for (Object value : collection) {
               out.writeInt32(fieldNumber, (Integer) value);
            }
            break;
         case FIXED32:
            for (Object value : collection) {
               out.writeFixed32(fieldNumber, (Integer) value);
            }
            break;
         case UINT32:
            for (Object value : collection) {
               out.writeUInt32(fieldNumber, (Integer) value);
            }
            break;
         case SFIXED32:
            for (Object value : collection) {
               out.writeSFixed32(fieldNumber, (Integer) value);
            }
            break;
         case SINT32:
            for (Object value : collection) {
               out.writeSInt32(fieldNumber, (Integer) value);
            }
            break;
         default:
            throw new IllegalStateException("Unexpected field type : " + type);
      }
   }

   @Override
   public void writeString(String fieldName, String value) throws IOException {
      Descriptors.FieldDescriptor fd = messageContext.getFieldByName(fieldName);

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

      //TODO this is a big performance problem due to usage of the notoriously inefficient String.getBytes in CodedOutputStream.writeStringNoTag
      messageContext.out.writeString(fd.getNumber(), value);
   }

   @Override
   public void writeBytes(String fieldName, byte[] value) throws IOException {
      Descriptors.FieldDescriptor fd = messageContext.getFieldByName(fieldName);

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
      Descriptors.FieldDescriptor fd = messageContext.getFieldByName(fieldName);

      if (value == null) {
         if (fd.isRequired()) {
            throw new IllegalArgumentException("A required field cannot be null : " + fieldName);
         }
         return;
      }

      checkFieldWrite(fd, false);

      if (fd.getType() == Descriptors.FieldDescriptor.Type.GROUP) {
         writeGroup(fieldName, fd, value, clazz);
      } else if (fd.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
         writeMessage(fieldName, fd, value, clazz);
      } else if (fd.getType() == Descriptors.FieldDescriptor.Type.ENUM) {
         writeEnum(fd, (Enum) value);
      } else {
         throw new IllegalArgumentException("Declared field type is not a message or an enum : " + fieldName);
      }
   }

   private void writeMessage(String fieldName, Descriptors.FieldDescriptor fd, Object value, Class clazz) throws IOException {
      BaseMarshaller marshaller = ctx.getMarshaller(clazz);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      CodedOutputStream out = CodedOutputStream.newInstance(baos);
      MessageDescriptor fdt = ctx.getInternalMessageDescriptor(fd.getMessageType().getFullName());
      enterContext(fieldName, fdt, out);
      marshall(value, marshaller, out);
      out.flush();
      exitContext(value);
      messageContext.out.writeTag(fd.getNumber(), WireFormat.WIRETYPE_LENGTH_DELIMITED);
      messageContext.out.writeRawVarint32(baos.size());
      messageContext.out.writeRawBytes(baos.toByteArray());
   }

   private void writeGroup(String fieldName, Descriptors.FieldDescriptor fd, Object value, Class clazz) throws IOException {
      messageContext.out.writeTag(fd.getNumber(), WireFormat.WIRETYPE_START_GROUP);
      MessageDescriptor fdt = ctx.getInternalMessageDescriptor(fd.getMessageType().getFullName());
      enterContext(fieldName, fdt, messageContext.out);
      BaseMarshaller marshaller = ctx.getMarshaller(clazz);
      marshall(value, marshaller, messageContext.out);
      exitContext(value);
      messageContext.out.writeTag(fd.getNumber(), WireFormat.WIRETYPE_END_GROUP);
   }

   private <T extends Enum<T>> void writeEnum(Descriptors.FieldDescriptor fd, T value) throws IOException {
      Class<? extends Enum> clazz = value.getClass();
      BaseMarshaller<? extends Enum> marshaller = ctx.getMarshaller(clazz);
      if (marshaller == null) {
         throw new IllegalArgumentException("No marshaller available for " + clazz);
      }

      if (marshaller instanceof EnumMarshaller) {
         EnumMarshaller<T> enumMarshaller = (EnumMarshaller<T>) marshaller;
         int enumValue = enumMarshaller.encode(value);
         boolean isValidValue = false;
         for (DescriptorProtos.EnumValueDescriptorProto evd : fd.getEnumType().toProto().getValueList()) {
            if (evd.getNumber() == enumValue) {
               isValidValue = true;
               break;
            }
         }
         if (!isValidValue) {
            throw new IllegalArgumentException("Undefined enum value : " + value);
         }
         messageContext.out.writeEnum(fd.getNumber(), enumValue);
      } else {
         RawProtobufMarshaller<T> rawMarshaller = (RawProtobufMarshaller<T>) marshaller;
         rawMarshaller.writeTo(ctx, messageContext.out, value);
      }
   }

   @Override
   public <T> void writeCollection(String fieldName, Collection<T> collection, Class<T> clazz) throws IOException {
      Descriptors.FieldDescriptor fd = messageContext.getFieldByName(fieldName);

      if (collection == null) {
         // a repeated field is never flagged as required
         return;
      }

      checkFieldWrite(fd, true);

      if (fd.getType() == Descriptors.FieldDescriptor.Type.GROUP) {
         for (Object t : collection) {
            writeGroup(fieldName, fd, t, clazz);
         }
      } else if (fd.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
         for (Object t : collection) {
            writeMessage(fieldName, fd, t, clazz);
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
      Descriptors.FieldDescriptor fd = messageContext.getFieldByName(fieldName);

      if (array == null) {
         // a repeated field is never flagged as required
         return;
      }

      checkFieldWrite(fd, true);

      if (fd.getType() == Descriptors.FieldDescriptor.Type.GROUP) {
         for (Object t : array) {
            writeGroup(fieldName, fd, t, clazz);
         }
      } else if (fd.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
         for (Object t : array) {
            writeMessage(fieldName, fd, t, clazz);
         }
      } else if (fd.getType() == Descriptors.FieldDescriptor.Type.ENUM) {
         for (Object t : array) {
            writeEnum(fd, (Enum) t);
         }
      } else {
         writePrimitiveCollection(fd, Arrays.asList(array), clazz);   //todo [anistor] optimize away the Arrays.asList( )
      }
   }

   private void checkFieldWrite(Descriptors.FieldDescriptor fd, boolean expectRepeated) {
      if (expectRepeated) {
         if (!fd.isRepeated()) {
            throw new IllegalArgumentException("This field is not repeated and cannot be written with the methods intended for collections or arrays: " + fd.getFullName());
         }
      } else {
         if (fd.isRepeated()) {
            throw new IllegalArgumentException("A repeated field should be written with one of the methods intended for collections or arrays: " + fd.getFullName());
         }
      }

      if (!messageContext.markField(fd.getNumber())) {
         throw new IllegalStateException("A field cannot be written twice : " + fd.getFullName());
      }

      if (ctx.getConfiguration().logOutOfSequenceWrites()
            && log.isEnabled(Logger.Level.WARN)
            && messageContext.getMaxSeenFieldNumber() > fd.getNumber()) {
         log.fieldWriteOutOfSequence(fd.getFullName());
      }
   }
}
