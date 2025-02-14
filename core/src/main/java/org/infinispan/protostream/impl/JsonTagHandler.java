package org.infinispan.protostream.impl;

import static org.infinispan.protostream.WrappedMessage.WRAPPED_ENUM;
import static org.infinispan.protostream.impl.JsonUtils.JSON_TYPE_FIELD;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.infinispan.protostream.TagHandler;
import org.infinispan.protostream.descriptors.AnnotatedDescriptor;
import org.infinispan.protostream.descriptors.EnumValueDescriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.GenericDescriptor;
import org.infinispan.protostream.descriptors.Type;

final class JsonTagHandler implements TagHandler {
   // Z-normalized RFC 3339 format
   private static final String RFC_3339_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

   private final int initNestingLevel;
   private final StringBuilder jsonOut;
   private final boolean prettyPrint;
   private JsonNestingLevel nestingLevel = null;

   /**
    * Have we written the "_type" field?
    */
   private boolean missingType = true;

   public JsonTagHandler(int initNestingLevel, StringBuilder jsonOut) {
      this.initNestingLevel = initNestingLevel;
      this.jsonOut = jsonOut;
      this.prettyPrint = initNestingLevel >= 0;
   }

   private void indent() {
      jsonOut.append('\n');
      jsonOut.append("   ".repeat(Math.max(0, initNestingLevel + nestingLevel.indent())));
   }

   void writeOutput(String content) {
      jsonOut.append(content);
   }

   JsonTagHandler next() {
      return new JsonTagHandler(initNestingLevel, jsonOut);
   }

   boolean isAlreadyStarted() {
      return nestingLevel != null && !nestingLevel.isFirstField();
   }

   @Override
   public void onStart(GenericDescriptor descriptor) {
      nestingLevel = new JsonNestingLevel(nestingLevel);
      if (prettyPrint) {
         indent();
         nestingLevel.incrIndent();
      }
      jsonOut.append('{');
      writeType(descriptor);
   }

   private void writeType(AnnotatedDescriptor descriptor) {
      if (descriptor != null && nestingLevel.previous() == null && nestingLevel.isFirstField()) {
         missingType = false;
         nestingLevel.setFirstField(false);
         if (prettyPrint) {
            indent();
         }
         jsonOut.append('\"').append(JSON_TYPE_FIELD).append('\"').append(':');
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
         nestingLevel.incrIndent();
      }
   }

   @Override
   public void onEndNested(int fieldNumber, FieldDescriptor fieldDescriptor) {
      if (nestingLevel.repeatedFieldDescriptor() != null) {
         endArraySlot();
      }
      if (prettyPrint) {
         nestingLevel.decrIndent();
         indent();
      }
      nestingLevel = nestingLevel.previous();
   }

   @Override
   public void onEnd() {
      if (nestingLevel == null)
         return;

      if (nestingLevel.repeatedFieldDescriptor() != null) {
         endArraySlot();
      }
      if (prettyPrint) {
         nestingLevel.decrIndent();
         indent();
      }
      jsonOut.append('}');
      nestingLevel = nestingLevel.previous();
      if (prettyPrint) {
         jsonOut.append('\n');
      }
   }

   private void startSlot(FieldDescriptor fieldDescriptor) {
      if (nestingLevel.repeatedFieldDescriptor() != null && !nestingLevel.repeatedFieldDescriptor().name.equals(fieldDescriptor.name)) {
         endArraySlot();
      }
      boolean map = nestingLevel.previous() != null
            && nestingLevel.previous().repeatedFieldDescriptor() != null
            && nestingLevel.previous().repeatedFieldDescriptor().isMap();
      if (nestingLevel.isFirstField()) {
         nestingLevel.setFirstField(false);
      } else {
         jsonOut.append(map ? ':' : ',');
      }
      if (!map) {
         if (!fieldDescriptor.isRepeated() || nestingLevel.repeatedFieldDescriptor() == null) {
            String fieldName = fieldDescriptor.getName();
            addNextLevel(fieldName);
         } else if (prettyPrint) {
            jsonOut.append(' ');
         }
      }
      if (fieldDescriptor.isRepeated() && nestingLevel.repeatedFieldDescriptor() == null) {
         nestingLevel.setRepeatedFieldDescriptor(fieldDescriptor);
         jsonOut.append(fieldDescriptor.isMap() ? '{' : '[');
      }
   }

   void addNextLevel(String fieldName) {
      if (prettyPrint) {
         indent();
      }

      jsonOut.append('"').append(fieldName).append("\":");

      if (prettyPrint) {
         jsonOut.append(' ');
      }
   }

   private void endArraySlot() {
      boolean map = nestingLevel.repeatedFieldDescriptor().isMap();
      if (prettyPrint && nestingLevel.repeatedFieldDescriptor().getType() == Type.MESSAGE) {
         indent();
      }
      nestingLevel.setRepeatedFieldDescriptor(null);
      jsonOut.append(map ? '}' : ']');
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
