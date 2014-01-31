package org.infinispan.protostream.impl;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.Descriptors;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import com.google.protobuf.ProtocolMessageEnum;
import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.Message;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.RawProtobufMarshaller;
import org.infinispan.protostream.SerializationContext;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

/**
 * @author anistor@redhat.com
 */
public final class ProtoStreamReaderImpl implements MessageMarshaller.ProtoStreamReader {

   private final SerializationContext ctx;

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

   private ReadMessageContext messageContext;

   public ProtoStreamReaderImpl(SerializationContext ctx) {
      this.ctx = ctx;
   }

   public <A> A read(CodedInputStream in, Class<A> clazz) throws IOException {
      if (MessageLite.class.isAssignableFrom(clazz)) {
         try {
            Parser<A> parser = (Parser) clazz.getDeclaredField("PARSER").get(null);
            return parser.parseFrom(messageContext.in);
         } catch (NoSuchFieldException e) {
            throw new IOException(e);
         } catch (IllegalAccessException e) {
            throw new IOException(e);
         }
      }

      BaseMarshaller<A> marshaller = ctx.getMarshaller(clazz);
      Descriptors.Descriptor messageDescriptor = ctx.getMessageDescriptor(marshaller.getTypeName());
      resetContext();
      enterContext(null, messageDescriptor, in);
      A a = marshaller instanceof MessageMarshaller ? ((MessageMarshaller<A>) marshaller).readFrom(this) : ((RawProtobufMarshaller<A>) marshaller).readFrom(ctx, in);
      exitContext();
      return a;
   }

   public <A> A read(CodedInputStream in, MessageMarshaller<A> marshaller) throws IOException {
      Descriptors.Descriptor messageDescriptor = ctx.getMessageDescriptor(marshaller.getTypeName());
      resetContext();
      enterContext(null, messageDescriptor, in);
      A a = marshaller.readFrom(this);
      exitContext();
      return a;
   }

   private void resetContext() {
      messageContext = null;
   }

   private void enterContext(String fieldName, Descriptors.Descriptor messageDescriptor, CodedInputStream in) {
      messageContext = new ReadMessageContext(messageContext, fieldName, messageDescriptor, in);
   }

   private void exitContext() {
      //todo on context exit need to validate that all required fields were seen in the stream (even if not read from api)

      //todo check now that we have seen in the stream all required fields (ie. the app that wrote the message did not break the protocol)

      // check that all required fields were read
      for (Descriptors.FieldDescriptor fd : messageContext.getFieldDescriptors().values()) {
         if (fd.isRequired() && !messageContext.getSeenFields().contains(fd.getNumber())) {
            throw new IllegalStateException("Field " + fd.getName() + " is declared as required but was never read by the MessageMarshaller");
         }
      }
      if (messageContext.getFieldDescriptors().size() != messageContext.getSeenFields().size()) {
         //todo log that not all declared fields were processed by the marshaller
      }
      messageContext = messageContext.getParentContext();
   }

   private Object readPrimitive(String fieldName, Descriptors.FieldDescriptor.JavaType javaType) throws IOException {
      Descriptors.FieldDescriptor fd = messageContext.getFieldByName(fieldName);
      if (fd.getType() == Descriptors.FieldDescriptor.Type.ENUM
            || fd.getType() == Descriptors.FieldDescriptor.Type.GROUP
            || fd.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
         throw new IllegalArgumentException("Declared field type is not a primitive : " + fd.getName());
      }
      if (fd.getJavaType() != javaType) {
         throw new IllegalArgumentException("Declared field type is not of the expected type : " + fd.getName());
      }
      checkNonRepeatedField(fd);
      int expectedTag = WireFormat.makeTag(fd.getNumber(), fd.getLiteType().getWireType());
      Object o = messageContext.unknownFieldSet.consumeTag(expectedTag);
      if (o != null) {
         if (javaType == Descriptors.FieldDescriptor.JavaType.STRING) {
            o = ((ByteString) o).toStringUtf8();
         }
         return o;
      }

      while (true) {
         int tag = messageContext.in.readTag();
         if (tag == 0) {
            break;
         }
         if (tag == expectedTag) {
            switch (fd.getType()) {
               case DOUBLE:
                  return messageContext.in.readDouble();
               case FLOAT:
                  return messageContext.in.readFloat();
               case BOOL:
                  return messageContext.in.readBool();
               case STRING:
                  return messageContext.in.readString();
               case BYTES:
                  return messageContext.in.readBytes().toByteArray();
               case INT32:
                  return messageContext.in.readInt32();
               case SFIXED32:
                  return messageContext.in.readSFixed32();
               case FIXED32:
                  return messageContext.in.readFixed32();
               case UINT32:
                  return messageContext.in.readUInt32();
               case SINT32:
                  return messageContext.in.readSInt32();
               case INT64:
                  return messageContext.in.readInt64();
               case UINT64:
                  return messageContext.in.readUInt64();
               case FIXED64:
                  return messageContext.in.readFixed64();
               case SFIXED64:
                  return messageContext.in.readSFixed64();
               case SINT64:
                  return messageContext.in.readSInt64();
               default:
                  throw new IOException("Unexpected field type : " + fd.getType());
            }
         }
         messageContext.unknownFieldSet.readSingleField(tag, messageContext.in);
      }

      if (fd.isRequired()) {
         throw new IllegalStateException("Field " + fd.getName() + " is required but is not present in the stream");
      }

      if (fd.hasDefaultValue()) {
         return fd.getDefaultValue();
      }

      return null;
   }

   @Override
   public Integer readInt(String fieldName) throws IOException {
      Object o = readPrimitive(fieldName, Descriptors.FieldDescriptor.JavaType.INT);
      if (o == null) return null;
      return o instanceof Integer ? (Integer) o : ((Number) o).intValue();  //todo [anistor] hack!
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
      Descriptors.FieldDescriptor fd = messageContext.getFieldByName(fieldName);
      checkNonRepeatedField(fd);

      if (fd.getType() == Descriptors.FieldDescriptor.Type.ENUM) {
         if (ProtocolMessageEnum.class.isAssignableFrom(clazz)) {
            return (A) readProtocolMessageEnum(fd, (Class<ProtocolMessageEnum>) clazz);
         } else {
            return (A) readEnum(fd, (Class<Enum>) clazz);
         }
      }

      //todo validate type is compatible with readObject
      int expectedTag = WireFormat.makeTag(fd.getNumber(), fd.getLiteType().getWireType());
      Object o = messageContext.unknownFieldSet.consumeTag(expectedTag);
      if (o != null) {
         ByteString byteString = (ByteString) o;
         CodedInputStream codedInputStream = byteString.newCodedInput();
         return readObject(fd, clazz, codedInputStream, byteString.size());
      }

      while (true) {
         int tag = messageContext.in.readTag();
         if (tag == 0) {
            break;
         }
         if (tag == expectedTag) {
            return readObject(fd, clazz, messageContext.in, -1);
         }
         messageContext.unknownFieldSet.readSingleField(tag, messageContext.in);
      }

      return null;
   }

   private <A extends ProtocolMessageEnum> A readProtocolMessageEnum(Descriptors.FieldDescriptor fd, Class<A> clazz) throws IOException {
      assert fd.getLiteType().getWireType() == WireFormat.WIRETYPE_VARINT;

      int expectedTag = WireFormat.makeTag(fd.getNumber(), WireFormat.WIRETYPE_VARINT);
      int enumValue;
      Object o = messageContext.unknownFieldSet.consumeTag(expectedTag);
      if (o != null) {
         enumValue = ((Long) o).intValue();      //todo why is this a Long and not an Integer?
      } else {
         while (true) {
            int tag = messageContext.in.readTag();
            if (tag == 0) {
               return null;
            }
            if (tag == expectedTag) {
               enumValue = messageContext.in.readEnum();
               break;
            }
            messageContext.unknownFieldSet.readSingleField(tag, messageContext.in);
         }
      }

      A decoded = decodeProtocolMessageEnum(clazz, enumValue);

      // the enum value was not recognized by the decoder so rather than discarding it we add it to the unknown
      if (decoded == null) {
         messageContext.unknownFieldSet.putVarintField(expectedTag, enumValue);
      }

      return decoded;
   }

   private <A extends ProtocolMessageEnum> A decodeProtocolMessageEnum(Class<A> clazz, int enumValue) throws IOException {
      try {
         Method valueOf = clazz.getDeclaredMethod("valueOf", int.class);
         return (A) valueOf.invoke(null, enumValue);
      } catch (NoSuchMethodException e) {
         throw new IOException(e);
      } catch (IllegalAccessException e) {
         throw new IOException(e);
      } catch (InvocationTargetException e) {
         throw new IOException(e);
      }
   }

   private <A extends Enum<A>> A readEnum(Descriptors.FieldDescriptor fd, Class<A> clazz) throws IOException {
      assert fd.getLiteType().getWireType() == WireFormat.WIRETYPE_VARINT;

      int expectedTag = WireFormat.makeTag(fd.getNumber(), WireFormat.WIRETYPE_VARINT);
      int enumValue;
      Object o = messageContext.unknownFieldSet.consumeTag(expectedTag);
      if (o != null) {
         enumValue = ((Long) o).intValue();      //todo why is this a Long and not an Integer?
      } else {
         while (true) {
            int tag = messageContext.in.readTag();
            if (tag == 0) {
               return null;
            }
            if (tag == expectedTag) {
               enumValue = messageContext.in.readEnum();
               break;
            }
            messageContext.unknownFieldSet.readSingleField(tag, messageContext.in);
         }
      }

      EnumMarshaller<A> enumMarshaller = (EnumMarshaller<A>) ctx.getMarshaller(clazz);
      A decoded = enumMarshaller.decode(enumValue);

      // the enum value was not recognized by the decoder so rather than discarding it we add it to the unknown
      if (decoded == null) {
         messageContext.unknownFieldSet.putVarintField(expectedTag, enumValue);
      }

      return decoded;
   }

   private <A> A readObject(Descriptors.FieldDescriptor fd, Class<A> clazz, CodedInputStream in, int length) throws IOException {
      BaseMarshaller<A> marshaller = ctx.getMarshaller(clazz);
      enterContext(fd.getName(), fd.getMessageType(), in);
      A a;
      if (fd.getType() == Descriptors.FieldDescriptor.Type.GROUP) {
         a = unmarshall(marshaller, in);
         in.checkLastTagWas(WireFormat.makeTag(fd.getNumber(), WireFormat.WIRETYPE_END_GROUP));
      } else if (fd.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
         if (length < 0) {
            length = in.readRawVarint32();
         }
         int oldLimit = in.pushLimit(length);
         a = unmarshall(marshaller, in);
         in.checkLastTagWas(0);
         in.popLimit(oldLimit);
      } else {
         throw new IllegalArgumentException("Declared field type is not a message or an enum : " + fd.getName());
      }
      exitContext();
      return a;
   }

   private <A> A unmarshall(BaseMarshaller<A> marshaller, CodedInputStream in) throws IOException {
      A a = marshaller instanceof MessageMarshaller ? ((MessageMarshaller<A>) marshaller).readFrom(this) : ((RawProtobufMarshaller<A>) marshaller).readFrom(ctx, in);
      messageContext.unknownFieldSet.readAllFields(messageContext.in);
      if (a instanceof Message && !messageContext.unknownFieldSet.isEmpty()) {
         ((Message) a).setUnknownFieldSet(messageContext.unknownFieldSet);
      }
      return a;
   }

   @Override
   public <A, C extends Collection<? super A>> C readCollection(String fieldName, C collection, Class<? extends A> clazz) throws IOException {
      Descriptors.FieldDescriptor fd = messageContext.getFieldByName(fieldName);
      checkRepeatedField(fd);

      if (primitiveTypes.contains(fd.getType())) {
         readPrimitiveCollection(fd, (Collection<Object>) collection, clazz);
         return collection;
      }

      //todo validate type is compatible with readObject
      int expectedTag = WireFormat.makeTag(fd.getNumber(), fd.getLiteType().getWireType());

      while (true) {
         Object o = messageContext.unknownFieldSet.consumeTag(expectedTag);
         if (o == null) {
            break;
         }
         ByteString byteString = (ByteString) o;
         CodedInputStream codedInputStream = byteString.newCodedInput();
         collection.add(readObject(fd, clazz, codedInputStream, byteString.size()));
      }

      while (true) {
         int tag = messageContext.in.readTag();
         if (tag == 0) {
            break;
         }
         if (tag == expectedTag) {
            collection.add(readObject(fd, clazz, messageContext.in, -1));
         } else {
            messageContext.unknownFieldSet.readSingleField(tag, messageContext.in);
         }
      }
      return collection;
   }

   private void readPrimitiveCollection(Descriptors.FieldDescriptor fd, Collection<? super Object> collection, Class clazz) throws IOException {
      int expectedTag = WireFormat.makeTag(fd.getNumber(), fd.getLiteType().getWireType());

      while (true) {
         Object o = messageContext.unknownFieldSet.consumeTag(expectedTag);
         if (o == null) {
            break;
         }
         collection.add(o);   //todo check that (o.getClass() == clazz)
      }

      while (true) {
         int tag = messageContext.in.readTag();
         if (tag == 0) {
            break;
         }
         if (tag == expectedTag) {
            switch (fd.getType()) {
               case DOUBLE:
                  collection.add(messageContext.in.readDouble());
                  break;
               case FLOAT:
                  collection.add(messageContext.in.readFloat());
                  break;
               case BOOL:
                  collection.add(messageContext.in.readBool());
                  break;
               case STRING:
                  collection.add(messageContext.in.readString());
                  break;
               case BYTES:
                  collection.add(messageContext.in.readBytes().toByteArray());
                  break;
               case INT64:
                  collection.add(messageContext.in.readInt64());
                  break;
               case UINT64:
                  collection.add(messageContext.in.readUInt64());
                  break;
               case FIXED64:
                  collection.add(messageContext.in.readFixed64());
                  break;
               case SFIXED64:
                  collection.add(messageContext.in.readSFixed64());
                  break;
               case SINT64:
                  collection.add(messageContext.in.readSInt64());
                  break;
               case INT32:
                  collection.add(messageContext.in.readInt32());
                  break;
               case FIXED32:
                  collection.add(messageContext.in.readFixed32());
                  break;
               case UINT32:
                  collection.add(messageContext.in.readUInt32());
                  break;
               case SFIXED32:
                  collection.add(messageContext.in.readSFixed32());
                  break;
               case SINT32:
                  collection.add(messageContext.in.readSInt32());
                  break;
               default:
                  throw new IllegalStateException("Unexpected field type : " + fd.getType());
            }
         } else {
            messageContext.unknownFieldSet.readSingleField(tag, messageContext.in);
         }
      }
   }

   @Override
   public <A> A[] readArray(String fieldName, Class<? extends A> clazz) throws IOException {
      List<A> list = readCollection(fieldName, new ArrayList<A>(), clazz);
      return list.toArray((A[]) Array.newInstance(clazz, list.size()));
   }

   private void checkRepeatedField(Descriptors.FieldDescriptor fd) {
      if (!fd.isRepeated()) {
         throw new IllegalArgumentException("This field is not repeated and cannot be read with one of the methods intended for collections or arrays: " + fd.getName());
      }
      if (!messageContext.getSeenFields().add(fd.getNumber())) {
         throw new IllegalArgumentException("Cannot read a field twice : " + fd.getName());
      }
   }

   private void checkNonRepeatedField(Descriptors.FieldDescriptor fd) {
      if (fd.isRepeated()) {
         throw new IllegalArgumentException("A repeated field should be read with one of the methods intended for collections or arrays: " + fd.getName());
      }
      if (!messageContext.getSeenFields().add(fd.getNumber())) {
         throw new IllegalArgumentException("Cannot read a field twice : " + fd.getName());
      }
   }
}
