package org.infinispan.protostream.impl;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.RawProtobufMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.WrappedMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author anistor@redhat.com
 */
public final class WrappedMessageMarshaller implements RawProtobufMarshaller<WrappedMessage> {

   public static final int WRAPPED_DOUBLE = 1;
   public static final int WRAPPED_FLOAT = 2;
   public static final int WRAPPED_INT64 = 3;
   public static final int WRAPPED_UINT64 = 4;
   public static final int WRAPPED_INT32 = 5;
   public static final int WRAPPED_FIXED64 = 6;
   public static final int WRAPPED_FIXED32 = 7;
   public static final int WRAPPED_BOOL = 8;
   public static final int WRAPPED_STRING = 9;
   public static final int WRAPPED_BYTES = 10;
   public static final int WRAPPED_UINT32 = 11;
   public static final int WRAPPED_SFIXED32 = 12;
   public static final int WRAPPED_SFIXED64 = 13;
   public static final int WRAPPED_SINT32 = 14;
   public static final int WRAPPED_SINT64 = 15;
   public static final int WRAPPED_DESCRIPTOR_FULL_NAME = 16;
   public static final int WRAPPED_MESSAGE_BYTES = 17;
   public static final int WRAPPED_ENUM = 18;
   public static final int WRAPPED_DESCRIPTOR_ID = 19;

   @Override
   public Class<? extends WrappedMessage> getJavaClass() {
      return WrappedMessage.class;
   }

   @Override
   public String getTypeName() {
      return WrappedMessage.PROTOBUF_TYPE_NAME;
   }

   @Override
   public WrappedMessage readFrom(SerializationContext ctx, CodedInputStream in) throws IOException {
      Object o = readWrappedMessage(ctx, in);
      return new WrappedMessage(o);
   }

   @Override
   public void writeTo(SerializationContext ctx, CodedOutputStream out, WrappedMessage wrappedMessage) throws IOException {
      writeWrappedMessage(ctx, out, wrappedMessage.getValue());
   }

   public static void writeWrappedMessage(SerializationContext ctx, CodedOutputStream out, Object t) throws IOException {
      if (t == null) {
         return;
      }

      if (t instanceof String) {
         out.writeString(WRAPPED_STRING, (String) t);
      } else if (t instanceof Long) {
         out.writeInt64(WRAPPED_INT64, (Long) t);
      } else if (t instanceof Integer) {
         out.writeInt32(WRAPPED_INT32, (Integer) t);
      } else if (t instanceof Double) {
         out.writeDouble(WRAPPED_DOUBLE, (Double) t);
      } else if (t instanceof Float) {
         out.writeFloat(WRAPPED_FLOAT, (Float) t);
      } else if (t instanceof Boolean) {
         out.writeBool(WRAPPED_BOOL, (Boolean) t);
      } else if (t instanceof byte[]) {
         byte[] bytes = (byte[]) t;
         out.writeTag(WRAPPED_BYTES, com.google.protobuf.WireFormat.WIRETYPE_LENGTH_DELIMITED);
         out.writeRawVarint32(bytes.length);
         out.writeRawBytes(bytes);
      } else if (t instanceof Enum) {
         // use an enum encoder
         EnumMarshaller enumMarshaller = (EnumMarshaller) ctx.getMarshaller((Class<Enum>) t.getClass());
         out.writeString(WRAPPED_DESCRIPTOR_FULL_NAME, enumMarshaller.getTypeName());
         out.writeEnum(WRAPPED_ENUM, enumMarshaller.encode((Enum) t));
      } else {
         // this is either an unknown primitive type or a message type
         // try to use a message marshaller
         BaseMarshaller marshaller = ctx.getMarshaller(t.getClass());
         out.writeString(WRAPPED_DESCRIPTOR_FULL_NAME, marshaller.getTypeName());

         ByteArrayOutputStream buffer = new ByteArrayOutputStream();
         ProtoStreamWriterImpl writer = new ProtoStreamWriterImpl((SerializationContextImpl) ctx);
         writer.write(CodedOutputStream.newInstance(buffer), t);

         out.writeTag(WRAPPED_MESSAGE_BYTES, com.google.protobuf.WireFormat.WIRETYPE_LENGTH_DELIMITED);
         out.writeRawVarint32(buffer.size());
         out.writeRawBytes(buffer.toByteArray());
      }
      out.flush();
   }

   public static Object readWrappedMessage(SerializationContext ctx, CodedInputStream in) throws IOException {
      String descriptorFullName = null;
      int enumValue = -1;
      byte[] messageBytes = null;
      Object value = null;
      int readTags = 0;

      int tag;
      while ((tag = in.readTag()) != 0) {
         readTags++;
         switch (tag) {
            case WRAPPED_DESCRIPTOR_FULL_NAME << 3 | com.google.protobuf.WireFormat.WIRETYPE_LENGTH_DELIMITED:
               descriptorFullName = in.readString();
               break;
            case WRAPPED_ENUM << 3 | com.google.protobuf.WireFormat.WIRETYPE_VARINT:
               enumValue = in.readEnum();
               break;
            case WRAPPED_MESSAGE_BYTES << 3 | com.google.protobuf.WireFormat.WIRETYPE_LENGTH_DELIMITED:
               messageBytes = in.readBytes().toByteArray();
               break;
            case WRAPPED_STRING << 3 | com.google.protobuf.WireFormat.WIRETYPE_LENGTH_DELIMITED:
               value = in.readString();
               break;
            case WRAPPED_BYTES << 3 | com.google.protobuf.WireFormat.WIRETYPE_LENGTH_DELIMITED:
               value = in.readBytes().toByteArray();
               break;
            case WRAPPED_BOOL << 3 | com.google.protobuf.WireFormat.WIRETYPE_VARINT:
               value = in.readBool();
               break;
            case WRAPPED_DOUBLE << 3 | com.google.protobuf.WireFormat.WIRETYPE_FIXED64:
               value = in.readDouble();
               break;
            case WRAPPED_FLOAT << 3 | com.google.protobuf.WireFormat.WIRETYPE_FIXED32:
               value = in.readFloat();
               break;
            case WRAPPED_FIXED32 << 3 | com.google.protobuf.WireFormat.WIRETYPE_FIXED32:
               value = in.readFixed32();
               break;
            case WRAPPED_SFIXED32 << 3 | com.google.protobuf.WireFormat.WIRETYPE_FIXED32:
               value = in.readSFixed32();
               break;
            case WRAPPED_FIXED64 << 3 | com.google.protobuf.WireFormat.WIRETYPE_FIXED64:
               value = in.readFixed64();
               break;
            case WRAPPED_SFIXED64 << 3 | com.google.protobuf.WireFormat.WIRETYPE_FIXED64:
               value = in.readSFixed64();
               break;
            case WRAPPED_INT64 << 3 | com.google.protobuf.WireFormat.WIRETYPE_VARINT:
               value = in.readInt64();
               break;
            case WRAPPED_UINT64 << 3 | com.google.protobuf.WireFormat.WIRETYPE_VARINT:
               value = in.readUInt64();
               break;
            case WRAPPED_SINT64 << 3 | com.google.protobuf.WireFormat.WIRETYPE_VARINT:
               value = in.readSInt64();
               break;
            case WRAPPED_INT32 << 3 | com.google.protobuf.WireFormat.WIRETYPE_VARINT:
               value = in.readInt32();
               break;
            case WRAPPED_UINT32 << 3 | com.google.protobuf.WireFormat.WIRETYPE_VARINT:
               value = in.readUInt32();
               break;
            case WRAPPED_SINT32 << 3 | com.google.protobuf.WireFormat.WIRETYPE_VARINT:
               value = in.readSInt32();
               break;
            default:
               throw new IllegalStateException("Unexpected tag : " + tag);
         }
      }

      if (value == null && descriptorFullName == null && messageBytes == null) {
         return null;
      }

      if (value != null) {
         if (readTags != 1) {
            throw new IOException("Invalid message encoding.");
         }
         return value;
      }

      if (descriptorFullName == null || readTags != 2) {
         throw new IOException("Invalid message encoding.");
      }

      if (messageBytes != null) {
         BaseMarshaller marshaller = ctx.getMarshaller(descriptorFullName);
         ByteArrayInputStream bais2 = new ByteArrayInputStream(messageBytes);
         CodedInputStream in2 = CodedInputStream.newInstance(bais2);
         if (marshaller instanceof MessageMarshaller) {
            ProtoStreamReaderImpl reader = new ProtoStreamReaderImpl((SerializationContextImpl) ctx);
            return reader.read(in2, (MessageMarshaller) marshaller);
         } else {
            return ((RawProtobufMarshaller) marshaller).readFrom(ctx, in2);
         }
      } else {
         EnumMarshaller enumMarshaller = (EnumMarshaller) ctx.getMarshaller(descriptorFullName);
         return enumMarshaller.decode(enumValue);
      }
   }
}
