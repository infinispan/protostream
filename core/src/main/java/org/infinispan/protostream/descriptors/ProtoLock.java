package org.infinispan.protostream.descriptors;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.impl.Log;
import org.infinispan.protostream.impl.json.JsonTree;
import org.infinispan.protostream.impl.json.PrettyJsonWriter;

/**
 * Reads/writes <a href="https://github.com/nilslice/protolock">protolock</a> files.
 */
public class ProtoLock {
   private final Map<String, FileDescriptor> descriptors;

   public ProtoLock(Collection<FileDescriptor> descriptors) {
      this.descriptors = descriptors.stream().collect(Collectors.toUnmodifiableMap(FileDescriptor::getName, Function.identity()));

   }

   public Map<String, FileDescriptor> descriptors() {
      return descriptors;
   }

   /**
    * Checks for compatibility between all the descriptors in this ProtoLock instance against those in the supplied one
    *
    * @param that
    * @param strict
    */
   public void checkCompatibility(ProtoLock that, boolean strict) {
      List<String> errors = new ArrayList<>();
      for (Map.Entry<String, FileDescriptor> descriptor : descriptors.entrySet()) {
         FileDescriptor d2 = that.descriptors.get(descriptor.getKey());
         if (d2 != null) {
            descriptor.getValue().checkCompatibility(d2, strict, errors);
         }
      }
      if (!errors.isEmpty()) {
         throw Log.LOG.incompatibleSchemaChanges(String.join("\n", errors));
      }
   }

   /**
    * Reads a proto.lock file. The stream is read to the end but not closed: the caller owns it.
    *
    * @throws IOException if the stream cannot be read, is not valid JSON or is not a valid proto.lock file
    */
   public static ProtoLock readLockFile(InputStream is) throws IOException {
      return readLockFile(is, Configuration.builder().build());
   }

   /**
    * Reads a proto.lock file. The stream is read to the end but not closed: the caller owns it.
    *
    * @throws IOException if the stream cannot be read, is not valid JSON or is not a valid proto.lock file
    */
   public static ProtoLock readLockFile(InputStream is, Configuration configuration) throws IOException {
      // A strict decoder: malformed UTF-8 fails instead of being replaced
      JsonTree tree = JsonTree.parse(new InputStreamReader(is, StandardCharsets.UTF_8.newDecoder()));
      List<FileDescriptor> descriptors = new ArrayList<>();
      JsonTree definitions = required(tree, "definitions", "the root object");
      String packageName = "";
      for (int i = 0; i < definitions.size(); i++) {
         JsonTree definition = definitions.get(i);
         FileDescriptor.Builder fdb = new FileDescriptor.Builder();
         String path = unescapePath(required(definition, "protopath", "definition " + i).asText());
         fdb.withName(path);
         String where = "'" + path + "'";
         JsonTree def = required(definition, "def", "definition " + where);
         if (def.has("package")) {
            packageName = required(def.get("package"), "name", "the package of " + where).asText();
            fdb.withPackageName(packageName);
         }
         if (def.has("imports")) {
            for (JsonTree n : def.get("imports")) {
               fdb.addDependency(required(n, "path", "an import of " + where).asText());
            }
         }
         readOptions(def, fdb, where);
         Map<String, Descriptor.Builder> messageBuilders = readMessages(def, packageName.isEmpty() ? "" : packageName + ".", where);
         if (def.has("enums")) {
            JsonTree enums = def.get("enums");
            for (int e = 0; e < enums.size(); e++) {
               JsonTree en = enums.get(e);
               EnumDescriptor.Builder eb = new EnumDescriptor.Builder();
               String enumName = required(en, "name", "an enum of " + where).asText();
               String enumWhere = "enum '" + enumName + "' of " + where;
               if (en.has("type_id")) {
                  eb.withDocumentation("@TypeId(" + en.get("type_id").asText() + ")");
               }
               JsonTree enumFields = required(en, "enum_fields", enumWhere);
               for (int v = 0; v < enumFields.size(); v++) {
                  JsonTree enumField = enumFields.get(v);
                  EnumValueDescriptor.Builder evb = new EnumValueDescriptor.Builder();
                  evb.withName(required(enumField, "name", "a value of " + enumWhere).asText());
                  if (enumField.has("integer")) {
                     evb.withTag(enumField.get("integer").asInt());
                  }
                  readOptions(enumField, evb, enumWhere);
                  eb.addValue(evb);
               }
               if (en.has("reserved_ids")) {
                  JsonTree reservedIds = en.get("reserved_ids");
                  reservedIds.forEach(n -> eb.addReserved(n.asInt()));
               }
               if (en.has("reserved_names")) {
                  JsonTree reservedNames = en.get("reserved_names");
                  reservedNames.forEach(n -> eb.addReserved(n.asText()));
               }
               readOptions(en, eb, enumWhere);
               int dot = enumName.lastIndexOf('.');
               if (dot > 0) {
                  eb.withName(enumName.substring(dot + 1));
                  Descriptor.Builder mb = messageBuilders.get(enumName.substring(0, dot));
                  if (mb == null) {
                     throw invalid("unknown message '" + enumName.substring(0, dot) + "' containing " + enumWhere);
                  }
                  mb.addEnum(eb);
               } else {
                  eb.withName(enumName);
                  fdb.addEnum(eb);
               }
            }
         }
         messageBuilders.values().forEach(fdb::addMessage);
         FileDescriptor fd = fdb.build();
         fd.setConfiguration(configuration);
         fd.parseAnnotations();
         descriptors.add(fd);
      }
      return new ProtoLock(descriptors);
   }

   private static Map<String, Descriptor.Builder> readMessages(JsonTree json, String namePrefix, String where) throws IOException {
      if (json.has("messages")) {
         Map<String, Descriptor.Builder> messageBuilders = new LinkedHashMap<>();
         JsonTree messages = json.get("messages");
         for (int m = 0; m < messages.size(); m++) {
            JsonTree message = messages.get(m);
            String name = required(message, "name", "a message of " + where).asText();
            String messageWhere = "message '" + namePrefix + name + "' of " + where;
            Descriptor.Builder mb = messageBuilders.computeIfAbsent(name, n -> new Descriptor.Builder().withName(n).withFullName(namePrefix + n));
            if (message.has("type_id")) {
               mb.withDocumentation("@TypeId(" + message.get("type_id").asText() + ")");
            }
            if (message.has("fields")) {
               JsonTree fields = message.get("fields");
               Map<String, OneOfDescriptor.Builder> oneOfBuilders = new LinkedHashMap<>();
               for (int f = 0; f < fields.size(); f++) {
                  JsonTree field = fields.get(f);
                  FieldDescriptor.Builder fb = new FieldDescriptor.Builder();
                  String fieldName = required(field, "name", "a field of " + messageWhere).asText();
                  String fieldWhere = "field '" + fieldName + "' of " + messageWhere;
                  fb.withName(fieldName);
                  fb.withNumber(required(field, "id", fieldWhere).asInt());
                  fb.withTypeName(required(field, "type", fieldWhere).asText());
                  if (field.has("is_repeated")) {
                     fb.withLabel(Label.REPEATED);
                  }
                  if (field.has("optional")) {
                     fb.withLabel(Label.OPTIONAL);
                  }
                  readOptions(field, fb, fieldWhere);
                  if (field.has("oneof_parent")) {
                     String oneOf = field.get("oneof_parent").asText();
                     fb.withLabel(Label.ONE_OF);
                     oneOfBuilders.computeIfAbsent(oneOf, n -> new OneOfDescriptor.Builder().withName(n)).addField(fb);
                  } else {
                     mb.addField(fb);
                  }
               }
               oneOfBuilders.values().forEach(mb::addOneOf);
            }
            if (message.has("maps")) {
               JsonTree maps = message.get("maps");
               for (int f = 0; f < maps.size(); f++) {
                  JsonTree map = maps.get(f);
                  MapDescriptor.Builder fb = new MapDescriptor.Builder();
                  fb.withKeyTypeName(required(map, "key_type", "a map of " + messageWhere).asText());
                  map = required(map, "field", "a map of " + messageWhere);
                  String mapName = required(map, "name", "a map of " + messageWhere).asText();
                  String mapWhere = "map '" + mapName + "' of " + messageWhere;
                  fb.withName(mapName);
                  fb.withNumber(required(map, "id", mapWhere).asInt());
                  fb.withValueTypeName(required(map, "type", mapWhere).asText());
                  readOptions(map, fb, mapWhere);
                  mb.addMap(fb);
               }
            }
            if (message.has("reserved_ids")) {
               JsonTree reservedIds = message.get("reserved_ids");
               reservedIds.forEach(n -> mb.addReserved(n.asInt()));

            }
            if (message.has("reserved_names")) {
               JsonTree reservedNames = message.get("reserved_names");
               reservedNames.forEach(n -> mb.addReserved(n.asText()));
            }
            readOptions(message, mb, messageWhere);
            Map<String, Descriptor.Builder> nested = readMessages(message, namePrefix + name + ".", where);
            nested.values().forEach(mb::addMessage);
         }
         return messageBuilders;
      } else {
         return Collections.emptyMap();
      }
   }

   private static void readOptions(JsonTree json, OptionContainer<?> container, String where) throws IOException {
      if (json.has("options")) {
         for (JsonTree n : json.get("options")) {
            String name = required(n, "name", "an option of " + where).asText();
            container.addOption(new Option(name, required(n, "value", "option '" + name + "' of " + where).asText()));
         }
      }
   }

   /**
    * @return the value of a field that every proto.lock file has
    * @throws IOException if the field is missing
    */
   private static JsonTree required(JsonTree json, String name, String where) throws IOException {
      JsonTree value = json == null ? null : json.get(name);
      if (value == null) {
         throw invalid("missing '" + name + "' in " + where);
      }
      return value;
   }

   private static IOException invalid(String message) {
      return new IOException("Invalid proto.lock file: " + message);
   }

   /**
    * Write a proto.lock file
    *
    * @param os the {@link OutputStream} to write to
    */
   public void writeLockFile(OutputStream os) throws IOException {
      PrettyJsonWriter j = new PrettyJsonWriter(os);
      j.writeStartObject();
      j.writeFieldName("definitions");
      j.writeStartArray();
      // Sort by path: the descriptors map has no stable iteration order, and lock files are committed to repositories
      for (Map.Entry<String, FileDescriptor> entry : new TreeMap<>(descriptors).entrySet()) {
         FileDescriptor fd = entry.getValue();
         j.writeStartObject();
         j.writeStringField("protopath", escapePath(entry.getKey()));
         j.writeFieldName("def");
         j.writeStartObject();
         j.writeFieldName("enums");
         j.writeStartArray();
         for (EnumDescriptor ed : fd.getEnumTypes()) {
            writeEnum(j, ed);
         }
         writeMessageEnums(j, fd.getMessageTypes());
         j.writeEndArray(); //enums
         if (!fd.getMessageTypes().isEmpty()) {
            j.writeFieldName("messages");
            j.writeStartArray();
            for (Descriptor md : fd.getMessageTypes()) {
               writeMessage(j, md);
            }
            j.writeEndArray(); // messages
         }
         if (!fd.getDependencies().isEmpty()) {
            j.writeFieldName("imports");
            j.writeStartArray();
            for (String path : fd.getDependencies()) {
               j.writeStartObject();
               j.writeStringField("path", path);
               j.writeEndObject();
            }
            j.writeEndArray(); // imports
         }
         if (fd.getPackage() != null) {
            j.writeFieldName("package");
            j.writeStartObject();
            j.writeStringField("name", fd.getPackage());
            j.writeEndObject(); // package
         }
         writeOptions(j, fd.getOptions());
         j.writeEndObject();  // def
         j.writeEndObject(); // definitions
      }
      j.writeEndArray();
      j.writeEndObject();
      j.flush();
   }

   private static void writeMessageEnums(PrettyJsonWriter j, List<Descriptor> mds) throws IOException {
      for (Descriptor md : mds) {
         for (EnumDescriptor ed : md.getEnumTypes()) {
            writeEnum(j, ed);
         }
         writeMessageEnums(j, md.getNestedTypes());
      }
   }

   private static void writeMessage(PrettyJsonWriter j, Descriptor md) throws IOException {
      boolean hasMaps = false;
      j.writeStartObject();
      j.writeStringField("name", md.getName());
      if (md.getTypeId() != null) {
         j.writeNumberField("type_id", md.getTypeId());
      }
      j.writeFieldName("fields");
      j.writeStartArray();
      for (OneOfDescriptor o : md.getOneOfs()) {
         for (FieldDescriptor f : o.getFields()) {
            writeField(j, f, o.getName());
         }
      }
      for (FieldDescriptor f : md.getFields()) {
         if (f instanceof MapDescriptor) {
            hasMaps = true;
         } else {
            writeField(j, f, null);
         }
      }
      j.writeEndArray(); // fields

      if (hasMaps) {
         j.writeFieldName("maps");
         j.writeStartArray();
         for (FieldDescriptor f : md.getFields()) {
            if (f instanceof MapDescriptor m) {
               j.writeStartObject();
               j.writeStringField("key_type", m.getKeyTypeName());
               j.writeFieldName("field");
               j.writeStartObject();
               j.writeNumberField("id", m.getNumber());
               j.writeStringField("name", m.getName());
               j.writeStringField("type", m.getTypeName());
               writeOptions(j, f.getOptions());
               j.writeEndObject();
               j.writeEndObject();
            }
         }
         j.writeEndArray();
      }
      writeReserved(j, md);
      if (!md.getNestedTypes().isEmpty()) {
         j.writeFieldName("messages");
         j.writeStartArray();
         for (Descriptor nm : md.getNestedTypes()) {
            writeMessage(j, nm);
         }
         j.writeEndArray();
      }
      j.writeEndObject();
   }

   private static void writeEnum(PrettyJsonWriter j, EnumDescriptor ed) throws IOException {
      j.writeStartObject();
      if (ed.getContainingType() != null) {
         j.writeStringField("name", ed.getContainingType().getName() + "." + ed.getName());
      } else {
         j.writeStringField("name", ed.getName());
      }
      if (ed.getTypeId() != null) {
         j.writeNumberField("type_id", ed.getTypeId());
      }
      j.writeFieldName("enum_fields");
      j.writeStartArray();
      for (EnumValueDescriptor ev : ed.getValues()) {
         j.writeStartObject();
         j.writeStringField("name", ev.getName());
         if (ev.getNumber() > 0) {
            j.writeNumberField("integer", ev.getNumber());
         }
         writeOptions(j, ev.getOptions());
         j.writeEndObject();
      }
      j.writeEndArray();
      writeReserved(j, ed);
      writeOptions(j, ed.getOptions());
      j.writeEndObject();
   }

   private static void writeReserved(PrettyJsonWriter j, ReservableDescriptor reservable) throws IOException {
      if (!reservable.getReservedNumbers().isEmpty()) {
         j.writeFieldName("reserved_ids");
         j.writeStartArray();
         for (Long l : reservable.getReservedNumbers()) {
            j.writeNumber(l);
         }
         j.writeEndArray();
      }
      if (!reservable.getReservedNames().isEmpty()) {
         j.writeFieldName("reserved_names");
         j.writeStartArray();
         // Sort: reserved names are held in a Set with no stable iteration order
         for (String n : new TreeSet<>(reservable.getReservedNames())) {
            j.writeString(n);
         }
         j.writeEndArray();
      }
   }

   private static void writeField(PrettyJsonWriter j, FieldDescriptor f, String parent) throws IOException {
      j.writeStartObject();
      j.writeNumberField("id", f.getNumber());
      j.writeStringField("name", f.getName());
      j.writeStringField("type", f.getTypeName());
      if (f.isRepeated()) {
         j.writeBooleanField("is_repeated", true);
      }
      if (f.getLabel() == Label.OPTIONAL) {
         j.writeBooleanField("optional", true);
      }
      if (parent != null) {
         j.writeStringField("oneof_parent", parent);
      }
      writeOptions(j, f.getOptions());
      j.writeEndObject();
   }

   private static void writeOptions(PrettyJsonWriter j, List<Option> options) throws IOException {
      if (!options.isEmpty()) {
         j.writeFieldName("options");
         j.writeStartArray();
         for (Option o : options) {
            j.writeStartObject();
            j.writeStringField("name", o.getName());
            j.writeStringField("value", o.getValue().toString());
            j.writeEndObject();
         }
         j.writeEndArray();
      }
   }

   private static String escapePath(String path) {
      return path.replace("/", ":/:");
   }

   private static String unescapePath(String escaped) {
      return escaped.replace(":/:", "/");
   }
}
