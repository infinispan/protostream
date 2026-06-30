package org.infinispan.protostream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.Type;
import org.infinispan.protostream.impl.Log;
import org.infinispan.protostream.impl.RandomAccessOutputStreamImpl;
import org.infinispan.protostream.impl.TagWriterImpl;

/**
 * Partially updates protobuf-serialized message bytes at the field level, without requiring Java POJOs.
 * Supports scalar field SET (including nested dotted paths), and collection ADD/REMOVE on repeated fields.
 *
 * @since 6.0
 */
public final class ProtobufFieldUpdater {

   public enum OperationType {
      SET,
      ADD,
      REMOVE
   }

   public record UpdateOperation(OperationType type, String[] propertyPath, List<Object> values) {
      public UpdateOperation {
         Objects.requireNonNull(type);
         Objects.requireNonNull(propertyPath);
         if (propertyPath.length == 0) {
            throw new IllegalArgumentException("propertyPath must not be empty");
         }
         Objects.requireNonNull(values);
      }
   }

   private ProtobufFieldUpdater() {
   }

   /**
    * Apply a list of update operations to the inner message bytes (NOT wrapped in WrappedMessage).
    *
    * @param descriptor the message descriptor
    * @param messageBytes the raw protobuf bytes of the message
    * @param operations the update operations to apply
    * @return updated protobuf bytes
    */
   public static byte[] update(Descriptor descriptor, byte[] messageBytes, List<UpdateOperation> operations) throws IOException {
      Map<Integer, List<Object>> fieldMap = collectFields(descriptor, messageBytes);

      for (UpdateOperation op : operations) {
         applyOperation(descriptor, fieldMap, op.type(), op.propertyPath(), 0, op.values());
      }

      return serializeFields(descriptor, fieldMap);
   }

   private static Map<Integer, List<Object>> collectFields(Descriptor descriptor, byte[] bytes) throws IOException {
      FieldCollector collector = new FieldCollector();
      ProtobufParser.INSTANCE.parse(collector, descriptor, bytes);
      return collector.fields;
   }

   private static void applyOperation(Descriptor descriptor, Map<Integer, List<Object>> fieldMap,
                                       OperationType type, String[] path, int pathIndex, List<Object> values) throws IOException {
      String fieldName = path[pathIndex];
      FieldDescriptor fd = descriptor.findFieldByName(fieldName);
      if (fd == null) {
         throw new IllegalArgumentException("Unknown field '" + fieldName + "' in message " + descriptor.getFullName());
      }
      int fieldNumber = fd.getNumber();
      boolean isLeaf = pathIndex == path.length - 1;

      if (!isLeaf) {
         if (fd.getType() != Type.MESSAGE) {
            throw new IllegalArgumentException("Field '" + fieldName + "' is not a message type, cannot traverse into it");
         }
         List<Object> existing = fieldMap.get(fieldNumber);
         NestedMessage nested;
         if (existing == null || existing.isEmpty()) {
            nested = new NestedMessage(new byte[0], fd);
            fieldMap.computeIfAbsent(fieldNumber, k -> new ArrayList<>()).add(nested);
            existing = fieldMap.get(fieldNumber);
         } else {
            nested = (NestedMessage) existing.get(0);
         }

         Descriptor nestedDescriptor = fd.getMessageType();
         Map<Integer, List<Object>> nestedFieldMap = collectFields(nestedDescriptor, nested.bytes());
         applyOperation(nestedDescriptor, nestedFieldMap, type, path, pathIndex + 1, values);
         byte[] updatedNestedBytes = serializeFields(nestedDescriptor, nestedFieldMap);
         existing.set(0, new NestedMessage(updatedNestedBytes, fd));
         return;
      }

      switch (type) {
         case SET -> {
            if (values.size() == 1 && values.get(0) == null) {
               fieldMap.remove(fieldNumber);
            } else {
               List<Object> converted = new ArrayList<>();
               for (Object v : values) {
                  converted.add(convertValue(fd, v));
               }
               fieldMap.put(fieldNumber, converted);
            }
         }
         case ADD -> {
            List<Object> existing = fieldMap.computeIfAbsent(fieldNumber, k -> new ArrayList<>());
            for (Object v : values) {
               existing.add(convertValue(fd, v));
            }
         }
         case REMOVE -> {
            List<Object> existing = fieldMap.get(fieldNumber);
            if (existing != null) {
               for (Object v : values) {
                  Object converted = convertValue(fd, v);
                  existing.removeIf(e -> Objects.equals(e, converted));
               }
               if (existing.isEmpty()) {
                  fieldMap.remove(fieldNumber);
               }
            }
         }
      }
   }

   private static Object convertValue(FieldDescriptor fd, Object value) {
      if (value == null) return null;
      return switch (fd.getType()) {
         case STRING -> value instanceof String s ? s : value.toString();
         case INT32, SINT32, UINT32, FIXED32, SFIXED32 -> {
            if (value instanceof Number n) yield n.intValue();
            yield Integer.parseInt(value.toString());
         }
         case INT64, SINT64, UINT64, FIXED64, SFIXED64 -> {
            if (value instanceof Number n) yield n.longValue();
            yield Long.parseLong(value.toString());
         }
         case FLOAT -> {
            if (value instanceof Number n) yield n.floatValue();
            yield Float.parseFloat(value.toString());
         }
         case DOUBLE -> {
            if (value instanceof Number n) yield n.doubleValue();
            yield Double.parseDouble(value.toString());
         }
         case BOOL -> {
            if (value instanceof Boolean b) yield b;
            yield Boolean.parseBoolean(value.toString());
         }
         case ENUM -> {
            if (value instanceof Number n) yield n.intValue();
            var enumDescriptor = fd.getEnumType();
            var enumValue = enumDescriptor.findValueByName(value.toString());
            if (enumValue == null) {
               throw new IllegalArgumentException("Unknown enum value '" + value + "' for enum " + enumDescriptor.getFullName());
            }
            yield enumValue.getNumber();
         }
         case BYTES -> {
            if (value instanceof byte[] b) yield b;
            throw new IllegalArgumentException("Cannot convert " + value.getClass() + " to bytes");
         }
         default -> value;
      };
   }

   private static byte[] serializeFields(Descriptor descriptor, Map<Integer, List<Object>> fieldMap) throws IOException {
      RandomAccessOutputStream baos = new RandomAccessOutputStreamImpl(128);
      TagWriter writer = TagWriterImpl.newInstance(null, baos);

      for (var entry : fieldMap.entrySet()) {
         int fieldNumber = entry.getKey();
         List<Object> values = entry.getValue();
         FieldDescriptor fd = descriptor != null ? descriptor.findFieldByNumber(fieldNumber) : null;

         for (Object value : values) {
            if (value instanceof NestedMessage nested) {
               writer.writeBytes(fieldNumber, nested.bytes());
            } else {
               writeField(writer, fieldNumber, fd, value);
            }
         }
      }

      writer.flush();
      return baos.toByteArray();
   }

   private static void writeField(TagWriter writer, int fieldNumber, FieldDescriptor fd, Object value) throws IOException {
      if (fd == null) {
         throw Log.LOG.unknownField(fieldNumber);
      }

      switch (fd.getType()) {
         case STRING -> writer.writeString(fieldNumber, (String) value);
         case INT32 -> writer.writeInt32(fieldNumber, ((Number) value).intValue());
         case INT64 -> writer.writeInt64(fieldNumber, ((Number) value).longValue());
         case UINT32 -> writer.writeUInt32(fieldNumber, ((Number) value).intValue());
         case UINT64 -> writer.writeUInt64(fieldNumber, ((Number) value).longValue());
         case SINT32 -> writer.writeSInt32(fieldNumber, ((Number) value).intValue());
         case SINT64 -> writer.writeSInt64(fieldNumber, ((Number) value).longValue());
         case FIXED32 -> writer.writeFixed32(fieldNumber, ((Number) value).intValue());
         case FIXED64 -> writer.writeFixed64(fieldNumber, ((Number) value).longValue());
         case SFIXED32 -> writer.writeSFixed32(fieldNumber, ((Number) value).intValue());
         case SFIXED64 -> writer.writeSFixed64(fieldNumber, ((Number) value).longValue());
         case FLOAT -> writer.writeFloat(fieldNumber, ((Number) value).floatValue());
         case DOUBLE -> writer.writeDouble(fieldNumber, ((Number) value).doubleValue());
         case BOOL -> writer.writeBool(fieldNumber, (Boolean) value);
         case ENUM -> writer.writeEnum(fieldNumber, ((Number) value).intValue());
         case BYTES -> writer.writeBytes(fieldNumber, (byte[]) value);
         default -> throw new IOException("Unsupported field type: " + fd.getType());
      }
   }

   private record NestedMessage(byte[] bytes, FieldDescriptor fieldDescriptor) {
   }

   private static class FieldCollector implements TagHandler {
      final Map<Integer, List<Object>> fields = new LinkedHashMap<>();
      private int depth = 0;
      private int nestedFieldNumber = -1;
      private FieldCollector nestedCollector;

      @Override
      public void onTag(int fieldNumber, FieldDescriptor fieldDescriptor, Object tagValue) {
         if (depth > 0 && nestedCollector != null) {
            nestedCollector.onTag(fieldNumber, fieldDescriptor, tagValue);
            return;
         }
         fields.computeIfAbsent(fieldNumber, k -> new ArrayList<>()).add(tagValue);
      }

      @Override
      public void onStartNested(int fieldNumber, FieldDescriptor fieldDescriptor) {
         if (depth > 0 && nestedCollector != null) {
            nestedCollector.onStartNested(fieldNumber, fieldDescriptor);
            depth++;
            return;
         }
         depth++;
         nestedFieldNumber = fieldNumber;
         nestedCollector = new FieldCollector();
      }

      @Override
      public void onEndNested(int fieldNumber, FieldDescriptor fieldDescriptor) {
         depth--;
         if (depth > 0 && nestedCollector != null) {
            nestedCollector.onEndNested(fieldNumber, fieldDescriptor);
            return;
         }
         if (nestedCollector != null) {
            byte[] nestedBytes;
            try {
               Descriptor nestedDescriptor = fieldDescriptor != null ? fieldDescriptor.getMessageType() : null;
               nestedBytes = serializeFields(nestedDescriptor, nestedCollector.fields);
            } catch (IOException e) {
               throw new RuntimeException(e);
            }
            fields.computeIfAbsent(nestedFieldNumber, k -> new ArrayList<>()).add(new NestedMessage(nestedBytes, fieldDescriptor));
            nestedCollector = null;
            nestedFieldNumber = -1;
         }
      }
   }
}
