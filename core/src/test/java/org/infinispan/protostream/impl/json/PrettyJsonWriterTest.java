package org.infinispan.protostream.impl.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * The expected output matches what the previously used JSON library's default pretty printer wrote for the same calls.
 */
public class PrettyJsonWriterTest {

   @Test
   public void testLayout() throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrettyJsonWriter w = new PrettyJsonWriter(baos);
      w.writeStartObject();
      w.writeFieldName("empty");
      w.writeStartObject();
      w.writeEndObject();
      w.writeFieldName("nested");
      w.writeStartObject();
      w.writeFieldName("inner");
      w.writeStartObject();
      w.writeNumberField("n", -5);
      w.writeEndObject();
      w.writeEndObject();
      w.writeFieldName("arr");
      w.writeStartArray();
      w.writeString("x");
      w.writeNumber(1);
      w.writeStartArray();
      w.writeEndArray();
      w.writeStartObject();
      w.writeEndObject();
      w.writeEndArray();
      w.writeFieldName("emptyArr");
      w.writeStartArray();
      w.writeEndArray();
      w.writeEndObject();
      w.flush();

      assertEquals("""
            {
              "empty" : { },
              "nested" : {
                "inner" : {
                  "n" : -5
                }
              },
              "arr" : [ "x", 1, [ ], { } ],
              "emptyArr" : [ ]
            }""", baos.toString(StandardCharsets.UTF_8));
   }

   @Test
   public void testArrayOfObjects() throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrettyJsonWriter w = new PrettyJsonWriter(baos);
      w.writeStartObject();
      w.writeFieldName("items");
      w.writeStartArray();
      w.writeStartObject();
      w.writeStringField("a", "1");
      w.writeEndObject();
      w.writeStartObject();
      w.writeStringField("b", "2");
      w.writeEndObject();
      w.writeEndArray();
      w.writeEndObject();
      w.flush();

      assertEquals("""
            {
              "items" : [ {
                "a" : "1"
              }, {
                "b" : "2"
              } ]
            }""", baos.toString(StandardCharsets.UTF_8));
   }

   @Test
   public void testValues() throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrettyJsonWriter w = new PrettyJsonWriter(baos);
      w.writeStartObject();
      w.writeBooleanField("t", true);
      w.writeBooleanField("f", false);
      w.writeNumberField("min", Long.MIN_VALUE);
      w.writeNumberField("max", Long.MAX_VALUE);
      w.writeEndObject();
      w.flush();

      assertEquals("""
            {
              "t" : true,
              "f" : false,
              "min" : -9223372036854775808,
              "max" : 9223372036854775807
            }""", baos.toString(StandardCharsets.UTF_8));
   }

   @Test
   public void testEscaping() throws IOException {
      // Quotes, backslashes and control characters are escaped (hex in upper case); HTML characters, '/', DEL and
      // other non-ASCII BMP characters are written as-is; characters outside the BMP are written as an escaped pair
      String value = "q\" b\\ <>&='/ \u0001\b\t\n\f\r\u001F\u007Fé中 😀";
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrettyJsonWriter w = new PrettyJsonWriter(baos);
      w.writeStartObject();
      w.writeStringField("k\"", value);
      w.writeEndObject();
      w.flush();

      String expected = "{\n  \"k\\\"\" : \"q\\\" b\\\\ <>&='/ \\u0001\\b\\t\\n\\f\\r\\u001F\u007Fé中 \\uD83D\\uDE00\"\n}";
      assertEquals(expected, baos.toString(StandardCharsets.UTF_8));
   }
}
