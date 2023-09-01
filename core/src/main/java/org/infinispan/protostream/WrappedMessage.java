package org.infinispan.protostream;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Iterator;

import org.infinispan.protostream.containers.ElementContainerAdapter;
import org.infinispan.protostream.containers.IndexedElementContainerAdapter;
import org.infinispan.protostream.containers.IterableElementContainerAdapter;
import org.infinispan.protostream.descriptors.WireType;
import org.infinispan.protostream.impl.BaseMarshallerDelegate;
import org.infinispan.protostream.impl.ByteArrayOutputStreamEx;
import org.infinispan.protostream.impl.EnumMarshallerDelegate;
import org.infinispan.protostream.impl.SerializationContextImpl;
import org.infinispan.protostream.impl.TagReaderImpl;
import org.infinispan.protostream.impl.TagWriterImpl;

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
    * This type is defined in message-wrapping.proto.
    */
   public static final String PROTOBUF_TYPE_NAME = "org.infinispan.protostream.WrappedMessage";

   /**
    * The type id of WrappedMessage itself.
    */
   public static final int PROTOBUF_TYPE_ID = 0;

   /**
    * Full path of the message-wrapping.proto resource file in classpath.
    */
   public static final String PROTO_FILE = "org/infinispan/protostream/message-wrapping.proto";

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
    * A wrapped char (marshalled as int32).
    */
   public static final int WRAPPED_CHAR = 20;

   /**
    * A wrapped short (marshalled as int32).
    */
   public static final int WRAPPED_SHORT = 21;

   /**
    * A wrapped byte (marshalled as int32).
    */
   public static final int WRAPPED_BYTE = 22;

   /**
    * A wrapped java.util.Date (marshalled as int64).
    */
   public static final int WRAPPED_DATE_MILLIS = 23;

   /**
    * A wrapped java.time.Instant (marshalled as int64 (seconds) and an int32 (nanos)).
    */
   public static final int WRAPPED_INSTANT_SECONDS = 24;

   /**
    * The nanoseconds of the java.time.Instant.
    */
   public static final int WRAPPED_INSTANT_NANOS = 25;

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
    * The name of the fully qualified message or enum type name, when the wrapped object is a message or enum.
    */
   public static final int WRAPPED_TYPE_NAME = 16;

   /**
    * @deprecated Use {@link #WRAPPED_TYPE_NAME} instead. This will be removed in ver. 5.
    */
   @Deprecated
   public static final int WRAPPED_DESCRIPTOR_FULL_NAME = WRAPPED_TYPE_NAME;

   /**
    * A byte array containing the encoded message.
    */
   public static final int WRAPPED_MESSAGE = 17;

   /**
    * The enum value.
    */
   public static final int WRAPPED_ENUM = 18;

   /**
    * The (optional) numeric type id of the wrapped message or enum. This is an alternative to {@link
    * #WRAPPED_TYPE_NAME}.
    */
   public static final int WRAPPED_TYPE_ID = 19;

   /**
    * @deprecated Use {@link #WRAPPED_TYPE_ID} instead. This will be removed in ver. 5.
    */
   @Deprecated
   public static final int WRAPPED_DESCRIPTOR_TYPE_ID = WRAPPED_TYPE_ID;

   /**
    * @deprecated Use {@link #WRAPPED_TYPE_ID} instead. This will be removed in ver. 5.
    */
   @Deprecated
   public static final int WRAPPED_DESCRIPTOR_ID = WRAPPED_TYPE_ID;

   /**
    * A flag indicating and empty/null message.
    */
   public static final int WRAPPED_EMPTY = 26;

   /**
    * The (optional) number of repeated elements.
    */
   public static final int WRAPPED_CONTAINER_SIZE = 27;

   public static final int WRAPPED_CONTAINER_TYPE_NAME = 28;

   public static final int WRAPPED_CONTAINER_TYPE_ID = 29;

   public static final int WRAPPED_CONTAINER_MESSAGE = 30;

   public static final String CONTAINER_SIZE_CONTEXT_PARAM = "containerSize";

   /**
    * The wrapped object or (boxed) primitive. Can also be an array or Collection.
    */
   private final Object value;

   public WrappedMessage(Object value) {
      this.value = value;
   }

   /**
    * Returns the wrapped value, which is either a primitive, an enum, or a message. The value can be {@code null} also.
    */
   public Object getValue() {
      return value;
   }

   static void write(ImmutableSerializationContext ctx, TagWriter out, Object t) throws IOException {
      writeMessage(ctx, out, t, false);
   }

   private static void writeMessage(ImmutableSerializationContext ctx, TagWriter out, Object t, boolean nulls) throws IOException {
      if (t == null) {
         if (nulls) {
            out.writeBool(WRAPPED_EMPTY, true);
         }
         return;
      }

      if (t instanceof String) {
         out.writeString(WRAPPED_STRING, (String) t);
      } else if (t instanceof Character) {
         out.writeInt32(WRAPPED_CHAR, ((Character) t).charValue());
      } else if (t instanceof Byte) {
         out.writeInt32(WRAPPED_BYTE, ((Byte) t).byteValue());
      } else if (t instanceof Short) {
         out.writeInt32(WRAPPED_SHORT, ((Short) t).shortValue());
      } else if (t instanceof Date) {
         out.writeInt64(WRAPPED_DATE_MILLIS, ((Date) t).getTime());
      } else if (t instanceof Instant) {
         Instant instant = (Instant) t;
         out.writeInt64(WRAPPED_INSTANT_SECONDS, instant.getEpochSecond());
         out.writeInt32(WRAPPED_INSTANT_NANOS, instant.getNano());
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
      } else {
         // This is either a message type or an enum. Try to lookup a marshaller.
         BaseMarshallerDelegate marshallerDelegate = ((SerializationContextImpl) ctx).getMarshallerDelegate(t);
         BaseMarshaller marshaller = marshallerDelegate.getMarshaller();

         if (marshaller instanceof ElementContainerAdapter) {
            writeContainer(ctx, out, marshallerDelegate, t);
         } else {
            // Write the type discriminator, either the fully qualified name or a numeric type id.
            String typeName = marshaller.getTypeName();
            int typeId = mapTypeIdOut(typeName, ctx);
            if (typeId < 0) {
               out.writeString(WRAPPED_TYPE_NAME, typeName);
            } else {
               out.writeUInt32(WRAPPED_TYPE_ID, typeId);
            }

            if (t.getClass().isEnum()) {
               ((EnumMarshallerDelegate) marshallerDelegate).encode(WRAPPED_ENUM, (Enum) t, out);
            } else {
               ByteArrayOutputStreamEx buffer = new ByteArrayOutputStreamEx();
               TagWriterImpl nestedCtx = TagWriterImpl.newInstanceNoBuffer(ctx, buffer);
               marshallerDelegate.marshall(nestedCtx, null, t);
               nestedCtx.flush();
               out.writeBytes(WRAPPED_MESSAGE, buffer.getByteBuffer());
            }
         }
      }
      out.flush();
   }

   private static void writeContainer(ImmutableSerializationContext ctx, TagWriter out, BaseMarshallerDelegate marshallerDelegate, Object container) throws IOException {
      BaseMarshaller containerMarshaller = marshallerDelegate.getMarshaller();
      String typeName = containerMarshaller.getTypeName();
      int typeId = mapTypeIdOut(typeName, ctx);

      if (typeId < 0) {
         out.writeString(WRAPPED_CONTAINER_TYPE_NAME, typeName);
      } else {
         out.writeUInt32(WRAPPED_CONTAINER_TYPE_ID, typeId);
      }

      int containerSize = ((ElementContainerAdapter) containerMarshaller).getNumElements(container);
      out.writeUInt32(WRAPPED_CONTAINER_SIZE, containerSize);

      ByteArrayOutputStreamEx buffer = new ByteArrayOutputStreamEx();
      TagWriterImpl nestedCtx = TagWriterImpl.newInstanceNoBuffer(ctx, buffer);
      marshallerDelegate.marshall(nestedCtx, null, container);
      nestedCtx.flush();
      out.writeBytes(WRAPPED_CONTAINER_MESSAGE, buffer.getByteBuffer());

      if (containerMarshaller instanceof IterableElementContainerAdapter) {
         Iterator elements = ((IterableElementContainerAdapter) containerMarshaller).getElements(container);
         for (int i = 0; i < containerSize; i++) {
            Object e = elements.next();
            writeMessage(ctx, out, e, true);
         }
         if (elements.hasNext()) {
            throw new IllegalStateException("Container number of elements mismatch");
         }
      } else if (containerMarshaller instanceof IndexedElementContainerAdapter) {
         IndexedElementContainerAdapter adapter = (IndexedElementContainerAdapter) containerMarshaller;
         for (int i = 0; i < containerSize; i++) {
            Object e = adapter.getElement(container, i);
            writeMessage(ctx, out, e, true);
         }
      } else {
         throw new IllegalStateException("Unknown container adapter kind : " + containerMarshaller.getJavaClass().getName());
      }
   }

   static <T> T read(ImmutableSerializationContext ctx, TagReader in) throws IOException {
      return readMessage(ctx, in, false);
   }

   private static <T> T readMessage(ImmutableSerializationContext ctx, TagReader in, boolean nulls) throws IOException {
      String typeName = null;
      Integer typeId = null;
      int enumValue = -1;
      byte[] messageBytes = null;
      Object value = null;
      int fieldCount = 0;
      int expectedFieldCount = 1;

      int tag;
      out:
      while ((tag = in.readTag()) != 0) {
         fieldCount++;
         switch (tag) {
            case WRAPPED_CONTAINER_SIZE << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_VARINT:
            case WRAPPED_CONTAINER_TYPE_ID << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_VARINT:
            case WRAPPED_CONTAINER_TYPE_NAME << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_LENGTH_DELIMITED:
            case WRAPPED_CONTAINER_MESSAGE << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_LENGTH_DELIMITED: {
               expectedFieldCount = 1;
               value = readContainer(ctx, in, tag);
               break out;
            }
            case WRAPPED_EMPTY << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_VARINT: {
               if (!nulls) {
                  throw new IllegalStateException("Encountered a null message but nulls are not accepted");
               }
               expectedFieldCount = 1;
               in.readBool(); // We ignore the actual boolean value! Will be returning null anyway.
               break out;
            }
            case WRAPPED_TYPE_NAME << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_LENGTH_DELIMITED: {
               expectedFieldCount = 2;
               typeName = in.readString();
               break;
            }
            case WRAPPED_TYPE_ID << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_VARINT: {
               expectedFieldCount = 2;
               typeId = mapTypeIdIn(in.readInt32(), ctx);
               break;
            }
            case WRAPPED_ENUM << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_VARINT: {
               expectedFieldCount = 2;
               enumValue = in.readEnum();
               break;
            }
            case WRAPPED_MESSAGE << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_LENGTH_DELIMITED: {
               expectedFieldCount = 2;
               messageBytes = in.readByteArray();
               break;
            }
            case WRAPPED_STRING << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_LENGTH_DELIMITED: {
               expectedFieldCount = 1;
               value = in.readString();
               break out;
            }
            case WRAPPED_CHAR << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_VARINT: {
               expectedFieldCount = 1;
               value = (char) in.readInt32();
               break out;
            }
            case WRAPPED_SHORT << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_VARINT: {
               expectedFieldCount = 1;
               value = (short) in.readInt32();
               break out;
            }
            case WRAPPED_BYTE << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_VARINT: {
               expectedFieldCount = 1;
               value = (byte) in.readInt32();
               break out;
            }
            case WRAPPED_DATE_MILLIS << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_VARINT: {
               expectedFieldCount = 1;
               value = new Date(in.readInt64());
               break out;
            }
            case WRAPPED_INSTANT_SECONDS << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_VARINT: {
               expectedFieldCount = 2;
               long seconds = in.readInt64();
               value = value == null ? Instant.ofEpochSecond(seconds, 0) : Instant.ofEpochSecond(seconds, ((Instant) value).getNano());
               break;
            }
            case WRAPPED_INSTANT_NANOS << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_VARINT: {
               expectedFieldCount = 2;
               int nanos = in.readInt32();
               value = value == null ? Instant.ofEpochSecond(0, nanos) : Instant.ofEpochSecond(((Instant) value).getEpochSecond(), nanos);
               break;
            }
            case WRAPPED_BYTES << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_LENGTH_DELIMITED: {
               expectedFieldCount = 1;
               value = in.readByteArray();
               break out;
            }
            case WRAPPED_BOOL << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_VARINT: {
               expectedFieldCount = 1;
               value = in.readBool();
               break out;
            }
            case WRAPPED_DOUBLE << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_FIXED64: {
               expectedFieldCount = 1;
               value = in.readDouble();
               break out;
            }
            case WRAPPED_FLOAT << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_FIXED32: {
               expectedFieldCount = 1;
               value = in.readFloat();
               break out;
            }
            case WRAPPED_FIXED32 << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_FIXED32: {
               expectedFieldCount = 1;
               value = in.readFixed32();
               break out;
            }
            case WRAPPED_SFIXED32 << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_FIXED32: {
               expectedFieldCount = 1;
               value = in.readSFixed32();
               break out;
            }
            case WRAPPED_FIXED64 << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_FIXED64: {
               expectedFieldCount = 1;
               value = in.readFixed64();
               break out;
            }
            case WRAPPED_SFIXED64 << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_FIXED64: {
               expectedFieldCount = 1;
               value = in.readSFixed64();
               break out;
            }
            case WRAPPED_INT64 << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_VARINT: {
               expectedFieldCount = 1;
               value = in.readInt64();
               break out;
            }
            case WRAPPED_UINT64 << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_VARINT: {
               expectedFieldCount = 1;
               value = in.readUInt64();
               break out;
            }
            case WRAPPED_SINT64 << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_VARINT: {
               expectedFieldCount = 1;
               value = in.readSInt64();
               break out;
            }
            case WRAPPED_INT32 << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_VARINT: {
               expectedFieldCount = 1;
               value = in.readInt32();
               break out;
            }
            case WRAPPED_UINT32 << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_VARINT: {
               expectedFieldCount = 1;
               value = in.readUInt32();
               break out;
            }
            case WRAPPED_SINT32 << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_VARINT: {
               expectedFieldCount = 1;
               value = in.readSInt32();
               break out;
            }
            default:
               throw new IllegalStateException("Unexpected tag : " + tag + " (Field number : "
                     + WireType.getTagFieldNumber(tag) + ", Wire type : " + WireType.getTagWireType(tag) + ")");
         }
      }

      if (value == null && typeName == null && typeId == null && messageBytes == null) {
         return null;
      }

      if (value != null) {
         if (fieldCount != expectedFieldCount) {
            throw new IOException("Invalid WrappedMessage encoding.");
         }
         return (T) value;
      }

      if (typeName == null && typeId == null || typeName != null && typeId != null || fieldCount != 2) {
         throw new IOException("Invalid WrappedMessage encoding.");
      }

      if (typeId != null) {
         typeName = ctx.getDescriptorByTypeId(typeId).getFullName();
      }
      BaseMarshallerDelegate marshallerDelegate = ((SerializationContextImpl) ctx).getMarshallerDelegate(typeName);
      if (messageBytes != null) {
         // it's a Message type
         TagReaderImpl nestedInput = TagReaderImpl.newInstance(ctx, messageBytes);
         return (T) marshallerDelegate.unmarshall(nestedInput, null);
      } else {
         // it's an Enum
         EnumMarshaller marshaller = (EnumMarshaller) marshallerDelegate.getMarshaller();
         T e = (T) marshaller.decode(enumValue);
         if (e == null) {
            // Unknown enum value cause by schema evolution. We cannot handle data loss here so we throw!
            throw new IOException("Unknown enum value " + enumValue + " for Protobuf enum type " + typeName);
         }
         return e;
      }
   }

   private static Object readContainer(ImmutableSerializationContext ctx, TagReader in, int tag) throws IOException {
      int containerSize = -1;
      String containerTypeName = null;
      Integer containerTypeId = null;
      byte[] containerMessage = null;

      int fieldCount = 0;
      while (tag != 0) {
         switch (tag) {
            case WRAPPED_CONTAINER_SIZE << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_VARINT:
               containerSize = in.readInt32();
               break;
            case WRAPPED_CONTAINER_TYPE_NAME << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_LENGTH_DELIMITED: {
               containerTypeName = in.readString();
               break;
            }
            case WRAPPED_CONTAINER_TYPE_ID << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_VARINT: {
               containerTypeId = mapTypeIdIn(in.readInt32(), ctx);
               break;
            }
            case WRAPPED_CONTAINER_MESSAGE << WireType.TAG_TYPE_NUM_BITS | WireType.WIRETYPE_LENGTH_DELIMITED:
               containerMessage = in.readByteArray();
               break;
            default:
               throw new IllegalStateException("Unexpected tag : " + tag + " (Field number : "
                     + WireType.getTagFieldNumber(tag) + ", Wire type : " + WireType.getTagWireType(tag) + ")");
         }

         if (++fieldCount == 3) {
            break;
         }

         tag = in.readTag();
      }

      if (fieldCount != 3 || containerSize < 0 || containerMessage == null
            || containerTypeId == null && containerTypeName == null
            || containerTypeId != null && containerTypeName != null) {
         throw new IOException("Invalid WrappedMessage encoding.");
      }

      if (containerTypeId != null) {
         containerTypeName = ctx.getDescriptorByTypeId(containerTypeId).getFullName();
      }
      BaseMarshallerDelegate<?> marshallerDelegate = ((SerializationContextImpl) ctx).getMarshallerDelegate(containerTypeName);

      BaseMarshaller<?> containerMarshaller = marshallerDelegate.getMarshaller();
      if (!(containerMarshaller instanceof ElementContainerAdapter)) {
         throw new IllegalStateException("The unmarshaller is not a container adapter : " + containerMarshaller.getJavaClass().getName());
      }
      TagReaderImpl nestedInput = TagReaderImpl.newNestedInstance((ProtobufTagMarshaller.ReadContext) in, containerMessage);

      // pass the size to the marshaller of the container object
      nestedInput.setParam(CONTAINER_SIZE_CONTEXT_PARAM, containerSize);
      Object container = marshallerDelegate.unmarshall(nestedInput, null);
      if (container == null) {
         throw new IllegalStateException("The unmarshalled container must not be null");
      }
      containerMessage = null;
      nestedInput = null;

      if (containerMarshaller instanceof IterableElementContainerAdapter) {
         IterableElementContainerAdapter adapter = (IterableElementContainerAdapter) containerMarshaller;
         for (int i = 0; i < containerSize; i++) {
            Object e = readMessage(ctx, in, true);
            adapter.appendElement(container, e);
         }
      } else if (containerMarshaller instanceof IndexedElementContainerAdapter) {
         IndexedElementContainerAdapter adapter = (IndexedElementContainerAdapter) containerMarshaller;
         for (int i = 0; i < containerSize; i++) {
            Object e = readMessage(ctx, in, true);
            adapter.setElement(container, i, e);
         }
      } else {
         throw new IllegalStateException("Unknown container adapter kind : " + containerMarshaller.getJavaClass().getName());
      }

      return container;
   }

   /**
    * Map type id to new value during reading, to support schema evolution.
    */
   private static int mapTypeIdIn(int typeId, ImmutableSerializationContext ctx) {
      WrappedMessageTypeIdMapper mapper = ctx.getConfiguration().wrappingConfig().wrappedMessageTypeIdMapper();
      return mapper == null ? typeId : mapper.mapTypeIdIn(typeId, ctx);
   }

   /**
    * Map type id to old value, during writing, to support schema evolution.
    */
   private static int mapTypeIdOut(String typeName, ImmutableSerializationContext ctx) {
      Integer typeId = ctx.getDescriptorByName(typeName).getTypeId();
      if (typeId == null) {
         return -1;
      }
      WrappedMessageTypeIdMapper mapper = ctx.getConfiguration().wrappingConfig().wrappedMessageTypeIdMapper();
      return mapper == null ? typeId : mapper.mapTypeIdOut(typeId, ctx);
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

   /**
    * Marshaller for WrappedMessage. This marshaller is not meant to handle unknown fields at the top level as they are
    * very unlikely to ever appear. The handling of unknown fields for the inner message type will work as usual.
    */
   static final BaseMarshaller<WrappedMessage> MARSHALLER = new ProtobufTagMarshaller<WrappedMessage>() {

      @Override
      public Class<WrappedMessage> getJavaClass() {
         return WrappedMessage.class;
      }

      @Override
      public String getTypeName() {
         return PROTOBUF_TYPE_NAME;
      }

      @Override
      public WrappedMessage read(ReadContext ctx) throws IOException {
         return new WrappedMessage(readMessage(ctx.getSerializationContext(), ctx.getReader(), false));
      }

      @Override
      public void write(WriteContext ctx, WrappedMessage wrappedMessage) throws IOException {
         writeMessage(ctx.getSerializationContext(), ctx.getWriter(), wrappedMessage.value, false);
      }
   };
}
