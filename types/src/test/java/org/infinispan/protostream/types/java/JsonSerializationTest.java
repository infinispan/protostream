package org.infinispan.protostream.types.java;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.config.Configuration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class JsonSerializationTest {

   private final TestParams params;
   private final ImmutableSerializationContext context;

   public JsonSerializationTest(TestParams params) {
      this.params = params;
      this.context = newContext();
   }

   @Test
   public void testSerializationMatches() throws Exception {
      byte[] bytes = ProtobufUtil.toWrappedByteArray(context, params.original, 512);
      String actual = ProtobufUtil.toCanonicalJSON(context, bytes, params.pretty);
      String expected = readFile(params.jsonFilePath);
      assertEquals(expected, actual);
   }

   @Parameterized.Parameters(name = "{0}")
   public static Object[][] marshallingElements() {
      return new Object[][] {
            new Object[] { new TestParams(new WrappedMessage(UUID.fromString("5efbb09b-37a7-4237-bddf-e4f271db82a8")), "json/wrapped-uuid.json", false) },
            new Object[] { new TestParams(new WrappedMessage(List.of(UUID.fromString("ade02349-2b40-4dc9-9ad2-00cac707220b"), UUID.fromString("781da88f-a320-4c9c-b7e5-5da7b18c7d73"))), "json/list-uuid.json", false) },
            new Object[] { new TestParams(new PrimitiveCollections(
                  List.of("hello", "world"),
                  new ArrayList<>(List.of("hello1", "world1")),
                  new HashSet<>(Set.of("hello2", "world2")),
                  new LinkedHashSet<>(Set.of("hello3")), // Only a single element because hash set can have any order.
                  new LinkedList<>(List.of("hello4", "world4")),
                  new TreeSet<>(List.of("hello5", "world5")),
                  new HashMap<>(Map.of("hello6", "world6", "hello7", "world7")),
                  new ArrayList<>(List.of(new Book("title1", "desc1", 2025), new Book("title2", "desc2", 2024))),
                  new HashMap<>()
            ), "json/many-collections.json", false) },
            new Object[] { new TestParams(new PrimitiveCollections(
                  List.of("hello", "world"),
                  new ArrayList<>(List.of("hello1", "world1")),
                  new HashSet<>(Set.of("hello2", "world2")),
                  new LinkedHashSet<>(Set.of("hello3")), // Only a single element because hash set can have any order.
                  new LinkedList<>(List.of("hello4", "world4")),
                  new TreeSet<>(List.of("hello5", "world5")),
                  new HashMap<>(Map.of("hello6", "world6", "hello7", "world7")),
                  new ArrayList<>(List.of(new Book("title1", "desc1", 2025), new Book("title2", "desc2", 2024))),
                  new HashMap<>()
            ), "json/many-collections-pretty.json", true) },
            new Object[] { new TestParams(List.of(
                  List.of(1, 2, 3),
                  Collections.singletonList(List.of(4, 5, 6)),
                  new WrappedMessage(Collections.singletonList(List.of("hello", "world"))),
                  new WrappedMessage(List.of(Collections.singletonList(1), UUID.fromString("91591e70-9a36-4d10-bf4b-bab04b68a12c"))),
                  Month.SEPTEMBER
            ), "json/nested-types-pretty.json", true)},
            new Object[] { new TestParams(Instant.ofEpochSecond(1744288313, 308361201), "json/instant.json", false) },
            new Object[] { new TestParams(new Date(1744288478165L), "json/date.json", false) },
            new Object[] { new TestParams(OffsetTime.of(23,  59, 59, 10, ZoneOffset.UTC), "json/offset-time.json", false) },
            new Object[] { new TestParams(new ArrayList<>(List.of(new Book("Book1", "Description1", 2020), new Book("Book2", "Description2", 2021))), "json/books-list.json", false) },
            new Object[] { new TestParams(ZonedDateTime.of(1985, 10, 26, 0, 59, 0, 0, ZoneId.of("+07:00")), "json/zoned-time.json", false) },
            new Object[] { new TestParams(LocalDateTime.of(1985, 10, 26, 0, 59, 0, 0), "json/local-date-time.json", false) },
      };
   }

   private record TestParams(Object original, String jsonFilePath, boolean pretty) { }

   private static String readFile(String path) throws URISyntaxException, IOException {
      ClassLoader loader = JsonSerializationTest.class.getClassLoader();
      URL url = Objects.requireNonNull(loader.getResource(path), path + " not found");
      return Files.readString(Path.of(url.toURI())).trim();
   }

   private static ImmutableSerializationContext newContext() {
      var config = Configuration.builder().wrapCollectionElements(true).build();
      var ctx = ProtobufUtil.newSerializationContext(config);
      register(new CommonTypesSchema(), ctx);
      register(new CommonContainerTypesSchema(), ctx);
      register(new BookSchemaImpl(), ctx);
      register(new ListSchemaImpl(), ctx);
      register(new PrimitiveCollectionsSchemaImpl(), ctx);
      return ctx;
   }

   private static void register(GeneratedSchema schema, SerializationContext context) {
      schema.registerSchema(context);
      schema.registerMarshallers(context);
   }
}
