package org.infinispan.protostream.impl;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Descriptors;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.UnknownFieldSet;
import org.infinispan.protostream.domain.Account;
import org.infinispan.protostream.domain.Address;
import org.infinispan.protostream.domain.Transaction;
import org.infinispan.protostream.domain.User;
import org.infinispan.protostream.domain.marshallers.AccountMarshaller;
import org.infinispan.protostream.domain.marshallers.AddressMarshaller;
import org.infinispan.protostream.domain.marshallers.GenderMarshaller;
import org.infinispan.protostream.domain.marshallers.TransactionMarshaller;
import org.infinispan.protostream.domain.marshallers.UserMarshaller;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author anistor@redhat.com
 */
public class MarshallingWithWrappingTest {

   @Test
   public void testMarshallIntArray() throws Exception {
      SerializationContext ctx = createContext();

      ctx.registerMarshaller(int[].class, new MessageMarshaller<int[]>() {
         @Override
         public String getFullName() {
            return "sample_bank_account.int_array";
         }

         @Override
         public int[] readFrom(ProtoStreamReader reader) throws IOException {
            Integer[] x = reader.readArray("theArray", Integer.class);
            int[] y = new int[x.length];
            for (int i = 0; i < x.length; i++) {
               y[i] = x[i];
            }
            return y;
         }

         @Override
         public void writeTo(ProtoStreamWriter writer, int[] ints) throws IOException {
            Integer[] x = new Integer[ints.length];
            for (int i = 0; i < x.length; i++) {
               x[i] = ints[i];
            }
            writer.writeArray("theArray", x, Integer.class);
         }
      });

      int[] t = new int[]{4, 7, 8};
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, t);
      Object x = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertTrue(x instanceof int[]);
   }

   @Test
   public void testMarshallUserList() throws Exception {
      SerializationContext ctx = createContext();

      ctx.registerMarshaller(ArrayList.class, new MessageMarshaller<ArrayList>() {

         @Override
         public String getFullName() {
            return "sample_bank_account.user_list";
         }

         @Override
         public ArrayList readFrom(ProtoStreamReader reader) throws IOException {
            return reader.readCollection("theList", new ArrayList<User>(), User.class);
         }

         @Override
         public void writeTo(ProtoStreamWriter writer, ArrayList list) throws IOException {
            writer.writeCollection("theList", list, User.class);
         }
      });

      List<User> users = new ArrayList<User>();
      users.add(createUser(1, "X1", "Y1"));
      users.add(createUser(2, "X2", "Y2"));
      users.add(createUser(3, "X3", "Y3"));
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, users);
      Object unmarshalled = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertTrue(unmarshalled instanceof ArrayList);
      assertEquals(3, ((List) unmarshalled).size());
   }

   @Test
   public void testMarshallLong() throws Exception {
      SerializationContext ctx = createContext();
      Long val = new Long(3);
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, val);
      Object x = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertTrue(x instanceof Long);
   }

   @Test
   public void testMarshallEnum() throws Exception {
      SerializationContext ctx = createContext();
      User.Gender t = User.Gender.MALE;
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, t);
      Object x = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertEquals(User.Gender.MALE, x);
   }

   @Test
   public void testMarshallObject() throws Exception {
      SerializationContext ctx = createContext();

      User user = createUser(1, "John", "Batman");

      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, user);

      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      CodedInputStream codedInputStream = CodedInputStream.newInstance(bais);
      UnknownFieldSet unknownFieldSet = new UnknownFieldSetImpl();
      unknownFieldSet.mergeFrom(codedInputStream);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      CodedOutputStream out = CodedOutputStream.newInstance(baos);
      unknownFieldSet.writeTo(out);
      byte[] bytes2 = baos.toByteArray();

      assertArrayEquals(bytes, bytes2);

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
      assertEquals(1, decoded.getAccountIds().get(0).intValue());
      assertEquals(3, decoded.getAccountIds().get(1).intValue());
   }

   private User createUser(int id, String name, String surname) {
      User user = new User();
      user.setId(id);
      user.setName(name);
      user.setSurname(surname);
      user.setGender(User.Gender.MALE);
      user.setAccountIds(Arrays.asList(1, 3));
      user.setAddresses(Collections.singletonList(new Address("Old Street", "XYZ42")));
      return user;
   }

   private SerializationContext createContext() throws IOException, Descriptors.DescriptorValidationException {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      ctx.registerProtofile("/bank.protobin");
      ctx.registerMarshaller(User.class, new UserMarshaller());
      ctx.registerMarshaller(User.Gender.class, new GenderMarshaller());
      ctx.registerMarshaller(Address.class, new AddressMarshaller());
      ctx.registerMarshaller(Account.class, new AccountMarshaller());
      ctx.registerMarshaller(Transaction.class, new TransactionMarshaller());
      return ctx;
   }
}
