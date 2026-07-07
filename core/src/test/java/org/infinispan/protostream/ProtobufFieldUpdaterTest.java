package org.infinispan.protostream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.domain.Address;
import org.infinispan.protostream.domain.User;
import org.infinispan.protostream.test.AbstractProtoStreamTest;
import org.junit.jupiter.api.Test;

public class ProtobufFieldUpdaterTest extends AbstractProtoStreamTest {

   @Test
   public void testUpdateStringField() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      byte[] updated = ProtobufFieldUpdater.update(descriptor, bytes, List.of(
            new ProtobufFieldUpdater.UpdateOperation(
                  ProtobufFieldUpdater.OperationType.SET,
                  new String[]{"name"},
                  List.of("UpdatedName"))
      ));

      User result = fromMessageBytes(ctx, updated, User.class);
      assertEquals("UpdatedName", result.getName());
      assertEquals("Batman", result.getSurname());
      assertEquals(1, result.getId());
   }

   @Test
   public void testUpdateIntField() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      byte[] updated = ProtobufFieldUpdater.update(descriptor, bytes, List.of(
            new ProtobufFieldUpdater.UpdateOperation(
                  ProtobufFieldUpdater.OperationType.SET,
                  new String[]{"id"},
                  List.of(999))
      ));

      User result = fromMessageBytes(ctx, updated, User.class);
      assertEquals(999, result.getId());
      assertEquals("John", result.getName());
   }

   @Test
   public void testUpdateMultipleFields() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      byte[] updated = ProtobufFieldUpdater.update(descriptor, bytes, List.of(
            new ProtobufFieldUpdater.UpdateOperation(
                  ProtobufFieldUpdater.OperationType.SET,
                  new String[]{"name"},
                  List.of("NewName")),
            new ProtobufFieldUpdater.UpdateOperation(
                  ProtobufFieldUpdater.OperationType.SET,
                  new String[]{"surname"},
                  List.of("NewSurname"))
      ));

      User result = fromMessageBytes(ctx, updated, User.class);
      assertEquals("NewName", result.getName());
      assertEquals("NewSurname", result.getSurname());
   }

   @Test
   public void testSetFieldToNull() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      user.setSalutation("Mr.");
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      List<Object> nullList = new java.util.ArrayList<>();
      nullList.add(null);
      byte[] updated = ProtobufFieldUpdater.update(descriptor, bytes, List.of(
            new ProtobufFieldUpdater.UpdateOperation(
                  ProtobufFieldUpdater.OperationType.SET,
                  new String[]{"salutation"},
                  nullList)
      ));

      User result = fromMessageBytes(ctx, updated, User.class);
      assertNull(result.getSalutation());
      assertEquals("John", result.getName());
   }

   @Test
   public void testAddToRepeatedField() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3)));
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      byte[] updated = ProtobufFieldUpdater.update(descriptor, bytes, List.of(
            new ProtobufFieldUpdater.UpdateOperation(
                  ProtobufFieldUpdater.OperationType.ADD,
                  new String[]{"accountIds"},
                  List.of(42))
      ));

      User result = fromMessageBytes(ctx, updated, User.class);
      assertTrue(result.getAccountIds().contains(1));
      assertTrue(result.getAccountIds().contains(3));
      assertTrue(result.getAccountIds().contains(42));
      assertEquals(3, result.getAccountIds().size());
   }

   @Test
   public void testRemoveFromRepeatedField() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3, 5)));
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      byte[] updated = ProtobufFieldUpdater.update(descriptor, bytes, List.of(
            new ProtobufFieldUpdater.UpdateOperation(
                  ProtobufFieldUpdater.OperationType.REMOVE,
                  new String[]{"accountIds"},
                  List.of(3))
      ));

      User result = fromMessageBytes(ctx, updated, User.class);
      assertTrue(result.getAccountIds().contains(1));
      assertTrue(result.getAccountIds().contains(5));
      assertEquals(2, result.getAccountIds().size());
   }

   @Test
   public void testPreservesUnmodifiedFields() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      user.setAddresses(List.of(new Address("Old Street", "XYZ42", -12)));
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      byte[] updated = ProtobufFieldUpdater.update(descriptor, bytes, List.of(
            new ProtobufFieldUpdater.UpdateOperation(
                  ProtobufFieldUpdater.OperationType.SET,
                  new String[]{"name"},
                  List.of("Changed"))
      ));

      User result = fromMessageBytes(ctx, updated, User.class);
      assertEquals("Changed", result.getName());
      assertEquals(1, result.getId());
      assertEquals("Batman", result.getSurname());
      assertEquals(1, result.getAddresses().size());
      assertEquals("Old Street", result.getAddresses().get(0).getStreet());
   }

   private User createTestUser() {
      User user = new User();
      user.setId(1);
      user.setName("John");
      user.setSurname("Batman");
      user.setGender(User.Gender.MALE);
      return user;
   }

   private byte[] toMessageBytes(SerializationContext ctx, Object obj) throws IOException {
      byte[] wrapped = ProtobufUtil.toWrappedByteArray(ctx, obj);
      return extractInnerMessageBytes(ctx, wrapped);
   }

   private <T> T fromMessageBytes(SerializationContext ctx, byte[] messageBytes, Class<T> clazz) throws IOException {
      byte[] wrapped = wrapMessageBytes(ctx, messageBytes, clazz);
      return ProtobufUtil.fromWrappedByteArray(ctx, wrapped);
   }

   private byte[] extractInnerMessageBytes(ImmutableSerializationContext ctx, byte[] wrappedBytes) throws IOException {
      var reader = org.infinispan.protostream.impl.TagReaderImpl.newInstance(ctx, wrappedBytes);
      byte[] innerBytes = null;
      int tag;
      while ((tag = reader.readTag()) != 0) {
         int fieldNumber = org.infinispan.protostream.descriptors.WireType.getTagFieldNumber(tag);
         if (fieldNumber == WrappedMessage.WRAPPED_MESSAGE) {
            innerBytes = reader.readByteArray();
         } else {
            reader.skipField(tag);
         }
      }
      if (innerBytes == null) {
         throw new IOException("No wrapped message found");
      }
      return innerBytes;
   }

   private <T> byte[] wrapMessageBytes(ImmutableSerializationContext ctx, byte[] innerBytes, Class<T> clazz) throws IOException {
      String typeName = ctx.getMarshaller(clazz).getTypeName();
      Integer typeId = ctx.getDescriptorByName(typeName).getTypeId();

      var baos = new org.infinispan.protostream.impl.RandomAccessOutputStreamImpl(innerBytes.length + 20);
      var writer = org.infinispan.protostream.impl.TagWriterImpl.newInstance(ctx, (RandomAccessOutputStream) baos);

      if (typeId != null && typeId >= 0) {
         writer.writeUInt32(WrappedMessage.WRAPPED_TYPE_ID, typeId);
      } else {
         writer.writeString(WrappedMessage.WRAPPED_TYPE_NAME, typeName);
      }
      writer.writeBytes(WrappedMessage.WRAPPED_MESSAGE, innerBytes);
      writer.flush();
      return baos.toByteArray();
   }
}
