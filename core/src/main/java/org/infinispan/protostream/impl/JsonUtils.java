package org.infinispan.protostream.impl;

import static org.infinispan.protostream.WrappedMessage.WRAPPED_BOOL;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_BYTES;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_CONTAINER_TYPE_NAME;
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
import static org.infinispan.protostream.WrappedMessage.WRAPPED_TYPE_ID;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_TYPE_NAME;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_UINT32;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_UINT64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufParser;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.TagHandler;
import org.infinispan.protostream.TagWriter;
import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.descriptors.AnnotatedDescriptor;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.EnumValueDescriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.GenericDescriptor;
import org.infinispan.protostream.descriptors.Label;
import org.infinispan.protostream.descriptors.MapDescriptor;
import org.infinispan.protostream.descriptors.Type;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Utility class for conversion to and from canonical JSON.
 *
 * @author anistor@redhat.com
 * @since 4.4
 */
public final class JsonUtils {

   // Z-normalized RFC 3339 format
   private static final String RFC_3339_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

   private static final JsonFactory jsonFactory = new JsonFactory();

   private static final String JSON_TYPE_FIELD = "_type";

   private static final String JSON_VALUE_FIELD = "_value";

   private JsonUtils() {
   }

   public static byte[] fromCanonicalJSON(ImmutableSerializationContext ctx, Reader reader) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(ProtobufUtil.DEFAULT_ARRAY_BUFFER_SIZE);

      try (reader; baos) {
         TagWriter writer = TagWriterImpl.newInstanceNoBuffer(ctx, baos);
         JsonParser parser = jsonFactory.createParser(reader);
         while (true) {
            JsonToken token = parser.nextToken();
            if (token == null) {
               break;
            }
            switch (token) {
               case START_OBJECT:
                  processDocument(ctx, parser, writer);
                  break;
               case VALUE_NULL:
                  // we got null input, we write nothing out
                  break;
               default:
                  throw new IllegalStateException("Invalid top level object! Found token: " + token);
            }
         }
         writer.flush();
         return baos.toByteArray();
      } catch (JsonProcessingException e) {
         throw new IllegalStateException("Invalid JSON", e);
      }
   }

   private static void processDocument(ImmutableSerializationContext ctx, JsonParser parser, TagWriter writer) throws IOException {
      while (true) {
         JsonToken token = parser.nextToken();
         if (token == null) {
            break;
         }
         switch (token) {
            case END_OBJECT: {
               return;
            }
            case FIELD_NAME: {
               String currentField = parser.getCurrentName();
               expectField(JSON_TYPE_FIELD, currentField);
               break;
            }
            case VALUE_STRING: {
               String topLevelTypeName = parser.getText();
               GenericDescriptor descriptorByName = null;
               Type fieldType = switch (topLevelTypeName) {
                  case "double" -> Type.DOUBLE;
                  case "float" -> Type.FLOAT;
                  case "int32" -> Type.INT32;
                  case "int64" -> Type.INT64;
                  case "fixed32" -> Type.FIXED32;
                  case "fixed64" -> Type.FIXED64;
                  case "bool" -> Type.BOOL;
                  case "string" -> Type.STRING;
                  case "bytes" -> Type.BYTES;
                  case "uint32" -> Type.UINT32;
                  case "uint64" -> Type.UINT64;
                  case "sfixed32" -> Type.SFIXED32;
                  case "sfixed64" -> Type.SFIXED64;
                  case "sint32" -> Type.SINT32;
                  case "sint64" -> Type.SINT64;
                  default -> {
                     descriptorByName = ctx.getDescriptorByName(topLevelTypeName);
                     yield descriptorByName instanceof EnumDescriptor ? Type.ENUM : Type.MESSAGE;
                  }
               };

               switch (fieldType) {
                  case ENUM:
                     processEnum(parser, writer, (EnumDescriptor) descriptorByName);
                     break;
                  case MESSAGE:
                     processObject(ctx, parser, writer, (Descriptor) descriptorByName, null, true);
                     break;
                  default:
                     processPrimitive(parser, writer, fieldType);
               }
            }
         }
      }
   }

   private static void processEnum(JsonParser parser, TagWriter writer, EnumDescriptor enumDescriptor) throws IOException {
      while (true) {
         JsonToken token = parser.nextToken();
         if (token == null) {
            break;
         }
         switch (token) {
            case END_OBJECT:
               return;
            case FIELD_NAME:
               String fieldName = parser.getCurrentName();
               expectField(JSON_VALUE_FIELD, fieldName);
               break;
            case VALUE_STRING: {
               String enumValueName = parser.getText();
               EnumValueDescriptor enumValueDescriptor = enumDescriptor.findValueByName(enumValueName);
               if (enumValueDescriptor == null) {
                  throw new IllegalStateException("Invalid enum value : '" + enumValueName + "'");
               }
               Integer topLevelTypeId = enumDescriptor.getTypeId();
               if (topLevelTypeId == null) {
                  writer.writeString(WRAPPED_TYPE_NAME, enumDescriptor.getFullName());
               } else {
                  writer.writeUInt32(WRAPPED_TYPE_ID, topLevelTypeId);
               }
               writer.writeEnum(WRAPPED_ENUM, enumValueDescriptor.getNumber());
               break;
            }
            case VALUE_NUMBER_INT: {
               int enumValueNumber = parser.getIntValue();
               EnumValueDescriptor enumValueDescriptor = enumDescriptor.findValueByNumber(enumValueNumber);
               if (enumValueDescriptor == null) {
                  throw new IllegalStateException("Invalid enum value : " + enumValueNumber);
               }
               Integer topLevelTypeId = enumDescriptor.getTypeId();
               if (topLevelTypeId == null) {
                  writer.writeString(WRAPPED_TYPE_NAME, enumDescriptor.getFullName());
               } else {
                  writer.writeUInt32(WRAPPED_TYPE_ID, topLevelTypeId);
               }
               writer.writeEnum(WRAPPED_ENUM, enumValueDescriptor.getNumber());
               break;
            }
            case VALUE_NULL:
               throw new IllegalStateException("Invalid enum value 'null'");
            case VALUE_TRUE:
            case VALUE_FALSE:
            case VALUE_NUMBER_FLOAT:
               throw new IllegalStateException("Invalid enum value '" + parser.getText() + "'");
            default:
               throw new IllegalStateException("Unexpected token : " + token);
         }
      }
   }

   private static void processObject(ImmutableSerializationContext ctx, JsonParser parser, TagWriter writer, Descriptor messageDescriptor, Integer fieldNumber, boolean topLevel) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(ProtobufUtil.DEFAULT_ARRAY_BUFFER_SIZE);
      TagWriter nestedWriter = TagWriterImpl.newInstanceNoBuffer(ctx, baos);

      String currentField = null;

      out:
      while (true) {
         JsonToken token = parser.nextToken();
         if (token == null) {
            break;
         }
         switch (token) {
            case END_OBJECT:
               break out;
            case START_ARRAY:
               processArray(ctx, messageDescriptor.getFullName(), currentField, parser, nestedWriter);
               break;
            case START_OBJECT: {
               FieldDescriptor fd = messageDescriptor.findFieldByName(currentField);
               if (fd.isMap()) {
                  processMap(ctx, (MapDescriptor) fd, parser, nestedWriter);
               } else {
                  Descriptor messageType = fd.getMessageType();
                  if (messageType == null) {
                     throw new IllegalStateException("Field '" + currentField + "' is not an object");
                  }
                  processObject(ctx, parser, nestedWriter, messageType, fd.getNumber(), false);
               }
               break;
            }
            case FIELD_NAME:
               currentField = parser.getCurrentName();
               break;
            case VALUE_STRING:
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
            case VALUE_TRUE:
            case VALUE_FALSE: {
               FieldDescriptor fd = messageDescriptor.findFieldByName(currentField);
               if (fd == null) {
                  throw new IllegalStateException("The field '" + currentField + "' was not found in the Protobuf schema");
               }

               if (fd.getType() == Type.ENUM) {
                  writeEnumField(parser, nestedWriter, fd, fd.getNumber());
               } else {
                  writeField(parser, nestedWriter, fd.getType(), fd.getNumber());
               }
               break;
            }
            case VALUE_NULL:
               // we got null in, we write nothing out
               break;
         }
      }

      if (topLevel) {
         Integer topLevelTypeId = messageDescriptor.getTypeId();
         if (topLevelTypeId == null) {
            writer.writeString(WRAPPED_TYPE_NAME, messageDescriptor.getFullName());
         } else {
            writer.writeUInt32(WRAPPED_TYPE_ID, topLevelTypeId);
         }
         nestedWriter.flush();
         writer.writeBytes(WRAPPED_MESSAGE, baos.toByteArray());
      } else {
         nestedWriter.flush();
         writer.writeBytes(fieldNumber, baos.toByteArray());
      }

      writer.flush();
   }

   private static void processMap(ImmutableSerializationContext ctx, MapDescriptor md, JsonParser parser, TagWriter writer) throws IOException {
      while (true) {
         JsonToken token = parser.nextToken();
         if (token == JsonToken.END_OBJECT) {
            break;
         }
         if (token != JsonToken.FIELD_NAME) {
            throw new IllegalStateException("Unexpected token");
         }
         ByteArrayOutputStream baos = new ByteArrayOutputStream(ProtobufUtil.DEFAULT_ARRAY_BUFFER_SIZE);
         TagWriter nestedWriter = TagWriterImpl.newInstanceNoBuffer(ctx, baos);
         String key = parser.getCurrentName();
         switch (md.getKeyType()) {
            case STRING -> nestedWriter.writeString(1, key);
            case INT32 -> nestedWriter.writeInt32(1, Integer.parseInt(key));
            case INT64 -> nestedWriter.writeInt64(1, Long.parseLong(key));
            case FIXED32 -> nestedWriter.writeFixed32(1, Integer.parseInt(key));
            case FIXED64 -> nestedWriter.writeFixed64(1, Long.parseLong(key));
            case SINT32 -> nestedWriter.writeSInt32(1, Integer.parseInt(key));
            case SINT64 -> nestedWriter.writeSInt64(1, Long.parseLong(key));
            case SFIXED32 -> nestedWriter.writeSFixed32(1, Integer.parseInt(key));
            case SFIXED64 -> nestedWriter.writeSFixed64(1, Long.parseLong(key));
            case UINT32 -> nestedWriter.writeUInt32(1, Integer.parseInt(key));
            case UINT64 -> nestedWriter.writeUInt64(1, Long.parseLong(key));
         }
         processMapValue(ctx, parser, nestedWriter, md);
         writer.writeBytes(md.getNumber(), baos.toByteArray());
         writer.flush();
      }

   }

   private static void processMapValue(ImmutableSerializationContext ctx, JsonParser parser, TagWriter writer, MapDescriptor md) throws IOException {
      JsonToken token = parser.nextToken();
      if (token == null) {
         return;
      }
      switch (token) {
         case START_OBJECT: {
            processObject(ctx, parser, writer, md.getMessageType(),2, false);
            break;
         }
         case VALUE_STRING:
            if (md.getType() == Type.ENUM) {
               writeEnumField(parser, writer, md, 2);
            } else {
               writer.writeString(2, parser.getValueAsString());
            }
            break;
         case VALUE_NUMBER_INT:
            switch (md.getType()) {
               case INT32 -> writer.writeInt32(2, parser.getIntValue());
               case INT64 -> writer.writeInt64(2, parser.getLongValue());
               case FIXED32 -> writer.writeFixed32(2, parser.getIntValue());
               case FIXED64 -> writer.writeFixed64(2, parser.getLongValue());
               case SINT32 -> writer.writeSInt32(2, parser.getIntValue());
               case SINT64 -> writer.writeSInt64(2, parser.getLongValue());
               case SFIXED32 -> writer.writeSFixed32(2, parser.getIntValue());
               case SFIXED64 -> writer.writeSFixed64(2, parser.getLongValue());
               case UINT32 -> writer.writeUInt32(2, parser.getIntValue());
               case UINT64 -> writer.writeUInt64(2, parser.getLongValue());
            }
            break;
         case VALUE_NUMBER_FLOAT:
            switch (md.getType()) {
               case FLOAT -> writer.writeFloat(2, parser.getFloatValue());
               case DOUBLE -> writer.writeDouble(2, parser.getDoubleValue());
            }
            break;
      }
      writer.flush();
   }


   private static void processPrimitive(JsonParser parser, TagWriter writer, Type fieldType) throws IOException {
      while (true) {
         JsonToken token = parser.nextToken();
         if (token == null) {
            break;
         }
         switch (token) {
            case END_OBJECT:
               return;
            case FIELD_NAME:
               String fieldName = parser.getCurrentName();
               expectField(JSON_VALUE_FIELD, fieldName);
               break;
            case VALUE_STRING:
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
            case VALUE_TRUE:
            case VALUE_FALSE:
               writeField(parser, writer, fieldType, getPrimitiveFieldId(fieldType));
               break;
            case VALUE_NULL:
               // we got null in, we do not output anything
               break;
            default:
               throw new IllegalStateException("Unexpected JSON token :" + token);
         }
      }
   }

   private static int getPrimitiveFieldId(Type primitiveType) {
      return switch (primitiveType) {
         case DOUBLE -> WRAPPED_DOUBLE;
         case FLOAT -> WRAPPED_FLOAT;
         case INT32 -> WRAPPED_INT32;
         case INT64 -> WRAPPED_INT64;
         case FIXED32 -> WRAPPED_FIXED32;
         case FIXED64 -> WRAPPED_FIXED64;
         case BOOL -> WRAPPED_BOOL;
         case STRING -> WRAPPED_STRING;
         case BYTES -> WRAPPED_BYTES;
         case UINT32 -> WRAPPED_UINT32;
         case UINT64 -> WRAPPED_UINT64;
         case SFIXED32 -> WRAPPED_SFIXED32;
         case SFIXED64 -> WRAPPED_SFIXED64;
         case SINT32 -> WRAPPED_SINT32;
         case SINT64 -> WRAPPED_SINT64;
         default -> throw new IllegalStateException("Unknown field type " + primitiveType);
      };
   }

   private static void processArray(ImmutableSerializationContext ctx, String type, String field, JsonParser parser, TagWriter writer) throws IOException {
      while (true) {
         JsonToken token = parser.nextToken();
         if (token == null) {
            break;
         }
         switch (token) {
            case END_ARRAY:
               return;
            case START_ARRAY:
               processArray(ctx, type, field, parser, writer); //todo [anistor] array in array does not seem to work since initial version
               break;
            case START_OBJECT: {
               Descriptor d = (Descriptor) ctx.getDescriptorByName(type);
               FieldDescriptor fd = d.findFieldByName(field);
               processObject(ctx, parser, writer, fd.getMessageType(), fd.getNumber(), false);
               break;
            }
            case VALUE_STRING:
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
            case VALUE_TRUE:
            case VALUE_FALSE: {
               Descriptor d = (Descriptor) ctx.getDescriptorByName(type);
               FieldDescriptor fd = d.findFieldByName(field);
               if (!fd.isRepeated()) {
                  if (token == JsonToken.VALUE_NUMBER_INT && Type.BYTES.equals(fd.getType())) {
                     // We want to parse a byte[] not only as Base64 but also from the JSON form: [7,7,7]
                     ArrayList<Byte> result = new ArrayList<>();
                     byte value;
                     while (token == JsonToken.VALUE_NUMBER_INT) {
                        value = parser.getByteValue();
                        result.add(value);
                        token = parser.nextToken();
                     }

                     byte[] binary = new byte[result.size()];
                     for (int i = 0; i < result.size(); i++) {
                        binary[i] = result.get(i);
                     }
                     writer.writeBytes(fd.getNumber(), binary);

                     if (token == JsonToken.END_ARRAY) {
                        return;
                     }
                  }
                  throw new IllegalStateException("Field '" + fd.getName() + "' is not an array");
               }
               if (fd.getType() == Type.ENUM) {
                  writeEnumField(parser, writer, fd, fd.getNumber());
               } else {
                  writeField(parser, writer, fd.getType(), fd.getNumber());
               }
               break;
            }
         }
      }
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
    * Converts a Protobuf encoded message to its <a href="https://developers.google.com/protocol-buffers/docs/proto3#json">
    * canonical JSON representation</a>.
    *
    * @param ctx         the serialization context
    * @param bytes       the Protobuf encoded message bytes to parse
    * @param prettyPrint indicates if the JSON output should use a 'pretty' human-readable format or a compact format
    * @return the JSON string representation
    * @throws IOException if I/O operations fail
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
            jsonOut.append("   ".repeat(Math.max(0, initNestingLevel + nestingLevel.indent)));
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
                  type = ((FieldDescriptor) descriptor).getTypeName();
               } else {
                  type = descriptor.getFullName();
               }
               jsonOut.append('\"').append(type).append('\"');
            }
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
                  escapeJson((String) tagValue, jsonOut, true);
                  break;
               case UINT64:
               case FIXED64:
                  jsonOut.append(Long.toUnsignedString((Long) tagValue));
                  break;
               case UINT32:
               case FIXED32:
                  jsonOut.append(Integer.toUnsignedString((Integer) tagValue));
                  break;
               case FLOAT:
                  Float f = (Float) tagValue;
                  if (f.isInfinite() || f.isNaN()) {
                     // Infinity and NaN need to be quoted
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
                  } else if (fieldNumber == WRAPPED_ENUM && fieldDescriptor.name.equals("wrappedEnum")) {
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
            if (!fieldDescriptor.isMap()) {
               jsonOut.append('{');
            }
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
            if (!fieldDescriptor.isMap()) {
               jsonOut.append('}');
            }
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
            if (nestingLevel.repeatedFieldDescriptor != null && !nestingLevel.repeatedFieldDescriptor.name.equals(fieldDescriptor.name)) {
               endArraySlot();
            }
            boolean map = nestingLevel.previous != null && nestingLevel.previous.repeatedFieldDescriptor != null && nestingLevel.previous.repeatedFieldDescriptor.isMap();
            if (nestingLevel.isFirstField) {
               nestingLevel.isFirstField = false;
            } else {
               jsonOut.append(map ? ':' : ',');
            }
            if (!map) {
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
            }
            if (fieldDescriptor.isRepeated() && nestingLevel.repeatedFieldDescriptor == null) {
               nestingLevel.repeatedFieldDescriptor = fieldDescriptor;
               jsonOut.append(fieldDescriptor.isMap() ? '{' : '[');
            }
         }

         private void endArraySlot() {
            boolean map = nestingLevel.repeatedFieldDescriptor.isMap();
            if (prettyPrint && nestingLevel.repeatedFieldDescriptor.getType() == Type.MESSAGE) {
               indent();
            }
            nestingLevel.repeatedFieldDescriptor = null;
            jsonOut.append(map ? '}' : ']');
         }
      };

      TagHandler wrapperHandler = new TagHandler() {

         private Integer typeId;
         private String typeName;
         private byte[] wrappedMessage;
         private Integer wrappedEnum;
         private String wrappedContainerType;

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
               case WRAPPED_TYPE_ID:
                  typeId = (Integer) tagValue;
                  break;
               case WRAPPED_TYPE_NAME:
                  typeName = (String) tagValue;
                  break;
               case WRAPPED_MESSAGE:
                  wrappedMessage = (byte[]) tagValue;
                  break;
               case WRAPPED_ENUM:
                  wrappedEnum = (Integer) tagValue;
                  break;
               case WRAPPED_CONTAINER_TYPE_NAME:
                  wrappedContainerType = (String) tagValue;
                  GenericDescriptor descriptorByName = ctx.getDescriptorByName(wrappedContainerType);
                  messageHandler.onStart(descriptorByName);
                  break;
               case WrappedMessage.WRAPPED_BYTE:
               case WrappedMessage.WRAPPED_SHORT:
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
                  if (wrappedContainerType != null) {
                     messageHandler.onTag(fieldNumber, new RepeatedFieldDescriptor(fieldDescriptor), tagValue);
                  } else {
                     messageHandler.onStart(null);
                     messageHandler.onTag(fieldNumber, fieldDescriptor, tagValue);
                     messageHandler.onEnd();
                  }
                  break;
            }
         }

         @Override
         public void onEnd() {
            if (wrappedContainerType != null) {
               messageHandler.onEnd();
            } else if (wrappedEnum != null) {
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

   private static void writeEnumField(JsonParser parser, TagWriter writer, FieldDescriptor fd, int fieldNumber) throws IOException {
      String value = parser.getText();
      EnumDescriptor enumDescriptor = fd.getEnumType();
      EnumValueDescriptor valueDescriptor = enumDescriptor.findValueByName(value);
      if (valueDescriptor == null) {
         throw new IllegalStateException("Invalid enum value '" + value + "'");
      }
      int choice = valueDescriptor.getNumber();
      writer.writeEnum(fieldNumber, choice);
   }

   private static void writeField(JsonParser parser, TagWriter writer, Type fieldType, int fieldNumber) throws IOException {
      //TODO [anistor] all these number parsing below just masks an issue with quoted vs unquoted numeric literals
      switch (fieldType) {
         case DOUBLE:
            writer.writeDouble(fieldNumber, Double.parseDouble(parser.getText()));
            break;
         case FLOAT:
            writer.writeFloat(fieldNumber, Float.parseFloat(parser.getText()));
            break;
         case INT64:
            writer.writeInt64(fieldNumber, Long.parseLong(parser.getText()));
            break;
         case UINT64:
            writer.writeUInt64(fieldNumber, Long.parseUnsignedLong(parser.getText()));
            break;
         case FIXED64:
            writer.writeFixed64(fieldNumber, Long.parseUnsignedLong(parser.getText()));
            break;
         case SFIXED64:
            writer.writeSFixed64(fieldNumber, Long.parseLong(parser.getText()));
            break;
         case SINT64:
            writer.writeSInt64(fieldNumber, Long.parseLong(parser.getText()));
            break;
         case INT32:
            writer.writeInt32(fieldNumber, Integer.parseInt(parser.getText()));
            break;
         case FIXED32:
            writer.writeFixed32(fieldNumber, Integer.parseUnsignedInt(parser.getText()));
            break;
         case UINT32:
            writer.writeUInt32(fieldNumber, Integer.parseUnsignedInt(parser.getText()));
            break;
         case SFIXED32:
            writer.writeSFixed32(fieldNumber, Integer.parseInt(parser.getText()));
            break;
         case SINT32:
            writer.writeSInt32(fieldNumber, Integer.parseInt(parser.getText()));
            break;
         case BOOL:
            writer.writeBool(fieldNumber, parser.getBooleanValue());
            break;
         case STRING:
            writer.writeString(fieldNumber, parser.getText());
            break;
         case BYTES:
            byte[] binary = parser.getBinaryValue();
            writer.writeBytes(fieldNumber, binary);
            break;
         default:
            throw new IllegalArgumentException("The Protobuf declared field type is not compatible with the written type : " + fieldType);
      }
   }

   private static void expectField(String expectedFieldName, String actualFieldName) {
      if (!expectedFieldName.equals(actualFieldName)) {
         throw new IllegalStateException("The document should contain a top level field '" + expectedFieldName + "'");
      }
   }

   // todo [anistor] do we really need html escaping? so far I'm keeping it so we behave like previous implementation

   /**
    * Escapes a string literal in order to have a valid JSON representation. Optionally it can also escape some html chars.
    */
   private static void escapeJson(String value, StringBuilder out, boolean htmlSafe) {
      out.append('"');
      int prev = 0;
      int len = value.length();
      for (int cur = 0; cur < len; cur++) {
         char ch = value.charAt(cur);
         String esc = null;
         if (ch < ' ') {
            esc = switch (ch) {
               case '\t' -> "\\t";
               case '\b' -> "\\b";
               case '\n' -> "\\n";
               case '\r' -> "\\r";
               case '\f' -> "\\f";
               default -> String.format("\\u%04x", (int) ch);
            };
         } else if (ch < 128) {
            if (ch == '"') {
               esc = "\\\"";
            } else if (ch == '\\') {
               esc = "\\\\";
            } else if (htmlSafe) {
               esc = switch (ch) {
                  case '<' -> "\\u003c";
                  case '>' -> "\\u003e";
                  case '&' -> "\\u0026";
                  case '=' -> "\\u003d";
                  case '\'' -> "\\u0027";
                  default -> esc;
               };
            }
         } else if (ch == '\u2028') {
            esc = "\\u2028";
         } else if (ch == '\u2029') {
            esc = "\\u2029";
         } else {
            continue;
         }
         if (esc != null) {
            if (prev < cur) {
               out.append(value, prev, cur);
            }
            prev = cur + 1;
            out.append(esc);
         }
      }
      if (prev < len) {
         out.append(value, prev, len);
      }
      out.append('"');
   }

   private static String formatDate(Date date) {
      return timestampFormat.get().format(date);
   }

   private static final ThreadLocal<DateFormat> timestampFormat = ThreadLocal.withInitial(() -> {
      // Z-normalized RFC 3339 format
      SimpleDateFormat sdf = new SimpleDateFormat(RFC_3339_DATE_FORMAT);
      GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
      calendar.setGregorianChange(new Date(Long.MIN_VALUE));
      sdf.setCalendar(calendar);
      return sdf;
   });
}
