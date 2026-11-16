package org.infinispan.protostream.impl.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class JsonTreeTest {

   @Test
   public void testAccessors() throws IOException {
      JsonTree tree = parse("{\"s\":\"a\\nb\",\"i\":42,\"f\":1.5e1,\"t\":true,\"n\":null,\"a\":[1,\"2\",{}],\"o\":{\"x\":1}}");
      assertTrue(tree.isObject());
      assertEquals(7, tree.size());
      assertEquals("a\nb", tree.get("s").asText());
      assertEquals("42", tree.get("i").asText());
      assertEquals(42, tree.get("i").asInt());
      assertEquals("1.5e1", tree.get("f").asText());
      assertEquals(15, tree.get("f").asInt());
      assertEquals("true", tree.get("t").asText());
      assertEquals(1, tree.get("t").asInt());
      assertTrue(tree.has("n"));
      assertEquals("null", tree.get("n").asText());
      assertFalse(tree.has("missing"));
      assertNull(tree.get("missing"));

      JsonTree array = tree.get("a");
      assertTrue(array.isArray());
      assertEquals(3, array.size());
      assertEquals(2, array.get(1).asInt());
      assertEquals("", array.get(2).asText());
      assertNull(array.get(3));
      assertNull(array.get("x"));
      List<String> elements = new ArrayList<>();
      array.forEach(e -> elements.add(e.asText()));
      assertEquals(List.of("1", "2", ""), elements);

      assertEquals(1, tree.get("o").get("x").asInt());
      assertNull(tree.get("s").get(0));
      assertEquals(0, tree.get("s").size());
      assertEquals(0, tree.get("s").asInt());
   }

   @Test
   public void testDuplicateFieldKeepsLastValue() throws IOException {
      assertEquals("2", parse("{\"a\":1,\"a\":2}").get("a").asText());
   }

   @Test
   public void testInvalidDocuments() {
      for (String json : List.of("", "  ", "{", "{} {}", "{\"a\":1,}", "[1]]")) {
         assertThrows(JsonLexer.ParseException.class, () -> parse(json), json);
      }
   }

   private static JsonTree parse(String json) throws IOException {
      return JsonTree.parse(new StringReader(json));
   }
}
