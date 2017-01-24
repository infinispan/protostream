package org.infinispan.protostream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.EnumValueDescriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.GenericDescriptor;
import org.infinispan.protostream.descriptors.Type;
import org.infinispan.protostream.impl.BaseMarshallerDelegate;
import org.infinispan.protostream.impl.ByteArrayOutputStreamEx;
import org.infinispan.protostream.impl.RawProtoStreamReaderImpl;
import org.infinispan.protostream.impl.RawProtoStreamWriterImpl;
import org.infinispan.protostream.impl.SerializationContextImpl;

/**
 * This is the entry point to the ProtoStream library. This class provides methods to write and read Java objects
 * to/from a Protobuf encoded data stream.
 *
 * @author anistor@redhat.com
 * @since 1.0
 */
public final class ProtobufUtil {

   /**
    * Classpath location of the message-wrapping.proto resource file.
    */
   private static final String WRAPPING_DEFINITIONS_RES = "/org/infinispan/protostream/message-wrapping.proto";

   private ProtobufUtil() {
   }

   public static SerializationContext newSerializationContext(Configuration configuration) {
      SerializationContextImpl serializationContext = new SerializationContextImpl(configuration);

      try {
         serializationContext.registerProtoFiles(FileDescriptorSource.fromResources(WRAPPING_DEFINITIONS_RES));
      } catch (IOException | DescriptorParserException e) {
         throw new RuntimeException("Failed to initialize serialization context", e);
      }

      serializationContext.registerMarshaller(new WrappedMessage.Marshaller());

      return serializationContext;
   }

   private static <A> void writeTo(ImmutableSerializationContext ctx, RawProtoStreamWriter out, A t) throws IOException {
      if (t == null) {
         throw new IllegalArgumentException("Object to marshall cannot be null");
      }
      BaseMarshallerDelegate marshallerDelegate = ((SerializationContextImpl) ctx).getMarshallerDelegate(t.getClass());
      marshallerDelegate.marshall(null, t, null, out);
      out.flush();
   }

   public static void writeTo(ImmutableSerializationContext ctx, OutputStream out, Object t) throws IOException {
      writeTo(ctx, RawProtoStreamWriterImpl.newInstance(out), t);
   }

   public static byte[] toByteArray(ImmutableSerializationContext ctx, Object t) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      writeTo(ctx, baos, t);
      return baos.toByteArray();
   }

   public static ByteBuffer toByteBuffer(ImmutableSerializationContext ctx, Object t) throws IOException {
      ByteArrayOutputStreamEx baos = new ByteArrayOutputStreamEx();
      writeTo(ctx, baos, t);
      return baos.getByteBuffer();
   }

   private static <A> A readFrom(ImmutableSerializationContext ctx, RawProtoStreamReader in, Class<A> clazz) throws IOException {
      BaseMarshallerDelegate<A> marshallerDelegate = ((SerializationContextImpl) ctx).getMarshallerDelegate(clazz);
      return marshallerDelegate.unmarshall(null, null, in);
   }

   public static <A> A readFrom(ImmutableSerializationContext ctx, InputStream in, Class<A> clazz) throws IOException {
      return readFrom(ctx, RawProtoStreamReaderImpl.newInstance(in), clazz);
   }

   public static <A> A fromByteArray(ImmutableSerializationContext ctx, byte[] bytes, Class<A> clazz) throws IOException {
      return readFrom(ctx, RawProtoStreamReaderImpl.newInstance(bytes), clazz);
   }

   //todo [anistor] what happens with remaining trailing bytes? signal error?
   public static <A> A fromByteArray(ImmutableSerializationContext ctx, byte[] bytes, int offset, int length, Class<A> clazz) throws IOException {
      return readFrom(ctx, RawProtoStreamReaderImpl.newInstance(bytes, offset, length), clazz);
   }

   public static <A> A fromByteBuffer(ImmutableSerializationContext ctx, ByteBuffer byteBuffer, Class<A> clazz) throws IOException {
      return readFrom(ctx, RawProtoStreamReaderImpl.newInstance(byteBuffer), clazz);
   }

   /**
    * Parses a top-level message that was wrapped according to the org.infinispan.protostream.WrappedMessage proto
    * definition.
    *
    * @param ctx
    * @param bytes
    * @return
    * @throws IOException
    */
   public static <A> A fromWrappedByteArray(ImmutableSerializationContext ctx, byte[] bytes) throws IOException {
      return fromWrappedByteArray(ctx, bytes, 0, bytes.length);
   }

   public static <A> A fromWrappedByteArray(ImmutableSerializationContext ctx, byte[] bytes, int offset, int length) throws IOException {
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes, offset, length);
      return WrappedMessage.readMessage(ctx, RawProtoStreamReaderImpl.newInstance(bais));
   }

   public static <A> A fromWrappedByteBuffer(ImmutableSerializationContext ctx, ByteBuffer byteBuffer) throws IOException {
      return WrappedMessage.readMessage(ctx, RawProtoStreamReaderImpl.newInstance(byteBuffer));
   }

   public static byte[] toWrappedByteArray(ImmutableSerializationContext ctx, Object t) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      WrappedMessage.writeMessage(ctx, RawProtoStreamWriterImpl.newInstance(baos), t);
      return baos.toByteArray();
   }

   public static ByteBuffer toWrappedByteBuffer(ImmutableSerializationContext ctx, Object t) throws IOException {
      ByteArrayOutputStreamEx baos = new ByteArrayOutputStreamEx();
      WrappedMessage.writeMessage(ctx, RawProtoStreamWriterImpl.newInstance(baos), t);
      return baos.getByteBuffer();
   }

   private static final class JsonNestingLevel {

      boolean isFirstField = true;

      FieldDescriptor repeatedFieldDescriptor;

      int indent;

      JsonNestingLevel previous;

      JsonNestingLevel(JsonNestingLevel previous) {
         this.previous = previous;
         this.indent = previous != null ? previous.indent + 1 : 0;
      }
   }

   /**
    * See https://developers.google.com/protocol-buffers/docs/proto3#json
    *
    * @param ctx
    * @param bytes
    * @return
    * @throws IOException
    */
   public static String toCanonicalJSON(ImmutableSerializationContext ctx, byte[] bytes) throws IOException {
      return toCanonicalJSON(ctx, bytes, true);
   }

   /**
    * See https://developers.google.com/protocol-buffers/docs/proto3#json
    *
    * @param ctx
    * @param bytes
    * @param prettyPrint
    * @return
    * @throws IOException
    */
   public static String toCanonicalJSON(ImmutableSerializationContext ctx, byte[] bytes, boolean prettyPrint) throws IOException {
      StringBuilder jsonOut = new StringBuilder();
      toCanonicalJSON(ctx, bytes, jsonOut, prettyPrint ? 0 : -1);
      return jsonOut.toString();
   }

   private static void toCanonicalJSON(ImmutableSerializationContext ctx, byte[] bytes, StringBuilder jsonOut, int initNestingLevel) throws IOException {
      if (bytes.length == 0) {
         // only null values get to be encoded to an empty byte array
         jsonOut.append("null");
         return;
      }

      Descriptor wrapperDescriptor = ctx.getMessageDescriptor(WrappedMessage.PROTOBUF_TYPE_NAME);

      boolean prettyPrint = initNestingLevel >= 0;

      TagHandler messageHandler = new TagHandler() {

         private JsonNestingLevel nestingLevel;

         private void indent() {
            jsonOut.append('\n');
            for (int k = initNestingLevel + nestingLevel.indent; k > 0; k--) {
               jsonOut.append("   ");
            }
         }

         @Override
         public void onStart() {
            nestingLevel = new JsonNestingLevel(null);
            if (prettyPrint) {
               indent();
               nestingLevel.indent++;
            }
            jsonOut.append('{');
         }

         @Override
         public void onTag(int fieldNumber, FieldDescriptor fieldDescriptor, Object tagValue) {
            if (fieldDescriptor == null) {
               // unknown field, ignore
               return;
            }
            startSlot(fieldDescriptor);

            switch (fieldDescriptor.getType()) {
               case STRING:
               case INT64:
               case SINT64:
               case UINT64:
               case FIXED64:
                  jsonOut.append('\"').append(tagValue).append('\"');
                  break;
               case FLOAT:
                  Float f = (Float) tagValue;
                  if (f.isInfinite() || f.isNaN()) {
                     jsonOut.append('\"').append(f).append('\"');
                  } else {
                     jsonOut.append(f);
                  }
                  break;
               case DOUBLE:
                  Double d = (Double) tagValue;
                  if (d.isInfinite() || d.isNaN()) {
                     jsonOut.append('\"').append(d).append('\"');
                  } else {
                     jsonOut.append(d);
                  }
                  break;
               case ENUM:
                  EnumValueDescriptor enumValue = fieldDescriptor.getEnumType().findValueByNumber((Integer) tagValue);
                  jsonOut.append('\"').append(enumValue.getName()).append('\"');
                  break;
               case BYTES:
                  String base64encoded = Base64.getEncoder().encodeToString((byte[]) tagValue);
                  jsonOut.append('\"').append(base64encoded).append('\"');
                  break;
               default:
                  if (tagValue instanceof Date) {
                     jsonOut.append('\"').append(formatDate((Date) tagValue)).append('\"');
                  } else {
                     jsonOut.append(tagValue);
                  }
            }
         }

         @Override
         public void onStartNested(int fieldNumber, FieldDescriptor fieldDescriptor) {
            if (fieldDescriptor == null) {
               // unknown field, ignore
               return;
            }
            startSlot(fieldDescriptor);

            nestingLevel = new JsonNestingLevel(nestingLevel);

            if (prettyPrint) {
               indent();
               nestingLevel.indent++;
            }

            jsonOut.append('{');
         }

         @Override
         public void onEndNested(int fieldNumber, FieldDescriptor fieldDescriptor) {
            if (prettyPrint) {
               nestingLevel.indent--;
               indent();
            }
            jsonOut.append('}');
            nestingLevel = nestingLevel.previous;
         }

         @Override
         public void onEnd() {
            if (nestingLevel.repeatedFieldDescriptor != null) {
               endArraySlot();
            }
            if (prettyPrint) {
               nestingLevel.indent--;
               indent();
            }
            jsonOut.append('}');
            nestingLevel = null;
            if (prettyPrint) {
               jsonOut.append('\n');
            }
         }

         private void startSlot(FieldDescriptor fieldDescriptor) {
            if (nestingLevel.repeatedFieldDescriptor != null && nestingLevel.repeatedFieldDescriptor != fieldDescriptor) {
               endArraySlot();
            }
            if (nestingLevel.isFirstField) {
               nestingLevel.isFirstField = false;
            } else {
               jsonOut.append(',');
            }
            if (!fieldDescriptor.isRepeated() || nestingLevel.repeatedFieldDescriptor == null) {
               if (prettyPrint) {
                  indent();
               }
               jsonOut.append('"').append(fieldDescriptor.getName()).append("\":");
            }
            if (prettyPrint) {
               jsonOut.append(' ');
            }
            if (fieldDescriptor.isRepeated() && nestingLevel.repeatedFieldDescriptor == null) {
               nestingLevel.repeatedFieldDescriptor = fieldDescriptor;
               jsonOut.append('[');
            }
         }

         private void endArraySlot() {
            if (prettyPrint && nestingLevel.repeatedFieldDescriptor.getType() == Type.MESSAGE) {
               indent();
            }
            nestingLevel.repeatedFieldDescriptor = null;
            jsonOut.append(']');
         }
      };

      TagHandler wrapperHandler = new TagHandler() {

         private Integer typeId;
         private String typeName;
         private byte[] wrappedMessage;
         private Integer wrappedEnum;

         private GenericDescriptor getDescriptor() {
            return typeId != null ? ctx.getDescriptorByTypeId(typeId) : ctx.getDescriptorByName(typeName);
         }

         @Override
         public void onTag(int fieldNumber, FieldDescriptor fieldDescriptor, Object tagValue) {
            if (fieldDescriptor == null) {
               // ignore unknown fields
               return;
            }
            switch (fieldNumber) {
               case WrappedMessage.WRAPPED_DESCRIPTOR_ID:
                  typeId = (Integer) tagValue;
                  break;
               case WrappedMessage.WRAPPED_DESCRIPTOR_FULL_NAME:
                  typeName = (String) tagValue;
                  break;
               case WrappedMessage.WRAPPED_MESSAGE:
                  wrappedMessage = (byte[]) tagValue;
                  break;
               case WrappedMessage.WRAPPED_ENUM:
                  wrappedEnum = (Integer) tagValue;
                  break;
               case WrappedMessage.WRAPPED_DOUBLE:
               case WrappedMessage.WRAPPED_FLOAT:
               case WrappedMessage.WRAPPED_INT64:
               case WrappedMessage.WRAPPED_UINT64:
               case WrappedMessage.WRAPPED_INT32:
               case WrappedMessage.WRAPPED_FIXED64:
               case WrappedMessage.WRAPPED_FIXED32:
               case WrappedMessage.WRAPPED_BOOL:
               case WrappedMessage.WRAPPED_STRING:
               case WrappedMessage.WRAPPED_BYTES:
               case WrappedMessage.WRAPPED_UINT32:
               case WrappedMessage.WRAPPED_SFIXED32:
               case WrappedMessage.WRAPPED_SFIXED64:
               case WrappedMessage.WRAPPED_SINT32:
               case WrappedMessage.WRAPPED_SINT64:
                  messageHandler.onStart();
                  messageHandler.onTag(fieldNumber, fieldDescriptor, tagValue);
                  messageHandler.onEnd();
                  break;
            }
         }

         @Override
         public void onEnd() {
            if (wrappedEnum != null) {
               EnumDescriptor enumDescriptor = (EnumDescriptor) getDescriptor();
               String enumConstantName = enumDescriptor.findValueByNumber(wrappedEnum).getName();
               FieldDescriptor fd = wrapperDescriptor.findFieldByNumber(WrappedMessage.WRAPPED_ENUM);
               messageHandler.onStart();
               messageHandler.onTag(WrappedMessage.WRAPPED_ENUM, fd, enumConstantName);
               messageHandler.onEnd();
            } else if (wrappedMessage != null) {
               try {
                  Descriptor messageDescriptor = (Descriptor) getDescriptor();
                  ProtobufParser.INSTANCE.parse(messageHandler, messageDescriptor, wrappedMessage);
               } catch (IOException e) {
                  throw new RuntimeException(e);
               }
            }
         }
      };

      ProtobufParser.INSTANCE.parse(wrapperHandler, wrapperDescriptor, bytes);
   }

   private static String formatDate(Date tagValue) {
      return timestampFormat.get().format(tagValue);
   }

   private static final ThreadLocal<DateFormat> timestampFormat = ThreadLocal.withInitial(() -> {
      // Z-normalized RFC 3339 format
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
      GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
      calendar.setGregorianChange(new Date(Long.MIN_VALUE));
      sdf.setCalendar(calendar);
      return sdf;
   });
}
