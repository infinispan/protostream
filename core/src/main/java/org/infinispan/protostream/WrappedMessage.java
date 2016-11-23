package org.infinispan.protostream;

import java.io.IOException;

import org.infinispan.protostream.impl.BaseMarshallerDelegate;
import org.infinispan.protostream.impl.ByteArrayOutputStreamEx;
import org.infinispan.protostream.impl.RawProtoStreamReaderImpl;
import org.infinispan.protostream.impl.RawProtoStreamWriterImpl;
import org.infinispan.protostream.impl.SerializationContextImpl;
import org.infinispan.protostream.impl.WireFormat;

/**
 * A wrapper for messages, enums or primitive types that encodes the type of the inner object/value and also helps keep
 * track of where the message ends. The need for this wrapper stems from two particular design choices in the Protocol
 * Buffers encoding.
 * <p>
 * 1. The Protocol Buffers encoding format does not contain any description of the message type that follows next in the
 * data stream, unlike for example the Java serialization format which provides information about classes that are saved
 * in a Serialization stream in the form of class descriptors which contain the fully qualified name of the class being
 * serialized. The Protocol Buffers client is expected to know what message type he is expecting to read from the
 * stream. This knowledge exists in most cases, statically, so this encoding scheme saves a lot of space by not
 * including redundant type descriptors in the stream by default. For all other use cases where data types are dynamic
 * you are on your own, but {@link WrappedMessage} is here to help you.
 * <p>
 * 2. The Protocol Buffer wire format is also not self-delimiting, so when reading a message we see just a stream of
 * fields and we are not able to determine when the fields of the current message end and the next message starts. The
 * protocol assumes that the whole contents of the stream is to be interpreted as a single message. If that's not the
 * case, then the user must provide his own way of delimiting messages either by using message start/stop markers or by
 * prefixing each message with its size or any other equivalent mechanism. {@link WrappedMessage} relies on an {@code
 * int32} size prefix.
 * <p>
 * So wherever you cannot statically decide what message type you'll be using and need to defer this until runtime, just
 * use {@link WrappedMessage}.
 *
 * @author anistor@redhat.com
 * @since 1.0
 */
public final class WrappedMessage {

   /**
    * The fully qualified Protobuf type name of this message.
    */
   public static final String PROTOBUF_TYPE_NAME = "org.infinispan.protostream.WrappedMessage";

   /**
    * A wrapped double.
    */
   public static final int WRAPPED_DOUBLE = 1;

   /**
    * A wrapped float.
    */
   public static final int WRAPPED_FLOAT = 2;

   /**
    * A wrapped int64.
    */
   public static final int WRAPPED_INT64 = 3;

   /**
    * A wrapped uint64.
    */
   public static final int WRAPPED_UINT64 = 4;

   /**
    * A wrapped int32.
    */
   public static final int WRAPPED_INT32 = 5;

   /**
    * A wrapped fixed64.
    */
   public static final int WRAPPED_FIXED64 = 6;

   /**
    * A wrapped fixed32.
    */
   public static final int WRAPPED_FIXED32 = 7;

   /**
    * A wrapped bool.
    */
   public static final int WRAPPED_BOOL = 8;

   /**
    * A wrapped string.
    */
   public static final int WRAPPED_STRING = 9;

   /**
    * A wrapped bytes.
    */
   public static final int WRAPPED_BYTES = 10;

   /**
    * A wrapped uint32.
    */
   public static final int WRAPPED_UINT32 = 11;

   /**
    * A wrapped sfixed32.
    */
   public static final int WRAPPED_SFIXED32 = 12;

   /**
    * A wrapped sfixed64.
    */
   public static final int WRAPPED_SFIXED64 = 13;

   /**
    * A wrapped sint32.
    */
   public static final int WRAPPED_SINT32 = 14;

   /**
    * A wrapped sint64.
    */
   public static final int WRAPPED_SINT64 = 15;

   /**
    * The name of the fully qualified message or enum descriptor, if the wrapped object is a message or enum.
    */
   public static final int WRAPPED_DESCRIPTOR_FULL_NAME = 16;

   /**
    * A byte array containing the encoded message.
    */
   public static final int WRAPPED_MESSAGE = 17;

   /**
    * @deprecated Use {@link #WRAPPED_MESSAGE} instead. To be removed in 4.1.
    */
   @Deprecated
   public static final int WRAPPED_MESSAGE_BYTES = WRAPPED_MESSAGE;

   /**
    * The enum value.
    */
   public static final int WRAPPED_ENUM = 18;

   /**
    * The (optional) type id of the wrapped message or enum. This is an alternative to {@link
    * #WRAPPED_DESCRIPTOR_FULL_NAME}.
    */
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
         // This is either an unknown primitive type or a message type. Try to use a message marshaller.
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
         out.writeBytes(WRAPPED_MESSAGE, buffer.getByteBuffer());
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
            case WRAPPED_MESSAGE << 3 | WireFormat.WIRETYPE_LENGTH_DELIMITED:
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
