package org.infinispan.protostream.types.java;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.impl.Log;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TypesMarshallingTest {

   private static final Log log = Log.LogFactory.getLog(MethodHandles.lookup().lookupClass());

   private final TestConfiguration testConfiguration;
   private final ImmutableSerializationContext context;

   public TypesMarshallingTest(TestConfiguration testConfiguration) {
      this.testConfiguration = testConfiguration;
      context = newContext(true);
   }

   @Override
   public String toString() {
      return "TypesMarshallingTest{" +
            "testConfiguration=" + testConfiguration +
            ", context=" + context +
            '}';
   }

   @Parameterized.Parameters
   public static Object[][] marshallingMethods() {
      return Arrays.stream(MarshallingMethodType.values())
            .flatMap(t -> switch (t) {
               case BYTE_ARRAY, INPUT_STREAM, JSON -> Stream.of(new TestConfiguration(t, false, false, null));
               default -> Stream.of(
                     new TestConfiguration(t, true, true, null),
                     new TestConfiguration(t, true, false, ArrayList::new),
                     new TestConfiguration(t, true, false, HashSet::new),
                     new TestConfiguration(t, true, false, LinkedHashSet::new),
                     new TestConfiguration(t, true, false, LinkedList::new),
                     new TestConfiguration(t, true, false, TreeSet::new));
            })
            .map(t -> new Object[]{t})
            .toArray(Object[][]::new);
   }

   @Test
   public void testUUID() throws IOException {
      testConfiguration.method.marshallAndUnmarshallTest(UUID.randomUUID(), context, false);
   }

   @Test
   public void testBitSet() throws IOException {
      var bytes = new byte[ThreadLocalRandom.current().nextInt(64)];
      ThreadLocalRandom.current().nextBytes(bytes);
      testConfiguration.method.marshallAndUnmarshallTest(BitSet.valueOf(bytes), context, false);
   }

   @Test
   public void testBigDecimal() throws IOException {
      testConfiguration.method.marshallAndUnmarshallTest(BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(-256, 256)), context, false);
   }

   @Test
   public void testBigInteger() throws IOException {
      testConfiguration.method.marshallAndUnmarshallTest(BigInteger.valueOf(ThreadLocalRandom.current().nextInt()), context, false);
   }

   @Test
   public void testContainerWithString() throws IOException {
      assumeTrue(testConfiguration.runTest);
      if (testConfiguration.isArray) {
         testConfiguration.method.marshallAndUnmarshallTest(stringArray(), context, true);
      } else {
         testConfiguration.method.marshallAndUnmarshallTest(stringCollection(testConfiguration.collectionBuilder), context, false);
      }
   }

   @Test
   public void testContainerWithBooks() throws IOException {
      assumeTrue(testConfiguration.runTest);
      if (testConfiguration.isArray) {
         testConfiguration.method.marshallAndUnmarshallTest(bookArray(), context, true);
      } else {
         testConfiguration.method.marshallAndUnmarshallTest(bookCollection(testConfiguration.collectionBuilder), context, false);
      }
   }

   @Test
   public void testPrimitiveCollectionCompatibility() throws IOException {
      assumeTrue(testConfiguration.method == MarshallingMethodType.WRAPPED_MESSAGE);
      var list = new ArrayList<>(List.of("a1", "a2", "a3"));

      // without wrapping enabled
      var oldCtx = newContext(false);

      // send with oldCtx: simulates previous version
      var data = ProtobufUtil.toWrappedByteArray(oldCtx, list, 512);
      // read with newCtx: simulates current version
      var listCopy = ProtobufUtil.fromWrappedByteArray(context, data);

      assertEquals(list, listCopy);

      // other way around
      // send with newCtx: simulates current version
      data = ProtobufUtil.toWrappedByteArray(context, list, 512);
      // read with oldCtx: simulates previous version
      listCopy = ProtobufUtil.fromWrappedByteArray(oldCtx, data);

      assertEquals(list, listCopy);
   }

   @Test
   public void testLocalDate() throws IOException {
      LocalDate date = LocalDate.of(1985, 10, 26);
      testConfiguration.method.marshallAndUnmarshallTest(date, context, false);
   }

   @Test
   public void testLocalDateTime() throws IOException {
      LocalDateTime dateTime = LocalDateTime.of(1985, 10, 26, 0, 59, 0, 0);
      testConfiguration.method.marshallAndUnmarshallTest(dateTime, context, false);
   }

   @Test
   public void testLocalTime() throws IOException {
      LocalTime time = LocalTime.of(23, 59, 59, 59);
      testConfiguration.method.marshallAndUnmarshallTest(time, context, false);
   }

   @Test
   public void testMonth() throws IOException {
      assumeTrue(testConfiguration.method == MarshallingMethodType.WRAPPED_MESSAGE || testConfiguration.method == MarshallingMethodType.JSON);
      testConfiguration.method.marshallAndUnmarshallTest(Month.OCTOBER, context, false);
   }

   @Test
   public void testMonthDay() throws IOException {
      MonthDay monthDay  = MonthDay.of(10, 26);
      testConfiguration.method.marshallAndUnmarshallTest(monthDay, context, false);
   }

   @Test
   public void testOffsetTime() throws IOException {
      OffsetTime offsetTime = OffsetTime.of(23,  59, 59, 10, ZoneOffset.UTC);
      testConfiguration.method.marshallAndUnmarshallTest(offsetTime, context, false);
   }

   @Test
   public void testPeriod() throws IOException {
      Period period = Period.of(10, 4, 3);
      testConfiguration.method.marshallAndUnmarshallTest(period, context, false);
   }

   @Test
   public void testYear() throws IOException {
      Year year = Year.of(1985);
      testConfiguration.method.marshallAndUnmarshallTest(year, context, false);
   }

   @Test
   public void testZoneId() throws IOException {
      ZoneId zid = ZoneId.systemDefault();
      testConfiguration.method.marshallAndUnmarshallTest(zid, context, false);
   }

   @Test
   public void testOffset() throws IOException {
      ZoneOffset offset = ZoneOffset.of("+07:00");
      testConfiguration.method.marshallAndUnmarshallTest(offset, context, false);
   }


   @Test
   public void testZonedTime() throws IOException {
      ZonedDateTime time = ZonedDateTime.of(1985, 10, 26, 0, 59, 0, 0, ZoneId.of("+07:00"));
      testConfiguration.method.marshallAndUnmarshallTest(time, context, false);
   }

   @Test
   public void testMultipleAdaptersForInterface() throws IOException {
      // Skip JSON test as WrappedMessage instances are not serialized/parsed with JSON correctly
      // https://github.com/infinispan/protostream/issues/379
      Assume.assumeFalse(testConfiguration.method == MarshallingMethodType.JSON);
      testConfiguration.method.marshallAndUnmarshallTest(Collections.emptyList(), context, false);
      testConfiguration.method.marshallAndUnmarshallTest(Collections.singletonList("1"), context, false);
      testConfiguration.method.marshallAndUnmarshallTest(List.of(), context, false);
      testConfiguration.method.marshallAndUnmarshallTest(List.of(1), context, false);
   }

   @FunctionalInterface
   public interface MarshallingMethod {
      void marshallAndUnmarshallTest(Object original, ImmutableSerializationContext ctx, boolean isArray) throws IOException;
   }

   public record TestConfiguration(MarshallingMethod method, boolean runTest, boolean isArray,
                                   Supplier<Collection<Object>> collectionBuilder) {

   }

   private static ImmutableSerializationContext newContext(boolean wrapCollectionElements) {
      var config = Configuration.builder().wrapCollectionElements(wrapCollectionElements).build();
      var ctx = ProtobufUtil.newSerializationContext(config);
      register(new CommonTypesSchema(), ctx);
      register(new CommonContainerTypesSchema(), ctx);
      register(new BookSchemaImpl(), ctx);
      register(new ListSchemaImpl(), ctx);
      return ctx;
   }

   private static void register(GeneratedSchema schema, SerializationContext context) {
      schema.registerSchema(context);
      schema.registerMarshallers(context);
   }

   private static Collection<Object> stringCollection(Supplier<Collection<Object>> supplier) {
      var collection = supplier.get();
      collection.add("a");
      collection.add("b");
      collection.add("c");
      return collection;
   }

   private static Collection<Object> bookCollection(Supplier<Collection<Object>> supplier) {
      var collection = supplier.get();
      collection.add(new Book("Book1", "Description1", 2020));
      collection.add(new Book("Book2", "Description2", 2021));
      collection.add(new Book("Book3", "Description3", 2022));
      return collection;
   }

   private static String[] stringArray() {
      return new String[]{"a", "b", "c"};
   }

   private static Object[] bookArray() {
      // cannot use new Book[] because there is no marshaller for it.
      return new Object[]{
            new Book("Book1", "Description1", 2020),
            new Book("Book2", "Description2", 2021),
            new Book("Book3", "Description3", 2022)
      };
   }

   enum MarshallingMethodType implements MarshallingMethod {
      WRAPPED_MESSAGE {
         @Override
         public void marshallAndUnmarshallTest(Object original, ImmutableSerializationContext ctx, boolean isArray) throws IOException {
            var bytes = ProtobufUtil.toWrappedByteArray(ctx, original, 512);
            var copy = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
            log.debugf("Wrapped Message: bytes length=%s, original=%s, copy=%s", bytes.length, original, copy);
            if (isArray) {
               assertArrayEquals((Object[]) original, (Object[]) copy);
            } else {
               assertEquals(original, copy);
            }
         }
      },
      INPUT_STREAM {
         @Override
         public void marshallAndUnmarshallTest(Object original, ImmutableSerializationContext ctx, boolean isArray) throws IOException {
            var baos = new ByteArrayOutputStream(512);
            ProtobufUtil.writeTo(ctx, baos, original);
            var bais = new ByteArrayInputStream(baos.toByteArray());
            var copy = ProtobufUtil.readFrom(ctx, bais, original.getClass());
            log.debugf("Input Stream: bytes length=%s, original=%s, copy=%s", baos.size(), original, copy);
            if (isArray) {
               assertArrayEquals((Object[]) original, (Object[]) copy);
            } else {
               assertEquals(original, copy);
            }
         }
      },
      BYTE_ARRAY {
         @Override
         public void marshallAndUnmarshallTest(Object original, ImmutableSerializationContext ctx, boolean isArray) throws IOException {
            var baos = new ByteArrayOutputStream(512);
            ProtobufUtil.writeTo(ctx, baos, original);
            var copy = ProtobufUtil.fromByteArray(ctx, baos.toByteArray(), original.getClass());
            log.debugf("Byte Array: bytes length=%s, original=%s, copy=%s", baos.size(), original, copy);
            if (isArray) {
               assertArrayEquals((Object[]) original, (Object[]) copy);
            } else {
               assertEquals(original, copy);
            }
         }
      },
      JSON {
         @Override
         public void marshallAndUnmarshallTest(Object original, ImmutableSerializationContext ctx, boolean isArray) throws IOException {
            var bytes = ProtobufUtil.toWrappedByteArray(ctx, original, 512);

            var json = ProtobufUtil.toCanonicalJSON(ctx, bytes);
            var jsonBytes = ProtobufUtil.fromCanonicalJSON(ctx, new StringReader(json));

            var copy = ProtobufUtil.fromWrappedByteArray(ctx, jsonBytes);

            log.debugf("JSON: JSON bytes length=%s, JSON String=%s, original=%s, copy=%s", jsonBytes.length, json, original, copy);
            if (isArray) {
               assertArrayEquals((Object[]) original, (Object[]) copy);
            } else {
               assertEquals(original, copy);
            }
         }
      }
   }

}
