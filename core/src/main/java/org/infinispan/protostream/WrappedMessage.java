package org.infinispan.protostream;

import java.io.IOException;

import org.infinispan.protostream.impl.BaseMarshallerDelegate;
import org.infinispan.protostream.impl.ByteArrayOutputStreamEx;
import org.infinispan.protostream.impl.RawProtoStreamReaderImpl;
import org.infinispan.protostream.impl.RawProtoStreamWriterImpl;
import org.infinispan.protostream.impl.SerializationContextImpl;
import org.infinispan.protostream.impl.WireFormat;

/**
 * @author anistor@redhat.com
 * @since 1.0
 */
public final class WrappedMessage {

   public static final String PROTOBUF_TYPE_NAME = "org.infinispan.protostream.WrappedMessage";

   // field numbers
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

   /**
    * The wrapped object or (boxed) primitive.
    */
   private final Object value;

   public WrappedMessage(Object value) {
      this.value = value;
   }

   public Object getValue() {
      return value;
   }

   public static void writeMessage(SerializationContext ctx, RawProtoStreamWriter out, Object t) throws IOException {
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
         out.writeBytes(WRAPPED_BYTES, (byte[]) t);
      } else if (t instanceof Enum) {
         // use an enum encoder
         EnumMarshaller enumMarshaller = (EnumMarshaller) ctx.getMarshaller((Class<Enum>) t.getClass());
         int encodedEnum = enumMarshaller.encode((Enum) t);
         Integer typeId = ctx.getTypeIdByName(enumMarshaller.getTypeName());
         if (typeId == null) {
            out.writeString(WRAPPED_DESCRIPTOR_FULL_NAME, enumMarshaller.getTypeName());
         } else {
            out.writeInt32(WRAPPED_DESCRIPTOR_ID, typeId);
         }
         out.writeEnum(WRAPPED_ENUM, encodedEnum);
      } else {
         // this is either an unknown primitive type or a message type
         // try to use a message marshaller
         BaseMarshallerDelegate marshallerDelegate = ((SerializationContextImpl) ctx).getMarshallerDelegate(t.getClass());
         ByteArrayOutputStreamEx buffer = new ByteArrayOutputStreamEx();
         RawProtoStreamWriter nestedOut = RawProtoStreamWriterImpl.newInstance(buffer);
         marshallerDelegate.marshall(null, t, null, nestedOut);
         nestedOut.flush();

         Integer typeId = ctx.getTypeIdByName(marshallerDelegate.getMarshaller().getTypeName());
         if (typeId == null) {
            out.writeString(WRAPPED_DESCRIPTOR_FULL_NAME, marshallerDelegate.getMarshaller().getTypeName());
         } else {
            out.writeInt32(WRAPPED_DESCRIPTOR_ID, typeId);
         }
         out.writeBytes(WRAPPED_MESSAGE_BYTES, buffer.getByteBuffer());
      }
      out.flush();
   }

   public static Object readMessage(SerializationContext ctx, RawProtoStreamReader in) throws IOException {
      String descriptorFullName = null;
      Integer typeId = null;
      int enumValue = -1;
      byte[] messageBytes = null;
      Object value = null;
      int readTags = 0;

      int tag;
      while ((tag = in.readTag()) != 0) {
         readTags++;
         switch (tag) {
            case WRAPPED_DESCRIPTOR_FULL_NAME << 3 | WireFormat.WIRETYPE_LENGTH_DELIMITED:
               descriptorFullName = in.readString();
               break;
            case WRAPPED_DESCRIPTOR_ID << 3 | WireFormat.WIRETYPE_VARINT:
               typeId = in.readInt32();
               break;
            case WRAPPED_ENUM << 3 | WireFormat.WIRETYPE_VARINT:
               enumValue = in.readEnum();
               break;
            case WRAPPED_MESSAGE_BYTES << 3 | WireFormat.WIRETYPE_LENGTH_DELIMITED:
               messageBytes = in.readByteArray();
               break;
            case WRAPPED_STRING << 3 | WireFormat.WIRETYPE_LENGTH_DELIMITED:
               value = in.readString();
               break;
            case WRAPPED_BYTES << 3 | WireFormat.WIRETYPE_LENGTH_DELIMITED:
               value = in.readByteArray();
               break;
            case WRAPPED_BOOL << 3 | WireFormat.WIRETYPE_VARINT:
               value = in.readBool();
               break;
            case WRAPPED_DOUBLE << 3 | WireFormat.WIRETYPE_FIXED64:
               value = in.readDouble();
               break;
            case WRAPPED_FLOAT << 3 | WireFormat.WIRETYPE_FIXED32:
               value = in.readFloat();
               break;
            case WRAPPED_FIXED32 << 3 | WireFormat.WIRETYPE_FIXED32:
               value = in.readFixed32();
               break;
            case WRAPPED_SFIXED32 << 3 | WireFormat.WIRETYPE_FIXED32:
               value = in.readSFixed32();
               break;
            case WRAPPED_FIXED64 << 3 | WireFormat.WIRETYPE_FIXED64:
               value = in.readFixed64();
               break;
            case WRAPPED_SFIXED64 << 3 | WireFormat.WIRETYPE_FIXED64:
               value = in.readSFixed64();
               break;
            case WRAPPED_INT64 << 3 | WireFormat.WIRETYPE_VARINT:
               value = in.readInt64();
               break;
            case WRAPPED_UINT64 << 3 | WireFormat.WIRETYPE_VARINT:
               value = in.readUInt64();
               break;
            case WRAPPED_SINT64 << 3 | WireFormat.WIRETYPE_VARINT:
               value = in.readSInt64();
               break;
            case WRAPPED_INT32 << 3 | WireFormat.WIRETYPE_VARINT:
               value = in.readInt32();
               break;
            case WRAPPED_UINT32 << 3 | WireFormat.WIRETYPE_VARINT:
               value = in.readUInt32();
               break;
            case WRAPPED_SINT32 << 3 | WireFormat.WIRETYPE_VARINT:
               value = in.readSInt32();
               break;
            default:
               throw new IllegalStateException("Unexpected tag : " + tag);
         }
      }

      if (value == null && descriptorFullName == null && typeId == null && messageBytes == null) {
         return null;
      }

      if (value != null) {
         if (readTags != 1) {
            throw new IOException("Invalid message encoding.");
         }
         return value;
      }

      if (descriptorFullName == null && typeId == null || descriptorFullName != null && typeId != null || readTags != 2) {
         throw new IOException("Invalid message encoding.");
      }

      if (typeId != null) {
         descriptorFullName = ctx.getTypeNameById(typeId);
      }
      BaseMarshallerDelegate marshallerDelegate = ((SerializationContextImpl) ctx).getMarshallerDelegate(descriptorFullName);
      if (messageBytes != null) {
         // it's a Message type
         RawProtoStreamReader nestedInput = RawProtoStreamReaderImpl.newInstance(messageBytes);
         return marshallerDelegate.unmarshall(null, null, nestedInput);
      } else {
         // it's an Enum
         EnumMarshaller marshaller = (EnumMarshaller) marshallerDelegate.getMarshaller();
         return marshaller.decode(enumValue);
      }
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      WrappedMessage other = (WrappedMessage) o;

      return value != null ? value.equals(other.value) : other.value == null;
   }

   @Override
   public int hashCode() {
      return value != null ? value.hashCode() : 0;
   }

   @Override
   public String toString() {
      return "WrappedMessage{value=" + value + '}';
   }

   public static final class Marshaller implements RawProtobufMarshaller<WrappedMessage> {

      @Override
      public Class<? extends WrappedMessage> getJavaClass() {
         return WrappedMessage.class;
      }

      @Override
      public String getTypeName() {
         return PROTOBUF_TYPE_NAME;
      }

      @Override
      public WrappedMessage readFrom(SerializationContext ctx, RawProtoStreamReader in) throws IOException {
         return new WrappedMessage(readMessage(ctx, in));
      }

      @Override
      public void writeTo(SerializationContext ctx, RawProtoStreamWriter out, WrappedMessage wrappedMessage) throws IOException {
         writeMessage(ctx, out, wrappedMessage.value);
      }
   }
}
