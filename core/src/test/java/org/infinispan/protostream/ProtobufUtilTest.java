package org.infinispan.protostream;

import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.Arrays;
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
public class ProtobufUtilTest extends AbstractProtoStreamTest {

   @Test
   public void testWrapping() throws Exception {
      SerializationContext ctx = createContext();

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
   public void testCanonicalJSON() throws Exception {
      SerializationContext ctx = createContext();

      User user = new User();
      user.setId(1);
      user.setName("John");
      user.setSurname("Batman");
      user.setGender(User.Gender.MALE);
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3)));
      user.setAddresses(Arrays.asList(new Address("Old Street", "XYZ42", -12), new Address("Bond Street", "W23", 2)));

      byte[] userBytes = ProtobufUtil.toWrappedByteArray(ctx, user);
      System.out.printf("Canonical JSON out:%s\n", ProtobufUtil.toCanonicalJSON(ctx, userBytes));

      Account account = new Account();
      account.setId(1);
      account.setDescription("test account");
      Date creationDate = new Date();
      account.setCreationDate(creationDate);
      List<byte[]> blurb = new ArrayList<>();
      blurb.add(new byte[0]);
      blurb.add(new byte[]{123});
      blurb.add(new byte[]{1, 2, 3, 4});
      account.setBlurb(blurb);

      byte[] accountBytes = ProtobufUtil.toWrappedByteArray(ctx, account);
      System.out.printf("Canonical JSON out:%s\n", ProtobufUtil.toCanonicalJSON(ctx, accountBytes));

      byte[] wrappedUserBytes = ProtobufUtil.toWrappedByteArray(ctx, new WrappedMessage(user));
      System.out.printf("Canonical JSON out:%s\n", ProtobufUtil.toCanonicalJSON(ctx, wrappedUserBytes));

      byte[] nullBytes = ProtobufUtil.toWrappedByteArray(ctx, null);
      System.out.printf("Canonical JSON out:%s\n", ProtobufUtil.toCanonicalJSON(ctx, nullBytes));

      byte[] booleanBytes = ProtobufUtil.toWrappedByteArray(ctx, true);
      System.out.printf("Canonical JSON out:%s\n", ProtobufUtil.toCanonicalJSON(ctx, booleanBytes));

      byte[] enumBytes = ProtobufUtil.toWrappedByteArray(ctx, User.Gender.FEMALE);
      System.out.printf("Canonical JSON out:%s\n", ProtobufUtil.toCanonicalJSON(ctx, enumBytes));

      byte[] floatBytes = ProtobufUtil.toWrappedByteArray(ctx, 3.14f);
      System.out.printf("Canonical JSON out:%s\n", ProtobufUtil.toCanonicalJSON(ctx, floatBytes));

      byte[] doubleBytes = ProtobufUtil.toWrappedByteArray(ctx, 3.14);
      System.out.printf("Canonical JSON out:%s\n", ProtobufUtil.toCanonicalJSON(ctx, doubleBytes));

      byte[] doubleNaNBytes = ProtobufUtil.toWrappedByteArray(ctx, Double.NaN);
      System.out.printf("Canonical JSON out:%s\n", ProtobufUtil.toCanonicalJSON(ctx, doubleNaNBytes));

      byte[] intBytes = ProtobufUtil.toWrappedByteArray(ctx, 1);
      System.out.printf("Canonical JSON out:%s\n", ProtobufUtil.toCanonicalJSON(ctx, intBytes));

      byte[] longBytes = ProtobufUtil.toWrappedByteArray(ctx, 777L);
      System.out.printf("Canonical JSON out:%s\n", ProtobufUtil.toCanonicalJSON(ctx, longBytes));

      byte[] stringBytes = ProtobufUtil.toWrappedByteArray(ctx, "Merry Christmas, you filthy animal. And a Happy New Year!");
      System.out.printf("Canonical JSON out:%s\n", ProtobufUtil.toCanonicalJSON(ctx, stringBytes, false));
   }
}
