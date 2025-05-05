package org.infinispan.protostream.impl.json;

import static com.fasterxml.jackson.core.JsonToken.START_OBJECT;
import static com.fasterxml.jackson.core.JsonToken.VALUE_NULL;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_BOOL;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_BYTES;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_CONTAINER_TYPE_ID;
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
import static org.infinispan.protostream.impl.json.JsonHelper.EMPTY_ARRAY;
import static org.infinispan.protostream.impl.json.JsonHelper.JSON_TYPE_FIELD;
import static org.infinispan.protostream.impl.json.JsonHelper.JSON_VALUE_FIELD;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Objects;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.RandomAccessOutputStream;
import org.infinispan.protostream.TagWriter;
import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.EnumValueDescriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.GenericDescriptor;
import org.infinispan.protostream.descriptors.Label;
import org.infinispan.protostream.descriptors.MapDescriptor;
import org.infinispan.protostream.descriptors.Type;
import org.infinispan.protostream.impl.RandomAccessOutputStreamImpl;
import org.infinispan.protostream.impl.TagWriterImpl;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Utility class for conversion from canonical JSON.
 *
 * <p>
 * The canonical JSON must follow an expected format to serialize transform back into a byte array. The JSON must
 * be generated with the {@link JsonWriter} to create the correct format for the JSON. The requirements include:
 * </p>
 * <ul>
 *    <li>Explicit types: The root element must contain the type of the written object. This is done with the
 *    {@link JsonHelper#JSON_TYPE_FIELD} field. This must be the first field in the JSON.</li>
 *
 *    <li>Value: Primitive values which are not contained by an outer object must write the value with the
 *    {@link JsonHelper#MAP_VALUE_FIELD} field.</li>
 * </ul>
 *
 * @author anistor@redhat.com
 * @author José Bolina
 * @since 6.0
 */
public final class JsonReader {

   private static final JsonFactory JSON_FACTORY = new JsonFactory();

   private JsonReader() { }

   public static byte[] fromJson(ImmutableSerializationContext ctx, Reader reader) throws IOException {
      try (reader) {
         JsonParser parser = JSON_FACTORY.createParser(reader);
         JsonToken token = parser.nextToken();

         if (token == null || token == VALUE_NULL)
            return EMPTY_ARRAY;

         if (token != START_OBJECT)
            throw new IllegalStateException("Invalid top level object! Found token: " + token);

         try (RandomAccessOutputStream raos = new RandomAccessOutputStreamImpl(ProtobufUtil.DEFAULT_ARRAY_BUFFER_SIZE)) {
            TagWriter writer = TagWriterImpl.newInstance(ctx, raos);
            processDocument(ctx, parser, writer, null, null);
            writer.flush();
            return raos.toByteArray();
         }
      } catch (JsonProcessingException e) {
         throw new IllegalStateException("Invalid JSON", e);
      }
   }

   private static void processDocument(ImmutableSerializationContext ctx, JsonParser parser, TagWriter writer, Descriptor descriptor, Integer fieldNumber) throws IOException {
      boolean keepParsing = true;
      while (keepParsing) {
         keepParsing = processSingleDocument(ctx, parser, writer, descriptor, fieldNumber);
      }
   }

   private static boolean processSingleDocument(ImmutableSerializationContext ctx, JsonParser parser, TagWriter writer, Descriptor messageDescriptor, Integer fieldNumber) throws IOException {
      while (true) {
         JsonToken token = parser.nextToken();
         if (token == null) return false;

         switch (token) {
            case END_ARRAY, END_OBJECT:
               return false;

            case FIELD_NAME: {
               String currentField = parser.currentName();
               readSingleField(ctx, parser, writer, messageDescriptor, currentField, fieldNumber);
               break;
            }
            case VALUE_STRING: {
               String topLevelTypeName = parser.getText();
               Type type = Type.primitiveFromString(topLevelTypeName);
               if (type != null) {
                  processPrimitive(parser, writer, type, fieldNumber);
                  break;
               }

               GenericDescriptor descriptorByName = ctx.getDescriptorByName(topLevelTypeName);
               if (descriptorByName instanceof EnumDescriptor d) {
                  processEnum(parser, writer, d);
                  return true;
               }

               processObject(ctx, parser, writer, (Descriptor) descriptorByName, fieldNumber, messageDescriptor == null);
               return true;
            }
         }
      }
   }

   private static void processObject(ImmutableSerializationContext ctx, JsonParser parser, TagWriter writer, Descriptor messageDescriptor, Integer fieldNumber, boolean topLevel) throws IOException {
      RandomAccessOutputStream raos = new RandomAccessOutputStreamImpl(ProtobufUtil.DEFAULT_ARRAY_BUFFER_SIZE);
      TagWriter nestedWriter = TagWriterImpl.newInstance(ctx, raos);
      boolean isContainerAdapter = JsonHelper.isContainerAdapter(ctx, messageDescriptor);

      String currentField = null;

      OUT:
      while (true) {
         // At this stage we are reading a known object.
         // Objects in ProtoStream is prefixed with the `_type` to identify the message descriptor.
         // At this point, we either read primitive values, or an embedded object/array.
         // In the latter case, it should trigger the recursive search again for a full document.
         JsonToken token = parser.nextToken();
         if (token == null) {
            break;
         }
         switch (token) {
            case END_OBJECT:
               break OUT;

            case FIELD_NAME:
               currentField = parser.currentName();
               break;

            // Starting to read a repeated value in ProtoStream. We delegate to an appropriate method to read each
            // document separately.
            case START_ARRAY: {
               if (!isContainerAdapter) {
                  FieldDescriptor fd = messageDescriptor.findFieldByName(currentField);
                  if (fd.isMap()) {
                     processMap(ctx, (MapDescriptor) fd, parser, nestedWriter);
                  } else {
                     processArray(ctx, parser, nestedWriter, messageDescriptor.getFullName(), currentField, fd.getNumber());
                  }
                  break;
               }

               // In case there is an array with a field that does not exist in the descriptor, it *must* be a container.
               // A container writes the values directly into the stream without a name, to have something in the JSON
               // representation, we use a field named `_container`.
               expectField(JSON_VALUE_FIELD, currentField);
               processArray(ctx, parser, nestedWriter, messageDescriptor.getFullName(), currentField, WRAPPED_MESSAGE);
               break;
            }

            // Starting a nested object.
            // It needs to go over all the process of reading the document, identifying the descriptor, etc.
            case START_OBJECT: {
               FieldDescriptor fd = messageDescriptor.findFieldByName(currentField);
               if (fd.isMap()) {
                  processMap(ctx, (MapDescriptor) fd, parser, nestedWriter);
               } else {
                  Descriptor messageType = fd.getMessageType();
                  if (messageType == null && !isInternalFieldAndObject(fd.getNumber())) {
                     throw new IllegalStateException("Field '" + currentField + "' is not an object");
                  }

                  // We are reading a nested object in the JSON that translate to a ONE_OF in ProtoStream.
                  // In this case, we don't need to pass the field number along to write the data since it is a single field.
                  Integer nestedFieldNumber = fd.getLabel() == Label.ONE_OF ? null : fd.getNumber();
                  if (messageType == null) {
                     processSingleDocument(ctx, parser, nestedWriter, messageType, nestedFieldNumber);
                  } else {
                     processObject(ctx, parser, nestedWriter, messageType, nestedFieldNumber, false);
                  }
               }
               break;
            }

            // We are reading a primitive value.
            // We the nested writer to keep the correct order of bytes.
            // The nested writer represents the local bytes, which might be wrapped again in the upper layer.
            case VALUE_STRING:
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
            case VALUE_TRUE:
            case VALUE_FALSE: {
               FieldDescriptor fd = isContainerAdapter
                     ? ((Descriptor) ctx.getDescriptorByTypeId(WrappedMessage.PROTOBUF_TYPE_ID)).findFieldByName(currentField)
                     : messageDescriptor.findFieldByName(currentField);

               if (fd == null) {
                  if (!JSON_TYPE_FIELD.equals(currentField))
                     throw new IllegalStateException("The field '" + currentField + "' was not found in the Protobuf schema");
                  break;
               }

               if (fd.getType() == Type.ENUM) {
                  writeEnumField(parser, nestedWriter, fd, fd.getNumber());
               } else {
                  writeField(parser, nestedWriter, fd.getType(), fd.getNumber());
               }
               break;
            }

            // We got null in, we write nothing out.
            case VALUE_NULL:
               break;
         }
      }

      // Now wrap the current object with the prefix bytes.
      // The prefix bytes usually involve the type id or name, and the field number.
      // Both values followed by the local bytes from the nested writer.
      if (WrappedMessage.knownWrappedDescriptor(ctx, messageDescriptor)) {
         // Known types are here for backwards compatibility.
         // These values are written manually, and as such we just write the bytes out.
         nestedWriter.flush();
         byte[] serialized = raos.toByteArray();
         writer.writeRawBytes(serialized, 0, serialized.length);
      } else if (topLevel) {
         // Otherwise, if this written by something else, we need to write the prefixed bytes.
         // The field number is optional, so it might be skipped in some occasions.
         nestedWriter.flush();
         byte[] nestedData = raos.toByteArray();

         // This is needed in occasions we need to wrap the nested message twice.
         // We have WRAPPED_MESSAGE_1, TYPE, WRAPPED_MESSAGE_2, <INNER_BUFFER>
         // Since we need the complete byte array when writing the bytes with WRAPPED_MESSAGE, we need this extra allocation.
         // In the new output we write: TYPE, WRAPPED_MESSAGE_2, <INNER_BUFFER>.
         // Then we flush it in the original writer with: WRAPPED_MESSAGE_1, <EXTRA_BUFFER>
         // In cases this extra wrapping is not needed, we can write directly to the original writer.
         RandomAccessOutputStream extraBuffer = null;
         TagWriter out = writer;
         if (fieldNumber != null) {
            extraBuffer = new RandomAccessOutputStreamImpl(nestedData.length + 128);
            out = TagWriterImpl.newInstance(ctx, extraBuffer);
         }

         Integer topLevelTypeId = messageDescriptor.getTypeId();
         if (topLevelTypeId == null) {
            int tag = isContainerAdapter
                  ? WRAPPED_CONTAINER_TYPE_NAME
                  : WRAPPED_TYPE_NAME;
            out.writeString(tag, messageDescriptor.getFullName());
         } else {
            int tag = isContainerAdapter
                  ? WRAPPED_CONTAINER_TYPE_ID
                  : WRAPPED_TYPE_ID;
            out.writeUInt32(tag, topLevelTypeId);
         }

         // A container adapter writes the bytes from the inner buffer directly to the output.
         // Otherwise, everything is always wrapped in a WrappedMessage.
         if (isContainerAdapter) {
            out.writeRawBytes(nestedData, 0, nestedData.length);
         } else {
            out.writeBytes(WRAPPED_MESSAGE, nestedData);
         }

         if (fieldNumber != null) {
            out.flush();
            writer.writeBytes(fieldNumber, Objects.requireNonNull(extraBuffer).toByteArray());
         }
      } else {
         nestedWriter.flush();
         writer.writeBytes(Objects.requireNonNull(fieldNumber), raos.toByteArray());
      }

      writer.flush();
   }

   private static void processArray(ImmutableSerializationContext ctx, JsonParser parser, TagWriter writer, String type, String field, Integer fieldNumber) throws IOException {
      OUT:
      while (true) {
         JsonToken token = parser.nextToken();
         if (token == null) {
            break;
         }
         switch (token) {
            case END_ARRAY:
               break OUT;
            case START_ARRAY:
               processArray(ctx, parser, writer, type, field, fieldNumber);
               break;
            case START_OBJECT: {
               Descriptor d = (Descriptor) ctx.getDescriptorByName(type);
               RandomAccessOutputStream raos = new RandomAccessOutputStreamImpl(ProtobufUtil.DEFAULT_ARRAY_BUFFER_SIZE);
               TagWriter nestedWriter = TagWriterImpl.newInstance(ctx, raos);
               processSingleDocument(ctx, parser, nestedWriter, d, fieldNumber);
               nestedWriter.flush();

               byte[] value = raos.toByteArray();
               writer.writeRawBytes(value, 0, value.length);
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

   private static void processMap(ImmutableSerializationContext ctx, MapDescriptor md, JsonParser parser, TagWriter writer) throws IOException {
      OUT:
      while (true) {
         JsonToken token = parser.nextToken();
         if (token == null)
            return;

         switch (token) {
            case END_OBJECT:
               break OUT;

            case FIELD_NAME: {
               ByteArrayOutputStream baos = new ByteArrayOutputStream(ProtobufUtil.DEFAULT_ARRAY_BUFFER_SIZE);
               TagWriter nestedWriter = TagWriterImpl.newInstance(ctx, baos);
               String key = parser.currentName();
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
      }
   }

   private static void processMapValue(ImmutableSerializationContext ctx, JsonParser parser, TagWriter writer, MapDescriptor md) throws IOException {
      JsonToken token = parser.nextToken();
      if (token == null) {
         return;
      }
      switch (token) {
         case START_OBJECT: {
            Descriptor descriptor = md.getMessageType();
            if (descriptor == null) {
               processSingleDocument(ctx, parser, writer, null, 2);
            } else {
               processObject(ctx, parser, writer, descriptor,2, false);
            }
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

   private static void readSingleField(ImmutableSerializationContext ctx, JsonParser parser, TagWriter writer, Descriptor messageDescriptor, String currentField, Integer outerFieldNumber) throws IOException {
      // If we don't have a descriptor, it means we are still ready the `_type` field in the JSON.
      // We assert that. Otherwise, we should have a descriptor already to identify the fields.
      if (messageDescriptor == null) {
         Type type = Type.primitiveFromString(currentField);
         if (type == null) {
            expectField(JSON_TYPE_FIELD, currentField);
         } else {
            processPrimitive(parser, writer, type, outerFieldNumber);
         }
         return;
      }

      // Just double check, if the message descriptor doesn't contain the field, assert it is a `_type`.
      // For example, we might have an enum descriptor, but we are reading the _type field.
      FieldDescriptor fd = messageDescriptor.findFieldByName(currentField);
      if (fd == null) {
         Type type = Type.primitiveFromString(currentField);
         if (type == null) {
            expectField(JSON_TYPE_FIELD, currentField);
         } else {
            // Check whether this primitive belongs to a container.
            // If so, we want to write the primitive directly, without associating it with the field number.
            processPrimitive(parser, writer, type, JsonHelper.isContainerAdapter(ctx, messageDescriptor) ? null : outerFieldNumber);
         }
         return;
      }

      JsonToken token = Objects.requireNonNull(parser.nextToken(), "Field name not followed by value");
      ByteArrayOutputStream baos = null;
      TagWriter out = writer;
      if (outerFieldNumber != null) {
         baos = new ByteArrayOutputStream(ProtobufUtil.DEFAULT_ARRAY_BUFFER_SIZE);
         out = TagWriterImpl.newInstance(ctx, baos);
      }

      switch (token) {
         case VALUE_STRING:
         case VALUE_NUMBER_INT:
         case VALUE_NUMBER_FLOAT:
         case VALUE_TRUE:
         case VALUE_FALSE: {
            if (fd.getType() == Type.ENUM) {
               writeEnumField(parser, out, fd, fd.getNumber());
            } else {
               writeField(parser, out, fd.getType(), fd.getNumber());
            }

            if (outerFieldNumber != null) {
               out.flush();
               writer.writeBytes(outerFieldNumber, baos.toByteArray());
            }

            break;
         }
         case VALUE_NULL:
            // we got null in, we write nothing out
            break;

         default:
            throw new IllegalStateException("Invalid token after field name: " + token);
      }
   }

   private static void processPrimitive(JsonParser parser, TagWriter writer, Type fieldType, Integer fieldId) throws IOException {
      JsonToken token = parser.nextToken();
      if (token == null) {
         return;
      }
      switch (token) {
         case END_OBJECT:
            return;
         case VALUE_STRING:
         case VALUE_NUMBER_INT:
         case VALUE_NUMBER_FLOAT:
         case VALUE_TRUE:
         case VALUE_FALSE:
            writeField(parser, writer, fieldType, fieldId == null ? getPrimitiveFieldId(fieldType) : fieldId);
            break;
         case VALUE_NULL:
            // we got null in, we do not output anything
            break;
         case FIELD_NAME:
            expectField("_value", parser.currentName());
            processPrimitive(parser, writer, fieldType, fieldId);
            break;
         default:
            throw new IllegalStateException("Unexpected JSON token: " + token);
      }
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
      switch (fieldType) {
         // Signed 64 bits values.
         // We retrieve these directly with the parser.
         case INT64 -> writer.writeInt64(fieldNumber, Long.parseLong(parser.getText()));
         case SFIXED64 -> writer.writeSFixed64(fieldNumber, Long.parseLong(parser.getText()));
         case SINT64 -> writer.writeSInt64(fieldNumber, Long.parseLong(parser.getText()));

         // Unsigned 64 bits values.
         // We transform these to string with the Long.toUnsignedString when writing the JSON.
         case UINT64 -> writer.writeUInt64(fieldNumber, Long.parseUnsignedLong(parser.getText()));
         case FIXED64 -> writer.writeFixed64(fieldNumber, Long.parseUnsignedLong(parser.getText()));

         // Signed 32 bits values.
         // We can read these directly from the parser.
         case INT32 -> writer.writeInt32(fieldNumber, Integer.parseInt(parser.getText()));
         case SFIXED32 -> writer.writeSFixed32(fieldNumber, Integer.parseInt(parser.getText()));
         case SINT32 -> writer.writeSInt32(fieldNumber, Integer.parseInt(parser.getText()));

         // Unsigned 32 bits values.
         // The same as the long values, we transform these to strings when writing the JSON.
         case FIXED32 -> writer.writeFixed32(fieldNumber, Integer.parseUnsignedInt(parser.getText()));
         case UINT32 -> writer.writeUInt32(fieldNumber, Integer.parseUnsignedInt(parser.getText()));

         // Basic primitive values now.
         // These can be read directly from the parser without any problems.
         case DOUBLE -> writer.writeDouble(fieldNumber, Double.parseDouble(parser.getText()));
         case FLOAT -> writer.writeFloat(fieldNumber, Float.parseFloat(parser.getText()));
         case BOOL -> writer.writeBool(fieldNumber, parser.getBooleanValue());
         case STRING -> writer.writeString(fieldNumber, parser.getText());
         case BYTES -> writer.writeBytes(fieldNumber, parser.getBinaryValue());

         // Throw for everything else.
         default -> throw new IllegalArgumentException("The Protobuf declared field type is not compatible with the written type : " + fieldType);
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

   private static boolean isInternalFieldAndObject(int fieldNumber) {
      return fieldNumber == WRAPPED_MESSAGE || fieldNumber == WRAPPED_ENUM;
   }

   private static void expectField(String expectedFieldName, String actualFieldName) {
      if (!expectedFieldName.equals(actualFieldName)) {
         throw new IllegalStateException(String.format("Expected field '%s' but it was '%s'", expectedFieldName, actualFieldName));
      }
   }
}
