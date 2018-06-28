package org.infinispan.protostream;

import static com.google.gson.stream.JsonToken.END_DOCUMENT;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_BOOL;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_BYTES;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_DESCRIPTOR_FULL_NAME;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_DESCRIPTOR_ID;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_DOUBLE;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_ENUM;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_FIXED32;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_FIXED64;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_FLOAT;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_INT32;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_INT64;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_MESSAGE;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_SFIXED32;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_SFIXED64;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_SINT32;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_SINT64;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_STRING;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_UINT32;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_UINT64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.AnnotatedDescriptor;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.EnumValueDescriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.GenericDescriptor;
import org.infinispan.protostream.descriptors.Label;
import org.infinispan.protostream.descriptors.Type;
import org.infinispan.protostream.impl.BaseMarshallerDelegate;
import org.infinispan.protostream.impl.ByteArrayOutputStreamEx;
import org.infinispan.protostream.impl.RawProtoStreamReaderImpl;
import org.infinispan.protostream.impl.RawProtoStreamWriterImpl;
import org.infinispan.protostream.impl.SerializationContextImpl;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;

/**
 * This is the entry point to the ProtoStream library. This class provides methods to write and read Java objects
 * to/from a Protobuf encoded data stream.
 *
 * @author anistor@redhat.com
 * @since 1.0
 */
public final class ProtobufUtil {

   private static final Gson GSON = new Gson();

   /**
    * Classpath location of the message-wrapping.proto resource file.
    */
   private static final String WRAPPING_DEFINITIONS_RES = "/org/infinispan/protostream/message-wrapping.proto";

   private static final String JSON_TYPE_FIELD = "_type";
   private static final String JSON_VALUE_FIELD = "_value";

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

   public static byte[] fromCanonicalJSON(ImmutableSerializationContext ctx, Reader reader) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      RawProtoStreamWriter writer = RawProtoStreamWriterImpl.newInstance(baos);

      JsonReader jsonReader = new JsonReader(reader);
      jsonReader.setLenient(false);

      try {
         JsonToken token = jsonReader.peek();
         while (jsonReader.hasNext() && !token.equals(END_DOCUMENT)) {
            token = jsonReader.peek();
            switch (token) {
               case BEGIN_OBJECT:
                  processJsonDocument(ctx, jsonReader, writer);
                  break;
               case NULL:
                  jsonReader.nextNull();
                  break;
               case END_DOCUMENT:
                  break;
               default:
                  throw new IllegalStateException("Invalid top level object! Found token: " + token);
            }
         }
         writer.flush();
         return baos.toByteArray();
      } catch (MalformedJsonException e) {
         throw new IllegalStateException("Invalid JSON", e);
      } finally {
         baos.close();
         reader.close();
      }
   }

   private static void writeEnumField(JsonReader reader, RawProtoStreamWriter writer, FieldDescriptor fd) throws IOException {
      String value = reader.nextString();
      EnumDescriptor enumDescriptor = fd.getEnumType();
      EnumValueDescriptor valueDescriptor = enumDescriptor.findValueByName(value);
      if (valueDescriptor == null) {
         throw new IllegalStateException("Invalid enum value '" + value + "'");
      }
      int choice = valueDescriptor.getNumber();
      writer.writeEnum(fd.getNumber(), choice);
   }

   private static void writeField(JsonReader reader, RawProtoStreamWriter writer, Type fieldType, int fieldId) throws IOException {
      switch (fieldType) {
         case DOUBLE:
            writer.writeDouble(fieldId, reader.nextDouble());
            break;
         case FLOAT:
            writer.writeFloat(fieldId, Float.valueOf(reader.nextString()));
            break;
         case INT64:
            writer.writeInt64(fieldId, reader.nextLong());
            break;
         case UINT64:
            writer.writeUInt64(fieldId, reader.nextLong());
            break;
         case FIXED64:
            writer.writeFixed64(fieldId, reader.nextLong());
            break;
         case SFIXED64:
            writer.writeSFixed64(fieldId, reader.nextLong());
            break;
         case SINT64:
            writer.writeSInt64(fieldId, reader.nextLong());
            break;
         case INT32:
            writer.writeInt32(fieldId, reader.nextInt());
            break;
         case FIXED32:
            writer.writeFixed32(fieldId, reader.nextInt());
            break;
         case UINT32:
            writer.writeUInt32(fieldId, reader.nextInt());
            break;
         case SFIXED32:
            writer.writeSFixed32(fieldId, reader.nextInt());
            break;
         case SINT32:
            writer.writeSInt32(fieldId, reader.nextInt());
            break;
         case BOOL:
            writer.writeBool(fieldId, reader.nextBoolean());
            break;
         case STRING:
            writer.writeString(fieldId, reader.nextString());
            break;
         case BYTES:
            byte[] decoded = Base64.getDecoder().decode(reader.nextString());
            writer.writeBytes(fieldId, decoded);
            break;
         default:
            throw new IllegalArgumentException("The Protobuf declared field type is not compatible with the written type : " + fieldType);
      }
   }

   private static void expectField(String expected, String value) {
      if (value == null || !value.equals(expected)) {
         throw new IllegalStateException("The document should contain a top level field '" + expected + "'");
      }
   }

   private static void expectField(FieldDescriptor descriptor, String fieldName) {
      if (descriptor == null)
         throw new IllegalStateException("The field '" + fieldName + "' was not found in the protobuf schema");
   }

   private static void processJsonDocument(ImmutableSerializationContext ctx, JsonReader reader, RawProtoStreamWriter writer) throws IOException {
      reader.beginObject();

      String currentField;
      String topLevelType;

      JsonToken token = reader.peek();
      while (reader.hasNext() && !(token == END_DOCUMENT)) {
         token = reader.peek();
         switch (token) {
            case NAME:
               currentField = reader.nextName();
               expectField(JSON_TYPE_FIELD, currentField);
               break;
            case STRING:
               topLevelType = reader.nextString();
               Type fieldType = getFieldType(ctx, topLevelType);
               switch (fieldType) {
                  case ENUM:
                     processEnum(reader, writer, (EnumDescriptor) ctx.getDescriptorByName(topLevelType));
                     break;
                  case MESSAGE:
                     processObject(ctx, reader, writer, topLevelType, null, true);
                     break;
                  default:
                     processPrimitive(reader, writer, fieldType);
                     break;
               }
         }
      }
   }

   private static void processEnum(JsonReader reader, RawProtoStreamWriter writer, EnumDescriptor enumDescriptor) throws IOException {
      while (reader.hasNext()) {
         JsonToken token = reader.peek();
         switch (token) {
            case NAME:
               String fieldName = reader.nextName();
               expectField(JSON_VALUE_FIELD, fieldName);
               break;
            case STRING:
               String read = reader.nextString();
               EnumValueDescriptor valueByName = enumDescriptor.findValueByName(read);
               if (valueByName == null) {
                  throw new IllegalStateException("Invalid enum value '" + read + "'");
               }
               int choice = valueByName.getNumber();
               Integer typeId = enumDescriptor.getTypeId();
               writer.writeInt32(WRAPPED_DESCRIPTOR_ID, typeId);
               writer.writeEnum(WRAPPED_ENUM, choice);
               break;
            case NULL:
               reader.nextNull();
               throw new IllegalStateException("Invalid enum value 'null'");
            case BOOLEAN:
               boolean bool = reader.nextBoolean();
               throw new IllegalStateException("Invalid enum value '" + bool + "'");
            case NUMBER:
               long number = reader.nextLong();
               throw new IllegalStateException("Invalid enum value '" + number + "'");
            default:
               throw new IllegalStateException("Unexpected token :" + token);
         }
      }
   }

   private static void processObject(ImmutableSerializationContext ctx, JsonReader reader, RawProtoStreamWriter writer, String type, Integer typeId, boolean topLevel) throws IOException {
      GenericDescriptor descriptorByName = ctx.getDescriptorByName(type);

      Descriptor messageDescriptor = (Descriptor) descriptorByName;

      Set<String> requiredFields = messageDescriptor.getFields().stream()
            .filter(FieldDescriptor::isRequired).map(FieldDescriptor::getName).collect(Collectors.toSet());

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      RawProtoStreamWriter objectWriter = RawProtoStreamWriterImpl.newInstance(baos);

      String currentField = null;
      while (reader.hasNext()) {
         JsonToken token = reader.peek();
         switch (token) {
            case BEGIN_ARRAY:
               processArray(ctx, type, currentField, reader, objectWriter);
               break;
            case BEGIN_OBJECT:
               reader.beginObject();
               FieldDescriptor descriptor = ((Descriptor) descriptorByName).findFieldByName(currentField);
               Descriptor messageType = descriptor.getMessageType();
               if (messageType == null) {
                  throw new IllegalStateException("Field '" + currentField + "' is not an object");
               }
               processObject(ctx, reader, objectWriter, messageType.getFullName(), descriptor.getNumber(), false);
               break;
            case NAME:
               currentField = reader.nextName();
               break;
            case STRING:
            case NUMBER:
            case BOOLEAN:
               FieldDescriptor fieldByName = ((Descriptor) descriptorByName).findFieldByName(currentField);
               expectField(fieldByName, currentField);
               if (fieldByName.getType() == Type.ENUM) {
                  writeEnumField(reader, objectWriter, fieldByName);
               } else {
                  writeField(reader, objectWriter, fieldByName.getType(), fieldByName.getNumber());
               }
               requiredFields.remove(fieldByName.getName());
               break;
            case NULL:
               reader.nextNull();
               break;
         }
      }

      if (!requiredFields.isEmpty()) {
         String missing = requiredFields.iterator().next();
         throw new IllegalStateException("Required field '" + missing + "' missing");
      }

      if (topLevel) {
         Integer tlt = descriptorByName.getTypeId();
         if (tlt == null) {
            writer.writeString(WRAPPED_DESCRIPTOR_FULL_NAME, type);
         } else {
            writer.writeInt32(WRAPPED_DESCRIPTOR_ID, tlt);
         }
         objectWriter.flush();
         writer.writeBytes(WRAPPED_MESSAGE, baos.toByteArray());
      } else {
         objectWriter.flush();
         writer.writeBytes(typeId, baos.toByteArray());
      }

      writer.flush();
      reader.endObject();

   }

   private static void processPrimitive(JsonReader reader, RawProtoStreamWriter writer, Type fieldType) throws IOException {
      while (reader.hasNext()) {
         JsonToken token = reader.peek();
         switch (token) {
            case NAME:
               String fieldName = reader.nextName();
               expectField(JSON_VALUE_FIELD, fieldName);
               break;
            case STRING:
            case NUMBER:
            case BOOLEAN:
               writeField(reader, writer, fieldType, getPrimitiveFieldId(fieldType));
               break;
            case NULL:
               reader.nextNull();
               break;
            default:
               throw new IllegalStateException("Unexpected token :" + token);
         }
      }
   }

   private static int getPrimitiveFieldId(Type primitiveType) {
      switch (primitiveType) {
         case DOUBLE:
            return WRAPPED_DOUBLE;
         case FLOAT:
            return WRAPPED_FLOAT;
         case INT32:
            return WRAPPED_INT32;
         case INT64:
            return WRAPPED_INT64;
         case FIXED32:
            return WRAPPED_FIXED32;
         case FIXED64:
            return WRAPPED_FIXED64;
         case BOOL:
            return WRAPPED_BOOL;
         case STRING:
            return WRAPPED_STRING;
         case BYTES:
            return WRAPPED_BYTES;
         case UINT32:
            return WRAPPED_UINT32;
         case UINT64:
            return WRAPPED_UINT64;
         case SFIXED32:
            return WRAPPED_SFIXED32;
         case SFIXED64:
            return WRAPPED_SFIXED64;
         case SINT32:
            return WRAPPED_SINT32;
         case SINT64:
            return WRAPPED_SINT64;
         default:
            throw new IllegalStateException("Unknown field type " + primitiveType);
      }
   }

   private static Type getFieldType(ImmutableSerializationContext ctx, String fullTypeName) {
      switch (fullTypeName) {
         case "double":
            return Type.DOUBLE;
         case "float":
            return Type.FLOAT;
         case "int32":
            return Type.INT32;
         case "int64":
            return Type.INT64;
         case "fixed32":
            return Type.FIXED32;
         case "fixed64":
            return Type.FIXED64;
         case "bool":
            return Type.BOOL;
         case "string":
            return Type.STRING;
         case "bytes":
            return Type.BYTES;
         case "uint32":
            return Type.UINT32;
         case "uint64":
            return Type.UINT64;
         case "sfixed32":
            return Type.SFIXED32;
         case "sfixed64":
            return Type.SFIXED64;
         case "sint32":
            return Type.SINT32;
         case "sint64":
            return Type.SINT64;
         default:
            GenericDescriptor descriptorByName = ctx.getDescriptorByName(fullTypeName);
            if (descriptorByName instanceof EnumDescriptor) {
               return Type.ENUM;
            }
            return Type.MESSAGE;
      }
   }

   private static void processArray(ImmutableSerializationContext ctx, String type, String field, JsonReader reader, RawProtoStreamWriter writer) throws IOException {
      reader.beginArray();
      while (reader.hasNext()) {
         JsonToken token = reader.peek();
         switch (token) {
            case BEGIN_ARRAY:
               processArray(ctx, type, field, reader, writer);
            case BEGIN_OBJECT:
               reader.beginObject();
               Descriptor d = (Descriptor) ctx.getDescriptorByName(type);
               FieldDescriptor fieldByName = d.findFieldByName(field);
               int number = fieldByName.getNumber();
               processObject(ctx, reader, writer, fieldByName.getMessageType().getFullName(), number, false);
               break;
            case STRING:
            case NUMBER:
            case BOOLEAN:
               Descriptor de = (Descriptor) ctx.getDescriptorByName(type);
               FieldDescriptor fd = de.findFieldByName(field);
               Type fieldType = fd.getType();
               if (!fd.isRepeated()) {
                  throw new IllegalStateException("Field '" + fd.getName() + "' is not an array");
               }
               if (fieldType == Type.ENUM) {
                  writeEnumField(reader, writer, fd);
               } else {
                  writeField(reader, writer, fieldType, fd.getNumber());
               }
               break;
         }
      }
      reader.endArray();
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

         /**
          * Have we written the "_type" field?
          */
         private boolean missingType = true;

         private void indent() {
            jsonOut.append('\n');
            for (int k = initNestingLevel + nestingLevel.indent; k > 0; k--) {
               jsonOut.append("   ");
            }
         }

         @Override
         public void onStart(GenericDescriptor descriptor) {
            nestingLevel = new JsonNestingLevel(null);
            if (prettyPrint) {
               indent();
               nestingLevel.indent++;
            }
            jsonOut.append('{');
            writeType(descriptor);
         }

         private void writeType(AnnotatedDescriptor descriptor) {
            if (descriptor != null && nestingLevel.previous == null && nestingLevel.isFirstField) {
               missingType = false;
               nestingLevel.isFirstField = false;
               if (prettyPrint) {
                  indent();
               }
               jsonOut.append('\"').append("_type").append('\"').append(':');
               if (prettyPrint) {
                  jsonOut.append(' ');
               }
               String type;
               if (descriptor instanceof FieldDescriptor) {
                  type = FieldDescriptor.class.cast(descriptor).getTypeName();
               } else {
                  type = descriptor.getFullName();
               }
               jsonOut.append('\"').append(type).append('\"');
            }
         }

         private String escape(String tagValue) {
            return GSON.toJson(tagValue);
         }

         @Override
         public void onTag(int fieldNumber, FieldDescriptor fieldDescriptor, Object tagValue) {
            if (fieldDescriptor == null) {
               // unknown field, ignore
               return;
            }

            if (missingType) {
               writeType(fieldDescriptor);
            }

            startSlot(fieldDescriptor);

            switch (fieldDescriptor.getType()) {
               case STRING:
                  jsonOut.append(escape((String) tagValue));
                  break;
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
                  } else if (fieldNumber == WRAPPED_ENUM) {
                     jsonOut.append('\"').append(tagValue).append('\"');
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
            if (nestingLevel.repeatedFieldDescriptor != null) {
               endArraySlot();
            }
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
               if (fieldDescriptor.getLabel() == Label.ONE_OF) {
                  jsonOut.append('"').append(JSON_VALUE_FIELD).append("\":");
               } else {
                  jsonOut.append('"').append(fieldDescriptor.getName()).append("\":");
               }
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
               case WRAPPED_DESCRIPTOR_ID:
                  typeId = (Integer) tagValue;
                  break;
               case WRAPPED_DESCRIPTOR_FULL_NAME:
                  typeName = (String) tagValue;
                  break;
               case WRAPPED_MESSAGE:
                  wrappedMessage = (byte[]) tagValue;
                  break;
               case WRAPPED_ENUM:
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
                  messageHandler.onStart(null);
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
               FieldDescriptor fd = wrapperDescriptor.findFieldByNumber(WRAPPED_ENUM);
               messageHandler.onStart(enumDescriptor);
               messageHandler.onTag(WRAPPED_ENUM, fd, enumConstantName);
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
