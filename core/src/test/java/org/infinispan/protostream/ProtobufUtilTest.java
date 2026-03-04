package org.infinispan.protostream;

import static org.infinispan.protostream.domain.Account.Currency.BRL;
import static org.infinispan.protostream.domain.Account.Currency.USD;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

import org.infinispan.protostream.domain.Account;
import org.infinispan.protostream.domain.Address;
import org.infinispan.protostream.domain.Item;
import org.infinispan.protostream.domain.Numerics;
import org.infinispan.protostream.domain.User;
import org.infinispan.protostream.test.AbstractProtoStreamTest;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

/**
 * @author anistor@redhat.com
 */
public class ProtobufUtilTest extends AbstractProtoStreamTest {

   @Test
   public void testComputeMessageSize() throws Exception {
      ImmutableSerializationContext ctx = createContext();

      User user = new User();
      user.setId(1);
      user.setName("John");
      user.setSurname("Batman");
      user.setGender(User.Gender.MALE);
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3)));
      user.setAddresses(Arrays.asList(new Address("Old Street", "XYZ42", -12), new Address("Bond Street", "W23", 2)));

      int expectedMessageSize = ProtobufUtil.toByteArray(ctx, user).length;

      int messageSize = ProtobufUtil.computeMessageSize(ctx, user);
      int estimateSize = ProtobufUtil.estimateMessageSize(ctx, user);

      assertTrue("Estimate was: " + estimateSize + " and actual is: " + messageSize, estimateSize >= messageSize);

      assertEquals(expectedMessageSize, messageSize);

      expectedMessageSize = ProtobufUtil.toWrappedByteArray(ctx, user).length;

      messageSize = ProtobufUtil.computeWrappedMessageSize(ctx, user);
      estimateSize = ProtobufUtil.estimateMessageSize(ctx, user);

      assertTrue("Estimate was: " + estimateSize + " and actual is: " + messageSize, estimateSize >= messageSize);

      assertEquals(expectedMessageSize, messageSize);
   }

   @Test(expected = MalformedProtobufException.class)
   public void testFromByteArrayWithExtraPadding() throws Exception {
      ImmutableSerializationContext ctx = createContext();

      User user = new User();
      user.setId(1);
      user.setName("John");
      user.setSurname("Batman");
      user.setGender(User.Gender.MALE);
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3)));
      user.setAddresses(Arrays.asList(new Address("Old Street", "XYZ42", -12), new Address("Bond Street", "W23", 2)));

      byte[] userBytes = ProtobufUtil.toByteArray(ctx, user);
      byte[] userBytesWithPadding = new byte[userBytes.length + 20];
      System.arraycopy(userBytes, 0, userBytesWithPadding, 0, userBytes.length);
      Arrays.fill(userBytesWithPadding, userBytes.length, userBytes.length + 20, (byte) 42);

      ProtobufUtil.fromByteArray(ctx, userBytesWithPadding, User.class); // this must fail
   }

   @Test(expected = IllegalStateException.class)
   public void testFromWrappedByteArrayWithExtraPadding() throws Exception {
      ImmutableSerializationContext ctx = createContext();

      User user = new User();
      user.setId(1);
      user.setName("John");
      user.setSurname("Batman");
      user.setGender(User.Gender.MALE);
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3)));
      user.setAddresses(Arrays.asList(new Address("Old Street", "XYZ42", -12), new Address("Bond Street", "W23", 2)));

      byte[] userBytes = ProtobufUtil.toWrappedByteArray(ctx, user);
      byte[] userBytesWithPadding = new byte[userBytes.length + 20];
      System.arraycopy(userBytes, 0, userBytesWithPadding, 0, userBytes.length);
      Arrays.fill(userBytesWithPadding, userBytes.length, userBytes.length + 20, (byte) 42);

      ProtobufUtil.fromWrappedByteArray(ctx, userBytesWithPadding); // this must fail
   }

   @Test
   public void testMessageWrapping() throws Exception {
      ImmutableSerializationContext ctx = createContext();

      User user = new User();
      user.setId(1);
      user.setName("John");
      user.setSurname("Batman");
      user.setGender(User.Gender.MALE);
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3)));
      user.setAddresses(Arrays.asList(new Address("Old Street", "XYZ42", -12), new Address("Bond Street", "W23", 2)));

      byte[] userBytes1 = ProtobufUtil.toWrappedByteArray(ctx, user);

      byte[] userBytes2 = ProtobufUtil.toByteArray(ctx, new WrappedMessage(user));

      // assert that toWrappedByteArray works correctly as a shorthand for toByteArray on a WrappedMessage
      assertArrayEquals(userBytes1, userBytes2);
   }

   @Test
   public void testMessageWrappingStream() throws Exception {
      ImmutableSerializationContext ctx = createContext();

      User user = new User();
      user.setId(1);
      user.setName("John");
      user.setSurname("Batman");
      user.setGender(User.Gender.MALE);
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3)));
      user.setAddresses(Arrays.asList(new Address("Old Street", "XYZ42", -12), new Address("Bond Street", "W23", 2)));

      byte[] userBytes1;
      try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
         ProtobufUtil.toWrappedStream(ctx, out, user);
         userBytes1 = out.toByteArray();
      }

      try (InputStream in = new ByteArrayInputStream(userBytes1)) {
         User user1 = ProtobufUtil.fromWrappedStream(ctx, in, userBytes1.length);
         assertEquals(user, user1);
      }
   }

   @Test
   public void testWithInvalidJson() throws Exception {
      Throwable error = testFromJson("john");
      assertTrue(error instanceof IllegalStateException);
      assertTrue(error.getMessage().contains("Invalid JSON"));
   }

   @Test
   public void testWithInvalidMetaField() throws Exception {
      Throwable error = testFromJson("{\"_type\":\"inexistent\",\"street\":\"Abbey Rd\",\"postCode\":\"NW89AY\",\"number\":\"true\"}");
      assertNotNull(error);
   }

   @Test
   public void testWithMismatchedFieldType() throws Exception {
      Throwable error = testFromJson("{\"_type\":\"sample_bank_account.User.Address\",\"street\":\"Abbey Rd\",\"postCode\":\"NW89AY\",\"number\":\"true\"}");
      assertTrue(error instanceof NumberFormatException);
   }

   @Test
   public void testWithMismatchedArrayType() throws Exception {
      Throwable error = testFromJson("{\"_type\":\"sample_bank_account.User\",\"id\":[1,2,3],\"accountIds\":[12,24],\"name\":\"John\",\"surname\":\"Batman\",\"gender\":\"MALE\"}");
      assertTrue(error instanceof IllegalStateException);
      assertTrue(error.getMessage().contains("not an array"));
   }

   @Test
   public void testWithMismatchedMessageType() throws Exception {
      Throwable error = testFromJson("{\"_type\":\"sample_bank_account.User\",\"id\":1,\"accountIds\":[12,24],\"name\":{\"name\":\"John\"},\"surname\":\"Batman\",\"gender\":\"MALE\"}");
      assertTrue(error instanceof IllegalStateException);
      assertTrue(error.getMessage().contains("Field 'name' is not an object"));
   }

   @Test
   public void testWithMismatchedArrayType2() throws Exception {
      Throwable error = testFromJson("{\"_type\":\"sample_bank_account.User\",\"id\":1,\"accountIds\":[12,24],\"name\":[1,2,3],\"surname\":\"Batman\",\"gender\":\"MALE\"}");
      assertTrue(error instanceof IllegalStateException);
      assertTrue(error.getMessage().contains("Field 'name' is not an array"));
   }

   @Test
   public void testJsonWithDifferentFieldOrder() throws Exception {
      SerializationContext ctx = createContext();
      String json = "{\"_type\":\"sample_bank_account.Account\",\"hardLimits\":{\"maxDailyLimit\":5,\"maxTransactionLimit\":35},\"limits\":{\"maxDailyLimit\":1.5,\"maxTransactionLimit\":3.5,\"payees\":[\"Madoff\", \"Ponzi\"]},\"description\":\"test account\",\"creationDate\":\"1500508800000\",\"blurb\":[\"\",\"ew==\",\"AQIDBA==\"],\"currencies\":[\"USD\",\"BRL\"],\"id\":1}";
      byte[] bytes = ProtobufUtil.fromCanonicalJSON(ctx, new StringReader(json));

      Account account = ProtobufUtil.fromWrappedByteArray(ctx, bytes);

      assertEquals(createAccount(), account);
   }

   @Test
   public void testJsonBytesWithIntArray() throws Exception {
      SerializationContext context = createContext();
      Item.ItemSchema.INSTANCE.register(context);

      Item java = new Item("c7", new byte[]{7, 7, 7}, new float[]{1.1f, 1.1f, 1.1f}, new int[]{7, 7, 7}, "bla bla bla");
      byte[] proto = ProtobufUtil.toWrappedByteArray(context, java);
      String json = ProtobufUtil.toCanonicalJSON(context, proto);

      // Covert back JSON -> Proto -> Java
      byte[] proto2 = ProtobufUtil.fromCanonicalJSON(context, new StringReader(json));
      Item java2 = ProtobufUtil.fromWrappedByteArray(context, proto2);
      assertEquals(java, java2);

      // Moreover, we want to parse also Java byte[] / Proto bytes fields in the JSON format `[7,7,7]`
      String alternativeJson = "{\"_type\":\"Item\",\"code\":\"c7\",\"byteVector\":[7,7,7],\"floatVector\":[1.1,1.1,1.1],\"integerVector\":[7,7,7],\"buggy\":\"bla bla bla\"}";
      byte[] proto3 = ProtobufUtil.fromCanonicalJSON(context, new StringReader(alternativeJson));
      Item java3 = ProtobufUtil.fromWrappedByteArray(context, proto3);
      assertEquals(java, java3);
   }

   @Test
   public void testJsonWithNull() throws Exception {
      SerializationContext ctx = createContext();
      String json = "null";
      byte[] bytes = ProtobufUtil.fromCanonicalJSON(ctx, new StringReader(json));

      Object obj = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertNull(obj);
   }

   @Test
   public void testWithMalformedJson() throws Exception {
      Throwable error = testFromJson("{'_type':'sample_bank_account.User.Address','street':'Abbey Rd',}");
      assertTrue(error instanceof IllegalStateException);
      assertTrue(error.getMessage().contains("Invalid JSON"));
   }

   @Test
   public void testWithInvalidTopLevelObject() throws Exception {
      Throwable error = testFromJson("[{\"a\":1}]");
      assertTrue(error instanceof IllegalStateException);
      assertTrue(error.getMessage().contains("Invalid top level object"));
   }

   @Test
   public void testWithMissingTypeSpecialField() throws Exception {
      Throwable error = testFromJson("{ \"street\":\"Abbey Rd\" }");
      assertTrue(error instanceof IllegalStateException);
      assertTrue(error.getMessage().contains("Expected field '_type' but it was 'street'"));
   }

   @Test
   public void testWithMissingValueSpecialField() throws Exception {
      Throwable error = testFromJson("{ \"_type\":\"double\", \"person\": true }");
      assertTrue(error instanceof IllegalStateException);
      assertTrue(error.getMessage().contains("Expected field '_value' but it was 'person"));
   }

   @Test
   public void testWithInvalidField() throws Exception {
      Throwable error = testFromJson("{\"_type\":\"sample_bank_account.User.Address\",\"rua\":\"Abbey Rd\",\"postCode\":\"NW89AY\",\"number\":3}");
      assertTrue(error instanceof IllegalStateException);
      assertTrue(error.getMessage().contains("field 'rua' was not found"));
   }

   @Test
   public void testWithInvalidEnumField() throws Exception {
      Throwable error = testFromJson("{\"_type\":\"sample_bank_account.User\",\"id\":1,\"accountIds\":[1,3],\"name\":\"John\",\"surname\":\"Batman\",\"gender\":\"NOT SURE\"}");
      assertTrue(error instanceof IllegalStateException);
      assertTrue(error.getMessage().contains("Invalid enum value 'NOT SURE'"));
   }

   @Test
   public void testWithNullEnumValue() throws Exception {
      Throwable error = testFromJson("{\"_type\":\"sample_bank_account.Account.Currency\",\"_value\": null}");
      assertTrue(error instanceof IllegalStateException);
      assertTrue(error.getMessage().contains("Invalid enum value 'null'"));
   }

   @Test
   public void testWithWrongTypeEnumValue() throws Exception {
      Throwable error = testFromJson("{\"_type\":\"sample_bank_account.Account.Currency\",\"_value\":true}");
      assertTrue(error instanceof IllegalStateException);
      assertTrue(error.getMessage().contains("Invalid enum value 'true'"));
   }

   @Test
   public void testCanonicalJSON() throws Exception {
      ImmutableSerializationContext ctx = createContext();

      User user = new User();
      user.setId(1);
      user.setName("John");
      user.setSurname("Batman");
      user.setGender(User.Gender.MALE);
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3)));
      user.setAddresses(Arrays.asList(new Address("Old Street", "XYZ42", -12, false), new Address("Bond Street", "W23", 2, true)));

      Address address = new Address("Abbey Rd", "NW89AY", 3);

      Account account = createAccount();

      testJsonConversion(ctx, address);
      testJsonConversion(ctx, 3.14);
      testJsonConversion(ctx, 777L);
      testJsonConversion(ctx, 3.14f);
      testJsonConversion(ctx, 1);
      testJsonConversion(ctx, null);
      testJsonConversion(ctx, true);
      testJsonConversion(ctx, "Merry Christmas, you filthy animal. And a Happy New Year!");
      testJsonConversion(ctx, User.Gender.FEMALE);
      testJsonConversion(ctx, account);
      testJsonConversion(ctx, user);
   }

   @Test
   public void testNumericPrimitives() throws Exception {
      ImmutableSerializationContext ctx = createContext();
      Numerics numerics = new Numerics(Byte.MAX_VALUE, Short.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE, Float.MAX_VALUE, Double.MAX_VALUE);

      testJsonConversion(ctx, numerics);

      testJsonConversion(ctx, 1);
      testJsonConversion(ctx, (byte) 1, false, Integer::byteValue);
      testJsonConversion(ctx, (short) 1, false, Integer::shortValue);
      testJsonConversion(ctx, (long) 1);
      testJsonConversion(ctx, (float) 1);
      testJsonConversion(ctx, (double) 1);
   }

   @Test
   public void testJsonLong() throws IOException {
      ImmutableSerializationContext ctx = createContext();

      User user = new User();
      user.setName("");
      user.setId(1);
      user.setQrCode(12345667L);

      byte[] marshalled = ProtobufUtil.toWrappedByteArray(ctx, user);
      String json = ProtobufUtil.toCanonicalJSON(ctx, marshalled, true);

      assertTrue(json.contains("\"qrCode\": 12345667"));
   }

   @Test
   public void testEscaping() throws Exception {
      ImmutableSerializationContext ctx = createContext();

      String unescapedString = "This is a line.\nThis is another line.\tA tab later in the same line";

      byte[] marshalled = ProtobufUtil.toWrappedByteArray(ctx, unescapedString);
      String json = ProtobufUtil.toCanonicalJSON(ctx, marshalled, false);

      assertEquals("{\"_type\":\"string\",\"_value\":\"This is a line.\\nThis is another line.\\tA tab later in the same line\"}", json);
   }

   @Test
   public void testLatin1EncodedString() throws Exception {
      ImmutableSerializationContext ctx = createContext();

      // String with Latin-1 specific characters
      String latin1String = "Grüße aus München und Zürich!";

      byte[] marshalled = ProtobufUtil.toWrappedByteArray(ctx, latin1String);
      String unmarshalledString = ProtobufUtil.fromWrappedByteArray(ctx, marshalled);

      assertEquals(latin1String, unmarshalledString);
   }

   @Test
   public void testUtf8EncodedString() throws Exception {
      ImmutableSerializationContext ctx = createContext();

      // String with UTF-8 specific characters (e.g., emojis, characters from other languages)
      String utf8String = "你好世界! This is a test with UTF-8 characters. 👋";

      byte[] marshalled = ProtobufUtil.toWrappedByteArray(ctx, utf8String);
      String unmarshalledString = ProtobufUtil.fromWrappedByteArray(ctx, marshalled);

      assertEquals(utf8String, unmarshalledString);
   }

   @Test
   public void testArrayOfEnum() throws Exception {
      Account account = createAccount();
      SerializationContext context = createContext();
      byte[] bytes = ProtobufUtil.toWrappedByteArray(context, account);

      Account acc = ProtobufUtil.fromWrappedByteArray(context, bytes);
      assertEquals(acc, account);
   }

   @Test
   public void test32BitsNumberBoundaries() throws IOException {
      ImmutableSerializationContext ctx = createContext();

      //uint32 / fixed32 are both unsigned integer
      // negative -> exception
      expectNumberFormatExceptionWithJson(ctx, "uint32", "-1");
      expectNumberFormatExceptionWithJson(ctx, "fixed32", "-1");
      // max uint32 value -> ok
      assertJsonNumber(ctx, "uint32", "4294967295");
      assertJsonNumber(ctx, "fixed32", "4294967295");
      // more than unsigned int value -> exception
      expectNumberFormatExceptionWithJson(ctx, "uint32", "4294967296");
      expectNumberFormatExceptionWithJson(ctx, "fixed32", "4294967296");

      // int32, sfixed32, sint32 are signed integers
      // less than Integer.MIN_VALUE -> exception
      expectNumberFormatExceptionWithJson(ctx, "int32", "-2147483649");
      expectNumberFormatExceptionWithJson(ctx, "sfixed32", "-2147483649");
      expectNumberFormatExceptionWithJson(ctx, "sint32", "-2147483649");
      // Integer.MIN_VALUE -> ok
      assertJsonNumber(ctx, "int32", "-2147483648");
      assertJsonNumber(ctx, "sfixed32", "-2147483648");
      assertJsonNumber(ctx, "sint32", "-2147483648");
      // Integer.MAX_VALUE -> ok
      assertJsonNumber(ctx, "int32", "2147483647");
      assertJsonNumber(ctx, "sfixed32", "2147483647");
      assertJsonNumber(ctx, "sint32", "2147483647");
      // greater than Integer.MAX_VALUE -> exception
      expectNumberFormatExceptionWithJson(ctx, "int32", "2147483648");
      expectNumberFormatExceptionWithJson(ctx, "sfixed32", "2147483648");
      expectNumberFormatExceptionWithJson(ctx, "sint32", "2147483648");
   }

   @Test
   public void test64BitsNumberBoundaries() throws IOException {
      ImmutableSerializationContext ctx = createContext();

      //uint64 / fixed64 are both unsigned longs
      // negative -> exception
      expectNumberFormatExceptionWithJson(ctx, "uint64", "-1");
      expectNumberFormatExceptionWithJson(ctx, "fixed64", "-1");
      // max uint64 value -> ok
      assertJsonNumber(ctx, "uint64", "18446744073709551615");
      assertJsonNumber(ctx, "fixed64", "18446744073709551615");
      // more than unsigned int value -> exception
      expectNumberFormatExceptionWithJson(ctx, "uint64", "18446744073709551616");
      expectNumberFormatExceptionWithJson(ctx, "fixed64", "18446744073709551616");

      // int64, sfixed64, sint64 are signed longs
      // less than Long.MIN_VALUE -> exception
      expectNumberFormatExceptionWithJson(ctx, "int64", "-9223372036854775809");
      expectNumberFormatExceptionWithJson(ctx, "sfixed64", "-9223372036854775809");
      expectNumberFormatExceptionWithJson(ctx, "sint64", "-9223372036854775809");
      // Long.MIN_VALUE -> ok
      assertJsonNumber(ctx, "int64", "-9223372036854775808");
      assertJsonNumber(ctx, "sfixed64", "-9223372036854775808");
      assertJsonNumber(ctx, "sint64", "-9223372036854775808");
      // Long.MAX_VALUE -> ok
      assertJsonNumber(ctx, "int64", "9223372036854775807");
      assertJsonNumber(ctx, "sfixed64", "9223372036854775807");
      assertJsonNumber(ctx, "sint64", "9223372036854775807");
      // greater than Long.MAX_VALUE -> exception
      expectNumberFormatExceptionWithJson(ctx, "int64", "9223372036854775808");
      expectNumberFormatExceptionWithJson(ctx, "sfixed64", "9223372036854775808");
      expectNumberFormatExceptionWithJson(ctx, "sint64", "9223372036854775808");
   }

   private static StringReader primitiveNumericJson(String type, String value) {
      return new StringReader(String.format("{\"_type\": \"%s\", \"_value\": %s}", type, value));
   }

   private static void assertJsonNumber(ImmutableSerializationContext ctx, String type, String number) throws IOException {
      byte[] bytes = ProtobufUtil.fromCanonicalJSON(ctx, primitiveNumericJson(type, number));
      String json = ProtobufUtil.toCanonicalJSON(ctx, bytes);
      assertTrue("type " + type + " not in " + json, json.contains(type));
      assertTrue("value " + number + " not in " + json, json.contains(number));
   }

   private static void expectNumberFormatExceptionWithJson(ImmutableSerializationContext ctx, String type, String number) {
      try {
         ProtobufUtil.fromCanonicalJSON(ctx, primitiveNumericJson(type, number));
         fail(number + " is not a valid " + type + " numeric value");
      } catch (Exception e) {
         assertTrue("Wrong exception " + e.getMessage(), e instanceof NumberFormatException);
      }
   }

   private Account createAccount() {
      Account account = new Account();
      account.setId(1);
      account.setDescription("test account");
      Account.Limits limits = new Account.Limits();
      limits.setMaxDailyLimit(1.5);
      limits.setMaxTransactionLimit(3.5);
      limits.setPayees(new String[]{"Madoff", "Ponzi"});
      account.setLimits(limits);
      Account.Limits hardLimits = new Account.Limits();
      hardLimits.setMaxDailyLimit(5d);
      hardLimits.setMaxTransactionLimit(35d);
      account.setHardLimits(hardLimits);
      Date creationDate = Date.from(LocalDate.of(2017, 7, 20).atStartOfDay().toInstant(ZoneOffset.UTC));
      account.setCreationDate(creationDate);
      List<byte[]> blurb = new ArrayList<>();
      blurb.add(new byte[0]);
      blurb.add(new byte[]{123});
      blurb.add(new byte[]{1, 2, 3, 4});
      account.setBlurb(blurb);
      account.setCurrencies(new Account.Currency[]{USD, BRL});
      return account;
   }

   private Throwable testFromJson(String json) throws IOException {
      ImmutableSerializationContext ctx = createContext();
      try {
         ProtobufUtil.fromCanonicalJSON(ctx, new StringReader(json));
      } catch (Throwable e) {
         return e;
      }
      return null;
   }

   private <T> void testJsonConversion(ImmutableSerializationContext ctx, T object, boolean prettyPrint) throws IOException {
      testJsonConversion(ctx, object, prettyPrint, Function.identity());
   }

   private <T, C> void testJsonConversion(ImmutableSerializationContext ctx, T object, boolean prettyPrint, Function<C, T> mapper) throws IOException {
      byte[] marshalled = ProtobufUtil.toWrappedByteArray(ctx, object);
      String json = ProtobufUtil.toCanonicalJSON(ctx, marshalled, prettyPrint);
      assertValid(json);
      byte[] bytes = ProtobufUtil.fromCanonicalJSON(ctx, new StringReader(json));

      C c = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      T t = mapper.apply(c);
      assertEquals(object, t);

      // No mapping was needed between the types.
      // For example, we can serialize a byte but that is transformed into an int. This check would fail.
      if (t == c) assertArrayEquals(marshalled, bytes);
   }

   private <T> void testJsonConversion(ImmutableSerializationContext ctx, T object) throws IOException {
      testJsonConversion(ctx, object, false);
   }

   private void assertValid(String json) {
      assertTrue(json == null || !json.isEmpty());
      try (JsonParser parser = new JsonFactory().createParser(json)) {
         while (parser.nextToken() != null) {
            // read all tokens and hope for no errors
         }
      } catch (Exception e) {
         fail("Invalid JSON found : " + json);
      }
   }

   @Test
   public void testLongNestedMessage() throws Exception {
      ImmutableSerializationContext ctx = createContext();

      User user = new User();
      user.setId(1);
      user.setName("John");
      user.setSurname("Batman");
      user.setGender(User.Gender.MALE);
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3)));
      user.setAddresses(Arrays.asList(
         new Address("Old Street", "XYZ42", -12),
         // This address will serialize to be longer than 127 requiring more than 1 byte per size
         new Address("Bond Street".repeat(12), "W23", 2),
         new Address("Long Foo".repeat(20), "Y791", 23)
      ));

      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, user);

      User userResult = ProtobufUtil.fromWrappedByteArray(ctx, bytes);

      assertEquals(user, userResult);
   }

   @Test
   public void testSchemaWithRepeatedFields() throws Exception {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      final String protoDefinition =
         """
            syntax = "proto2";
            message ParentEntity {
                required string value=1;
                repeated ChildEntity childs=2;
            }
            message ChildEntity {
               required string name=1;
            }
            """;
      ctx.registerProtoFiles(FileDescriptorSource.fromString("parent_child_definition.proto", protoDefinition));

      final String jsonWithNestedWithType = """
         {
            "_type": "ParentEntity",
            "value": "Rosa",
            "childs": [
               {
                  "_type": "ChildEntity",
                  "name":"Rosita"
               }
            ]
         }""";
      byte[] protobuf = ProtobufUtil.fromCanonicalJSON(ctx, new StringReader(jsonWithNestedWithType));
      String converted = ProtobufUtil.toCanonicalJSON(ctx, protobuf);
      assertValid(converted);
      assertEquals(jsonWithNestedWithType.replaceAll("[\\s\\n]+", ""), converted);

      final String jsonWithNestedWithTypeWithoutType = """
          {
            "_type": "ParentEntity",
            "value": "Rosa",
            "childs": [
               {
                  "name":"Rosita"
               }
            ]
         }""";
      protobuf = ProtobufUtil.fromCanonicalJSON(ctx, new StringReader(jsonWithNestedWithTypeWithoutType));
      converted = ProtobufUtil.toCanonicalJSON(ctx, protobuf);
      assertValid(converted);
      assertEquals(jsonWithNestedWithType.replaceAll("[\\s\\n]+", ""), converted);
   }

   @Test
   public void testSchemaWithRepeatedFields2() throws Exception {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      final String protoDefinition = """
         syntax = "proto2";
         message Outer {
             optional string name=1;
             repeated Middle middle=2;
         }
         message Middle {
            optional string name=1;
            repeated Inner inner=2;
         }
         message Inner {
            optional string name=1;
         }""";
      ctx.registerProtoFiles(FileDescriptorSource.fromString("multilevel.proto", protoDefinition));

      final String json = """
         {
            "_type": "Outer",
            "name": "outside",
            "middle": [
               {
                  "_type": "Middle",
                  "name": "between",
                  "inner": [
                     {
                        "_type": "Inner",
                        "name": "inside"
                     }
                  ]
               }
            ]
         }""";
      byte[] protobuf = ProtobufUtil.fromCanonicalJSON(ctx, new StringReader(json));
      String converted = ProtobufUtil.toCanonicalJSON(ctx, protobuf);
      assertValid(converted);
      assertEquals(json.replaceAll("[\\s\\n]+", ""), converted);
   }

   @Test
   public void testNestedRepeatedFieldsMultipleElements() throws Exception {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      final String protoDefinition = """
         syntax = "proto2";
         message Outer {
             optional string name=1;
             repeated Middle middle=2;
         }
         message Middle {
            optional string name=1;
            repeated Inner inner=2;
         }
         message Inner {
            optional string name=1;
         }""";
      ctx.registerProtoFiles(FileDescriptorSource.fromString("multilevel.proto", protoDefinition));

      final String json = """
         {
            "_type": "Outer",
            "name": "outside",
            "middle": [
               {
                  "_type": "Middle",
                  "name": "first",
                  "inner": [
                     {
                        "_type": "Inner",
                        "name": "a"
                     },
                     {
                        "_type": "Inner",
                        "name": "b"
                     }
                  ]
               },
               {
                  "_type": "Middle",
                  "name": "second",
                  "inner": [
                     {
                        "_type": "Inner",
                        "name": "c"
                     }
                  ]
               }
            ]
         }""";
      byte[] protobuf = ProtobufUtil.fromCanonicalJSON(ctx, new StringReader(json));
      String converted = ProtobufUtil.toCanonicalJSON(ctx, protobuf);
      assertValid(converted);
      assertEquals(json.replaceAll("[\\s\\n]+", ""), converted);
   }

   @Test
   public void testThreeLevelNestedRepeatedSameFieldNumber() throws Exception {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      final String protoDefinition = """
         syntax = "proto2";
         message L1 {
             optional string name=1;
             repeated L2 items=2;
         }
         message L2 {
            optional string name=1;
            repeated L3 items=2;
         }
         message L3 {
            optional string name=1;
            repeated L4 items=2;
         }
         message L4 {
            optional string name=1;
         }""";
      ctx.registerProtoFiles(FileDescriptorSource.fromString("deep_nesting.proto", protoDefinition));

      final String json = """
         {
            "_type": "L1",
            "name": "level1",
            "items": [
               {
                  "_type": "L2",
                  "name": "level2",
                  "items": [
                     {
                        "_type": "L3",
                        "name": "level3",
                        "items": [
                           {
                              "_type": "L4",
                              "name": "level4"
                           }
                        ]
                     }
                  ]
               }
            ]
         }""";
      byte[] protobuf = ProtobufUtil.fromCanonicalJSON(ctx, new StringReader(json));
      String converted = ProtobufUtil.toCanonicalJSON(ctx, protobuf);
      assertValid(converted);
      assertEquals(json.replaceAll("[\\s\\n]+", ""), converted);
   }

   @Test
   public void testNestedRepeatedWithPrimitiveSibling() throws Exception {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      final String protoDefinition = """
         syntax = "proto2";
         message Parent {
             optional string name=1;
             repeated Child children=2;
         }
         message Child {
            optional string name=1;
            repeated Grandchild grandchildren=2;
            repeated string tags=3;
         }
         message Grandchild {
            optional string name=1;
         }""";
      ctx.registerProtoFiles(FileDescriptorSource.fromString("mixed_repeated.proto", protoDefinition));

      final String json = """
         {
            "_type": "Parent",
            "name": "root",
            "children": [
               {
                  "_type": "Child",
                  "name": "child1",
                  "grandchildren": [
                     {
                        "_type": "Grandchild",
                        "name": "gc1"
                     },
                     {
                        "_type": "Grandchild",
                        "name": "gc2"
                     }
                  ],
                  "tags": ["x","y"]
               }
            ]
         }""";
      byte[] protobuf = ProtobufUtil.fromCanonicalJSON(ctx, new StringReader(json));
      String converted = ProtobufUtil.toCanonicalJSON(ctx, protobuf);
      assertValid(converted);
      assertEquals(json.replaceAll("[\\s\\n]+", ""), converted);
   }

   @Test
   public void testSchemaWithRepeatedFieldsSameNames() throws Exception {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      final String protoDefinition =
         """
            syntax = "proto2";
            message ParentEntity {
                required string value=1;
                repeated ChildEntity child=2;
            }
            message ChildEntity {
               required string child=1;
               required string name=2;
               required string surname=3;
            }
            """;
      ctx.registerProtoFiles(FileDescriptorSource.fromString("parent_child_definition.proto", protoDefinition));

      final String jsonWithNestedWithType = """
         {
            "_type": "ParentEntity",
            "value": "Rosa",
            "child": [
               {
                  "_type": "ChildEntity",
                  "child":"Rosita",
                  "name": "Rosa",
                  "surname":"Rosae"
               }
            ]
         }""";
      byte[] protobuf = ProtobufUtil.fromCanonicalJSON(ctx, new StringReader(jsonWithNestedWithType));
      String converted = ProtobufUtil.toCanonicalJSON(ctx, protobuf);
      assertValid(converted);
      assertEquals(jsonWithNestedWithType.replaceAll("[\\s\\n]+", ""), converted);

      final String jsonWithNestedWithTypeWithoutType = """
          {
            "_type": "ParentEntity",
            "value": "Rosa",
            "child": [
               {
                  "child":"Rosita",
                  "name": "Rosa",
                  "surname":"Rosae"
               }
            ]
         }""";
      protobuf = ProtobufUtil.fromCanonicalJSON(ctx, new StringReader(jsonWithNestedWithTypeWithoutType));
      converted = ProtobufUtil.toCanonicalJSON(ctx, protobuf);
      assertValid(converted);
      assertEquals(jsonWithNestedWithType.replaceAll("[\\s\\n]+", ""), converted);
   }


   @Test
   public void testJsonSerializationWithoutMarshaller() throws Exception {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      final String protoDefinition =
         """
            syntax = "proto2";
            message Person {
               optional string name = 1;
            
               message Address {
                 optional string street = 1;
                 optional string city = 2;
                 optional string zip = 3;
               }
            
               optional Address address = 2;
            }""";
      ctx.registerProtoFiles(FileDescriptorSource.fromString("person_definition.proto", protoDefinition));

      String json = "{\"_type\":\"Person\", \"name\":\"joe\", \"address\":{\"_type\":\"Person.Address\", \"street\":\"\", \"city\":\"London\", \"zip\":\"0\"}}";
      byte[] protobuf = ProtobufUtil.fromCanonicalJSON(ctx, new StringReader(json));
      String converted = ProtobufUtil.toCanonicalJSON(ctx, protobuf);
      assertValid(converted);
      assertEquals(json.replaceAll("[\\s\\n]+", ""), converted);
   }

   @Test
   public void testSchemaWithReservedJsonFieldsRoot() {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      final String protoDefinition =
         """
            syntax = "proto2";
            message Person {
               optional string _type = 1;
               optional string name = 2;
            
               message Address {
                 optional string _type = 1;
                 optional string street = 2;
                 optional string city = 3;
                 optional string zip = 4;
               }
            
               optional Address address = 3;
            }""";
      ctx.registerProtoFiles(FileDescriptorSource.fromString("person_definition.proto", protoDefinition));

      String json = "{\"_type\":\"Person\", \"name\":\"joe\", \"address\":{\"_type\":\"Person.Address\", \"street\":\"\", \"city\":\"London\", \"zip\":\"0\"}}";
      String message = assertThrows(IllegalStateException.class, () -> ProtobufUtil.fromCanonicalJSON(ctx, new StringReader(json)))
         .getMessage();

      assertTrue("Does not contain:\n" + message, message.contains("Message for 'Person'"));
      assertTrue("Does not contain:\n" + message, message.contains("Field number '1' has reserved name '_type'"));
   }

   @Test
   public void testSchemaWithReservedJsonFieldsNested() {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      final String protoDefinition =
         """
            syntax = "proto2";
            message Person {
               optional string name = 1;
            
               message Address {
                 optional string street = 1;
                 optional string _type = 2;
                 optional string city = 3;
                 optional string zip = 4;
               }
            
               optional Address address = 2;
            }""";
      ctx.registerProtoFiles(FileDescriptorSource.fromString("person_definition.proto", protoDefinition));

      String json = "{\"_type\":\"Person\", \"name\":\"joe\", \"address\":{\"_type\":\"Person.Address\", \"street\":\"\", \"city\":\"London\", \"zip\":\"0\"}}";
      String message = assertThrows(IllegalStateException.class, () -> ProtobufUtil.fromCanonicalJSON(ctx, new StringReader(json)))
         .getMessage();

      assertTrue("Does not contain:\n" + message, message.contains("Message for 'Person.Address'"));
      assertTrue("Does not contain:\n" + message, message.contains("Field number '2' has reserved name '_type'"));
   }

   @Test
   public void testStructuredObject() throws IOException {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      final String protoDefinition = """
         syntax = "proto2";
         package mypackage;
         message Commands {
            optional string verify = 1;
            optional string decrypt = 2;
         }
         message Mail {
            optional int64 id = 1;
            optional Authentication authentication = 2;
            optional Recipients recipients = 3;
            optional Template template = 4;
         }
         message Authentication {
            optional string senderName = 1;
            optional string senderEmailId = 2;
            optional string senderPassword = 3;
            optional string emailSmtpServer = 4;
            optional string emailServerPort = 5;
            required bool ssl = 6;
         }
         message CONFIGURATIONS {
            repeated Mail mails = 1;
            optional Commands commands = 2;
            optional int32 ttl1 = 3;
            optional int32 ttl2 = 4;
            optional Output output = 5;
         }
         message Output {
            optional string path = 1;
            optional int32 timeToLive = 2;
            optional string tempFilePath = 3;
            optional int32 maxRecordCount = 4;
         }
         message Template {
            optional string description = 1;
            optional string subject = 2;
            optional string body = 3;
         }
         message Recipients {
            repeated string recipients = 1;
            repeated string recipientsCc = 2;
         }""";
      ctx.registerProtoFiles(FileDescriptorSource.fromString("configuration.proto", protoDefinition));

      String json = """
         {
           "_type": "mypackage.CONFIGURATIONS",
           "mails": [
             {
               "id": 1,
               "authentication": {
                 "senderName": "sender",
                 "senderEmailId": "user@domain.tld",
                 "senderPassword": "",
                 "emailSmtpServer": "smtp.domain.tld",
                 "emailServerPort": "25",
                 "ssl": false
               },
               "template": {
                 "description": "This is a test",
                 "subject": "This is a test",
                 "body": "This is a test"
               }
             },
             {
               "id": 2,
               "authentication": {
                 "senderName": "sender",
                 "senderEmailId": "user@domain.tld",
                 "senderPassword": "",
                 "emailSmtpServer": "smtp.domain.tld",
                 "emailServerPort": "25",
                 "ssl": false
               },
               "template": {
                 "description": "This is a test",
                 "subject": "This is a test",
                 "body": "This is a test"
               }
             },
             {
               "id": 3,
               "authentication": {
                 "senderName": "sender",
                 "senderEmailId": "user@domain.tld",
                 "senderPassword": "",
                 "emailSmtpServer": "smtp.domain.tld",
                 "emailServerPort": "25",
                 "ssl": false
               },
               "recipients": {
                 "recipients": [
                   "recipient@domain.tld"
                 ]
               },
               "template": {
                 "description": "This is a test",
                 "subject": "This is a test",
                 "body": "This is a test"
               }
             },
             {
               "id": 4,
               "authentication": {
                 "senderName": "sender",
                 "senderEmailId": "user@domain.tld",
                 "senderPassword": "",
                 "emailSmtpServer": "smtp.domain.tld",
                 "emailServerPort": "25",
                 "ssl": false
               },
               "template": {
                 "description": "This is a test",
                 "subject": "This is a test",
                 "body": "This is a test"
               }
             }
           ],
           "commands": {
             "verify": "verify-command",
             "decrypt": "decrypt-command"
           },
           "ttl1": 540,
           "ttl2": 30,
           "output": {
             "path": "/report",
             "timeToLive": 540,
             "tempFilePath": "/report/temp",
             "maxRecordCount": 5000000
           }
         }""";
      byte[] protobuf = ProtobufUtil.fromCanonicalJSON(ctx, new StringReader(json));
      String converted = ProtobufUtil.toCanonicalJSON(ctx, protobuf);
      assertValid(converted);
   }
}
