package org.infinispan.protostream.impl;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Descriptors;
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
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * @author anistor@redhat.com
 */
public class MarshallingTest {

   @Test
   public void testMarshallObject() throws Exception {
      SerializationContext ctx = createContext();

      User user = new User();
      user.setId(1);
      user.setName("John");
      user.setSurname("Batman");
      user.setGender(User.Gender.MALE);
      user.setAccountIds(Arrays.asList(1, 3));
      user.setAddresses(Collections.singletonList(new Address("Old Street", "XYZ42")));

      byte[] bytes = ProtobufUtil.toByteArray(ctx, user);

      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      CodedInputStream codedInputStream = CodedInputStream.newInstance(bais);
      UnknownFieldSet unknownFieldSet = new UnknownFieldSetImpl();
      unknownFieldSet.readAllFields(codedInputStream);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      CodedOutputStream out = CodedOutputStream.newInstance(baos);
      unknownFieldSet.writeTo(out);
      byte[] bytes2 = baos.toByteArray();

      assertArrayEquals(bytes, bytes2);

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
      assertEquals(1, decoded.getAccountIds().get(0).intValue());
      assertEquals(3, decoded.getAccountIds().get(1).intValue());
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
