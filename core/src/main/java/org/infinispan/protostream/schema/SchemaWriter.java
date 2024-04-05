package org.infinispan.protostream.schema;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @since 5.0
 */
class SchemaWriter {
   private final int indent;
   private int offset;

   SchemaWriter(int indent) {
      this.indent = indent;
   }

   SchemaWriter() {
      this(2);
   }

   public void write(Writer w, Schema s) throws IOException {
      w.write("syntax = \"");
      w.write(s.getSyntax().toString());
      w.write("\";\n");
      if (s.getPackageName() != null) {
         w.write("package ");
         w.write(s.getPackageName());
         w.write(";\n");
      }

      for (String d : s.getDependencies()) {
         w.write("import \"");
         w.write(d);
         w.write("\";\n");
      }
      for (String d : s.getPublicDependencies()) {
         w.write("import public \"");
         w.write(d);
         w.write("\";\n");
      }
      writeOptions(w, s.getOptions(), false);
      writeEnums(w, s.getEnums());
      writeMessages(w, s.getMessages());
   }

   private void writeComments(Writer w, Collection<String> comments) throws IOException {
      if (!comments.isEmpty()) {
         writeOffset(w);
         w.write("/*\n");
         for (String c : comments) {
            writeOffset(w);
            w.write(" * ");
            w.write(c);
            w.write("\n");
         }
         writeOffset(w);
         w.write(" */\n");
      }
   }

   private void writeOffset(Writer w) throws IOException {
      w.write(" ".repeat(offset));
   }

   private void writeMessages(Writer w, Collection<Message> messages) throws IOException {
      for (Message m : messages) {
         writeComments(w, m.getComments());
         writeOffset(w);
         w.write("message ");
         w.write(m.getName());
         w.write(" {\n");
         indent();
         writeOptions(w, m.getOptions(), false);
         writeEnums(w, m.getNestedEnums().values());
         writeMessages(w, m.getNestedMessages().values());
         writeFields(w, m.getFields());
         writeOneOfs(w, m.getOneOfs());
         writeReserved(w, m.getReservedNumbers());
         writeReserved(w, m.getReservedNames());
         outdent();
         writeOffset(w);
         w.write("}\n");
      }
   }

   private void writeReserved(Writer w, Set<String> reservedNames) throws IOException {
      if (!reservedNames.isEmpty()) {
         writeOffset(w);
         w.write("reserved ");
         w.write(reservedNames.stream().collect(Collectors.joining("\", \"", "\"", "\"")));
         w.write(";\n");
      }
   }

   private void writeReserved(Writer w, ReservedNumbers reservedNumbers) throws IOException {
      if (!reservedNumbers.isEmpty()) {
         writeOffset(w);
         w.write("reserved ");
         for (int i = reservedNumbers.nextSetBit(0); i >= 0; ) {
            w.write(Integer.toString(i));
            var j = reservedNumbers.nextSetBit(i + 1);
            if (j < 0) {
               break;
            } else if (j == i + 1) {
               j = reservedNumbers.nextClearBit(i + 1);
               w.write(" to ");
               w.write(Integer.toString(j - 1));
               i = reservedNumbers.nextSetBit(j);
               if (i > 0) {
                  w.write(", ");
               }
            } else {
               w.write(", ");
               i = j;
            }
         }
         w.write(";\n");
      }
   }

   private void writeOneOfs(Writer w, List<OneOf> oneOfs) throws IOException {
      for (OneOf o : oneOfs) {
         writeComments(w, o.getComments());
         writeOffset(w);
         w.write("oneof ");
         w.write(o.getName());
         w.write(" {\n");
         indent();
         writeFields(w, o.getFields());
         outdent();
         writeOffset(w);
         w.write("}\n");
      }
   }

   private void writeFields(Writer w, Map<String, Field> fields) throws IOException {
      for (Field f : fields.values()) {
         writeComments(w, f.getComments());
         writeOffset(w);
         if (f instanceof org.infinispan.protostream.schema.Map m) {
            w.write("map<");
            w.write(m.getType().toString());
            w.write(", ");
            w.write(m.getValueType().toString());
            w.write("> ");
         } else if (f.isRepeated()) {
            w.write("repeated ");
            w.write(f.getType().toString());
            w.write(" ");
         } else {
            w.write(f.getType().toString());
            w.write(" ");
         }
         w.write(f.getName());
         w.write(" = ");
         w.write(Integer.toString(f.getNumber()));
         w.write(";\n");
      }
   }

   private void writeEnums(Writer w, Collection<Enum> enums) throws IOException {
      for (Enum e : enums) {
         writeComments(w, e.getComments());
         writeOffset(w);
         w.write("enum ");
         w.write(e.getName());
         w.write(" {\n");
         indent();
         writeOptions(w, e.getOptions(), false);
         for (EnumValue v : e.getValues().values()) {
            writeComments(w, v.getComments());
            writeOffset(w);
            w.write(v.getName());
            w.write(" = ");
            w.write(Integer.toString(v.getNumber()));
            writeOptions(w, v.getOptions(), true);
            w.write(";\n");
         }
         writeReserved(w, e.getReservedNumbers());
         writeReserved(w, e.getReservedNames());
         outdent();
         w.write("};\n");
      }
   }

   private void writeOptions(Writer w, Map<String, Object> options, boolean single) throws IOException {
      if (options.isEmpty()) {
         return;
      }
      if (single) {
         w.write(" [");
      }
      for (Iterator<Map.Entry<String, Object>> it = options.entrySet().iterator(); it.hasNext(); ) {
         Map.Entry<String, Object> o = it.next();
         if (!single) {
            writeOffset(w);
            w.write("option ");
         }
         w.write(o.getKey());
         w.write(" = ");
         if (o.getValue() instanceof String) {
            w.write("\"");
            w.write(o.getValue().toString());
            w.write("\"");
         } else {
            w.write(o.getValue().toString());
         }
         if (single) {
            if (it.hasNext()) {
               w.write(", ");
            }
         } else {
            w.write(";\n");
         }
      }
      if (single) {
         w.write("]");
      }
   }

   private void indent() {
      offset += indent;
   }

   private void outdent() {
      offset -= indent;
   }
}
