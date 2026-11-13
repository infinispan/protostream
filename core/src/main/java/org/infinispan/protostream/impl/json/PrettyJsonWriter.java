package org.infinispan.protostream.impl.json;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A minimal streaming JSON writer with pretty-printing, used to write <code>proto.lock</code> files.
 * <p>
 * The layout matches the default pretty printer of the JSON library previously used for these files, so existing lock
 * files are not reformatted: object fields on separate lines indented by two spaces per object level, <code>" : "</code>
 * between names and values, arrays kept inline (<code>[ a, b ]</code>), and <code>{ }</code> / <code>[ ]</code> for empty
 * containers. Lines always end with <code>\n</code>, whatever the platform.
 * <p>
 * This writer does not validate the call sequence: callers must write a well-formed document.
 */
public final class PrettyJsonWriter {

   private static final char[] HEX = "0123456789ABCDEF".toCharArray();

   private final Writer out;
   private final Deque<Container> stack = new ArrayDeque<>();
   private int objectDepth;

   public PrettyJsonWriter(OutputStream os) {
      this.out = new OutputStreamWriter(os, StandardCharsets.UTF_8);
   }

   public void writeStartObject() throws IOException {
      beforeValue();
      out.write('{');
      stack.push(new Container(true));
      objectDepth++;
   }

   public void writeEndObject() throws IOException {
      Container object = stack.pop();
      objectDepth--;
      if (object.entries > 0) {
         newLine();
      } else {
         out.write(' ');
      }
      out.write('}');
   }

   public void writeStartArray() throws IOException {
      beforeValue();
      out.write('[');
      stack.push(new Container(false));
   }

   public void writeEndArray() throws IOException {
      stack.pop();
      out.write(" ]");
   }

   public void writeFieldName(String name) throws IOException {
      Container object = stack.peek();
      if (object.entries++ > 0) {
         out.write(',');
      }
      newLine();
      writeQuoted(name);
      out.write(" : ");
   }

   public void writeString(String value) throws IOException {
      beforeValue();
      writeQuoted(value);
   }

   public void writeNumber(long value) throws IOException {
      beforeValue();
      out.write(Long.toString(value));
   }

   public void writeStringField(String name, String value) throws IOException {
      writeFieldName(name);
      writeString(value);
   }

   public void writeNumberField(String name, long value) throws IOException {
      writeFieldName(name);
      writeNumber(value);
   }

   public void writeBooleanField(String name, boolean value) throws IOException {
      writeFieldName(name);
      out.write(value ? "true" : "false");
   }

   /**
    * Flushes buffered output to the underlying stream, without closing it.
    */
   public void flush() throws IOException {
      out.flush();
   }

   /**
    * Writes the separator that goes before a value inside an array. Values inside an object follow their field name,
    * which already wrote the separator.
    */
   private void beforeValue() throws IOException {
      Container container = stack.peek();
      if (container != null && !container.object) {
         out.write(container.entries++ > 0 ? ", " : " ");
      }
   }

   private void newLine() throws IOException {
      out.write('\n');
      for (int i = 0; i < objectDepth; i++) {
         out.write("  ");
      }
   }

   private void writeQuoted(String s) throws IOException {
      out.write('"');
      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         switch (c) {
            case '"' -> out.write("\\\"");
            case '\\' -> out.write("\\\\");
            case '\b' -> out.write("\\b");
            case '\f' -> out.write("\\f");
            case '\n' -> out.write("\\n");
            case '\r' -> out.write("\\r");
            case '\t' -> out.write("\\t");
            default -> {
               // Control characters, and surrogates (characters outside the BMP are written as an escaped pair)
               if (c < 0x20 || Character.isSurrogate(c)) {
                  out.write("\\u");
                  out.write(HEX[(c >> 12) & 0xF]);
                  out.write(HEX[(c >> 8) & 0xF]);
                  out.write(HEX[(c >> 4) & 0xF]);
                  out.write(HEX[c & 0xF]);
               } else {
                  out.write(c);
               }
            }
         }
      }
      out.write('"');
   }

   private static final class Container {
      final boolean object;
      int entries;

      Container(boolean object) {
         this.object = object;
      }
   }
}
