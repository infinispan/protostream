package org.infinispan.protostream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.infinispan.protostream.domain.Address;
import org.infinispan.protostream.domain.User;
import org.infinispan.protostream.test.AbstractProtoStreamTest;
import org.junit.Test;

/**
 * @author anistor@redhat.com
 */
public class WrappingTest extends AbstractProtoStreamTest {

   @Test
   public void testMarshallIntArray() throws Exception {
      MessageMarshaller<int[]> m = new MessageMarshaller<int[]>() {
         @Override
         public String getTypeName() {
            return "sample_bank_account.int_array";
         }

         @Override
         public Class<int[]> getJavaClass() {
            return int[].class;
         }

         @Override
         public int[] readFrom(ProtoStreamReader reader) throws IOException {
            //TODO [anistor] also support arrays of unboxed primitives directly, ie.  int[] theArray = reader.readArray("theArray", int.class);
            Integer[] theArray = reader.readArray("theArray", Integer.class);

            // now we unbox ...
            int[] unboxedArray = new int[theArray.length];
            for (int i = 0; i < theArray.length; i++) {
               unboxedArray[i] = theArray[i];
            }
            return unboxedArray;
         }

         @Override
         public void writeTo(ProtoStreamWriter writer, int[] intArray) throws IOException {
            Integer[] theArray = new Integer[intArray.length];
            for (int i = 0; i < theArray.length; i++) {
               theArray[i] = intArray[i];
            }
            writer.writeArray("theArray", theArray, Integer.class);
         }
      };

      roundtrip(new int[]{4, 7, 8}, m);
   }

   private static class UserList extends ArrayList<User> {
   }

   @Test
   public void testMarshallUserList() throws Exception {
      MessageMarshaller<UserList> m = new MessageMarshaller<UserList>() {

         @Override
         public String getTypeName() {
            return "sample_bank_account.user_list";
         }

         @Override
         public Class<UserList> getJavaClass() {
            return UserList.class;
         }

         @Override
         public UserList readFrom(ProtoStreamReader reader) throws IOException {
            return reader.readCollection("theList", new UserList(), User.class);
         }

         @Override
         public void writeTo(ProtoStreamWriter writer, UserList list) throws IOException {
            writer.writeCollection("theList", list, User.class);
         }
      };

      List<User> users = new UserList();
      users.add(createUser(1, "X1", "Y1"));
      users.add(createUser(2, "X2", "Y2"));
      users.add(createUser(3, "X3", "Y3"));

      List list = (List) roundtrip(users, m);

      assertTrue(list.get(0) instanceof User);
      assertTrue(list.get(1) instanceof User);
      assertTrue(list.get(2) instanceof User);
      assertEquals(1, ((User) list.get(0)).getId());
      assertEquals(2, ((User) list.get(1)).getId());
      assertEquals(3, ((User) list.get(2)).getId());
   }

   @Test
   public void testMarshallByte() throws Exception {
      roundtrip((byte) 3);
   }

   @Test
   public void testMarshallShort() throws Exception {
      roundtrip((short) 3);
   }

   @Test
   public void testMarshallChar() throws Exception {
      roundtrip('c');
   }

   @Test
   public void testMarshallFloat() throws Exception {
      roundtrip(3.14);
   }

   @Test
   public void testMarshallDouble() throws Exception {
      roundtrip(3.14d);
   }

   @Test
   public void testMarshallBoolean() throws Exception {
      roundtrip(true);
      roundtrip(false);
   }

   @Test
   public void testMarshallInt() throws Exception {
      roundtrip(3);
   }

   @Test
   public void testMarshallLong() throws Exception {
      roundtrip(3L);
   }

   @Test
   public void testMarshallString() throws Exception {
      roundtrip("xyz");
   }

   @Test
   public void testMarshallBytes() throws Exception {
      roundtrip(new byte[]{1, 2, 3, 4});
   }

   @Test
   public void testMarshallEnum() throws Exception {
      roundtrip(User.Gender.MALE);
      roundtrip(User.Gender.FEMALE);
   }

   @Test
   public void testMarshallDate() throws Exception {
      roundtrip(new Date(System.currentTimeMillis()));
   }

   @Test
   public void testMarshallInstant() throws Exception {
      roundtrip(Instant.now());
   }

   @Test
   public void testMarshallObject() throws Exception {
      User user = createUser(1, "John", "Batman");

      User decoded = (User) roundtrip(user);

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
   public void testMarshallNull() throws Exception {
      roundtrip(null);
   }

   private Object roundtrip(Object in, BaseMarshaller... marshallers) throws Exception {
      SerializationContext ctx = createContext();
      for (BaseMarshaller m : marshallers) {
         ctx.registerMarshaller(m);
      }

      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, in);
      assertNotNull(bytes);

      Object out = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      if (in != null) {
         assertNotNull(out);
         assertEquals(in.getClass(), out.getClass());
      } else {
         assertNull(out);
      }

      if (in instanceof byte[]) {
         assertArrayEquals((byte[]) in, (byte[]) out);
      } else if (in instanceof int[]) {
         assertArrayEquals((int[]) in, (int[]) out);
      } else {
         assertEquals(in, out);
      }

      return out;
   }

   private User createUser(int id, String name, String surname) {
      User user = new User();
      user.setId(id);
      user.setName(name);
      user.setSurname(surname);
      user.setGender(User.Gender.MALE);
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3)));
      user.setAddresses(Collections.singletonList(new Address("Old Street", "XYZ42", -12)));
      return user;
   }
}
