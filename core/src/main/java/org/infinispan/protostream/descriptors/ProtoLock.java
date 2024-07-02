package org.infinispan.protostream.descriptors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.impl.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

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

   public static ProtoLock readLockFile(InputStream is) throws IOException {
      return readLockFile(is, Configuration.builder().build());
   }

   public static ProtoLock readLockFile(InputStream is, Configuration configuration) throws IOException {
      JsonFactory jsonFactory = new JsonFactory();
      jsonFactory.setCodec(new ObjectMapper());
      JsonParser json = jsonFactory.createParser(is);
      TreeNode tree = json.getCodec().readTree(json);
      List<FileDescriptor> descriptors = new ArrayList<>();
      ArrayNode definitions = (ArrayNode) tree.get("definitions");
      String packageName = "";
      for (int i = 0; i < definitions.size(); i++) {
         JsonNode definition = definitions.get(i);
         FileDescriptor.Builder fdb = new FileDescriptor.Builder();
         fdb.withName(unescapePath(definition.get("protopath").asText()));
         JsonNode def = definition.get("def");
         if (def.has("package")) {
            packageName = def.get("package").get("name").asText();
            fdb.withPackageName(packageName);
         }
         if (def.has("imports")) {
            def.get("imports").forEach(n -> fdb.addDependency(n.get("path").asText()));
         }
         readOptions(def, fdb);
         Map<String, Descriptor.Builder> messageBuilders = readMessages(def, packageName.isEmpty() ? "" : packageName + ".");
         if (def.has("enums")) {
            ArrayNode enums = (ArrayNode) def.get("enums");
            for (int e = 0; e < enums.size(); e++) {
               JsonNode en = enums.get(e);
               EnumDescriptor.Builder eb = new EnumDescriptor.Builder();
               if (en.has("type_id")) {
                  eb.withDocumentation("@TypeId(" + en.get("type_id").asText() + ")");
               }
               ArrayNode enumFields = (ArrayNode) en.get("enum_fields");
               for (int v = 0; v < enumFields.size(); v++) {
                  JsonNode enumField = enumFields.get(v);
                  EnumValueDescriptor.Builder evb = new EnumValueDescriptor.Builder();
                  evb.withName(enumField.get("name").asText());
                  if (enumField.has("integer")) {
                     evb.withTag(enumField.get("integer").asInt());
                  }
                  readOptions(enumField, evb);
                  eb.addValue(evb);
               }
               if (en.has("reserved_ids")) {
                  ArrayNode reservedIds = (ArrayNode) en.get("reserved_ids");
                  reservedIds.forEach(n -> eb.addReserved(n.asInt()));
               }
               if (en.has("reserved_names")) {
                  ArrayNode reservedNames = (ArrayNode) en.get("reserved_names");
                  reservedNames.forEach(n -> eb.addReserved(n.asText()));
               }
               readOptions(en, eb);
               String enumName = en.get("name").asText();
               int dot = enumName.lastIndexOf('.');
               if (dot > 0) {
                  eb.withName(enumName.substring(dot + 1));
                  Descriptor.Builder mb = messageBuilders.get(enumName.substring(0, dot));
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

   private static Map<String, Descriptor.Builder> readMessages(JsonNode json, String namePrefix) {
      if (json.has("messages")) {
         Map<String, Descriptor.Builder> messageBuilders = new HashMap<>();
         ArrayNode messages = (ArrayNode) json.get("messages");
         for (int m = 0; m < messages.size(); m++) {
            JsonNode message = messages.get(m);
            String name = message.get("name").asText();
            Descriptor.Builder mb = messageBuilders.computeIfAbsent(name, n -> new Descriptor.Builder().withName(n).withFullName(namePrefix + n));
            if (message.has("type_id")) {
               mb.withDocumentation("@TypeId(" + message.get("type_id").asText() + ")");
            }
            if (message.has("fields")) {
               ArrayNode fields = (ArrayNode) message.get("fields");
               Map<String, OneOfDescriptor.Builder> oneOfBuilders = new HashMap<>();
               for (int f = 0; f < fields.size(); f++) {
                  JsonNode field = fields.get(f);
                  FieldDescriptor.Builder fb = new FieldDescriptor.Builder();
                  fb.withName(field.get("name").asText());
                  fb.withNumber(field.get("id").asInt());
                  fb.withTypeName(field.get("type").asText());
                  if (field.has("is_repeated")) {
                     fb.withLabel(Label.REPEATED);
                  }
                  if (field.has("optional")) {
                     fb.withLabel(Label.OPTIONAL);
                  }
                  readOptions(field, fb);
                  if (message.has("oneof_parent")) {
                     String oneOf = message.get("oneof_parent").asText();
                     oneOfBuilders.computeIfAbsent(oneOf, n -> new OneOfDescriptor.Builder().withName(n)).addField(fb);
                  } else {
                     mb.addField(fb);
                  }
               }
            }
            if (message.has("maps")) {
               ArrayNode maps = (ArrayNode) message.get("maps");
               for (int f = 0; f < maps.size(); f++) {
                  JsonNode map = maps.get(f);
                  MapDescriptor.Builder fb = new MapDescriptor.Builder();
                  fb.withKeyTypeName(map.get("key_type").asText());
                  map = map.get("field");
                  fb.withName(map.get("name").asText());
                  fb.withNumber(map.get("id").asInt());
                  fb.withValueTypeName(map.get("type").asText());
                  readOptions(map, fb);
                  mb.addMap(fb);
               }
            }
            if (message.has("reserved_ids")) {
               ArrayNode reservedIds = (ArrayNode) message.get("reserved_ids");
               reservedIds.forEach(n -> mb.addReserved(n.asInt()));

            }
            if (message.has("reserved_names")) {
               ArrayNode reservedNames = (ArrayNode) message.get("reserved_names");
               reservedNames.forEach(n -> mb.addReserved(n.asText()));
            }
            readOptions(message, mb);
            Map<String, Descriptor.Builder> nested = readMessages(message, namePrefix + name + ".");
            nested.values().forEach(mb::addMessage);
         }
         return messageBuilders;
      } else {
         return Collections.emptyMap();
      }
   }

   private static void readOptions(JsonNode json, OptionContainer<?> container) {
      if (json.has("options")) {
         json.get("options").forEach(n -> container.addOption(new Option(n.get("name").asText(), n.get("value").asText())));
      }
   }

   /**
    * Write a proto.lock file
    *
    * @param os the {@link OutputStream} to write to
    */
   public void writeLockFile(OutputStream os) throws IOException {
      JsonFactory jsonFactory = new JsonFactory();
      JsonGenerator j = jsonFactory.createGenerator(os);
      j.setPrettyPrinter(new DefaultPrettyPrinter());
      j.writeStartObject();
      j.writeFieldName("definitions");
      j.writeStartArray();
      for (Map.Entry<String, FileDescriptor> entry : descriptors.entrySet()) {
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

   private static void writeMessageEnums(JsonGenerator j, List<Descriptor> mds) throws IOException {
      for (Descriptor md : mds) {
         for (EnumDescriptor ed : md.getEnumTypes()) {
            writeEnum(j, ed);
         }
         writeMessageEnums(j, md.getNestedTypes());
      }
   }

   private static void writeMessage(JsonGenerator j, Descriptor md) throws IOException {
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

   private static void writeEnum(JsonGenerator j, EnumDescriptor ed) throws IOException {
      j.writeStartObject();
      if (ed.getContainingType() != null) {
         j.writeStringField("name", ed.getContainingType().getName() + "." + ed.getName());
      } else {
         j.writeStringField("name", ed.getName());
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

   private static void writeReserved(JsonGenerator j, ReservableDescriptor reservable) throws IOException {
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
         for (String n : reservable.getReservedNames()) {
            j.writeString(n);
         }
         j.writeEndArray();
      }
   }

   private static void writeField(JsonGenerator j, FieldDescriptor f, String parent) throws IOException {
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

   private static void writeOptions(JsonGenerator j, List<Option> options) throws IOException {
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
