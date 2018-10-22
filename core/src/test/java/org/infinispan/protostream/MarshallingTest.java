package org.infinispan.protostream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
   public void testMarshallUser() throws Exception {
      ImmutableSerializationContext ctx = createContext();

      User user = new User();
      user.setId(1);
      user.setName("John");
      user.setSurname("Batman");
      user.setGender(User.Gender.MALE);
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3)));
      user.setAddresses(Collections.singletonList(new Address("Old Street", "XYZ42", -12)));

      byte[] bytes = ProtobufUtil.toByteArray(ctx, user);

      User decoded = ProtobufUtil.fromByteArray(ctx, bytes, User.class);

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

      byte[] bytes = ProtobufUtil.toByteArray(ctx, account);

      Account decoded = ProtobufUtil.fromByteArray(ctx, bytes, Account.class);

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
}
