package org.infinispan.protostream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
      SerializationContext ctx = createContext();

      ctx.registerMarshaller(new MessageMarshaller<int[]>() {
         @Override
         public String getTypeName() {
            return "sample_bank_account.int_array";
         }

         @Override
         public Class<? extends int[]> getJavaClass() {
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
      });

      int[] testArray = new int[]{4, 7, 8};
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, testArray);
      Object unmarshalled = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertTrue(unmarshalled instanceof int[]);
      int[] unmarshalledArray = (int[]) unmarshalled;
      assertArrayEquals(testArray, unmarshalledArray);
   }

   @Test
   public void testMarshallUserList() throws Exception {
      SerializationContext ctx = createContext();

      ctx.registerMarshaller(new MessageMarshaller<ArrayList>() {

         @Override
         public String getTypeName() {
            return "sample_bank_account.user_list";
         }

         @Override
         public Class<? extends ArrayList> getJavaClass() {
            return ArrayList.class;
         }

         @Override
         public ArrayList readFrom(ProtoStreamReader reader) throws IOException {
            return reader.readCollection("theList", new ArrayList<>(), User.class);
         }

         @Override
         public void writeTo(ProtoStreamWriter writer, ArrayList list) throws IOException {
            writer.writeCollection("theList", list, User.class);
         }
      });

      List<User> users = new ArrayList<>();
      users.add(createUser(1, "X1", "Y1"));
      users.add(createUser(2, "X2", "Y2"));
      users.add(createUser(3, "X3", "Y3"));
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, users);
      Object obj = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertTrue(obj instanceof ArrayList);
      List list = (List) obj;
      assertTrue(list.get(0) instanceof User);
      assertTrue(list.get(1) instanceof User);
      assertTrue(list.get(2) instanceof User);
      assertEquals(1, ((User) list.get(0)).getId());
      assertEquals(2, ((User) list.get(1)).getId());
      assertEquals(3, ((User) list.get(2)).getId());
   }

   @Test(expected = IllegalArgumentException.class)
   public void testMarshallByte() throws Exception {
      testMarshallPrimitive((byte) 3);   //there is no support for byte
   }

   @Test(expected = IllegalArgumentException.class)
   public void testMarshallShort() throws Exception {
      testMarshallPrimitive((short) 3);  //there is no support for short
   }

   @Test
   public void testMarshallFloat() throws Exception {
      testMarshallPrimitive(3.14);
   }

   @Test
   public void testMarshallDouble() throws Exception {
      testMarshallPrimitive(3.14d);
   }

   @Test
   public void testMarshallBoolean() throws Exception {
      testMarshallPrimitive(true);
      testMarshallPrimitive(false);
   }

   @Test
   public void testMarshallInt() throws Exception {
      testMarshallPrimitive(3);
   }

   @Test
   public void testMarshallLong() throws Exception {
      testMarshallPrimitive(3L);
   }

   @Test
   public void testMarshallString() throws Exception {
      testMarshallPrimitive("xyz");
   }

   @Test
   public void testMarshallBytes() throws Exception {
      SerializationContext ctx = createContext();
      byte[] value = new byte[]{1, 2, 3, 4};
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, value);
      Object obj = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertTrue(obj instanceof byte[]);
      assertArrayEquals(value, (byte[]) obj);
   }

   @Test
   public void testMarshallEnum() throws Exception {
      testMarshallPrimitive(User.Gender.MALE);
   }

   private void testMarshallPrimitive(Object value) throws Exception {
      if (value == null || value.getClass().isArray()) {
         throw new IllegalArgumentException("nulls or arrays are not accepted");
      }
      SerializationContext ctx = createContext();
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, value);
      Object obj = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertEquals(value.getClass(), obj.getClass());
      assertEquals(value, obj);
   }

   @Test
   public void testMarshallObject() throws Exception {
      SerializationContext ctx = createContext();

      User user = createUser(1, "John", "Batman");

      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, user);

      User decoded = (User) ProtobufUtil.fromWrappedByteArray(ctx, bytes);

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
      SerializationContext ctx = createContext();
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, null);
      Object obj = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertNull(obj);
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
