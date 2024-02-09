package org.infinispan.protostream.impl.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.EnumValueDescriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.descriptors.Label;
import org.infinispan.protostream.descriptors.OneOfDescriptor;
import org.infinispan.protostream.descriptors.Option;
import org.infinispan.protostream.descriptors.Type;
import org.junit.Test;

/**
 * @since 15.0
 **/

public class ParserTest {

   @Test
   public void testParser() throws IOException, ParseException {
      try (Reader r = new InputStreamReader(ParserTest.class.getClassLoader().getResourceAsStream("sample_bank_account/bank.proto"))) {
         FileDescriptor input = ProtoParser.parse("bank.proto", r, Configuration.builder().build());
         assertEquals(FileDescriptor.Syntax.PROTO3, input.getSyntax());

         assertEquals("sample_bank_account", input.getPackage());
         assertEquals(0, input.getDependants().size());

         assertEquals(7, input.getMessageTypes().size());

         // User
         Descriptor message = assertMessage(input, 0, "User", 12, 1, 1);
         assertField(message, 0, Label.OPTIONAL, Type.INT32, "id", 1);
         assertField(message, 1, Label.REPEATED, Type.INT32, "accountIds", 2);
         assertField(message, 2, Label.OPTIONAL, Type.STRING, "name", 3);
         assertField(message, 3, Label.OPTIONAL, Type.STRING, "surname", 4);
         assertField(message, 4, Label.OPTIONAL, Type.STRING, "salutation", 5);
         assertField(message, 5, Label.REPEATED, "Address", "addresses", 6);
         assertField(message, 6, Label.OPTIONAL, Type.INT32, "age", 7);
         assertField(message, 7, Label.OPTIONAL, "Gender", "gender", 8);
         assertField(message, 8, Label.OPTIONAL, Type.STRING, "notes", 9);
         assertField(message, 9, Label.OPTIONAL, Type.FIXED64, "creationDate", 10);
         assertField(message, 10, Label.OPTIONAL, Type.FIXED64, "passwordExpirationDate", 11);
         assertField(message, 11, Label.OPTIONAL, Type.INT64, "qrCode", 12);
         assertEnum(message, 0, "Gender", "MALE", "FEMALE", "UNSPECIFIED");

         // User.Address
         message = assertMessage(message, 0, "Address", 4, 0, 0);
         assertField(message, 0, Label.OPTIONAL, Type.STRING, "street", 1);
         assertField(message, 1, Label.OPTIONAL, Type.STRING, "postCode", 2);
         assertField(message, 2, Label.OPTIONAL, Type.INT32, "number", 3);
         assertField(message, 3, Label.OPTIONAL, Type.BOOL, "isCommercial", 4);

         // Account
         message = assertMessage(input, 1, "Account", 7, 1, 1);
         assertReserved(message, "alpha", "beta", "gamma");
         assertReserved(message, 8, 10, 11, 13, 14, 15, 17, 19, 20, 22);
         assertField(message, 0, Label.OPTIONAL, Type.INT32, "id", 1);
         assertField(message, 1, Label.OPTIONAL, Type.STRING, "description", 2);
         assertField(message, 2, Label.OPTIONAL, Type.FIXED64, "creationDate", 3);
         assertField(message, 3, Label.OPTIONAL, "Limits", "limits", 4);
         assertField(message, 4, Label.OPTIONAL, "Limits", "hardLimits", 5);
         assertField(message, 5, Label.REPEATED, Type.BYTES, "blurb", 6);
         assertField(message, 6, Label.REPEATED, "Currency", "currencies", 7);
         assertEnum(message, 0, "Currency", "EUR", "GBP", "USD", "BRL");

         // Account.Limits
         message = assertMessage(message, 0, "Limits", 3, 0, 0);
         assertField(message, 0, Label.OPTIONAL, Type.DOUBLE, "maxDailyLimit", 1);
         assertField(message, 1, Label.OPTIONAL, Type.DOUBLE, "maxTransactionLimit", 2);
         assertField(message, 2, Label.REPEATED, Type.STRING, "payees", 3);

         // Transaction
         message = assertMessage(input, 2, "Transaction", 9, 0, 0);
         assertField(message, 0, Label.OPTIONAL, Type.INT32, "id", 1);
         assertField(message, 1, Label.OPTIONAL, Type.STRING, "description", 2);
         assertField(message, 2, Label.OPTIONAL, Type.STRING, "longDescription", 3);
         assertField(message, 3, Label.OPTIONAL, Type.STRING, "notes", 4);
         assertField(message, 4, Label.OPTIONAL, Type.INT32, "accountId", 5);
         assertField(message, 5, Label.OPTIONAL, Type.FIXED64, "date", 6);
         assertField(message, 6, Label.OPTIONAL, Type.DOUBLE, "amount", 7);
         assertField(message, 7, Label.OPTIONAL, Type.BOOL, "isDebit", 8);
         assertField(message, 8, Label.OPTIONAL, Type.BOOL, "isValid", 9);
         OneOfDescriptor oneof = assertOneOf(message, 0, "choice");
         assertField(oneof, 0, Label.ONE_OF, Type.STRING, "one", 10);
         assertField(oneof, 1, Label.ONE_OF, Type.INT32, "or_the_other", 11);

         // int_array
         message = assertMessage(input, 4, "int_array", 1, 0, 0);
         assertField(message, 0, Label.REPEATED, Type.INT32, "theArray", 1);

         // int_array
         message = assertMessage(input, 5, "user_list", 1, 0, 0);
         assertField(message, 0, Label.REPEATED, "User", "theList", 1);
      }
   }

   private void assertReserved(Descriptor message, String... names) {
      for(String name : names) {
         assertTrue(name, message.isReserved(name));
      }
   }

   private void assertReserved(Descriptor message, int... numbers) {
      for(int number : numbers) {
         assertTrue(message.isReserved(number));
      }
   }

   private void assertEnum(Descriptor message, int index, String name, String... values) {
      EnumDescriptor enumElement = message.getEnumTypes().get(index);
      assertEquals(name, enumElement.getName());
      assertEquals(values.length, enumElement.getValues().size());
      int i = 0;
      for (EnumValueDescriptor entry : enumElement.getValues()) {
         assertEquals(values[i], entry.getName());
         assertEquals(i, entry.getNumber());
         i++;
      }
   }

   private OneOfDescriptor assertOneOf(Descriptor message, int index, String name) {
      OneOfDescriptor oneof = message.getOneOfs().get(index);
      assertEquals(name, oneof.getName());
      return oneof;
   }

   private void assertField(Descriptor message, int index, Label label, String type, String name, int number) {
      FieldDescriptor field = message.getFields().get(index);
      assertEquals(label, field.getLabel());
      assertEquals(type, field.getTypeName());
      assertEquals(name, field.getName());
      assertEquals(number, field.getNumber());
   }

   private void assertField(Descriptor message, int index, Label label, Type type, String name, int number) {
      assertField(message.getFields().get(index), label, type, name, number);
   }

   private void assertField(OneOfDescriptor oneof, int index, Label label, Type type, String name, int number) {
      assertField(oneof.getFields().get(index), label, type, name, number);
   }

   private void assertField(FieldDescriptor field, Label label, Type type, String name, int number) {
      assertEquals(label, field.getLabel());
      assertEquals(type, field.getType());
      assertEquals(name, field.getName());
      assertEquals(number, field.getNumber());
   }

   private void assertFieldOptions(Descriptor message, int index, String... options) {
      FieldDescriptor field = message.getFields().get(index);
      assertEquals(options.length / 2, field.getOptions().size());
      int i = 0;
      for (Option option : field.getOptions()) {
         assertEquals(options[i++], option.getName());
         assertEquals(options[i++], option.getValue());
      }
   }

   private Descriptor assertMessage(Descriptor message, int index, String name, int fieldCount, int messageCount, int enumCount) {
      return getDescriptor(message.getNestedTypes().get(index), name, fieldCount, messageCount, enumCount);
   }

   private static Descriptor assertMessage(FileDescriptor file, int index, String name, int fieldCount, int messageCount, int enumCount) {
      return getDescriptor(file.getMessageTypes().get(index), name, fieldCount, messageCount, enumCount);
   }

   private static Descriptor getDescriptor(Descriptor descriptor, String name, int fieldCount, int messageCount, int enumCount) {
      assertEquals(name, descriptor.getName());
      assertEquals("Message " + name + " fields", fieldCount, descriptor.getFields().size());
      assertEquals("Message " + name + " messages", messageCount, descriptor.getNestedTypes().size());
      assertEquals("Message " + name + " enums", enumCount, descriptor.getEnumTypes().size());
      return descriptor;
   }
}
