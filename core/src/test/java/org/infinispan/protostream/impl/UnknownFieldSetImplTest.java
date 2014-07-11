package org.infinispan.protostream.impl;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.UnknownFieldSet;
import org.infinispan.protostream.domain.Address;
import org.infinispan.protostream.domain.User;
import org.infinispan.protostream.test.AbstractProtoStreamTest;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author anistor@redhat.com
 */
public class UnknownFieldSetImplTest extends AbstractProtoStreamTest {

   @Test
   public void testReadWrite() throws Exception {
      SerializationContext ctx = createContext();

      User user = new User();
      user.setId(1);
      user.setName("John");
      user.setSurname("Batman");
      user.setGender(User.Gender.MALE);
      user.setAccountIds(new HashSet<Integer>(Arrays.asList(1, 3)));
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
   }
}
