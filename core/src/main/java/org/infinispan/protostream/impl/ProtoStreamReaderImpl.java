package org.infinispan.protostream.impl;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.Descriptors;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.UnknownFieldSet;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

/**
 * @author anistor@redhat.com
 */
public final class ProtoStreamReaderImpl implements MessageMarshaller.ProtoStreamReader {

   private static final Log log = Log.LogFactory.getLog(ProtoStreamReaderImpl.class);

   private static final EnumSet<Descriptors.FieldDescriptor.Type> primitiveTypes = EnumSet.of(
         Descriptors.FieldDescriptor.Type.DOUBLE,
         Descriptors.FieldDescriptor.Type.FLOAT,
         Descriptors.FieldDescriptor.Type.INT64,
         Descriptors.FieldDescriptor.Type.UINT64,
         Descriptors.FieldDescriptor.Type.INT32,
         Descriptors.FieldDescriptor.Type.FIXED64,
         Descriptors.FieldDescriptor.Type.FIXED32,
         Descriptors.FieldDescriptor.Type.BOOL,
         Descriptors.FieldDescriptor.Type.STRING,
         Descriptors.FieldDescriptor.Type.BYTES,
         Descriptors.FieldDescriptor.Type.UINT32,
         Descriptors.FieldDescriptor.Type.SFIXED32,
         Descriptors.FieldDescriptor.Type.SFIXED64,
         Descriptors.FieldDescriptor.Type.SINT32,
         Descriptors.FieldDescriptor.Type.SINT64
   );

   private final SerializationContextImpl ctx;

   private ReadMessageContext messageContext;

   public ProtoStreamReaderImpl(SerializationContextImpl ctx) {
      this.ctx = ctx;
   }

   ReadMessageContext pushContext(String fieldName, MessageMarshallerDelegate<?> marshallerDelegate, CodedInputStream in) {
      messageContext = new ReadMessageContext(messageContext, fieldName, marshallerDelegate, in);
      return messageContext;
   }

   void popContext() {
      messageContext = messageContext.getParentContext();
   }

   UnknownFieldSet getUnknownFieldSet() {
      return messageContext.unknownFieldSet;
   }

   public <A> A read(CodedInputStream in, Class<A> clazz) throws IOException {
      messageContext = null;
      BaseMarshallerDelegate<A> marshallerDelegate = ctx.getMarshallerDelegate(clazz);
      return marshallerDelegate.unmarshall(null, null, this, in);
   }

   public <A> A read(CodedInputStream in, MessageMarshaller<A> marshaller) throws IOException {
      messageContext = null;
      BaseMarshallerDelegate<A> marshallerDelegate = ctx.getMarshallerDelegate(marshaller.getTypeName());
      return marshallerDelegate.unmarshall(null, null, this, in);
   }

   private Object readPrimitive(String fieldName, Descriptors.FieldDescriptor.JavaType javaType) throws IOException {
      Descriptors.FieldDescriptor fd = messageContext.marshallerDelegate.getFieldsByName().get(fieldName);
      Descriptors.FieldDescriptor.Type type = fd.getType();
      if (type == Descriptors.FieldDescriptor.Type.ENUM
            || type == Descriptors.FieldDescriptor.Type.GROUP
            || type == Descriptors.FieldDescriptor.Type.MESSAGE) {
         throw new IllegalArgumentException("Declared field type is not a primitive : " + fd.getFullName());
      }
      if (fd.getJavaType() != javaType) {
         throw new IllegalArgumentException("Declared field type is not of the expected type : " + fd.getFullName());
      }
      checkFieldRead(fd, false);
      int expectedTag = WireFormat.makeTag(fd.getNumber(), fd.getLiteType().getWireType());

      Object o = messageContext.unknownFieldSet.consumeTag(expectedTag);
      if (o != null) {
         return convertWireTypeToJavaType(type, o);
      }

      CodedInputStream in = messageContext.in;
      while (true) {
         int tag = in.readTag();
         if (tag == 0) {
            break;
         }
         if (tag == expectedTag) {
            switch (fd.getType()) {
               case DOUBLE:
                  return in.readDouble();
               case FLOAT:
                  return in.readFloat();
               case BOOL:
                  return in.readBool();
               case STRING:
                  return in.readString();
               case BYTES:
                  return in.readBytes().toByteArray();
               case INT32:
                  return in.readInt32();
               case SFIXED32:
                  return in.readSFixed32();
               case FIXED32:
                  return in.readFixed32();
               case UINT32:
                  return in.readUInt32();
               case SINT32:
                  return in.readSInt32();
               case INT64:
                  return in.readInt64();
               case UINT64:
                  return in.readUInt64();
               case FIXED64:
                  return in.readFixed64();
               case SFIXED64:
                  return in.readSFixed64();
               case SINT64:
                  return in.readSInt64();
               default:
                  throw new IOException("Unexpected field type : " + fd.getType());
            }
         }
         messageContext.unknownFieldSet.readSingleField(tag, in);
      }

      if (fd.isRequired()) {
         throw new IOException("Field " + fd.getFullName() + " is required but is not present in the stream");
      }

      if (fd.hasDefaultValue()) {
         return fd.getDefaultValue();
      }

      return null;
   }

   private Object convertWireTypeToJavaType(Descriptors.FieldDescriptor.Type type, Object o) {
      if (type == Descriptors.FieldDescriptor.Type.STRING) {
         o = ((ByteString) o).toStringUtf8();
      } else if (type == Descriptors.FieldDescriptor.Type.BYTES) {
         o = ((ByteString) o).toByteArray();
      } else if (type == Descriptors.FieldDescriptor.Type.INT32
            || type == Descriptors.FieldDescriptor.Type.UINT32
            || type == Descriptors.FieldDescriptor.Type.SINT32) {
         o = ((Long) o).intValue();
      } else if (type == Descriptors.FieldDescriptor.Type.FIXED32
            || type == Descriptors.FieldDescriptor.Type.SFIXED32) {
         //o is an Integer
      } else if (type == Descriptors.FieldDescriptor.Type.INT64
            || type == Descriptors.FieldDescriptor.Type.UINT64
            || type == Descriptors.FieldDescriptor.Type.FIXED64
            || type == Descriptors.FieldDescriptor.Type.SFIXED64
            || type == Descriptors.FieldDescriptor.Type.SINT64) {
         //o is a Long
      } else if (type == Descriptors.FieldDescriptor.Type.BOOL) {
         o = ((Long) o) != 0;
      } else if (type == Descriptors.FieldDescriptor.Type.FLOAT) {
         o = Float.intBitsToFloat((Integer) o);
      } else if (type == Descriptors.FieldDescriptor.Type.DOUBLE) {
         o = Double.longBitsToDouble((Long) o);
      }
      return o;
   }

   @Override
   public Integer readInt(String fieldName) throws IOException {
      return (Integer) readPrimitive(fieldName, Descriptors.FieldDescriptor.JavaType.INT);
   }

   @Override
   public Long readLong(String fieldName) throws IOException {
      return (Long) readPrimitive(fieldName, Descriptors.FieldDescriptor.JavaType.LONG);
   }

   @Override
   public Float readFloat(String fieldName) throws IOException {
      return (Float) readPrimitive(fieldName, Descriptors.FieldDescriptor.JavaType.FLOAT);
   }

   @Override
   public Double readDouble(String fieldName) throws IOException {
      return (Double) readPrimitive(fieldName, Descriptors.FieldDescriptor.JavaType.DOUBLE);
   }

   @Override
   public Boolean readBoolean(String fieldName) throws IOException {
      return (Boolean) readPrimitive(fieldName, Descriptors.FieldDescriptor.JavaType.BOOLEAN);
   }

   @Override
   public String readString(String fieldName) throws IOException {
      return (String) readPrimitive(fieldName, Descriptors.FieldDescriptor.JavaType.STRING);
   }

   @Override
   public byte[] readBytes(String fieldName) throws IOException {
      return (byte[]) readPrimitive(fieldName, Descriptors.FieldDescriptor.JavaType.BYTE_STRING);
   }

   @Override
   public <A> A readObject(String fieldName, Class<? extends A> clazz) throws IOException {
      Descriptors.FieldDescriptor fd = messageContext.marshallerDelegate.getFieldsByName().get(fieldName);
      checkFieldRead(fd, false);

      if (fd.getType() == Descriptors.FieldDescriptor.Type.ENUM) {
         return ctx.getMarshallerDelegate(clazz).unmarshall(fieldName, fd, this, messageContext.in);
      }

      //todo validate type is compatible with readObject
      int expectedTag = WireFormat.makeTag(fd.getNumber(), fd.getLiteType().getWireType());
      Object o = messageContext.unknownFieldSet.consumeTag(expectedTag);
      if (o != null) {
         ByteString byteString = (ByteString) o;
         return readNestedObject(fieldName, fd, clazz, byteString.newCodedInput(), byteString.size());
      }

      while (true) {
         int tag = messageContext.in.readTag();
         if (tag == 0) {
            break;
         }
         if (tag == expectedTag) {
            return readNestedObject(fieldName, fd, clazz, messageContext.in, -1);
         }
         messageContext.unknownFieldSet.readSingleField(tag, messageContext.in);
      }

      return null;
   }

   /**
    * Read an Object or an Enum.
    *
    * @param length the actual length of the nested object or -1 if the length should be read from the stream
    */
   private <A> A readNestedObject(String fieldName, Descriptors.FieldDescriptor fd, Class<A> clazz, CodedInputStream in, int length) throws IOException {
      BaseMarshallerDelegate<A> marshallerDelegate = ctx.getMarshallerDelegate(clazz);
      A a;
      if (fd.getType() == Descriptors.FieldDescriptor.Type.GROUP) {
         a = marshallerDelegate.unmarshall(fieldName, fd, this, in);
         in.checkLastTagWas(WireFormat.makeTag(fd.getNumber(), WireFormat.WIRETYPE_END_GROUP));
      } else if (fd.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
         if (length < 0) {
            length = in.readRawVarint32();
         }
         int oldLimit = in.pushLimit(length);
         a = marshallerDelegate.unmarshall(fieldName, fd, this, in);
         in.checkLastTagWas(0);
         in.popLimit(oldLimit);
      } else {
         throw new IllegalArgumentException("Declared field type is not a message or an enum : " + fd.getFullName());
      }
      return a;
   }

   @Override
   public <A, C extends Collection<? super A>> C readCollection(String fieldName, C collection, Class<? extends A> elementClass) throws IOException {
      Descriptors.FieldDescriptor fd = messageContext.marshallerDelegate.getFieldsByName().get(fieldName);
      checkFieldRead(fd, true);

      if (primitiveTypes.contains(fd.getType())) {
         readPrimitiveCollection(fd, (Collection<Object>) collection, elementClass);
         return collection;
      }

      //todo validate type is compatible with readCollection
      int expectedTag = WireFormat.makeTag(fd.getNumber(), fd.getLiteType().getWireType());

      while (true) {
         Object o = messageContext.unknownFieldSet.consumeTag(expectedTag);
         if (o == null) {
            break;
         }
         ByteString byteString = (ByteString) o;
         CodedInputStream codedInputStream = byteString.newCodedInput();
         collection.add(readNestedObject(fieldName, fd, elementClass, codedInputStream, byteString.size()));
      }

      while (true) {
         int tag = messageContext.in.readTag();
         if (tag == 0) {
            break;
         }
         if (tag == expectedTag) {
            collection.add(readNestedObject(fieldName, fd, elementClass, messageContext.in, -1));
         } else {
            messageContext.unknownFieldSet.readSingleField(tag, messageContext.in);
         }
      }
      return collection;
   }

   private void readPrimitiveCollection(Descriptors.FieldDescriptor fd, Collection<? super Object> collection, Class elementClass) throws IOException {
      int expectedTag = WireFormat.makeTag(fd.getNumber(), fd.getLiteType().getWireType());
      Descriptors.FieldDescriptor.Type type = fd.getType();

      while (true) {
         Object o = messageContext.unknownFieldSet.consumeTag(expectedTag);
         if (o == null) {
            break;
         }
         collection.add(convertWireTypeToJavaType(type, o));   //todo check that (o.getClass() == elementClass)
      }

      while (true) {
         int tag = messageContext.in.readTag();
         if (tag == 0) {
            break;
         }
         if (tag == expectedTag) {
            Object value;
            switch (type) {
               case DOUBLE:
                  value = messageContext.in.readDouble();
                  break;
               case FLOAT:
                  value = messageContext.in.readFloat();
                  break;
               case BOOL:
                  value = messageContext.in.readBool();
                  break;
               case STRING:
                  value = messageContext.in.readString();
                  break;
               case BYTES:
                  value = messageContext.in.readBytes().toByteArray();
                  break;
               case INT64:
                  value = messageContext.in.readInt64();
                  break;
               case UINT64:
                  value = messageContext.in.readUInt64();
                  break;
               case FIXED64:
                  value = messageContext.in.readFixed64();
                  break;
               case SFIXED64:
                  value = messageContext.in.readSFixed64();
                  break;
               case SINT64:
                  value = messageContext.in.readSInt64();
                  break;
               case INT32:
                  value = messageContext.in.readInt32();
                  break;
               case FIXED32:
                  value = messageContext.in.readFixed32();
                  break;
               case UINT32:
                  value = messageContext.in.readUInt32();
                  break;
               case SFIXED32:
                  value = messageContext.in.readSFixed32();
                  break;
               case SINT32:
                  value = messageContext.in.readSInt32();
                  break;
               default:
                  throw new IllegalStateException("Unexpected field type : " + type);
            }
            collection.add(value);
         } else {
            messageContext.unknownFieldSet.readSingleField(tag, messageContext.in);
         }
      }
   }

   @Override
   public <A> A[] readArray(String fieldName, Class<? extends A> elementClass) throws IOException {
      List<A> list = readCollection(fieldName, new ArrayList<A>(), elementClass);
      return list.toArray((A[]) Array.newInstance(elementClass, list.size()));
   }

   private void checkFieldRead(Descriptors.FieldDescriptor fd, boolean expectRepeated) {
      if (expectRepeated) {
         if (!fd.isRepeated()) {
            throw new IllegalArgumentException("This field is not repeated and cannot be read with the methods intended for collections or arrays: " + fd.getFullName());
         }
      } else {
         if (fd.isRepeated()) {
            throw new IllegalArgumentException("A repeated field should be read with one of the methods intended for collections or arrays: " + fd.getFullName());
         }
      }

      if (!messageContext.markField(fd.getNumber())) {
         throw new IllegalStateException("A field cannot be read twice : " + fd.getFullName());
      }

      if (ctx.getConfiguration().logOutOfSequenceReads()
            && log.isEnabled(Logger.Level.WARN)
            && messageContext.getMaxSeenFieldNumber() > fd.getNumber()) {
         log.fieldReadOutOfSequence(fd.getFullName());
      }
   }
}
