package org.infinispan.protostream;

import static org.infinispan.protostream.domain.Account.Currency.BRL;
import static org.infinispan.protostream.domain.Account.Currency.USD;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.domain.Account;
import org.infinispan.protostream.domain.Address;
import org.infinispan.protostream.domain.User;
import org.infinispan.protostream.test.AbstractProtoStreamTest;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * @author anistor@redhat.com
 */
public class ProtobufUtilTest extends AbstractProtoStreamTest {

   private static final Gson GSON = new Gson();

   @Test(expected = InvalidProtocolBufferException.class)
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
   public void testWrappedMessageTypeMapper() throws Exception {
      WrappedMessageTypeMapper mapper = new WrappedMessageTypeMapper() {
         @Override
         public int mapTypeId(int typeId, boolean isReading, ImmutableSerializationContext ctx) {
            if (typeId == 100042) { // change typeId ouf User
               return 100021;
            }
            return typeId;
         }

         @Override
         public String mapTypeName(String typeName, boolean isReading, ImmutableSerializationContext ctx) {
            return typeName;
         }
      };

      Configuration cfg = Configuration.builder()
            .wrappingConfig()
            .wrappedMessageTypeMapper(mapper)
            .build();

      ImmutableSerializationContext ctx = createContext(cfg);

      // this has TypeId 100042
      User user = new User();
      user.setId(1);
      user.setName("John");
      user.setSurname("Batman");
      user.setGender(User.Gender.MALE);

      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, user);

      int[] seenTypeId = new int[] { -1 };

      TagHandler tagHandler = new TagHandler() {
         @Override
         public void onTag(int fieldNumber, FieldDescriptor fieldDescriptor, Object tagValue) {
            if (fieldNumber == WrappedMessage.WRAPPED_DESCRIPTOR_TYPE_ID) {
               seenTypeId[0] = (Integer) tagValue;
            }
         }
      };

      Descriptor wrappedMessageDescriptor = ctx.getMessageDescriptor(WrappedMessage.PROTOBUF_TYPE_NAME);
      ProtobufParser.INSTANCE.parse(tagHandler, wrappedMessageDescriptor, bytes);

      assertEquals(100021, seenTypeId[0]);
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
   public void testMissingRequiredField() throws Exception {
      Throwable error = testFromJson("{\"_type\":\"sample_bank_account.User.Address\",\"street\":\"Abbey Rd\",\"postCode\":\"NW89AY\",\"number\": 12}");
      assertTrue(error instanceof IllegalStateException);
      assertTrue(error.getMessage().contains("Required field 'isCommercial' missing"));
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
      assertTrue(error.getMessage().contains("should contain a top level field '_type'"));
   }

   @Test
   public void testWithMissingValueSpecialField() throws Exception {
      Throwable error = testFromJson("{ \"_type\":\"double\", \"person\": true }");
      assertTrue(error instanceof IllegalStateException);
      assertTrue(error.getMessage().contains("should contain a top level field '_value'"));
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
   public void testEscaping() throws Exception {
      ImmutableSerializationContext ctx = createContext();

      String unescapedString = "This is a line.\nThis is another line.\tA tab later in the same line";

      byte[] marshalled = ProtobufUtil.toWrappedByteArray(ctx, unescapedString);
      String json = ProtobufUtil.toCanonicalJSON(ctx, marshalled, false);

      assertEquals("{\"_type\":\"string\",\"_value\":\"This is a line.\\nThis is another line.\\tA tab later in the same line\"}", json);
   }

   @Test
   public void testArrayOfEnum() throws Exception {
      Account account = createAccount();
      SerializationContext context = createContext();
      byte[] bytes = ProtobufUtil.toWrappedByteArray(context, account);

      Account acc = ProtobufUtil.fromWrappedByteArray(context, bytes);
      assertEquals(acc, account);
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
      byte[] marshalled = ProtobufUtil.toWrappedByteArray(ctx, object);
      String json = ProtobufUtil.toCanonicalJSON(ctx, marshalled, prettyPrint);
      assertValid(json);
      byte[] bytes = ProtobufUtil.fromCanonicalJSON(ctx, new StringReader(json));
      assertEquals(object, ProtobufUtil.fromWrappedByteArray(ctx, bytes));
      assertArrayEquals(marshalled, bytes);
   }

   private <T> void testJsonConversion(ImmutableSerializationContext ctx, T object) throws IOException {
      testJsonConversion(ctx, object, false);
   }

   private void assertValid(String json) {
      try {
         GSON.fromJson(json, Object.class);
      } catch (JsonSyntaxException e) {
         fail("Invalid json found:" + json);
      }
   }
}
