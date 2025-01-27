package org.infinispan.protostream.impl;

import java.io.IOException;
import java.io.Reader;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.impl.json.JsonReader;
import org.infinispan.protostream.impl.json.JsonWriter;

/**
 * Utility class for conversion to and from canonical JSON.
 *
 * @author anistor@redhat.com
 * @since 4.4
 */
public final class JsonUtils {
   private static final String NULL = "null";

   private JsonUtils() { }

   public static byte[] fromCanonicalJSON(ImmutableSerializationContext ctx, Reader reader) throws IOException {
      return JsonReader.fromJson(ctx, reader);
   }

   /**
    * Converts a Protobuf encoded message to its <a href="https://developers.google.com/protocol-buffers/docs/proto3#json">
    * canonical JSON representation</a>.
    *
    * @param ctx         the serialization context
    * @param bytes       the Protobuf encoded message bytes to parse
    * @param prettyPrint indicates if the JSON output should use a 'pretty' human-readable format or a compact format
    * @return the JSON string representation
    * @throws IOException if I/O operations fail
    */
   public static String toCanonicalJSON(ImmutableSerializationContext ctx, byte[] bytes, boolean prettyPrint) throws IOException {
      if (bytes.length == 0) {
         // only null values get to be encoded to an empty byte array
         return NULL;
      }

      return JsonWriter.toJson(ctx, bytes, prettyPrint);
   }
}
