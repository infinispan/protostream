package org.infinispan.protostream.impl.json;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal, read-only JSON tree, used to read <code>proto.lock</code> files.
 * <p>
 * Accessors are lenient in the same way as Jackson's <code>JsonNode</code>: {@link #get(String)} returns
 * <code>null</code> for missing fields, {@link #asText()} and {@link #asInt()} convert scalars. Duplicate field names
 * keep the last value.
 */
public final class JsonTree implements Iterable<JsonTree> {

   private final JsonLexer.Token type;
   // A Map for objects, a List for arrays, the text for strings and numbers, null otherwise
   private final Object value;

   private JsonTree(JsonLexer.Token type, Object value) {
      this.type = type;
      this.value = value;
   }

   /**
    * Reads a single JSON document. The reader is read to the end but not closed: the caller owns it.
    *
    * @throws JsonLexer.ParseException if the input is empty or is not valid JSON
    */
   public static JsonTree parse(Reader reader) throws IOException {
      JsonLexer lexer = new JsonLexer(reader);
      JsonLexer.Token token = lexer.nextToken();
      if (token == null) {
         throw new JsonLexer.ParseException("No content to read: the input is empty");
      }
      JsonTree tree = read(lexer, token);
      lexer.nextToken(); // Rejects trailing content
      return tree;
   }

   private static JsonTree read(JsonLexer lexer, JsonLexer.Token token) throws IOException {
      switch (token) {
         case START_OBJECT: {
            Map<String, JsonTree> fields = new LinkedHashMap<>();
            for (token = lexer.nextToken(); token != JsonLexer.Token.END_OBJECT; token = lexer.nextToken()) {
               String name = lexer.currentName();
               fields.put(name, read(lexer, lexer.nextToken()));
            }
            return new JsonTree(JsonLexer.Token.START_OBJECT, fields);
         }
         case START_ARRAY: {
            List<JsonTree> elements = new ArrayList<>();
            for (token = lexer.nextToken(); token != JsonLexer.Token.END_ARRAY; token = lexer.nextToken()) {
               elements.add(read(lexer, token));
            }
            return new JsonTree(JsonLexer.Token.START_ARRAY, elements);
         }
         default:
            return new JsonTree(token, lexer.getText());
      }
   }

   public boolean isObject() {
      return type == JsonLexer.Token.START_OBJECT;
   }

   public boolean isArray() {
      return type == JsonLexer.Token.START_ARRAY;
   }

   /**
    * @return the value of the field, or <code>null</code> if this is not an object or has no such field
    */
   public JsonTree get(String name) {
      return isObject() ? fields().get(name) : null;
   }

   /**
    * @return the element at the index, or <code>null</code> if this is not an array or the index is out of bounds
    */
   public JsonTree get(int index) {
      if (!isArray()) {
         return null;
      }
      List<JsonTree> elements = elements();
      return index >= 0 && index < elements.size() ? elements.get(index) : null;
   }

   /**
    * @return whether this is an object with the field, even if its value is <code>null</code>
    */
   public boolean has(String name) {
      return isObject() && fields().containsKey(name);
   }

   /**
    * @return the number of fields or elements, 0 for scalars
    */
   public int size() {
      if (isObject()) {
         return fields().size();
      }
      return isArray() ? elements().size() : 0;
   }

   /**
    * @return strings unescaped, numbers as written, <code>true</code>/<code>false</code>/<code>null</code> for literals
    * and an empty string for containers
    */
   public String asText() {
      return switch (type) {
         case START_OBJECT, START_ARRAY -> "";
         case VALUE_TRUE -> "true";
         case VALUE_FALSE -> "false";
         case VALUE_NULL -> "null";
         default -> (String) value;
      };
   }

   /**
    * @return the value of numbers (truncated if fractional) or numeric strings, 1 for <code>true</code>, 0 otherwise
    */
   public int asInt() {
      return switch (type) {
         case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT, VALUE_STRING -> {
            String text = ((String) value).trim();
            try {
               yield Integer.parseInt(text);
            } catch (NumberFormatException e) {
               try {
                  yield (int) Double.parseDouble(text);
               } catch (NumberFormatException e2) {
                  yield 0;
               }
            }
         }
         case VALUE_TRUE -> 1;
         default -> 0;
      };
   }

   /**
    * Iterates over the elements of an array or the field values of an object; scalars have none.
    */
   @Override
   public Iterator<JsonTree> iterator() {
      if (isObject()) {
         return fields().values().iterator();
      }
      return isArray() ? elements().iterator() : Collections.emptyIterator();
   }

   @SuppressWarnings("unchecked")
   private Map<String, JsonTree> fields() {
      return (Map<String, JsonTree>) value;
   }

   @SuppressWarnings("unchecked")
   private List<JsonTree> elements() {
      return (List<JsonTree>) value;
   }
}
