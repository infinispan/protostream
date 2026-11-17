package org.infinispan.protostream.impl.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Checks that {@link JsonLexer} tokenizes and converts values exactly like the Jackson parser it replaces, with
 * Jackson's default configuration, except where a difference is intended.
 */
public class JsonLexerTest {

   private static final JsonFactory JACKSON = new JsonFactory();

   private static final String ERROR = "ERROR";

   private static final List<String> VALID = List.of(
         "", " \t\r\n ", "{}", "[]", "null", "true", "false", "0", "-0", "\"\"", "{ }", "[ ]",
         "{\"a\":1,\"b\":[1,2,{\"c\":null}],\"d\":{\"e\":true,\"f\":false}}",
         "[[[]],[{}],{\"x\":[[]]}]",
         "{\"a\" : 1 , \"b\" :\n[ 1 ,\t2 ] }",
         "{\"dup\":1,\"dup\":2}",
         "{\"\":\"empty name\"}",
         // Numbers
         "[0,1,-1,10,-10,123456789,2147483647,2147483648,-2147483648,-2147483649]",
         "[9223372036854775807,9223372036854775808,-9223372036854775808,-9223372036854775809,99999999999999999999999]",
         "[-128,-129,127,128,255,256,1000]",
         "[0.0,-0.0,1.5,-1.5,1e5,1E5,1e+5,1e-5,1E-5,-1.25e10,0e0,0.1e1]",
         "[1.00000017881393432617187499,3.4028235e38,3.5e38,1e-46,1.7976931348623157e308,1e309,4.9e-324,1e-400]",
         "[3.141592653589793238462643383279502884197]",
         // Strings and escapes
         "[\"plain\",\"\\\"\\\\\\/\\b\\f\\n\\r\\t\",\"\\u0041\\u00e9\\u00E9\\u20AC\",\"\\uD83D\\uDE00\",\"\\uD800\",\"\\uDC00x\"]",
         "[\"é€😀\",\"\u007f\",\"\u2028\u2029\",\"/<>&='\"]",
         "{\"na\\u006De\":\"v\",\"line\\nbreak\":1}",
         // Base64
         "[\"\",\"AQ==\",\"AQI=\",\"AQID\",\"AQIDBA==\",\"+/+/\",\"AA==AA==\",\" AQID\",\"AQID \",\"AQID AQID\",\"\\u0041QID\",\"AQID\\n\"]",
         "[\"AQ\",\"AQI\",\"A\",\"A===\",\"AQ=A\",\"AQ ID\",\"_-_-\",\"AQ\\u003d=\",\"====\",\"AQ==\\u0000\"]"
   );

   private static final List<String> INVALID = List.of(
         "{", "[", "}", "]", "{]", "[}", "[1,]", "{\"a\":1,}", "[,1]", "{,}", "[1 2]", "{\"a\" 1}", "{\"a\":}",
         "{a:1}", "{'a':1}", "['a']", "{\"a\":1 \"b\":2}", "{1:2}", "[\"unterminated", "\"", ":", ",",
         "tru", "nul", "fals", "True", "NULL", "truex", "[truex]", "nullnull", "NaN", "Infinity", "-Infinity", "undefined",
         "01", "-01", "00", "+1", "-", "1.", "1.e5", ".5", "1e", "1e+", "1E-", "0x10", "1_000", "[1-2]", "--1", "1.5.5",
         "[\"\\x\"]", "[\"\\u12\"]", "[\"\\u12G4\"]", "[\"\\U0041\"]", "[\"\\'\"]", "[\"\u0000\"]", "[\"\t\"]", "[\"\n\"]",
         "\uFEFF{}", "\f{}", "\u00a0{}", "{}\u0000", "/* c */ {}", "// c\n{}", "# c\n{}", "[1]x", "[1]]",
         "{\"a\":1:2}", "{\"a\":true:2}", "[1:2]", "[null:1]"
   );

   @Test
   public void testValidInputMatchesJackson() {
      for (String json : VALID) {
         assertSameTrace(json);
         assertTrue(!trace(json, false, false).contains(ERROR) || json.contains("AQ\""), () -> "Should be valid: " + json);
      }
   }

   @Test
   public void testInvalidInputMatchesJackson() {
      for (String json : INVALID) {
         assertTrue(isError(trace(json, false, false)), () -> "Should be rejected: " + json);
         assertSameTrace(json);
      }
   }

   @Test
   public void testLimitsMatchJackson() {
      assertSameTrace("[".repeat(JsonLexer.MAX_DEPTH) + "]".repeat(JsonLexer.MAX_DEPTH));
      assertSameTrace("{\"a\":".repeat(JsonLexer.MAX_DEPTH - 1) + "[]" + "}".repeat(JsonLexer.MAX_DEPTH - 1));
      assertSameTrace("[".repeat(JsonLexer.MAX_DEPTH + 1) + "]".repeat(JsonLexer.MAX_DEPTH + 1));
      String digits = "1".repeat(JsonLexer.MAX_NUMBER_LENGTH);
      assertSameTrace("[" + digits + "]");
      assertSameTrace("[" + digits + "1]");
      assertSameTrace("[-" + digits + "]");
      assertSameTrace("[-" + digits + "1]");
      assertSameTrace("[1." + digits.substring(1) + "]");
      assertSameTrace("[1." + digits + "]");
      assertSameTrace("[-1." + digits.substring(2) + "e-1]");
      assertSameTrace("[-1." + digits.substring(2) + "e-12]");
      assertSameTrace("[1e" + digits.substring(1) + "]");
      assertSameTrace("[1e" + digits + "]");
      assertSameTrace("{\"" + "n".repeat(JsonLexer.MAX_NAME_LENGTH) + "\":1}");
      assertSameTrace("{\"" + "n".repeat(JsonLexer.MAX_NAME_LENGTH + 1) + "\":1}");
      // Strings spanning several reads from the underlying Reader
      assertSameTrace("[\"" + "s".repeat(100_000) + "\\n" + "é".repeat(20_000) + "\"]");
      assertSameTrace("[\"" + Base64.getEncoder().encodeToString(new byte[50_000]) + "\"]");
   }

   @Test
   public void testMutatedInputMatchesJackson() {
      String[] seeds = {
            "{\"_type\":\"org.Msg\",\"id\":-12,\"f\":1.5e3,\"s\":\"a\\\"b\\u00e9\",\"b\":[true,false,null],\"o\":{\"x\":[]}}",
            "[0,-0.5,1E+2,\"AQID\",{\"k\":\"v\"},[[1]],\"\\uD83D\\uDE00\"]",
            "{\"a\" : [ 255 , 256 , -129 ] , \"b\" : \"AQ==\" }"
      };
      String alphabet = "{}[]:,\"\\ -+.eE0123456789abfnrtuAQ=/\u0000\u00e9\n";
      Random random = new Random(751);
      for (int i = 0; i < 20_000; i++) {
         StringBuilder sb = new StringBuilder(seeds[i % seeds.length]);
         for (int m = random.nextInt(3) + 1; m > 0 && sb.length() > 0; m--) {
            int at = random.nextInt(sb.length());
            char ch = alphabet.charAt(random.nextInt(alphabet.length()));
            switch (random.nextInt(3)) {
               case 0 -> sb.insert(at, ch);
               case 1 -> sb.deleteCharAt(at);
               default -> sb.setCharAt(at, ch);
            }
         }
         assertSameTrace(sb.toString());
      }
   }

   @Test
   public void testTrailingContentIsRejected() {
      // Jackson accepts a sequence of root values; JsonLexer only accepts whitespace after the root value
      for (String json : List.of("{} {}", "{}{}", "[] 1", "1 2", "\"a\"\"b\"", "null null")) {
         assertTrue(isError(trace(json, false, false)), json);
         assertTrue(!isError(trace(json, true, false, false)), json);
      }
   }

   @Test
   public void testCurrentName() throws IOException {
      JsonLexer lexer = new JsonLexer(new StringReader("{\"a\":{\"b\":[1]},\"c\":2}"));
      assertNull(lexer.currentName());
      assertEquals(JsonLexer.Token.START_OBJECT, lexer.nextToken());
      assertNull(lexer.currentName());
      assertEquals(JsonLexer.Token.FIELD_NAME, lexer.nextToken());
      assertEquals("a", lexer.currentName());
      assertEquals(JsonLexer.Token.START_OBJECT, lexer.nextToken());
      assertEquals("a", lexer.currentName());
      assertEquals(JsonLexer.Token.FIELD_NAME, lexer.nextToken());
      assertEquals("b", lexer.currentName());
      assertEquals(JsonLexer.Token.START_ARRAY, lexer.nextToken());
      assertEquals("b", lexer.currentName());
      assertEquals(JsonLexer.Token.VALUE_NUMBER_INT, lexer.nextToken());
      assertNull(lexer.currentName());
      assertEquals(JsonLexer.Token.END_ARRAY, lexer.nextToken());
      assertEquals("b", lexer.currentName());
      assertEquals(JsonLexer.Token.END_OBJECT, lexer.nextToken());
      assertEquals("a", lexer.currentName());
      assertEquals(JsonLexer.Token.FIELD_NAME, lexer.nextToken());
      assertEquals(JsonLexer.Token.VALUE_NUMBER_INT, lexer.nextToken());
      assertEquals("c", lexer.currentName());
      assertEquals(JsonLexer.Token.END_OBJECT, lexer.nextToken());
      assertNull(lexer.nextToken());
      assertNull(lexer.nextToken());
   }

   @Test
   public void testErrorsReportPosition() {
      JsonLexer.ParseException e = assertThrows(JsonLexer.ParseException.class, () -> {
         JsonLexer lexer = new JsonLexer(new StringReader("{\n  \"a\": tru\n}"));
         while (lexer.nextToken() != null) {
            // consume
         }
      });
      assertTrue(e.getMessage().endsWith("at line 2, column 11"), e.getMessage());
   }

   /**
    * Both parsers must produce the same trace, except that on invalid input JsonLexer may stop earlier: it rejects a
    * literal or number not followed by a delimiter right away, where Jackson may first return it as a token. When
    * decoding Base64 straight from the input, Jackson skips any control character, even unescaped ones: validity is
    * decided by the plain token trace.
    */
   private static void assertSameTrace(String json) {
      boolean invalid = isError(trace(json, true, false));
      for (boolean binary : new boolean[]{false, true}) {
         List<String> expected = trace(json, true, binary);
         List<String> actual = trace(json, false, binary);
         if (binary && invalid && !isError(expected)) {
            assertTrue(isError(actual), () -> "Should be rejected: " + json);
         } else if (isError(expected) && isError(actual)) {
            List<String> prefix = expected.subList(0, Math.min(actual.size(), expected.size()) - 1);
            assertEquals(prefix, actual.subList(0, actual.size() - 1), () -> "JsonLexer read past Jackson's error in: " + json);
         } else {
            assertEquals(expected, actual, () -> (binary ? "Base64 values" : "Tokens") + " differ for: " + json);
         }
      }
   }

   private static boolean isError(List<String> trace) {
      // A failed Base64 conversion ends the trace too: Jackson only reads strings when they are accessed
      return !trace.isEmpty() && trace.get(trace.size() - 1).endsWith(ERROR);
   }

   /**
    * Records every token with its text and the value of each accessor that applies to it. With <code>binary</code>,
    * strings are only read with <code>getBinaryValue</code>, which Jackson decodes straight from the input.
    */
   private static List<String> trace(String json, boolean jackson, boolean binary) {
      return trace(json, jackson, binary, true);
   }

   private static List<String> trace(String json, boolean jackson, boolean binary, boolean strictRoot) {
      List<String> trace = new ArrayList<>();
      try {
         Tokens tokens = jackson
               ? new JacksonTokens(JACKSON.createParser(new StringReader(json)), strictRoot)
               : new LexerTokens(new JsonLexer(new StringReader(json)));
         for (String token = tokens.next(); token != null; token = tokens.next()) {
            trace.add(token);
            if (binary) {
               if (token.equals("VALUE_STRING")) {
                  String value = attempt(() -> Base64.getEncoder().encodeToString(tokens.binary()));
                  trace.add("binary=" + value);
                  if (value.equals(ERROR)) {
                     // Jackson has abandoned the string half-way: the following tokens are not comparable
                     return trace;
                  }
               }
               continue;
            }
            trace.add("text=" + tokens.text() + " name=" + tokens.name());
            if (token.equals("VALUE_NUMBER_INT")) {
               trace.add("int=" + attempt(() -> String.valueOf(tokens.intValue())));
               trace.add("long=" + attempt(() -> String.valueOf(tokens.longValue())));
               trace.add("byte=" + attempt(() -> String.valueOf(tokens.byteValue())));
            }
            if (token.startsWith("VALUE_NUMBER")) {
               trace.add("float=" + attempt(() -> String.valueOf(tokens.floatValue())));
               trace.add("double=" + attempt(() -> String.valueOf(tokens.doubleValue())));
            }
            if (token.equals("VALUE_TRUE") || token.equals("VALUE_FALSE")) {
               trace.add("boolean=" + attempt(() -> String.valueOf(tokens.booleanValue())));
            }
         }
      } catch (JsonProcessingException | JsonLexer.ParseException e) {
         trace.add(ERROR);
      } catch (IOException e) {
         throw new AssertionError(e);
      }
      return trace;
   }

   private static String attempt(Accessor accessor) throws IOException {
      try {
         return accessor.get();
      } catch (JsonProcessingException | JsonLexer.ParseException e) {
         return ERROR;
      }
   }

   private interface Accessor {
      String get() throws IOException;
   }

   private interface Tokens {
      String next() throws IOException;
      String text() throws IOException;
      String name() throws IOException;
      int intValue() throws IOException;
      long longValue() throws IOException;
      byte byteValue() throws IOException;
      float floatValue() throws IOException;
      double doubleValue() throws IOException;
      boolean booleanValue() throws IOException;
      byte[] binary() throws IOException;
   }

   /**
    * With <code>strictRoot</code>, a second root value is reported as an error, like JsonLexer does.
    */
   private static final class JacksonTokens implements Tokens {
      private final JsonParser p;
      private final boolean strictRoot;
      private boolean rootRead;

      JacksonTokens(JsonParser p, boolean strictRoot) {
         this.p = p;
         this.strictRoot = strictRoot;
      }

      @Override
      public String next() throws IOException {
         com.fasterxml.jackson.core.JsonToken t = p.nextToken();
         if (t == null) {
            return null;
         }
         // After a START_* token, the parsing context is already the new container's
         boolean atRoot = (t.isStructStart() ? p.getParsingContext().getParent() : p.getParsingContext()).inRoot();
         if (strictRoot && atRoot && !t.isStructEnd()) {
            if (rootRead) {
               throw new com.fasterxml.jackson.core.JsonParseException(p, "Trailing content");
            }
            rootRead = true;
         }
         return t.name();
      }

      public String text() throws IOException { return p.getText(); }
      public String name() throws IOException { return p.currentName(); }
      public int intValue() throws IOException { return p.getIntValue(); }
      public long longValue() throws IOException { return p.getLongValue(); }
      public byte byteValue() throws IOException { return p.getByteValue(); }
      // Jackson derives the second floating-point conversion from the first: use a fresh parser for each
      public float floatValue() throws IOException { return reparse().getFloatValue(); }
      public double doubleValue() throws IOException { return reparse().getDoubleValue(); }

      private JsonParser reparse() throws IOException {
         JsonParser number = JACKSON.createParser(p.getText());
         number.nextToken();
         return number;
      }
      public boolean booleanValue() throws IOException { return p.getBooleanValue(); }
      public byte[] binary() throws IOException { return p.getBinaryValue(); }
   }

   private record LexerTokens(JsonLexer l) implements Tokens {
      @Override
      public String next() throws IOException {
         JsonLexer.Token t = l.nextToken();
         return t == null ? null : t.name();
      }

      public String text() { return l.getText(); }
      public String name() { return l.currentName(); }
      public int intValue() throws IOException { return l.getIntValue(); }
      public long longValue() throws IOException { return l.getLongValue(); }
      public byte byteValue() throws IOException { return l.getByteValue(); }
      public float floatValue() throws IOException { return l.getFloatValue(); }
      public double doubleValue() throws IOException { return l.getDoubleValue(); }
      public boolean booleanValue() throws IOException { return l.getBooleanValue(); }
      public byte[] binary() throws IOException { return l.getBinaryValue(); }
   }
}
