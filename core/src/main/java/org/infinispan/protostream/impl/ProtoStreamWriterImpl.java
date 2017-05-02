package org.infinispan.protostream.impl;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.MessageContext;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.TagWriter;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.Type;
import org.infinispan.protostream.descriptors.WireType;
import org.jboss.logging.Logger;

/**
 * @author anistor@redhat.com
 */
final class ProtoStreamWriterImpl implements MessageMarshaller.ProtoStreamWriter {

   private static final Log log = Log.LogFactory.getLog(ProtoStreamWriterImpl.class);

   /**
    * The size of the buffer used when copying stream to stream.
    */
   private static final int CHUNK_SIZE = 4096;

   private final TagWriterImpl ctx;

   private final SerializationContextImpl serCtx;

   private WriteMessageContext messageContext;

   static final class WriteMessageContext extends MessageContext<WriteMessageContext> {

      final TagWriterImpl out;

      WriteMessageContext(WriteMessageContext parent, FieldDescriptor fieldDescriptor, Descriptor messageDescriptor, TagWriterImpl out) {
         super(parent, fieldDescriptor, messageDescriptor);
         this.out = out;
      }
   }

   ProtoStreamWriterImpl(TagWriterImpl ctx, SerializationContextImpl serCtx) {
      this.ctx = ctx;
      this.serCtx = serCtx;
   }

   WriteMessageContext enterContext(FieldDescriptor fd, Descriptor messageDescriptor, TagWriterImpl out) {
      messageContext = new WriteMessageContext(messageContext, fd, messageDescriptor, out);
      return messageContext;
   }

   void exitContext() {
      messageContext = messageContext.getParentContext();
   }

   @Override
   public ImmutableSerializationContext getSerializationContext() {
      return serCtx;
   }

   @Override
   public void writeInt(String fieldName, int value) throws IOException {
      final FieldDescriptor fd = messageContext.getFieldByName(fieldName);
      checkFieldWrite(fd);
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
            throw new IllegalArgumentException("The Protobuf declared field type is not compatible with the written type : " + fd.getFullName());
      }
   }

   @Override
   public void writeInt(String fieldName, Integer value) throws IOException {
      final FieldDescriptor fd = messageContext.getFieldByName(fieldName);
      checkFieldWrite(fd);
      if (value == null) {
         if (fd.isRequired()) {
            throw new IllegalArgumentException("A required field cannot be null : " + fd.getFullName());
         }
         return;
      }
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
            throw new IllegalArgumentException("The Protobuf declared field type is not compatible with the written type : " + fd.getFullName());
      }
   }

   @Override
   public void writeInts(String fieldName, int[] array) throws IOException {
      final FieldDescriptor fd = messageContext.getFieldByName(fieldName);
      checkRepeatedFieldWrite(fd);
      if (array == null) {
         // a repeated field can never be flagged as required
         return;
      }
      final TagWriter out = messageContext.out;
      final int fieldNumber = fd.getNumber();
      switch (fd.getType()) {
         case INT32:
            for (int value : array) {
               out.writeInt32(fieldNumber, value);
            }
            break;
         case FIXED32:
            for (int value : array) {
               out.writeFixed32(fieldNumber, value);
            }
            break;
         case UINT32:
            for (int value : array) {
               out.writeUInt32(fieldNumber, value);
            }
            break;
         case SFIXED32:
            for (int value : array) {
               out.writeSFixed32(fieldNumber, value);
            }
            break;
         case SINT32:
            for (int value : array) {
               out.writeSInt32(fieldNumber, value);
            }
            break;
         default:
            throw new IllegalArgumentException("The Protobuf declared field type is not compatible with the written type : " + fd.getFullName());
      }
   }

   @Override
   public void writeLong(String fieldName, long value) throws IOException {
      final FieldDescriptor fd = messageContext.getFieldByName(fieldName);
      checkFieldWrite(fd);
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
            throw new IllegalArgumentException("The Protobuf declared field type is not compatible with the written type : " + fd.getFullName());
      }
   }

   @Override
   public void writeLong(String fieldName, Long value) throws IOException {
      final FieldDescriptor fd = messageContext.getFieldByName(fieldName);
      checkFieldWrite(fd);
      if (value == null) {
         if (fd.isRequired()) {
            throw new IllegalArgumentException("A required field cannot be null : " + fd.getFullName());
         }
         return;
      }
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
            throw new IllegalArgumentException("The Protobuf declared field type is not compatible with the written type : " + fd.getFullName());
      }
   }

   @Override
   public void writeLongs(String fieldName, long[] array) throws IOException {
      final FieldDescriptor fd = messageContext.getFieldByName(fieldName);
      checkRepeatedFieldWrite(fd);
      if (array == null) {
         // a repeated field can never be flagged as required
         return;
      }
      final TagWriter out = messageContext.out;
      final int fieldNumber = fd.getNumber();
      switch (fd.getType()) {
         case INT64:
            for (long value : array) {
               out.writeInt64(fieldNumber, value);
            }
            break;
         case FIXED64:
            for (long value : array) {
               out.writeFixed64(fieldNumber, value);
            }
            break;
         case UINT64:
            for (long value : array) {
               out.writeUInt64(fieldNumber, value);
            }
            break;
         case SFIXED64:
            for (long value : array) {
               out.writeSFixed64(fieldNumber, value);
            }
            break;
         case SINT64:
            for (long value : array) {
               out.writeSInt64(fieldNumber, value);
            }
            break;
         default:
            throw new IllegalArgumentException("The Protobuf declared field type is not compatible with the written type : " + fd.getFullName());
      }
   }

   @Override
   public void writeDate(String fieldName, Date value) throws IOException {
      if (value != null) {
         writeLong(fieldName, value.getTime());
      }
   }

   @Override
   public void writeInstant(String fieldName, Instant value) throws IOException {
      if (value != null) {
         writeLong(fieldName, value.toEpochMilli());
      }
   }

   @Override
   public void writeDouble(String fieldName, double value) throws IOException {
      final FieldDescriptor fd = messageContext.getFieldByName(fieldName);
      checkFieldWrite(fd);
      if (fd.getType() != Type.DOUBLE) {
         throw new IllegalArgumentException("The Protobuf declared field type is not compatible with the written type : " + fd.getFullName());
      }
      messageContext.out.writeDouble(fd.getNumber(), value);
   }

   @Override
   public void writeDouble(String fieldName, Double value) throws IOException {
      final FieldDescriptor fd = messageContext.getFieldByName(fieldName);
      if (fd.getType() != Type.DOUBLE) {
         throw new IllegalArgumentException("The Protobuf declared field type is not compatible with the written type : " + fd.getFullName());
      }
      checkFieldWrite(fd);
      if (value == null) {
         if (fd.isRequired()) {
            throw new IllegalArgumentException("A required field cannot be null : " + fd.getFullName());
         }
         return;
      }
      messageContext.out.writeDouble(fd.getNumber(), value);
   }

   @Override
   public void writeDoubles(String fieldName, double[] array) throws IOException {
      final FieldDescriptor fd = messageContext.getFieldByName(fieldName);
      if (fd.getType() != Type.DOUBLE) {
         throw new IllegalArgumentException("The Protobuf declared field type is not compatible with the written type : " + fd.getFullName());
      }
      checkRepeatedFieldWrite(fd);
      if (array == null) {
         // a repeated field can never be flagged as required
         return;
      }
      final TagWriter out = messageContext.out;
      final int fieldNumber = fd.getNumber();
      for (double value : array) {
         out.writeDouble(fieldNumber, value);
      }
   }

   @Override
   public void writeFloat(String fieldName, float value) throws IOException {
      final FieldDescriptor fd = messageContext.getFieldByName(fieldName);
      if (fd.getType() != Type.FLOAT) {
         throw new IllegalArgumentException("The Protobuf declared field type is not compatible with the written type : " + fd.getFullName());
      }
      checkFieldWrite(fd);
      messageContext.out.writeFloat(fd.getNumber(), value);
   }

   @Override
   public void writeFloat(String fieldName, Float value) throws IOException {
      final FieldDescriptor fd = messageContext.getFieldByName(fieldName);
      if (fd.getType() != Type.FLOAT) {
         throw new IllegalArgumentException("The Protobuf declared field type is not compatible with the written type : " + fd.getFullName());
      }
      checkFieldWrite(fd);
      if (value == null) {
         if (fd.isRequired()) {
            throw new IllegalArgumentException("A required field cannot be null : " + fd.getFullName());
         }
         return;
      }
      messageContext.out.writeFloat(fd.getNumber(), value);
   }

   @Override
   public void writeFloats(String fieldName, float[] array) throws IOException {
      final FieldDescriptor fd = messageContext.getFieldByName(fieldName);
      if (fd.getType() != Type.FLOAT) {
         throw new IllegalArgumentException("The Protobuf declared field type is not compatible with the written type : " + fd.getFullName());
      }
      checkRepeatedFieldWrite(fd);
      if (array == null) {
         // a repeated field can never be flagged as required
         return;
      }
      final TagWriter out = messageContext.out;
      final int fieldNumber = fd.getNumber();
      for (float value : array) {
         out.writeFloat(fieldNumber, value);
      }
   }

   @Override
   public void writeBoolean(String fieldName, boolean value) throws IOException {
      final FieldDescriptor fd = messageContext.getFieldByName(fieldName);
      if (fd.getType() != Type.BOOL) {
         throw new IllegalArgumentException("The Protobuf declared field type is not compatible with the written type : " + fd.getFullName());
      }
      checkFieldWrite(fd);
      messageContext.out.writeBool(fd.getNumber(), value);
   }

   @Override
   public void writeBoolean(String fieldName, Boolean value) throws IOException {
      final FieldDescriptor fd = messageContext.getFieldByName(fieldName);
      if (fd.getType() != Type.BOOL) {
         throw new IllegalArgumentException("The Protobuf declared field type is not compatible with the written type : " + fd.getFullName());
      }
      checkFieldWrite(fd);
      if (value == null) {
         if (fd.isRequired()) {
            throw new IllegalArgumentException("A required field cannot be null : " + fd.getFullName());
         }
         return;
      }
      messageContext.out.writeBool(fd.getNumber(), value);
   }

   @Override
   public void writeBooleans(String fieldName, boolean[] array) throws IOException {
      final FieldDescriptor fd = messageContext.getFieldByName(fieldName);
      if (fd.getType() != Type.BOOL) {
         throw new IllegalArgumentException("The Protobuf declared field type is not compatible with the written type : " + fd.getFullName());
      }
      checkRepeatedFieldWrite(fd);
      if (array == null) {
         // a repeated field can never be flagged as required
         return;
      }
      final TagWriter out = messageContext.out;
      final int fieldNumber = fd.getNumber();
      for (boolean value : array) {
         out.writeBool(fieldNumber, value);
      }
   }

   @Override
   public void writeString(String fieldName, String value) throws IOException {
      final FieldDescriptor fd = messageContext.getFieldByName(fieldName);
      checkFieldWrite(fd);
      if (fd.getType() != Type.STRING) {
         throw new IllegalArgumentException("Declared field type is not of type string : " + fd.getFullName());
      }
      if (value == null) {
         if (fd.isRequired()) {
            throw new IllegalArgumentException("A required field cannot be null : " + fd.getFullName());
         }
         return;
      }
      messageContext.out.writeString(fd.getNumber(), value);
   }

   @Override
   public void writeBytes(String fieldName, byte[] value) throws IOException {
      final FieldDescriptor fd = messageContext.getFieldByName(fieldName);
      checkFieldWrite(fd);
      if (fd.getType() != Type.BYTES) {
         throw new IllegalArgumentException("Declared field type is not of type bytes : " + fd.getFullName());
      }
      if (value == null) {
         if (fd.isRequired()) {
            throw new IllegalArgumentException("A required field cannot be null : " + fd.getFullName());
         }
         return;
      }
      messageContext.out.writeBytes(fd.getNumber(), value);
   }

   @Override
   public void writeBytes(String fieldName, InputStream input) throws IOException {
      final FieldDescriptor fd = messageContext.getFieldByName(fieldName);
      checkFieldWrite(fd);
      if (fd.getType() != Type.BYTES) {
         throw new IllegalArgumentException("Declared field type is not of type bytes : " + fd.getFullName());
      }
      if (input == null) {
         throw new IllegalArgumentException("The input stream cannot be null");
      }

      int len = 0;
      List<byte[]> chunks = new LinkedList<>();
      int bufLen;
      byte[] buffer = new byte[CHUNK_SIZE];
      while ((bufLen = input.read(buffer)) != -1) {
         chunks.add(buffer);
         len += bufLen;
         buffer = new byte[CHUNK_SIZE];
      }
      input.close();

      TagWriter out = messageContext.out;
      out.writeTag(fd.getNumber(), WireType.WIRETYPE_LENGTH_DELIMITED);
      out.writeVarint32(len);
      for (byte[] chunk : chunks) {
         out.writeRawBytes(buffer, 0, chunk == buffer ? bufLen : CHUNK_SIZE);
      }
   }

   @Override
   public <E> void writeObject(String fieldName, E value, Class<? extends E> clazz) throws IOException {
      final FieldDescriptor fd = messageContext.getFieldByName(fieldName);
      checkFieldWrite(fd);
      if (value == null) {
         if (fd.isRequired()) {
            throw new IllegalArgumentException("A required field cannot be null : " + fd.getFullName());
         }
         return;
      }
      if (fd.getType() == Type.GROUP) {
         writeGroup(fd, value, clazz);
      } else if (fd.getType() == Type.MESSAGE) {
         writeMessage(fd, value, clazz);
      } else if (fd.getType() == Type.ENUM) {
         writeEnum(fd, (Enum) value);
      } else {
         throw new IllegalArgumentException("Declared field type is not a message or an enum : " + fd.getFullName());
      }
   }

   @Override
   public <E extends Enum<E>> void writeEnum(String fieldName, E value, Class<E> clazz) throws IOException {
      writeEnum(fieldName, value);
   }

   @Override
   public <E extends Enum<E>> void writeEnum(String fieldName, E value) throws IOException {
      final FieldDescriptor fd = messageContext.getFieldByName(fieldName);
      if (fd.getType() != Type.ENUM) {
         throw new IllegalArgumentException("Declared field type is not an enum : " + fd.getFullName());
      }
      checkFieldWrite(fd);
      if (value == null) {
         if (fd.isRequired()) {
            throw new IllegalArgumentException("A required field cannot be null : " + fd.getFullName());
         }
         return;
      }
      writeEnum(fd, value);
   }

   private void writeMessage(FieldDescriptor fd, Object value, Class<?> clazz) throws IOException {
      BaseMarshallerDelegate marshallerDelegate = serCtx.getMarshallerDelegate(clazz);
      ByteArrayOutputStreamEx nestedBaos = new ByteArrayOutputStreamEx();
      TagWriterImpl nestedOut = TagWriterImpl.newNestedInstance(messageContext.out, nestedBaos);
      marshallerDelegate.marshall(nestedOut, fd, value);
      nestedOut.flush();
      messageContext.out.writeBytes(fd.getNumber(), nestedBaos.getByteBuffer());
   }

   private void writeGroup(FieldDescriptor fd, Object value, Class<?> clazz) throws IOException {
      BaseMarshallerDelegate marshallerDelegate = serCtx.getMarshallerDelegate(clazz);
      messageContext.out.writeTag(fd.getNumber(), WireType.WIRETYPE_START_GROUP);
      marshallerDelegate.marshall(messageContext.out, fd, value);
      messageContext.out.writeTag(fd.getNumber(), WireType.WIRETYPE_END_GROUP);
   }

   private <T extends Enum<T>> void writeEnum(FieldDescriptor fd, T value) throws IOException {
      BaseMarshallerDelegate<T> marshallerDelegate = (BaseMarshallerDelegate<T>) serCtx.getMarshallerDelegate(value.getClass());
      marshallerDelegate.marshall(messageContext.out, fd, value);
   }

   @Override
   public <E> void writeCollection(String fieldName, Collection<? super E> collection, Class<E> elementClass) throws IOException {
      final FieldDescriptor fd = messageContext.getFieldByName(fieldName);
      checkRepeatedFieldWrite(fd);
      if (collection == null) {
         // a repeated field can never be flagged as required
         return;
      }

      final TagWriter out = messageContext.out;
      final int fieldNumber = fd.getNumber();
      switch (fd.getType()) {
         case GROUP:
            for (Object t : collection) {
               validateElement(t, elementClass);
               writeGroup(fd, t, elementClass);
            }
            break;
         case MESSAGE:
            for (Object t : collection) {
               validateElement(t, elementClass);
               writeMessage(fd, t, elementClass);
            }
            break;
         case ENUM:
            for (Object t : collection) {
               validateElement(t, elementClass);
               writeEnum(fd, (Enum) t);
            }
            break;
         case DOUBLE:
            validateElementClass(elementClass, Double.class);
            for (Object value : collection) {
               validateElement(value, elementClass);
               out.writeDouble(fieldNumber, (Double) value);
            }
            break;
         case FLOAT:
            validateElementClass(elementClass, Float.class);
            for (Object value : collection) {
               validateElement(value, elementClass);
               out.writeFloat(fieldNumber, (Float) value);
            }
            break;
         case BOOL:
            validateElementClass(elementClass, Boolean.class);
            for (Object value : collection) {
               validateElement(value, elementClass);
               out.writeBool(fieldNumber, (Boolean) value);
            }
            break;
         case STRING:
            validateElementClass(elementClass, String.class);
            for (Object value : collection) {
               validateElement(value, elementClass);
               out.writeString(fieldNumber, (String) value);
            }
            break;
         case BYTES:
            validateElementClass(elementClass, byte[].class);
            for (Object value : collection) {
               validateElement(value, elementClass);
               out.writeBytes(fieldNumber, (byte[]) value);
            }
            break;
         case INT64:
            validateElementClass(elementClass, Long.class);
            for (Object value : collection) {
               validateElement(value, elementClass);
               out.writeInt64(fieldNumber, (Long) value);
            }
            break;
         case UINT64:
            validateElementClass(elementClass, Long.class);
            for (Object value : collection) {
               validateElement(value, elementClass);
               out.writeUInt64(fieldNumber, (Long) value);
            }
            break;
         case FIXED64:
            validateElementClass(elementClass, Long.class);
            for (Object value : collection) {
               validateElement(value, elementClass);
               out.writeFixed64(fieldNumber, (Long) value);
            }
            break;
         case SFIXED64:
            validateElementClass(elementClass, Long.class);
            for (Object value : collection) {
               validateElement(value, elementClass);
               out.writeSFixed64(fieldNumber, (Long) value);
            }
            break;
         case SINT64:
            validateElementClass(elementClass, Long.class);
            for (Object value : collection) {
               validateElement(value, elementClass);
               out.writeSInt64(fieldNumber, (Long) value);
            }
            break;
         case INT32:
            validateElementClass(elementClass, Integer.class);
            for (Object value : collection) {
               validateElement(value, elementClass);
               out.writeInt32(fieldNumber, (Integer) value);
            }
            break;
         case FIXED32:
            validateElementClass(elementClass, Integer.class);
            for (Object value : collection) {
               validateElement(value, elementClass);
               out.writeFixed32(fieldNumber, (Integer) value);
            }
            break;
         case UINT32:
            validateElementClass(elementClass, Integer.class);
            for (Object value : collection) {
               validateElement(value, elementClass);
               out.writeUInt32(fieldNumber, (Integer) value);
            }
            break;
         case SFIXED32:
            validateElementClass(elementClass, Integer.class);
            for (Object value : collection) {
               validateElement(value, elementClass);
               out.writeSFixed32(fieldNumber, (Integer) value);
            }
            break;
         case SINT32:
            validateElementClass(elementClass, Integer.class);
            for (Object value : collection) {
               validateElement(value, elementClass);
               out.writeSInt32(fieldNumber, (Integer) value);
            }
            break;
         default:
            throw new IllegalArgumentException("The Protobuf declared field type is not compatible with the written type : " + fd.getFullName());
      }
   }

   @Override
   public <E> void writeArray(String fieldName, E[] array, Class<? extends E> elementClass) throws IOException {
      final FieldDescriptor fd = messageContext.getFieldByName(fieldName);
      checkRepeatedFieldWrite(fd);
      if (array == null) {
         // a repeated field can never be flagged as required
         return;
      }

      final TagWriter out = messageContext.out;
      final int fieldNumber = fd.getNumber();
      switch (fd.getType()) {
         case GROUP:
            for (Object t : array) {
               validateElement(t, elementClass);
               writeGroup(fd, t, elementClass);
            }
            break;
         case MESSAGE:
            for (Object t : array) {
               validateElement(t, elementClass);
               writeMessage(fd, t, elementClass);
            }
            break;
         case ENUM:
            for (Object t : array) {
               validateElement(t, elementClass);
               writeEnum(fd, (Enum) t);
            }
            break;
         case DOUBLE:
            validateElementClass(elementClass, Double.class);
            for (Object value : array) {
               validateElement(value, elementClass);
               out.writeDouble(fieldNumber, (Double) value);
            }
            break;
         case FLOAT:
            validateElementClass(elementClass, Float.class);
            for (Object value : array) {
               validateElement(value, elementClass);
               out.writeFloat(fieldNumber, (Float) value);
            }
            break;
         case BOOL:
            validateElementClass(elementClass, Boolean.class);
            for (Object value : array) {
               validateElement(value, elementClass);
               out.writeBool(fieldNumber, (Boolean) value);
            }
            break;
         case STRING:
            validateElementClass(elementClass, String.class);
            for (Object value : array) {
               validateElement(value, elementClass);
               out.writeString(fieldNumber, (String) value);
            }
            break;
         case BYTES:
            validateElementClass(elementClass, byte[].class);
            for (Object value : array) {
               validateElement(value, elementClass);
               out.writeBytes(fieldNumber, (byte[]) value);
            }
            break;
         case INT64:
            validateElementClass(elementClass, Long.class);
            for (Object value : array) {
               validateElement(value, elementClass);
               out.writeInt64(fieldNumber, (Long) value);
            }
            break;
         case UINT64:
            validateElementClass(elementClass, Long.class);
            for (Object value : array) {
               validateElement(value, elementClass);
               out.writeUInt64(fieldNumber, (Long) value);
            }
            break;
         case FIXED64:
            validateElementClass(elementClass, Long.class);
            for (Object value : array) {
               validateElement(value, elementClass);
               out.writeFixed64(fieldNumber, (Long) value);
            }
            break;
         case SFIXED64:
            validateElementClass(elementClass, Long.class);
            for (Object value : array) {
               validateElement(value, elementClass);
               out.writeSFixed64(fieldNumber, (Long) value);
            }
            break;
         case SINT64:
            validateElementClass(elementClass, Long.class);
            for (Object value : array) {
               validateElement(value, elementClass);
               out.writeSInt64(fieldNumber, (Long) value);
            }
            break;
         case INT32:
            validateElementClass(elementClass, Integer.class);
            for (Object value : array) {
               validateElement(value, elementClass);
               out.writeInt32(fieldNumber, (Integer) value);
            }
            break;
         case FIXED32:
            validateElementClass(elementClass, Integer.class);
            for (Object value : array) {
               validateElement(value, elementClass);
               out.writeFixed32(fieldNumber, (Integer) value);
            }
            break;
         case UINT32:
            validateElementClass(elementClass, Integer.class);
            for (Object value : array) {
               validateElement(value, elementClass);
               out.writeUInt32(fieldNumber, (Integer) value);
            }
            break;
         case SFIXED32:
            validateElementClass(elementClass, Integer.class);
            for (Object value : array) {
               validateElement(value, elementClass);
               out.writeSFixed32(fieldNumber, (Integer) value);
            }
            break;
         case SINT32:
            validateElementClass(elementClass, Integer.class);
            for (Object value : array) {
               validateElement(value, elementClass);
               out.writeSInt32(fieldNumber, (Integer) value);
            }
            break;
         default:
            throw new IllegalArgumentException("The Protobuf declared field type is not compatible with the written type : " + fd.getFullName());
      }
   }

   private void validateElementClass(Class<?> elementClass, Class<?> expectedElementClass) {
      if (elementClass != expectedElementClass) {
         throw new IllegalArgumentException("elementClass argument should be " + expectedElementClass.getCanonicalName());
      }
   }

   private void validateElement(Object element, Class<?> elementClass) {
      if (element == null) {
         throw new IllegalArgumentException("Collection or array element cannot be null");
      }
      if (element.getClass() != elementClass) {
         throw new IllegalArgumentException("Collection or array element is expected to be an instance of " + elementClass.getCanonicalName());
      }
   }

   /**
    * Check repeatability and write-once for a non-repeatable field.
    */
   private void checkFieldWrite(FieldDescriptor fd) {
      if (fd.isRepeated()) {
         throw new IllegalStateException("A repeated field should be written with one of the methods intended for collections or arrays: " + fd.getFullName());
      }

      if (!messageContext.markField(fd.getNumber())) {
         throw new IllegalStateException("A field cannot be written twice : " + fd.getFullName());
      }

      if (serCtx.getConfiguration().logOutOfSequenceWrites()
            && log.isEnabled(Logger.Level.WARN)
            && messageContext.getMaxSeenFieldNumber() > fd.getNumber()) {
         log.fieldWriteOutOfSequence(fd.getFullName());
      }
   }

   /**
    * Check repeatability and write-once for a repeatable field.
    */
   private void checkRepeatedFieldWrite(FieldDescriptor fd) {
      if (!fd.isRepeated()) {
         throw new IllegalStateException("This field is not repeated and cannot be written with the methods intended for collections or arrays: " + fd.getFullName());
      }

      if (!messageContext.markField(fd.getNumber())) {
         throw new IllegalStateException("A field cannot be written twice : " + fd.getFullName());
      }

      if (serCtx.getConfiguration().logOutOfSequenceWrites()
            && log.isEnabled(Logger.Level.WARN)
            && messageContext.getMaxSeenFieldNumber() > fd.getNumber()) {
         log.fieldWriteOutOfSequence(fd.getFullName());
      }
   }
}
