package org.infinispan.protostream.impl;

import static org.infinispan.protostream.WrappedMessage.WRAPPED_BOOL;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_BYTES;
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
import java.util.Base64;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

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
      ByteArrayOutputStream baos = new ByteArrayOutputStreamEx(ProtobufUtil.DEFAULT_ARRAY_BUFFER_SIZE);
      TagWriter writer = TagWriterImpl.newInstanceNoBuffer(ctx, baos);

      JsonParser parser = jsonFactory.createParser(reader);

      try {
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
      } finally {
         baos.close();
         reader.close();
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
               Type fieldType;
               switch (topLevelTypeName) {
                  case "double":
                     fieldType = Type.DOUBLE;
                     break;
                  case "float":
                     fieldType = Type.FLOAT;
                     break;
                  case "int32":
                     fieldType = Type.INT32;
                     break;
                  case "int64":
                     fieldType = Type.INT64;
                     break;
                  case "fixed32":
                     fieldType = Type.FIXED32;
                     break;
                  case "fixed64":
                     fieldType = Type.FIXED64;
                     break;
                  case "bool":
                     fieldType = Type.BOOL;
                     break;
                  case "string":
                     fieldType = Type.STRING;
                     break;
                  case "bytes":
                     fieldType = Type.BYTES;
                     break;
                  case "uint32":
                     fieldType = Type.UINT32;
                     break;
                  case "uint64":
                     fieldType = Type.UINT64;
                     break;
                  case "sfixed32":
                     fieldType = Type.SFIXED32;
                     break;
                  case "sfixed64":
                     fieldType = Type.SFIXED64;
                     break;
                  case "sint32":
                     fieldType = Type.SINT32;
                     break;
                  case "sint64":
                     fieldType = Type.SINT64;
                     break;
                  default:
                     descriptorByName = ctx.getDescriptorByName(topLevelTypeName);
                     fieldType = descriptorByName instanceof EnumDescriptor ? Type.ENUM : Type.MESSAGE;
               }

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
      Set<String> requiredFields = messageDescriptor.getFields().stream()
            .filter(FieldDescriptor::isRequired)
            .map(FieldDescriptor::getName)
            .collect(Collectors.toCollection(HashSet::new));

      TagWriter nestedWriter;
      if (topLevel) {
         Integer topLevelTypeId = messageDescriptor.getTypeId();
         if (topLevelTypeId == null) {
            writer.writeString(WRAPPED_TYPE_NAME, messageDescriptor.getFullName());
         } else {
            writer.writeUInt32(WRAPPED_TYPE_ID, topLevelTypeId);
         }
         nestedWriter = writer.subWriter(WRAPPED_MESSAGE, false).getWriter();
      } else {
         nestedWriter = writer.subWriter(fieldNumber, true).getWriter();
      }

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
               Descriptor messageType = fd.getMessageType();
               if (messageType == null) {
                  throw new IllegalStateException("Field '" + currentField + "' is not an object");
               }
               processObject(ctx, parser, nestedWriter, messageType, fd.getNumber(), false);
               requiredFields.remove(currentField);
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
                  writeEnumField(parser, nestedWriter, fd);
               } else {
                  writeField(parser, nestedWriter, fd.getType(), fd.getNumber());
               }
               requiredFields.remove(currentField);
               break;
            }
            case VALUE_NULL:
               // we got null in, we write nothing out
               break;
         }
      }

      if (!requiredFields.isEmpty()) {
         String missing = requiredFields.iterator().next();
         throw new IllegalStateException("Required field '" + missing + "' missing");
      }

      nestedWriter.close();
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
                  throw new IllegalStateException("Field '" + fd.getName() + "' is not an array");
               }
               if (fd.getType() == Type.ENUM) {
                  writeEnumField(parser, writer, fd);
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

   private static void writeEnumField(JsonParser parser, TagWriter writer, FieldDescriptor fd) throws IOException {
      String value = parser.getText();
      EnumDescriptor enumDescriptor = fd.getEnumType();
      EnumValueDescriptor valueDescriptor = enumDescriptor.findValueByName(value);
      if (valueDescriptor == null) {
         throw new IllegalStateException("Invalid enum value '" + value + "'");
      }
      int choice = valueDescriptor.getNumber();
      writer.writeEnum(fd.getNumber(), choice);
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
            switch (ch) {
               case '\t':
                  esc = "\\t";
                  break;
               case '\b':
                  esc = "\\b";
                  break;
               case '\n':
                  esc = "\\n";
                  break;
               case '\r':
                  esc = "\\r";
                  break;
               case '\f':
                  esc = "\\f";
                  break;
               default:
                  esc = String.format("\\u%04x", (int) ch);
            }
         } else if (ch < 128) {
            if (ch == '"') {
               esc = "\\\"";
            } else if (ch == '\\') {
               esc = "\\\\";
            } else if (htmlSafe) {
               switch (ch) {
                  case '<':
                     esc = "\\u003c";
                     break;
                  case '>':
                     esc = "\\u003e";
                     break;
                  case '&':
                     esc = "\\u0026";
                     break;
                  case '=':
                     esc = "\\u003d";
                     break;
                  case '\'':
                     esc = "\\u0027";
                     break;
               }
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
