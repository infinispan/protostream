package org.infinispan.protostream.impl;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.TagReader;
import org.infinispan.protostream.TagWriter;
import org.infinispan.protostream.domain.Address;
import org.infinispan.protostream.domain.User;
import org.infinispan.protostream.test.AbstractProtoStreamTest;
import org.junit.Test;

/**
 * @author anistor@redhat.com
 */
public class UnknownFieldSetImplTest extends AbstractProtoStreamTest {

   private byte[] createMarshalledObject() throws IOException {
      ImmutableSerializationContext ctx = createContext();
      User user = new User();
      user.setId(1);
      user.setName("John");
      user.setSurname("Batman");
      user.setGender(User.Gender.MALE);
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3)));
      user.setAddresses(Collections.singletonList(new Address("Old Street", "XYZ42", -12)));
      return ProtobufUtil.toByteArray(ctx, user);
   }

   private byte[] marshall(UnknownFieldSetImpl unknownFieldSet) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      TagWriter tagWriter = TagWriterImpl.newInstance(null, baos);
      unknownFieldSet.writeTo(tagWriter);
      tagWriter.flush();
      return baos.toByteArray();
   }

   private UnknownFieldSetImpl unmarshall(byte[] bytes) throws IOException {
      TagReader tagReader = TagReaderImpl.newInstance(null, new ByteArrayInputStream(bytes));
      UnknownFieldSetImpl unknownFieldSet = new UnknownFieldSetImpl();
      unknownFieldSet.readAllFields(tagReader);
      return unknownFieldSet;
   }

   @Test
   public void testProtobufRoundtrip() throws Exception {
      byte[] bytes = createMarshalledObject();

      UnknownFieldSetImpl unknownFieldSet = unmarshall(bytes);

      byte[] bytes2 = marshall(unknownFieldSet);

      assertArrayEquals(bytes, bytes2);
   }

   @Test
   public void testSerializationRoundtrip() throws Exception {
      byte[] bytes = createMarshalledObject();

      UnknownFieldSetImpl unknownFieldSet = unmarshall(bytes);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(unknownFieldSet);
      oos.flush();
      byte[] bytes2 = baos.toByteArray();

      ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes2));
      UnknownFieldSetImpl unserialized = (UnknownFieldSetImpl) ois.readObject();

      byte[] bytes3 = marshall(unserialized);

      assertArrayEquals(bytes, bytes3);
   }
}
