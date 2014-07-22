package org.infinispan.protostream.impl;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.Type;
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

   WriteMessageContext pushContext(String fieldName, MessageMarshallerDelegate<?> marshallerDelegate, CodedOutputStream out) {
      messageContext = new WriteMessageContext(messageContext, fieldName, marshallerDelegate, out);
      return messageContext;
   }

   void popContext() {
      messageContext = messageContext.getParentContext();
   }

   public void write(CodedOutputStream out, Object value) throws IOException {
      messageContext = null;
      BaseMarshallerDelegate marshallerDelegate = ctx.getMarshallerDelegate(value.getClass());
      marshallerDelegate.marshall(null, null, value, this, out);
      out.flush();
   }

   @Override
   public void writeInt(String fieldName, Integer value) throws IOException {
      FieldDescriptor fd = messageContext.marshallerDelegate.getFieldsByName().get(fieldName);

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
      FieldDescriptor fd = messageContext.marshallerDelegate.getFieldsByName().get(fieldName);

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
      FieldDescriptor fd = messageContext.marshallerDelegate.getFieldsByName().get(fieldName);

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
      FieldDescriptor fd = messageContext.marshallerDelegate.getFieldsByName().get(fieldName);

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
      FieldDescriptor fd = messageContext.marshallerDelegate.getFieldsByName().get(fieldName);

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
      FieldDescriptor fd = messageContext.marshallerDelegate.getFieldsByName().get(fieldName);

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
      FieldDescriptor fd = messageContext.marshallerDelegate.getFieldsByName().get(fieldName);

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
      FieldDescriptor fd = messageContext.marshallerDelegate.getFieldsByName().get(fieldName);

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
      FieldDescriptor fd = messageContext.marshallerDelegate.getFieldsByName().get(fieldName);

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
      FieldDescriptor fd = messageContext.marshallerDelegate.getFieldsByName().get(fieldName);

      if (value == null) {
         if (fd.isRequired()) {
            throw new IllegalArgumentException("A required field cannot be null : " + fieldName);
         }
         return;
      }

      writeBoolean(fieldName, value.booleanValue());
   }

   private void writePrimitiveCollection(FieldDescriptor fd, Collection<?> collection, Class elementClass) throws IOException {
      CodedOutputStream out = messageContext.out;
      int fieldNumber = fd.getNumber();
      Type type = fd.getType();
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
      FieldDescriptor fd = messageContext.marshallerDelegate.getFieldsByName().get(fieldName);

      if (value == null) {
         if (fd.isRequired()) {
            throw new IllegalArgumentException("A required field cannot be null : " + fieldName);
         }
         return;
      }

      checkFieldWrite(fd, false);

      if (fd.getType() != Type.STRING) {
         throw new IllegalArgumentException("Declared field type is not of type String : " + fieldName);
      }

      //TODO this is a big performance problem due to usage of the notoriously inefficient String.getBytes in CodedOutputStream.writeStringNoTag
      messageContext.out.writeString(fd.getNumber(), value);
   }

   @Override
   public void writeBytes(String fieldName, byte[] value) throws IOException {
      FieldDescriptor fd = messageContext.marshallerDelegate.getFieldsByName().get(fieldName);

      if (value == null) {
         if (fd.isRequired()) {
            throw new IllegalArgumentException("A required field cannot be null : " + fieldName);
         }
         return;
      }

      checkFieldWrite(fd, false);

      if (fd.getType() != Type.BYTES) {
         throw new IllegalArgumentException("Declared field type is not of type byte[] : " + fieldName);
      }

      messageContext.out.writeTag(fd.getNumber(), WireFormat.WIRETYPE_LENGTH_DELIMITED);
      messageContext.out.writeRawVarint32(value.length);
      messageContext.out.writeRawBytes(value);
   }

   @Override
   public <E> void writeObject(String fieldName, E value, Class<? extends E> clazz) throws IOException {
      FieldDescriptor fd = messageContext.marshallerDelegate.getFieldsByName().get(fieldName);

      if (value == null) {
         if (fd.isRequired()) {
            throw new IllegalArgumentException("A required field cannot be null : " + fieldName);
         }
         return;
      }

      checkFieldWrite(fd, false);

      if (fd.getType() == Type.GROUP) {
         writeGroup(fieldName, fd, value, clazz);
      } else if (fd.getType() == Type.MESSAGE) {
         writeMessage(fieldName, fd, value, clazz);
      } else if (fd.getType() == Type.ENUM) {
         writeEnum(fieldName, fd, (Enum) value);
      } else {
         throw new IllegalArgumentException("Declared field type is not a message or an enum : " + fieldName);
      }
   }

   private void writeMessage(String fieldName, FieldDescriptor fd, Object value, Class clazz) throws IOException {
      BaseMarshallerDelegate marshallerDelegate = ctx.getMarshallerDelegate(clazz);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      CodedOutputStream out = CodedOutputStream.newInstance(baos);
      marshallerDelegate.marshall(fieldName, fd, value, this, out);
      out.flush();
      messageContext.out.writeTag(fd.getNumber(), WireFormat.WIRETYPE_LENGTH_DELIMITED);
      messageContext.out.writeRawVarint32(baos.size());
      messageContext.out.writeRawBytes(baos.toByteArray());
   }

   private void writeGroup(String fieldName, FieldDescriptor fd, Object value, Class clazz) throws IOException {
      BaseMarshallerDelegate marshallerDelegate = ctx.getMarshallerDelegate(clazz);
      messageContext.out.writeTag(fd.getNumber(), WireFormat.WIRETYPE_START_GROUP);
      marshallerDelegate.marshall(fieldName, fd, value, this, messageContext.out);
      messageContext.out.writeTag(fd.getNumber(), WireFormat.WIRETYPE_END_GROUP);
   }

   private <T extends Enum<T>> void writeEnum(String fieldName, FieldDescriptor fd, T value) throws IOException {
      BaseMarshallerDelegate<T> marshallerDelegate = (BaseMarshallerDelegate<T>) ctx.getMarshallerDelegate(value.getClass());
      marshallerDelegate.marshall(fieldName, fd, value, this, messageContext.out);
   }

   @Override
   public <E> void writeCollection(String fieldName, Collection<? super E> collection, Class<E> elementClass) throws IOException {
      FieldDescriptor fd = messageContext.marshallerDelegate.getFieldsByName().get(fieldName);

      if (collection == null) {
         // a repeated field is never flagged as required
         return;
      }

      checkFieldWrite(fd, true);

      if (fd.getType() == Type.GROUP) {
         for (Object t : collection) {
            writeGroup(fieldName, fd, t, elementClass);
         }
      } else if (fd.getType() == Type.MESSAGE) {
         for (Object t : collection) {
            writeMessage(fieldName, fd, t, elementClass);
         }
      } else if (fd.getType() == Type.ENUM) {
         for (Object t : collection) {
            writeEnum(fieldName, fd, (Enum) t);
         }
      } else {
         writePrimitiveCollection(fd, collection, elementClass);
      }
   }

   @Override
   public <E> void writeArray(String fieldName, E[] array, Class<? extends E> elementClass) throws IOException {
      FieldDescriptor fd = messageContext.marshallerDelegate.getFieldsByName().get(fieldName);

      if (array == null) {
         // a repeated field is never flagged as required
         return;
      }

      checkFieldWrite(fd, true);

      if (fd.getType() == Type.GROUP) {
         for (Object t : array) {
            writeGroup(fieldName, fd, t, elementClass);
         }
      } else if (fd.getType() == Type.MESSAGE) {
         for (Object t : array) {
            writeMessage(fieldName, fd, t, elementClass);
         }
      } else if (fd.getType() == Type.ENUM) {
         for (Object t : array) {
            writeEnum(fieldName, fd, (Enum) t);
         }
      } else {
         writePrimitiveCollection(fd, Arrays.asList(array), elementClass);   //todo [anistor] optimize away the Arrays.asList( )
      }
   }

   private void checkFieldWrite(FieldDescriptor fd, boolean expectRepeated) {
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
