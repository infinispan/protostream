package org.infinispan.protostream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.Label;
import org.infinispan.protostream.domain.Account;
import org.infinispan.protostream.domain.Address;
import org.infinispan.protostream.domain.User;
import org.infinispan.protostream.test.AbstractProtoStreamTest;
import org.junit.jupiter.api.Test;

public class ProtobufMessageEditorTest extends AbstractProtoStreamTest {

   @Test
   public void testReadScalarFields() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);

      FieldDescriptor id = descriptor.findFieldByName("id");
      FieldDescriptor name = descriptor.findFieldByName("name");
      FieldDescriptor surname = descriptor.findFieldByName("surname");
      FieldDescriptor salutation = descriptor.findFieldByName("salutation");
      FieldDescriptor notes = descriptor.findFieldByName("notes");

      assertTrue(editor.hasField(id));
      assertTrue(editor.hasField(name));
      assertFalse(editor.hasField(salutation));
      assertFalse(editor.hasField(notes));

      assertEquals(1, editor.getCount(id));
      assertEquals(1, editor.readField(id));
      assertEquals("John", editor.readField(name));
      assertEquals("Batman", editor.readField(surname));
      assertNull(editor.readField(salutation));
      assertNull(editor.readField(notes));
   }

   @Test
   public void testReadRepeatedFields() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3)));
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);

      FieldDescriptor accountIds = descriptor.findFieldByName("accountIds");
      assertEquals(2, editor.getCount(accountIds));
      // assert the decoded values, not just the count
      assertEquals(Set.of(1, 3), new HashSet<>(editor.readRepeated(accountIds)));
      // unknown field names are rejected, absent-but-known fields read as null
      assertThrows(IllegalArgumentException.class, () -> editor.readPath(new String[]{"missingField"}));
   }

   @Test
   public void testReadPathNested() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      user.setAddresses(List.of(new Address("Side Street", "67890", 8)));
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);

      // repeated message leaf: traverses the first occurrence
      assertEquals("Side Street", editor.readPath(new String[]{"addresses", "street"}));
      assertEquals(8, editor.readPath(new String[]{"addresses", "number"}));
      // absent intermediate chain
      User userWithout = createTestUser();
      byte[] bytesWithout = toMessageBytes(ctx, userWithout);
      ProtobufMessageEditor editorWithout = ProtobufMessageEditor.of(ctx, descriptor, bytesWithout);
      assertNull(editorWithout.readPath(new String[]{"addresses", "street"}));
   }

   @Test
   public void testReadPathRepeatedScalarLeaf() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3)));
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);

      List<Object> ids = (List<Object>) editor.readPath(new String[]{"accountIds"});
      assertNotNull(ids);
      assertEquals(Set.of(1, 3), new HashSet<>(ids));
   }

   @Test
   public void testReadPathMessageLeafMaterialized() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      user.setAddresses(List.of(new Address("Side Street", "67890", 8), new Address("Back Street", "90123", 9)));
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);

      List<Address> addresses = (List<Address>) editor.readPath(new String[]{"addresses"});
      assertNotNull(addresses);
      assertEquals(2, addresses.size());
      assertEquals("Side Street", addresses.get(0).getStreet());
      assertEquals("Back Street", addresses.get(1).getStreet());

      // non-repeated nested message leaf
      Account account = new Account();
      account.setDescription("Checking");
      Account.Limits limits = new Account.Limits();
      limits.setMaxDailyLimit(123.5);
      limits.setMaxTransactionLimit(456.5);
      account.setLimits(limits);
      byte[] accountBytes = toMessageBytes(ctx, account);

      Descriptor accountDescriptor = ctx.getMessageDescriptor("sample_bank_account.Account");
      ProtobufMessageEditor accountEditor = ProtobufMessageEditor.of(ctx, accountDescriptor, accountBytes);
      assertEquals(123.5, accountEditor.readPath(new String[]{"limits", "maxDailyLimit"}));

      Account.Limits materialized = (Account.Limits) accountEditor.readPath(new String[]{"limits"});
      assertNotNull(materialized);
      assertEquals(123.5, materialized.getMaxDailyLimit());
      assertEquals(456.5, materialized.getMaxTransactionLimit());
   }

   @Test
   public void testReadPathMessageLeafRequiresContext() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      user.setAddresses(List.of(new Address("Side Street", "67890", 8)));
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(descriptor, bytes);
      assertThrows(IllegalStateException.class, () -> editor.readPath(new String[]{"addresses"}));
   }

   @Test
   public void testNestedEditor() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      user.setAddresses(List.of(new Address("Side Street", "67890", 8), new Address("Back Street", "90123", 9)));
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      FieldDescriptor addresses = descriptor.findFieldByName("addresses");
      Descriptor addressDescriptor = addresses.getMessageType();

      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);
      assertEquals(2, editor.getCount(addresses));

      ProtobufMessageEditor first = editor.nestedEditor(addresses, 0);
      ProtobufMessageEditor second = editor.nestedEditor(addresses, 1);
      ProtobufMessageEditor third = editor.nestedEditor(addresses, 2);

      assertEquals("Side Street", first.readField(addressDescriptor.findFieldByName("street")));
      assertEquals(8, first.readField(addressDescriptor.findFieldByName("number")));
      assertEquals("Back Street", second.readField(addressDescriptor.findFieldByName("street")));
      // absent occurrence: editor over an empty message
      assertFalse(third.hasField(addressDescriptor.findFieldByName("street")));
   }

   @Test
   public void testGetMessageBytes() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      user.setAddresses(List.of(new Address("Main Street", "12345", 7)));
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      FieldDescriptor addresses = descriptor.findFieldByName("addresses");

      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);
      byte[] nested = editor.getMessageBytes(addresses, 0);
      assertNotNull(nested);
      Address decoded = fromMessageBytes(ctx, nested, Address.class);
      assertEquals("Main Street", decoded.getStreet());
      assertNull(editor.getMessageBytes(addresses, 1));
   }

   @Test
   public void testSetFieldVerbatimPassThrough() throws Exception {
      // Golden bytes: only the touched field is re-encoded, everything else (known or unknown) is
      // copied verbatim, in the original order.
      Descriptor descriptor = goldenDescriptor();
      FieldDescriptor f2 = descriptor.findFieldByName("f2");

      byte[] input = hex("0A 02 61 62 "                       // f1 = "ab"
            + "90 03 01 "                                      // unknown field 50 (varint) = 1
            + "10 07 "                                         // f2 = 7
            + "9D 03 DE AD BE EF "                             // unknown field 51 (fixed32)
            + "19 00 00 00 00 00 00 0C 40 "                    // f3 (double) = 1.5
            + "B3 03 08 09 B4 03 "                              // unknown field 54 (group) { 1: 9 }
            + "22 03 78 79 7A "                                 // f4 (bytes) = "xyz"
            + "A1 03 01 02 03 04 05 06 07 08 "                  // unknown field 52 (fixed64)
            + "AA 03 01 AB");                                   // unknown field 53 (length-delimited) = 0xAB

      ProtobufMessageEditor editor = ProtobufMessageEditor.of(descriptor, input);
      editor.setField(f2, 1000);

      byte[] expected = hex("0A 02 61 62 "
            + "90 03 01 "
            + "10 E8 07 "                                      // f2 = 1000 (only re-encoded field)
            + "9D 03 DE AD BE EF "
            + "19 00 00 00 00 00 00 0C 40 "
            + "B3 03 08 09 B4 03 "
            + "22 03 78 79 7A "
            + "A1 03 01 02 03 04 05 06 07 08 "
            + "AA 03 01 AB");
      assertArrayEquals(expected, editor.toByteArray());
   }

   @Test
   public void testSetRepeatedEmittedAtFirstOccurrence() throws Exception {
      Descriptor descriptor = goldenDescriptor();
      FieldDescriptor f2 = descriptor.findFieldByName("f2");

      byte[] input = hex("0A 02 61 62 " + "10 07 " + "10 08 " + "22 03 78 79 7A"); // f1, f2=7, f2=8, f4
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(descriptor, input);
      editor.setValues(f2, List.of(42));

      // SET replaces all occurrences, emitted at the position of the first occurrence
      assertArrayEquals(hex("0A 02 61 62 " + "10 2A " + "22 03 78 79 7A"), editor.toByteArray());
   }

   @Test
   public void testAddAppendedAtEnd() throws Exception {
      Descriptor descriptor = goldenDescriptor();
      FieldDescriptor f2 = descriptor.findFieldByName("f2");

      byte[] input = hex("0A 02 61 62 " + "10 07"); // f1, f2=7
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(descriptor, input);
      editor.addField(f2, 99);

      // ADD keeps the original occurrence and appends the new one at the end of the message
      assertArrayEquals(hex("0A 02 61 62 " + "10 07 " + "10 63"), editor.toByteArray());
   }

   @Test
   public void testRemoveSkipsMatchingOccurrences() throws Exception {
      Descriptor descriptor = goldenDescriptor();
      FieldDescriptor f2 = descriptor.findFieldByName("f2");

      byte[] input = hex("10 07 " + "10 08 " + "10 07"); // f2=7, f2=8, f2=7
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(descriptor, input);
      editor.removeValue(f2, 7);

      assertArrayEquals(hex("10 08"), editor.toByteArray());
   }

   @Test
   public void testRemoveAllThenAppend() throws Exception {
      Descriptor descriptor = goldenDescriptor();
      FieldDescriptor f2 = descriptor.findFieldByName("f2");

      byte[] input = hex("10 07 " + "10 08"); // f2=7, f2=8
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(descriptor, input);
      editor.removeField(f2);
      editor.addField(f2, 9);

      // all original occurrences removed; the appended value is at the end of the message
      assertArrayEquals(hex("10 09"), editor.toByteArray());
   }

   @Test
   public void testSetAbsentFieldAppendedAtEnd() throws Exception {
      Descriptor descriptor = goldenDescriptor();
      FieldDescriptor f2 = descriptor.findFieldByName("f2");

      byte[] input = hex("0A 02 61 62"); // f1 only
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(descriptor, input);
      editor.setField(f2, 5);

      assertArrayEquals(hex("0A 02 61 62 " + "10 05"), editor.toByteArray());
   }

   @Test
   public void testNoOpsProduceIdenticalBytes() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3)));
      user.setAddresses(List.of(new Address("Old Street", "XYZ42", -12)));
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);
      // only reads
      editor.readField(descriptor.findFieldByName("name"));
      editor.readPath(new String[]{"addresses", "street"});
      assertFalse(editor.hasChanges());
      assertArrayEquals(bytes, editor.toByteArray());
   }

   @Test
   public void testRemoveValueAndMatching() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3, 5)));
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      FieldDescriptor accountIds = descriptor.findFieldByName("accountIds");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);

      editor.removeValue(accountIds, 3);
      editor.removeMatching(accountIds, v -> ((Number) v).intValue() == 5);

      User result = fromMessageBytes(ctx, editor.toByteArray(), User.class);
      assertEquals(Set.of(1), new HashSet<>(result.getAccountIds()));
   }

   @Test
   public void testSetOverwritesPendingAdds() throws Exception {
      Descriptor descriptor = goldenDescriptor();
      FieldDescriptor f2 = descriptor.findFieldByName("f2");

      byte[] input = hex("10 07"); // f2=7
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(descriptor, input);
      editor.addField(f2, 99);
      editor.setValues(f2, List.of(42));

      assertArrayEquals(hex("10 2A"), editor.toByteArray());
   }

   @Test
   public void testAddThenRemoveSameValue() throws Exception {
      Descriptor descriptor = goldenDescriptor();
      FieldDescriptor f2 = descriptor.findFieldByName("f2");

      byte[] input = hex("10 07 "); // f2=7
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(descriptor, input);
      editor.addField(f2, 7);
      editor.removeValue(f2, 7);

      // both the original and the appended occurrence are removed
      assertArrayEquals(new byte[0], editor.toByteArray());
   }

   @Test
   public void testRemoveThenAddSameValueRestoresBytes() throws Exception {
      Descriptor descriptor = goldenDescriptor();
      FieldDescriptor f1 = descriptor.findFieldByName("f1");
      FieldDescriptor f2 = descriptor.findFieldByName("f2");

      byte[] input = hex("0A 02 61 62 " + "10 07");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(descriptor, input);
      editor.setField(f1, "ab"); // same value, still a change from the editor's perspective
      assertTrue(editor.hasChanges());
      editor.removeField(f2);
      editor.addField(f2, 7);

      // the field was removed and re-added with the same value; emission reproduces the input
      assertArrayEquals(input, editor.toByteArray());
   }

   @Test
   public void testAddOnAbsentChainIsNoOp() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser(); // no addresses
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);
      editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.ADD, new String[]{"addresses", "street"}, List.of("Nowhere")));

      // ADD on an absent intermediate chain is a no-op: byte-identical output, no parent created
      assertArrayEquals(bytes, editor.toByteArray());
   }

   @Test
   public void testApplyCreatesMissingIntermediateChain() throws Exception {
      SerializationContext ctx = createContext();
      Account account = new Account(); // no limits
      account.setDescription("Checking");
      byte[] bytes = toMessageBytes(ctx, account);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.Account");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);
      editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.SET,
            new String[]{"limits", "maxDailyLimit"},
            List.of(123.5)));
      editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.SET,
            new String[]{"limits", "maxTransactionLimit"},
            List.of(456.5)));

      Account result = fromMessageBytes(ctx, editor.toByteArray(), Account.class);
      assertNotNull(result.getLimits());
      assertEquals(123.5, result.getLimits().getMaxDailyLimit());
      assertEquals(456.5, result.getLimits().getMaxTransactionLimit());
   }

   @Test
   public void testApplyNoOpRemoveDoesNotCreateEmptyNested() throws Exception {
      SerializationContext ctx = createContext();
      Account account = new Account(); // no limits
      account.setDescription("Checking");
      byte[] bytes = toMessageBytes(ctx, account);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.Account");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);
      editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.REMOVE,
            new String[]{"limits", "maxDailyLimit"},
            List.of(1.0)));

      // a no-op REMOVE must not create an empty nested message
      assertFalse(editor.hasChanges());
      assertArrayEquals(bytes, editor.toByteArray());
   }

   @Test
   public void testApplyAddRemoveRepeatedMessage() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      user.setAddresses(List.of(new Address("Old Street", "XYZ42", -12), new Address("Bond Street", "W23", 2)));
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);
      editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.ADD,
            new String[]{"addresses"},
            List.of(new Address("New Street", "ABC1", 3))));
      editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.REMOVE,
            new String[]{"addresses"},
            List.of(new Address("Old Street", "XYZ42", -12))));

      User result = fromMessageBytes(ctx, editor.toByteArray(), User.class);
      assertEquals(2, result.getAddresses().size());
      assertEquals("Bond Street", result.getAddresses().get(0).getStreet());
      assertEquals("New Street", result.getAddresses().get(1).getStreet());
   }

   @Test
   public void testMalformedVectors() {
      Descriptor descriptor = goldenDescriptor();
      // truncated varint (continuation bit without a following byte)
      assertThrows(MalformedProtobufException.class, () -> ProtobufMessageEditor.of(descriptor, new byte[]{(byte) 0x80}));
      // tag with a field number smaller than 1
      assertThrows(MalformedProtobufException.class, () -> ProtobufMessageEditor.of(descriptor, new byte[]{0x00}));
      // tag with an invalid wire type
      assertThrows(MalformedProtobufException.class, () -> ProtobufMessageEditor.of(descriptor, new byte[]{0x06}));
      // length-delimited value longer than the remaining buffer
      assertThrows(MalformedProtobufException.class, () -> ProtobufMessageEditor.of(descriptor, new byte[]{0x0A, (byte) 0xFF}));
      // length-delimited value with a negative (huge) length
      assertThrows(MalformedProtobufException.class, () -> ProtobufMessageEditor.of(descriptor, new byte[]{0x0A, (byte) 0x80, 0x01}));
      // truncated varint value
      assertThrows(MalformedProtobufException.class, () -> ProtobufMessageEditor.of(descriptor, new byte[]{0x08, (byte) 0x80}));
      // unterminated group
      assertThrows(MalformedProtobufException.class, () -> ProtobufMessageEditor.of(descriptor, hex("B3 03 08 09")));
      // truncated fixed64
      assertThrows(MalformedProtobufException.class, () -> ProtobufMessageEditor.of(descriptor, hex("19 00 00 00")));
   }

   @Test
   public void testEquivalenceWithDeserialization() throws Exception {
      SerializationContext ctx = createContext();
      User user = new User();
      user.setId(1);
      user.setName("John");
      user.setSurname("Batman");
      user.setGender(User.Gender.MALE);
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3, 5)));
      user.setAddresses(List.of(new Address("Old Street", "XYZ42", -12)));
      user.setAge(42);
      user.setQrCode(123456789L);
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);
      editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.SET, new String[]{"name"}, List.of("Jane")));
      editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.ADD, new String[]{"accountIds"}, List.of(42)));
      editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.REMOVE, new String[]{"accountIds"}, List.of(1)));
      editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.SET, new String[]{"addresses", "street"}, List.of("Renamed Old")));
      editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.ADD, new String[]{"addresses"}, List.of(new Address("Extra", "EX1", 9))));
      editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.REMOVE, new String[]{"age"}, List.of(42)));

      User result = fromMessageBytes(ctx, editor.toByteArray(), User.class);
      assertEquals("Jane", result.getName());
      assertEquals("Batman", result.getSurname());
      assertTrue(result.getAccountIds().contains(3));
      assertTrue(result.getAccountIds().contains(5));
      assertTrue(result.getAccountIds().contains(42));
      assertFalse(result.getAccountIds().contains(1));
      assertEquals(2, result.getAddresses().size());
      assertEquals("Renamed Old", result.getAddresses().get(0).getStreet());
      assertEquals("Extra", result.getAddresses().get(1).getStreet());
      assertNull(result.getAge());
      assertEquals(123456789L, result.getQrCode().longValue());
   }

   @Test
   public void testMapFieldsRejected() throws Exception {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      ctx.registerProtoFiles(FileDescriptorSource.fromString("map_fields.proto", """
            syntax = "proto2";
            package maptest;
            message M {
               map<string, int32> counts = 1;
               optional string name = 2;
            }
            """));
      Descriptor descriptor = ctx.getMessageDescriptor("maptest.M");
      FieldDescriptor counts = descriptor.findFieldByName("counts");

      // valid M { counts: {"a" -> 5}, name: "x" }
      byte[] bytes = hex("0A 05 0A 01 61 10 05 12 01 78");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);

      // span-level access works for map fields, and unmodified occurrences pass through verbatim
      assertTrue(editor.hasField(counts));
      assertEquals(1, editor.getCount(counts));
      assertArrayEquals(bytes, editor.toByteArray());

      // decoding, writing and removing map values is rejected loudly
      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> editor.readField(counts));
      assertTrue(ex.getMessage().contains("Map fields are not supported"), ex.getMessage());
      assertThrows(IllegalArgumentException.class, () -> editor.readRepeated(counts));
      assertThrows(IllegalArgumentException.class, () -> editor.setField(counts, 5));
      assertThrows(IllegalArgumentException.class, () -> editor.setValues(counts, List.of(5)));
      assertThrows(IllegalArgumentException.class, () -> editor.addField(counts, 5));
      assertThrows(IllegalArgumentException.class, () -> editor.removeValue(counts, 5));
      assertThrows(IllegalArgumentException.class, () -> editor.removeMatching(counts, v -> true));

      // a dotted path traversing a map field is rejected as well
      assertThrows(IllegalArgumentException.class, () -> editor.readPath(new String[]{"counts", "key"}));
      assertThrows(IllegalArgumentException.class, () -> editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.SET, new String[]{"counts"}, List.of(5))));
   }

   @Test
   public void testNestedSetOnRepeatedMessagePreservesSiblings() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      user.setAddresses(List.of(new Address("Old Street", "XYZ42", -12), new Address("Second Street", "ABC1", 7)));
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);
      editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.SET, new String[]{"addresses", "street"}, List.of("Renamed")));

      User result = fromMessageBytes(ctx, editor.toByteArray(), User.class);
      // the first occurrence is edited, the sibling is preserved verbatim
      assertEquals(2, result.getAddresses().size());
      assertEquals("Renamed", result.getAddresses().get(0).getStreet());
      assertEquals("Second Street", result.getAddresses().get(1).getStreet());
      assertEquals("ABC1", result.getAddresses().get(1).getPostCode());
   }

   @Test
   public void testApplyAfterDirectSetFieldUsesPendingValue() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      user.setAddresses(List.of(new Address("Old Street", "XYZ42", -12), new Address("Second Street", "ABC1", 7)));
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      FieldDescriptor addresses = descriptor.findFieldByName("addresses");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);

      // a direct replacement of the whole repeated field...
      editor.setField(addresses, new Address("Direct Street", "D1", 1));
      // ...followed by a dotted-path edit must apply to the pending value, not resurrect the originals
      editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.SET, new String[]{"addresses", "street"}, List.of("Renamed")));

      User result = fromMessageBytes(ctx, editor.toByteArray(), User.class);
      assertEquals(1, result.getAddresses().size());
      assertEquals("Renamed", result.getAddresses().get(0).getStreet());
      assertEquals("D1", result.getAddresses().get(0).getPostCode());
   }

   @Test
   public void testRemoveMatchingDropsPendingAdds() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3)));
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      FieldDescriptor accountIds = descriptor.findFieldByName("accountIds");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);

      editor.addField(accountIds, 99);
      // no original occurrence matches, but the pending value must still be dropped
      editor.removeMatching(accountIds, v -> v.equals(99));

      User result = fromMessageBytes(ctx, editor.toByteArray(), User.class);
      assertEquals(Set.of(1, 3), result.getAccountIds());
   }

   @Test
   public void testReadPackedRepeated() throws Exception {
      // repeated int32 vals = 1, packed encoding of [1, 2, 3]: tag 0A, length 3, three varints
      Descriptor descriptor = new Descriptor.Builder()
            .withName("Packed").withFullName("Packed")
            .addField(new FieldDescriptor.Builder().withNumber(1).withName("vals").withTypeName("int32").withLabel(Label.REPEATED))
            .build();
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(null, descriptor, hex("0A 03 01 02 03"));
      assertEquals(List.of(1, 2, 3), editor.readRepeated(descriptor.findFieldByName("vals")));
      assertEquals(1, editor.readField(descriptor.findFieldByName("vals")));

      // zero-length packed occurrence: no values, but the occurrence is present
      ProtobufMessageEditor empty = ProtobufMessageEditor.of(null, descriptor, hex("0A 00"));
      assertNull(empty.readField(descriptor.findFieldByName("vals")));
      assertEquals(List.of(), empty.readRepeated(descriptor.findFieldByName("vals")));
      assertEquals(1, empty.getCount(descriptor.findFieldByName("vals")));
   }

   @Test
   public void testRemoveFromPackedOccurrence() throws Exception {
      Descriptor descriptor = new Descriptor.Builder()
            .withName("Packed").withFullName("Packed")
            .addField(new FieldDescriptor.Builder().withNumber(1).withName("vals").withTypeName("int32").withLabel(Label.REPEATED))
            .build();
      FieldDescriptor vals = descriptor.findFieldByName("vals");

      // remove a value from the middle of a packed occurrence: the survivors are re-encoded
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(null, descriptor, hex("0A 03 01 02 03"));
      editor.removeValue(vals, 2);
      assertEquals(List.of(1, 3), ProtobufMessageEditor.of(null, descriptor, editor.toByteArray()).readRepeated(vals));

      // remove by predicate from a packed occurrence
      ProtobufMessageEditor editor2 = ProtobufMessageEditor.of(null, descriptor, hex("0A 03 01 02 03"));
      editor2.removeMatching(vals, v -> ((Integer) v) >= 3);
      assertEquals(List.of(1, 2), ProtobufMessageEditor.of(null, descriptor, editor2.toByteArray()).readRepeated(vals));

      // removing every value of a packed occurrence drops the field
      ProtobufMessageEditor editor3 = ProtobufMessageEditor.of(null, descriptor, hex("0A 03 01 02 03"));
      editor3.removeValue(vals, 1);
      editor3.removeValue(vals, 2);
      editor3.removeValue(vals, 3);
      assertEquals(List.of(), ProtobufMessageEditor.of(null, descriptor, editor3.toByteArray()).readRepeated(vals));

      // a removal that matches nothing leaves the packed span byte-identical
      byte[] packed = hex("0A 03 01 02 03");
      ProtobufMessageEditor editor4 = ProtobufMessageEditor.of(null, descriptor, packed);
      editor4.removeValue(vals, 42);
      assertArrayEquals(packed, editor4.toByteArray());
   }

   @Test
   public void testWireIncompatibleOccurrenceTreatedAsUnknown() throws Exception {
      // f_int32 (field 2, varint): occurrence 0 has an incompatible length-delimited wire type,
      // occurrence 1 is a regular varint
      Descriptor descriptor = new Descriptor.Builder()
            .withName("W").withFullName("W")
            .addField(new FieldDescriptor.Builder().withNumber(2).withName("f_int32").withTypeName("int32"))
            .build();
      FieldDescriptor f = descriptor.findFieldByName("f_int32");
      byte[] data = hex("12 03 61 62 63 10 05"); // field 2 wt2 "abc", then field 2 varint 5

      ProtobufMessageEditor editor = ProtobufMessageEditor.of(null, descriptor, data);
      // reads are blind to the wire-incompatible occurrence, exactly like protobuf readers
      assertEquals(5, editor.readField(f));
      assertEquals(List.of(5), editor.readRepeated(f));
      assertEquals(1, editor.getCount(f));

      // an edit of the field preserves the incompatible occurrence as unknown data
      editor.setField(f, 9);
      byte[] out = editor.toByteArray();
      // the incompatible span comes first and verbatim, then the new value
      assertArrayEquals(hex("12 03 61 62 63 10 09"), out);
   }

   @Test
   public void testInvalidFieldNumberRejected() throws Exception {
      Descriptor descriptor = goldenDescriptor();
      // tag varint 0x80 0x80 0x80 0x80 0x10 encodes field number 2^29 with wire type 0
      assertThrows(MalformedProtobufException.class,
            () -> ProtobufMessageEditor.of(descriptor, hex("80 80 80 80 10")));
   }

   @Test
   public void testRemoveValueAfterClearAndAdd() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3)));
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      FieldDescriptor accountIds = descriptor.findFieldByName("accountIds");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);

      editor.setField(accountIds, null); // clear
      editor.addField(accountIds, 99);   // add after the clear
      editor.removeValue(accountIds, 99); // remove must reach the pending addition

      User result = fromMessageBytes(ctx, editor.toByteArray(), User.class);
      assertTrue(result.getAccountIds() == null || result.getAccountIds().isEmpty());
   }

   @Test
   public void testFailedOperationLeavesEditorUnchanged() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3)));
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);
      // the second value cannot be converted to int32: the operation must fail atomically
      assertThrows(NumberFormatException.class, () -> editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.ADD, new String[]{"accountIds"}, Arrays.asList(5, "x"))));
      assertFalse(editor.hasChanges());
      assertArrayEquals(bytes, editor.toByteArray());
   }

   @Test
   public void testNestedSetAfterRemoveFieldRecreates() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      user.setAddresses(List.of(new Address("Old Street", "XYZ42", -12)));
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      FieldDescriptor addresses = descriptor.findFieldByName("addresses");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);

      editor.removeField(addresses);
      editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.SET, new String[]{"addresses", "street"}, List.of("New Street")));

      // the later dotted SET wins: the field is recreated with just the edited content
      ProtobufMessageEditor reread = ProtobufMessageEditor.of(ctx, descriptor, editor.toByteArray());
      assertEquals(1, reread.getCount(addresses));
      assertEquals("New Street", reread.readPath(new String[]{"addresses", "street"}));
      assertNull(reread.readPath(new String[]{"addresses", "postCode"}));
   }

   @Test
   public void testSetToEmptyThenNestedSet() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      user.setAddresses(List.of(new Address("Old Street", "XYZ42", -12)));
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      FieldDescriptor addresses = descriptor.findFieldByName("addresses");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);

      editor.setField(addresses, null); // SET to empty
      editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.SET, new String[]{"addresses", "street"}, List.of("Fresh")));

      // the later dotted SET starts from an empty message: old sibling values must not survive
      ProtobufMessageEditor reread = ProtobufMessageEditor.of(ctx, descriptor, editor.toByteArray());
      assertEquals(1, reread.getCount(addresses));
      assertEquals("Fresh", reread.readPath(new String[]{"addresses", "street"}));
      assertNull(reread.readPath(new String[]{"addresses", "postCode"}));
   }

   @Test
   public void testRemoveFirstOccurrenceThenNestedSetEditsSurvivor() throws Exception {
      SerializationContext ctx = createContext();
      Address first = new Address("First Street", "AAA", 1);
      Address second = new Address("Second Street", "BBB", 2);
      User user = createTestUser();
      user.setAddresses(List.of(first, second));
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      FieldDescriptor addresses = descriptor.findFieldByName("addresses");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);

      editor.removeValue(addresses, first);
      editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.SET, new String[]{"addresses", "street"}, List.of("Renamed")));

      // the removed occurrence stays gone; the dotted edit targets the surviving occurrence
      User result = fromMessageBytes(ctx, editor.toByteArray(), User.class);
      assertEquals(1, result.getAddresses().size());
      assertEquals("Renamed", result.getAddresses().get(0).getStreet());
      assertEquals("BBB", result.getAddresses().get(0).getPostCode());
   }

   @Test
   public void testRemovalTargetsEditedOccurrence() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      user.setAddresses(List.of(new Address("Old Street", "XYZ42", -12)));
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      FieldDescriptor addresses = descriptor.findFieldByName("addresses");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);

      editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.SET, new String[]{"addresses", "street"}, List.of("Edited")));
      // remove the edited value: the pending removal must apply to the overridden occurrence
      editor.removeValue(addresses, new Address("Edited", "XYZ42", -12));

      User result = fromMessageBytes(ctx, editor.toByteArray(), User.class);
      assertTrue(result.getAddresses() == null || result.getAddresses().isEmpty());
   }

   @Test
   public void testDirectEditBetweenDottedEditsKeepsEarlierEdit() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      user.setAddresses(List.of(new Address("Old Street", "XYZ42", -12)));
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      FieldDescriptor addresses = descriptor.findFieldByName("addresses");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);

      // dotted edit, then a direct edit that invalidates the nested editor without replacing,
      // then another dotted edit: the first edit must survive the round trip
      editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.SET, new String[]{"addresses", "street"}, List.of("First")));
      editor.addField(addresses, new Address("Other", "OTR", 7));
      editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.SET, new String[]{"addresses", "postCode"}, List.of("PST")));

      ProtobufMessageEditor reread = ProtobufMessageEditor.of(ctx, descriptor, editor.toByteArray());
      assertEquals(2, reread.getCount(addresses));
      assertEquals("First", reread.readPath(new String[]{"addresses", "street"}));
      assertEquals("PST", reread.readPath(new String[]{"addresses", "postCode"}));
      assertEquals("Other", reread.nestedEditor(addresses, 1).readPath(new String[]{"street"}));
   }

   @Test
   public void testDirectEditAfterDottedEditOfAbsentFieldKeepsCreated() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser(); // no addresses
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      FieldDescriptor addresses = descriptor.findFieldByName("addresses");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);

      // a dotted edit creates the field, a direct edit invalidates the nested editor, another
      // dotted edit must build on the created occurrence instead of discarding it
      editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.SET, new String[]{"addresses", "street"}, List.of("First")));
      editor.addField(addresses, new Address("Other", "OTR", 7));
      editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.SET, new String[]{"addresses", "postCode"}, List.of("PST")));

      ProtobufMessageEditor reread = ProtobufMessageEditor.of(ctx, descriptor, editor.toByteArray());
      assertEquals(2, reread.getCount(addresses));
      assertEquals("First", reread.readPath(new String[]{"addresses", "street"}));
      assertEquals("PST", reread.readPath(new String[]{"addresses", "postCode"}));
   }

   @Test
   public void testFailedAddFieldLeavesEditorUnchanged() throws Exception {
      SerializationContext ctx = createContext();
      User user = createTestUser();
      user.setAddresses(List.of(new Address("Old Street", "XYZ42", -12)));
      byte[] bytes = toMessageBytes(ctx, user);

      Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
      FieldDescriptor addresses = descriptor.findFieldByName("addresses");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);

      editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.SET, new String[]{"addresses", "street"}, List.of("First")));
      // the value cannot be marshalled (no marshaller for Integer): the call must fail atomically
      // and must not destroy the earlier dotted edit through nested-editor invalidation
      assertThrows(IllegalArgumentException.class, () -> editor.addField(addresses, 12345));
      editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.SET, new String[]{"addresses", "postCode"}, List.of("PST")));

      ProtobufMessageEditor reread = ProtobufMessageEditor.of(ctx, descriptor, editor.toByteArray());
      assertEquals("First", reread.readPath(new String[]{"addresses", "street"}));
      assertEquals("PST", reread.readPath(new String[]{"addresses", "postCode"}));
   }

   @Test
   public void testRemoveMatchingIgnoresWireIncompatibleOccurrence() throws Exception {
      // f_int32 (field 3): occurrence 0 has an incompatible length-delimited wire type
      Descriptor descriptor = new Descriptor.Builder()
            .withName("R").withFullName("R")
            .addField(new FieldDescriptor.Builder().withNumber(3).withName("g").withTypeName("int32"))
            .build();
      FieldDescriptor g = descriptor.findFieldByName("g");
      byte[] data = hex("1A 03 61 62 63 18 2A"); // field 3 wt2 "abc" (incompatible), field 3 varint 42

      ProtobufMessageEditor editor = ProtobufMessageEditor.of(null, descriptor, data);
      // the wire-incompatible occurrence is invisible to removeMatching, like every other read
      editor.removeMatching(g, v -> ((Integer) v) == 42);
      // the incompatible span survives verbatim, the matching value is gone
      assertEquals(List.of(), ProtobufMessageEditor.of(null, descriptor, editor.toByteArray()).readRepeated(g));
      assertArrayEquals(hex("1A 03 61 62 63"), editor.toByteArray());
   }

   @Test
   public void testNonMatchingRemoveIsTrueNoOp() throws Exception {
      // non-minimal but valid varints in the nested occurrence: a no-op REMOVE must leave the
      // bytes untouched (no re-encoding through an occurrence override)
      byte[] data = hex("8A 00 88 00 0A 02 41 30 12 02 42 30 12 03 74 61 67"); // inner {a:"A0", b:"B0"}, tag "tag"
      SerializationContext ctx = createContext();
      ctx.registerProtoFiles(FileDescriptorSource.fromString("noop.proto", """
            syntax = "proto2";
            package nooptest;
            message Outer {
               optional Inner inner = 1;
               optional string tag = 2;
            }
            message Inner {
               optional string a = 1;
               optional string b = 2;
            }
            """));
      Descriptor descriptor = ctx.getMessageDescriptor("nooptest.Outer");

      ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, data);
      editor.apply(new ProtobufFieldUpdater.UpdateOperation(
            ProtobufFieldUpdater.OperationType.REMOVE, new String[]{"inner", "a"}, List.of("ZZZ")));
      assertFalse(editor.hasChanges());
      assertArrayEquals(data, editor.toByteArray());
   }

   @Test
   public void testMismatchedEndGroupRejected() {
      Descriptor descriptor = goldenDescriptor();
      // group field 7 opened (B3 03 = tag 59 = field 7, START_GROUP), closed by END_GROUP of field 8 (44)
      assertThrows(MalformedProtobufException.class,
            () -> ProtobufMessageEditor.of(descriptor, hex("B3 03 08 09 44")));
   }

   @Test
   public void testHugeLengthRejectedOnDecode() {
      Descriptor descriptor = goldenDescriptor();
      // f1 (string) with a length varint near Long.MAX_VALUE: the bounds check must not overflow
      // into an unchecked exception, whether at indexing time or at decode time
      assertThrows(MalformedProtobufException.class, () -> ProtobufMessageEditor.of(descriptor,
            hex("0A FF FF FF FF FF FF FF FF FF 00")));
   }

   @Test
   public void testPathDepthLimitRejected() throws Exception {      Descriptor descriptor = goldenDescriptor();
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(descriptor, new byte[0]);
      String[] deepPath = new String[257];
      Arrays.fill(deepPath, "f1");
      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> editor.apply(
            new ProtobufFieldUpdater.UpdateOperation(ProtobufFieldUpdater.OperationType.SET, deepPath, List.of(1))));
      assertTrue(ex.getMessage().contains("maximum depth"), ex.getMessage());
   }

   @Test
   public void testEquivalenceFuzz() throws Exception {
      // seeded randomized equivalence: random users, random op sequences; the mutated POJO is the oracle
      Random random = new Random(42); // NOSONAR: deterministic seed on purpose
      for (int iter = 0; iter < 200; iter++) {
         SerializationContext ctx = createContext();
         User user = randomUser(random);
         byte[] bytes = toMessageBytes(ctx, user);

         Descriptor descriptor = ctx.getMessageDescriptor("sample_bank_account.User");
         ProtobufMessageEditor editor = ProtobufMessageEditor.of(ctx, descriptor, bytes);
         // mixing addField with a dotted-path edit of an absent field is not a supported combination
         boolean hadAddresses = user.getAddresses() != null && !user.getAddresses().isEmpty();

         int opCount = 1 + random.nextInt(5);
         for (int i = 0; i < opCount; i++) {
            switch (random.nextInt(6)) {
               case 0 -> {
                  String name = "name" + random.nextInt(100);
                  editor.apply(new ProtobufFieldUpdater.UpdateOperation(
                        ProtobufFieldUpdater.OperationType.SET, new String[]{"name"}, List.of(name)));
                  user.setName(name);
               }
               case 1 -> {
                  Integer age = random.nextBoolean() ? random.nextInt(100) : null;
                  editor.apply(new ProtobufFieldUpdater.UpdateOperation(
                        ProtobufFieldUpdater.OperationType.SET, new String[]{"age"}, age == null ? List.of() : List.of(age)));
                  user.setAge(age);
               }
               case 2 -> {
                  int id = random.nextInt(1000);
                  editor.apply(new ProtobufFieldUpdater.UpdateOperation(
                        ProtobufFieldUpdater.OperationType.ADD, new String[]{"accountIds"}, List.of(id)));
                  if (user.getAccountIds() == null) {
                     user.setAccountIds(new HashSet<>());
                  }
                  user.getAccountIds().add(id);
               }
               case 3 -> {
                  if (user.getAccountIds() != null && !user.getAccountIds().isEmpty()) {
                     Integer victim = user.getAccountIds().iterator().next();
                     editor.apply(new ProtobufFieldUpdater.UpdateOperation(
                           ProtobufFieldUpdater.OperationType.REMOVE, new String[]{"accountIds"}, List.of(victim)));
                     user.getAccountIds().remove(victim);
                  }
               }
               case 4 -> {
                  // a dotted SET creates a partial intermediate when absent, which the sample
                  // domain marshallers cannot read back; only exercise it on existing addresses
                  if (hadAddresses) {
                     String street = "street" + random.nextInt(100);
                     editor.apply(new ProtobufFieldUpdater.UpdateOperation(
                           ProtobufFieldUpdater.OperationType.SET, new String[]{"addresses", "street"}, List.of(street)));
                     user.getAddresses().get(0).setStreet(street);
                  }
               }
               case 5 -> {
                  Address extra = new Address("Extra" + random.nextInt(100), "EX" + random.nextInt(100), random.nextInt(50));
                  editor.apply(new ProtobufFieldUpdater.UpdateOperation(
                        ProtobufFieldUpdater.OperationType.ADD, new String[]{"addresses"}, List.of(extra)));
                  if (user.getAddresses() == null) {
                     user.setAddresses(new ArrayList<>());
                  }
                  user.getAddresses().add(extra);
               }
            }
         }

         User result = fromMessageBytes(ctx, editor.toByteArray(), User.class);
         assertEquals(user.getName(), result.getName(), "iter " + iter);
         assertEquals(user.getAge(), result.getAge(), "iter " + iter);
         assertEquals(user.getAccountIds() == null ? Set.of() : user.getAccountIds(),
               result.getAccountIds() == null ? Set.of() : result.getAccountIds(), "iter " + iter);
         assertEquals(user.getAddresses() == null ? 0 : user.getAddresses().size(),
               result.getAddresses() == null ? 0 : result.getAddresses().size(), "iter " + iter);
         if (user.getAddresses() != null && !user.getAddresses().isEmpty()) {
            assertEquals(user.getAddresses().get(0).getStreet(), result.getAddresses().get(0).getStreet(), "iter " + iter);
         }
      }
   }

   private User randomUser(Random random) {
      User user = createTestUser();
      user.setName("name" + random.nextInt(100));
      if (random.nextBoolean()) {
         user.setAge(random.nextInt(100));
      }
      if (random.nextBoolean()) {
         user.setAccountIds(new HashSet<>(Arrays.asList(random.nextInt(100), random.nextInt(100), random.nextInt(100))));
      }
      if (random.nextBoolean()) {
         int n = 1 + random.nextInt(3);
         List<Address> addresses = new ArrayList<>();
         for (int i = 0; i < n; i++) {
            addresses.add(new Address("Street" + random.nextInt(100), "Z" + random.nextInt(10000), random.nextInt(100)));
         }
         user.setAddresses(addresses);
      }
      return user;
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
      return ProtobufUtil.splitWrappedMessage(wrapped).message();
   }

   private <T> T fromMessageBytes(SerializationContext ctx, byte[] messageBytes, Class<T> clazz) throws IOException {
      // re-wrap using the type info of the registered marshaller (typeId when assigned, else typeName)
      String typeName = ctx.getMarshaller(clazz).getTypeName();
      Integer typeId = ctx.getDescriptorByName(typeName).getTypeId();
      byte[] wrapped = typeId != null
            ? ProtobufUtil.joinWrappedMessage(null, typeId, messageBytes, null)
            : ProtobufUtil.joinWrappedMessage(typeName, null, messageBytes, null);
      return ProtobufUtil.fromWrappedByteArray(ctx, wrapped);
   }

   private static Descriptor goldenDescriptor() {
      return new Descriptor.Builder()
            .withName("GoldenMsg")
            .withFullName("com.example.GoldenMsg")
            .addField(new FieldDescriptor.Builder().withNumber(1).withName("f1").withTypeName("string"))
            .addField(new FieldDescriptor.Builder().withNumber(2).withName("f2").withTypeName("int32"))
            .addField(new FieldDescriptor.Builder().withNumber(3).withName("f3").withTypeName("double"))
            .addField(new FieldDescriptor.Builder().withNumber(4).withName("f4").withTypeName("bytes"))
            .build();
   }

   private static byte[] hex(String hex) {
      String[] parts = hex.trim().split("\\s+");
      byte[] bytes = new byte[parts.length];
      for (int i = 0; i < parts.length; i++) {
         bytes[i] = (byte) Integer.parseInt(parts[i], 16);
      }
      return bytes;
   }
}
