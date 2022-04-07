package org.infinispan.protostream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.infinispan.protostream.domain.Account;
import org.infinispan.protostream.domain.Address;
import org.infinispan.protostream.domain.User;
import org.infinispan.protostream.test.AbstractProtoStreamTest;
import org.junit.Test;

/**
 * @author anistor@redhat.com
 */
public class MarshallingTest extends AbstractProtoStreamTest {

   @Test
   public void testMarshallUserUsingByteArray() throws Exception {
      doMarshallUserTest(useByteArray());
   }

   @Test
   public void testMarshallUserUsingInputStream() throws Exception {
      doMarshallUserTest(useInputStream());
   }

   private void doMarshallUserTest(EncoderMethod<User> method) throws Exception {
      ImmutableSerializationContext ctx = createContext();

      User user = new User();
      user.setId(1);
      user.setName("John");
      user.setSurname("Batman");
      user.setGender(User.Gender.MALE);
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3)));
      user.setAddresses(Collections.singletonList(new Address("Old Street", "XYZ42", -12)));

      User decoded = method.encodeAndDecode(user, ctx);

      assertEquals(1, decoded.getId());
      assertEquals("John", decoded.getName());
      assertEquals("Batman", decoded.getSurname());
      assertEquals(User.Gender.MALE, decoded.getGender());

      assertNotNull(decoded.getAddresses());
      assertEquals(1, decoded.getAddresses().size());
      assertEquals("Old Street", decoded.getAddresses().get(0).getStreet());
      assertEquals("XYZ42", decoded.getAddresses().get(0).getPostCode());

      assertNotNull(decoded.getAccountIds());
      assertEquals(2, decoded.getAccountIds().size());
      assertTrue(decoded.getAccountIds().contains(1));
      assertTrue(decoded.getAccountIds().contains(3));
   }

   @Test
   public void testMarshallAccount() throws Exception {
      doMarshallAccountTest(useByteArray());
   }

   @Test
   public void testMarshallAccountUsingInputStream() throws Exception {
      doMarshallAccountTest(useInputStream());
   }

   private void doMarshallAccountTest(EncoderMethod<Account> encoderMethod) throws Exception {
      ImmutableSerializationContext ctx = createContext();

      Account account = new Account();
      account.setId(1);
      account.setDescription("test account");
      account.setCurrencies(new Account.Currency[]{Account.Currency.BRL});
      Date creationDate = new Date();
      account.setCreationDate(creationDate);
      Account.Limits limits = new Account.Limits();
      limits.setMaxDailyLimit(0.0);
      limits.setMaxTransactionLimit(0.0);
      account.setHardLimits(limits);
      List<byte[]> blurb = new ArrayList<>();
      blurb.add(new byte[0]);
      blurb.add(new byte[]{1, 2, 3});
      account.setBlurb(blurb);

      Account decoded = encoderMethod.encodeAndDecode(account, ctx);

      assertEquals(1, decoded.getId());
      assertEquals("test account", decoded.getDescription());
      assertEquals(creationDate, decoded.getCreationDate());

      assertNotNull(decoded.getBlurb());
      assertEquals(2, decoded.getBlurb().size());
      assertEquals(0, decoded.getBlurb().get(0).length);
      assertEquals(3, decoded.getBlurb().get(1).length);
      assertArrayEquals(new byte[]{1, 2, 3}, decoded.getBlurb().get(1));
      assertArrayEquals(new Account.Currency[]{Account.Currency.BRL}, decoded.getCurrencies());
   }

   @FunctionalInterface
   private interface EncoderMethod<T> {
      T encodeAndDecode(T object, ImmutableSerializationContext ctx) throws IOException;
   }

   private static <T> EncoderMethod<T> useByteArray() {
      return (object, ctx) -> {
         ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
         ProtobufUtil.writeTo(ctx, baos, object);
         //noinspection unchecked
         return ProtobufUtil.fromByteArray(ctx, baos.toByteArray(), (Class<T>) object.getClass());
      };
   }

   private static <T> EncoderMethod<T> useInputStream() {
      return (object, ctx) -> {
         ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
         ProtobufUtil.writeTo(ctx, baos, object);
         InputStream is = new ByteArrayInputStream(baos.toByteArray());
         //noinspection unchecked
         return ProtobufUtil.readFrom(ctx, is, (Class<T>) object.getClass());
      };
   }
}
