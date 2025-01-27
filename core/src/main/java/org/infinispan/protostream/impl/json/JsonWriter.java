package org.infinispan.protostream.impl.json;

import static org.infinispan.protostream.impl.json.JsonHelper.JSON_WHITESPACE;

import java.io.IOException;
import java.util.List;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufParser;
import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.descriptors.Descriptor;

/**
 * Transforms a byte stream into the canonical JSON format.
 *
 * @author José Bolina
 */
public final class JsonWriter {

   private JsonWriter() { }

   public static String toJson(ImmutableSerializationContext ctx, byte[] bytes, boolean pretty) throws IOException {
      Descriptor descriptor = ctx.getMessageDescriptor(WrappedMessage.PROTOBUF_TYPE_NAME);
      BaseJsonWriter handler = new RootJsonWriter(ctx);
      ProtobufParser.INSTANCE.parse(handler, descriptor, bytes);
      return astToString(handler.ast, pretty);
   }

   private static String astToString(List<JsonTokenWriter> ast, boolean pretty) {
      if (pretty)
         return astToPrettyString(ast);

      // No need to prettify the JSON output, we can just delegate to the writers.
      // There is no need to write whitespaces in this case.
      StringBuilder out = new StringBuilder();
      for (JsonTokenWriter writer : ast) {
         writer.append(out);
      }
      return out.toString();
   }

   private static String astToPrettyString(List<JsonTokenWriter> ast) {
      StringBuilder out = new StringBuilder();
      int level = 0;
      JsonToken last = null;

      // Verify token by token how to indent to create a pretty JSON.
      // This approach follows the JSON definition in the class doc to replace the whitespace with the appropriate format.
      for (JsonTokenWriter writer : ast) {
         JsonToken token = writer.token();

         // If the token marks a closing object, we're up one level in the indentation.
         // This must be verified before writing the closing token.
         if (token.isEnd()) {
            level--;
            out.append(System.lineSeparator());
         }

         // Adjust the token to be written in the same level as the indentation.
         // In case the previous token was a colon, we do not insert the indentation:
         // <ws><ws><ws>"property":<ws>{
         // This handles the case a new nested object is starting.
         if (token == JsonToken.STRING || token == JsonToken.VALUE || token.isEnd() || token == JsonToken.LEFT_BRACE) {
            if (last != JsonToken.COLON)
               out.append(indentation(level));
         }

         writer.append(out);

         // After a colon we insert a single whitespace.
         // This separates a property from next token (new object, a value, an array).
         if (token == JsonToken.COLON) {
            out.append(" ");
         }

         // In case this is an opening token, new object or array, we are one level deeper in the indentation.
         if (token.isOpen()) {
            level++;
         }

         // After opening an object or after a key-value is written, we break a line.
         if (token.isOpen() || token == JsonToken.COMMA)
            out.append(System.lineSeparator());

         last = token;
      }
      return out.toString();
   }

   private static String indentation(int level) {
      return JSON_WHITESPACE.repeat(level);
   }
}
