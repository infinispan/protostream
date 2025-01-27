package org.infinispan.protostream.impl.json;

import static org.infinispan.protostream.impl.json.JsonHelper.NULL;

import org.infinispan.protostream.impl.Appendable;

/**
 * Writer for a JSON token.
 *
 * <p>
 * These writers are utilized when transforming the {@link JsonToken} into the final string representation. Most of the
 * tokens in the JSON use a single character, except <code>STRING</code> and the value. A <code>STRING</code> represent
 * the property/key in the JSON, and the value is the actual value.
 * </p>
 */
interface JsonTokenWriter extends Appendable {

   /**
    * @return Which token is backing this writer.
    */
   JsonToken token();

   /**
    * Writer for a JSON key/property.
    *
    * <p>
    * This property is always a string.
    * </p>
    *
    * @param content The property name.
    * @return The property writer.
    */
   static JsonTokenWriter string(String content) {
      return new ContentToken(JsonToken.STRING, content, true);
   }

   /**
    * Writer for a JSON primitive value.
    *
    * <p>
    * The value is any primitive object. It can be a string, number, or null.
    * </p>
    *
    * @param content The value to output.
    * @param quoted Whether this value need to be written between quotes.
    * @return The value writer.
    */
   static JsonTokenWriter value(Object content, boolean quoted) {
      return new ContentToken(JsonToken.VALUE, content, quoted);
   }

   record ContentToken(JsonToken token, Object content, boolean quoted) implements JsonTokenWriter {
      @Override
      public void append(StringBuilder out) {
         if (quoted) out.append('"');
         switch (token) {
            case STRING -> out.append(content);
            case VALUE -> {
               if (content == null) {
                  out.append(NULL);
                  break;
               }

               out.append(content);
            }
         }
         if (quoted) out.append('"');
      }
   }
}
