package org.infinispan.protostream.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.UnknownFieldSet;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.JavaType;
import org.infinispan.protostream.descriptors.Type;
import org.jboss.logging.Logger;

/**
 * @author anistor@redhat.com
 */
final class ProtoStreamReaderImpl implements MessageMarshaller.ProtoStreamReader {

   private static final Log log = Log.LogFactory.getLog(ProtoStreamReaderImpl.class);

   private static final EnumSet<Type> primitiveTypes = EnumSet.of(
         Type.DOUBLE,
         Type.FLOAT,
         Type.INT64,
         Type.UINT64,
         Type.INT32,
         Type.FIXED64,
         Type.FIXED32,
         Type.BOOL,
         Type.STRING,
         Type.BYTES,
         Type.UINT32,
         Type.SFIXED32,
         Type.SFIXED64,
         Type.SINT32,
         Type.SINT64
   );

   private final SerializationContextImpl ctx;

   private ReadMessageContext messageContext;

   ProtoStreamReaderImpl(SerializationContextImpl ctx) {
      this.ctx = ctx;
   }

   ReadMessageContext pushContext(FieldDescriptor fd, MessageMarshallerDelegate<?> marshallerDelegate, RawProtoStreamReader in) {
      messageContext = new ReadMessageContext(messageContext, fd == null ? null : fd.getName(), marshallerDelegate, in);
      return messageContext;
   }

   void popContext() {
      messageContext = messageContext.getParentContext();
   }

   UnknownFieldSet getUnknownFieldSet() {
      return messageContext.unknownFieldSet;
   }

   private Object readPrimitive(String fieldName, JavaType javaType) throws IOException {
      final FieldDescriptor fd = messageContext.marshallerDelegate.getFieldByName(fieldName);
      final Type type = fd.getType();
      if (type == Type.ENUM
            || type == Type.GROUP
            || type == Type.MESSAGE) {
         throw new IllegalArgumentException("Declared field type is not a primitive : " + fd.getFullName());
      }
      if (fd.getJavaType() != javaType) {
         throw new IllegalArgumentException("Declared field type is not of the expected type : " + fd.getFullName());
      }
      checkFieldRead(fd, false);
      final int expectedTag = WireFormat.makeTag(fd.getNumber(), type.getWireType());

      Object o = messageContext.unknownFieldSet.consumeTag(expectedTag);
      if (o != null) {
         return convertWireTypeToJavaType(type, o);
      }

      RawProtoStreamReader in = messageContext.in;
      while (true) {
         int tag = in.readTag();
         if (tag == 0) {
            break;
         }
         if (tag == expectedTag) {
            switch (type) {
               case DOUBLE:
                  return in.readDouble();
               case FLOAT:
                  return in.readFloat();
               case BOOL:
                  return in.readBool();
               case STRING:
                  return in.readString();
               case BYTES:
                  return in.readByteArray();
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
                  throw new IOException("Unexpected field type : " + type);
            }
         }
         messageContext.unknownFieldSet.readSingleField(tag, in);
      }

      if (fd.hasDefaultValue()) {
         return fd.getDefaultValue();
      }

      if (fd.isRequired()) {
         throw new IOException("Field " + fd.getFullName() + " is required but is not present in the stream");
      }

      return null;
   }

   private Object convertWireTypeToJavaType(Type type, Object o) {
      if (type == Type.STRING) {
         try {
            o = new String((byte[]) o, "UTF-8");
         } catch (UnsupportedEncodingException e) {
            // Hell is freezing
            throw new RuntimeException("UTF-8 not supported", e);
         }
      } else if (type == Type.BYTES) {
         o = (byte[]) o;
      } else if (type == Type.INT32
            || type == Type.UINT32
            || type == Type.SINT32) {
         o = ((Long) o).intValue();
      } else if (type == Type.FIXED32
            || type == Type.SFIXED32) {
         //o is an Integer
         o = (Integer) o;
      } else if (type == Type.INT64
            || type == Type.UINT64
            || type == Type.FIXED64
            || type == Type.SFIXED64
            || type == Type.SINT64) {
         //o is a Long
         o = (Long) o;
      } else if (type == Type.BOOL) {
         o = ((Long) o) != 0;
      } else if (type == Type.FLOAT) {
         o = Float.intBitsToFloat((Integer) o);
      } else if (type == Type.DOUBLE) {
         o = Double.longBitsToDouble((Long) o);
      }
      return o;
   }

   @Override
   public ImmutableSerializationContext getSerializationContext() {
      return ctx;
   }

   @Override
   public Integer readInt(String fieldName) throws IOException {
      return (Integer) readPrimitive(fieldName, JavaType.INT);
   }

   @Override
   public Long readLong(String fieldName) throws IOException {
      return (Long) readPrimitive(fieldName, JavaType.LONG);
   }

   @Override
   public Date readDate(String fieldName) throws IOException {
      Long tstamp = readLong(fieldName);
      return tstamp == null ? null : new Date(tstamp);
   }

   @Override
   public Float readFloat(String fieldName) throws IOException {
      return (Float) readPrimitive(fieldName, JavaType.FLOAT);
   }

   @Override
   public Double readDouble(String fieldName) throws IOException {
      return (Double) readPrimitive(fieldName, JavaType.DOUBLE);
   }

   @Override
   public Boolean readBoolean(String fieldName) throws IOException {
      return (Boolean) readPrimitive(fieldName, JavaType.BOOLEAN);
   }

   @Override
   public String readString(String fieldName) throws IOException {
      return (String) readPrimitive(fieldName, JavaType.STRING);
   }

   @Override
   public byte[] readBytes(String fieldName) throws IOException {
      return (byte[]) readPrimitive(fieldName, JavaType.BYTE_STRING);
   }

   @Override
   public InputStream readBytesAsInputStream(String fieldName) throws IOException {
      byte[] bytes = readBytes(fieldName);
      return bytes != null ? new ByteArrayInputStream(bytes) : null;
   }

   @Override
   public <E extends Enum<E>> E readEnum(String fieldName, Class<E> clazz) throws IOException {
      return readObject(fieldName, clazz);
   }

   @Override
   public <E> E readObject(String fieldName, Class<E> clazz) throws IOException {
      final FieldDescriptor fd = messageContext.marshallerDelegate.getFieldByName(fieldName);
      checkFieldRead(fd, false);

      if (fd.getType() == Type.ENUM) {
         return ctx.getMarshallerDelegate(clazz).unmarshall(fd, this, messageContext.in);
      }

      //todo validate type is compatible with readObject
      final int expectedTag = WireFormat.makeTag(fd.getNumber(), fd.getType().getWireType());
      Object o = messageContext.unknownFieldSet.consumeTag(expectedTag);
      if (o != null) {
         byte[] byteArray = (byte[]) o;
         return readNestedObject(fd, clazz, RawProtoStreamReaderImpl.newInstance(byteArray), byteArray.length);
      }

      while (true) {
         int tag = messageContext.in.readTag();
         if (tag == 0) {
            break;
         }
         if (tag == expectedTag) {
            return readNestedObject(fd, clazz, messageContext.in, -1);
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
   private <A> A readNestedObject(FieldDescriptor fd, Class<A> clazz, RawProtoStreamReader in, int length) throws IOException {
      BaseMarshallerDelegate<A> marshallerDelegate = ctx.getMarshallerDelegate(clazz);
      A a;
      if (fd.getType() == Type.GROUP) {
         a = marshallerDelegate.unmarshall(fd, this, in);
         in.checkLastTagWas(WireFormat.makeTag(fd.getNumber(), WireFormat.WIRETYPE_END_GROUP));
      } else if (fd.getType() == Type.MESSAGE) {
         if (length < 0) {
            length = in.readRawVarint32();
         }
         int oldLimit = in.pushLimit(length);
         a = marshallerDelegate.unmarshall(fd, this, in);
         in.checkLastTagWas(0);
         in.popLimit(oldLimit);
      } else {
         throw new IllegalArgumentException("Declared field type is not a message or an enum : " + fd.getFullName());
      }
      return a;
   }

   @Override
   public <E, C extends Collection<? super E>> C readCollection(String fieldName, C collection, Class<E> elementClass) throws IOException {
      final FieldDescriptor fd = messageContext.marshallerDelegate.getFieldByName(fieldName);
      checkFieldRead(fd, true);

      if (primitiveTypes.contains(fd.getType())) {
         readPrimitiveCollection(fd, (Collection<Object>) collection, elementClass);
         return collection;
      }

      //todo validate type is compatible with readCollection
      final int expectedTag = WireFormat.makeTag(fd.getNumber(), fd.getType().getWireType());

      while (true) {
         Object o = messageContext.unknownFieldSet.consumeTag(expectedTag);
         if (o == null) {
            break;
         }
         byte[] byteArray = (byte[]) o;
         RawProtoStreamReader in = RawProtoStreamReaderImpl.newInstance(byteArray);
         collection.add(readNestedObject(fd, elementClass, in, byteArray.length));
      }

      while (true) {
         int tag = messageContext.in.readTag();
         if (tag == 0) {
            break;
         }
         if (tag == expectedTag) {
            collection.add(readNestedObject(fd, elementClass, messageContext.in, -1));
         } else {
            messageContext.unknownFieldSet.readSingleField(tag, messageContext.in);
         }
      }
      return collection;
   }

   private void readPrimitiveCollection(FieldDescriptor fd, Collection<? super Object> collection, Class elementClass) throws IOException {
      final int expectedTag = WireFormat.makeTag(fd.getNumber(), fd.getType().getWireType());
      Type type = fd.getType();

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
                  value = messageContext.in.readByteArray();
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
   public <E> E[] readArray(String fieldName, Class<? extends E> elementClass) throws IOException {
      List<E> list = readCollection(fieldName, new ArrayList<E>(), elementClass);
      return list.toArray((E[]) Array.newInstance(elementClass, list.size()));
   }

   private void checkFieldRead(FieldDescriptor fd, boolean expectRepeated) {
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
