package org.infinispan.protostream.annotations.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.Level;
import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoMessage;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.annotations.ProtoReserved;
import org.infinispan.protostream.annotations.ProtoReserved.Range;
import org.infinispan.protostream.annotations.ProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoSchemaBuilderException;
import org.infinispan.protostream.annotations.impl.testdomain.Simple;
import org.infinispan.protostream.annotations.impl.testdomain.TestArraysAndCollectionsClass;
import org.infinispan.protostream.annotations.impl.testdomain.TestArraysAndCollectionsClass2;
import org.infinispan.protostream.annotations.impl.testdomain.TestClass;
import org.infinispan.protostream.annotations.impl.testdomain.TestClass3;
import org.infinispan.protostream.annotations.impl.testdomain.TestEnum;
import org.infinispan.protostream.annotations.impl.testdomain.subpackage.TestClass2;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.domain.User;
import org.infinispan.protostream.impl.parser.SquareProtoParser;
import org.infinispan.protostream.test.AbstractProtoStreamTest;
import org.infinispan.protostream.test.ExpectedLogMessage;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public class ProtoSchemaBuilderTest extends AbstractProtoStreamTest {

   @Rule
   public ExpectedException exception = ExpectedException.none();

   @Rule
   public ExpectedLogMessage expectedLogMessage = ExpectedLogMessage.any();

   @Test
   public void testMain() throws Exception {
      File tmpdir = new File(System.getProperty("java.io.tmpdir"));
      File generatedSchemaFile = new File(tmpdir, "ProtoSchemaBuilderTest.proto");
      generatedSchemaFile.delete();
      File bankSchemaFile = new File(tmpdir, "bank.proto");
      Files.copy(getClass().getResourceAsStream("/sample_bank_account/bank.proto"), bankSchemaFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

      String[] args = {
            "-s", "sample_bank_account/bank.proto=" + bankSchemaFile.getAbsolutePath(),
            "-m", "org.infinispan.protostream.domain.marshallers.UserMarshaller",
            "-m", "org.infinispan.protostream.domain.marshallers.GenderMarshaller",
            "-f", generatedSchemaFile.getAbsolutePath(),
            "-p", "my_package",
            "org.infinispan.protostream.domain.Note"
      };
      ProtoSchemaBuilder.main(args);

      assertTrue(generatedSchemaFile.exists());
      assertTrue(generatedSchemaFile.length() > 0);

      generatedSchemaFile.delete();
   }

   @Test
   public void testNullFileName() throws Exception {
      exception.expect(ProtoSchemaBuilderException.class);
      exception.expectMessage("fileName cannot be null");

      SerializationContext ctx = createContext();
      ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
      protoSchemaBuilder.addClass(Simple.class).build(ctx);
   }

   /**
    * Demonstrates that a class with no fields is legal (but a warning is logged).
    */
   @ProtoDoc("@TypeId(100100)")
   @ProtoName("NoFields")
   static class NoProtoFields {
   }

   @Test
   public void testNoAnnotatedFields() throws Exception {
      expectedLogMessage.expect(1, Level.WARN, ".*NoProtoFields does not have any @ProtoField annotated members.*");

      SerializationContext ctx = createContext();
      String schema = new ProtoSchemaBuilder()
            .fileName("no_fields.proto")
            .addClass(NoProtoFields.class)
            .build(ctx);
      assertTrue(schema.contains("message NoFields {\n}\n"));

      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, new NoProtoFields());
      Object o = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertTrue(o instanceof NoProtoFields);
   }

   static class NonStandardPropertyAccessors {

      private long ttl;

      private long timestamp;

      @ProtoField(number = 1, defaultValue = "100")
      public long ttl() {
         return ttl;
      }

      public void ttl(long ttl) {
         this.ttl = ttl;
      }

      public long timestamp() {
         return timestamp;
      }

      @ProtoField(number = 2, defaultValue = "0")
      public void timestamp(long timestamp) {
         this.timestamp = timestamp;
      }
   }

   @Test
   public void testNoAnnotatedFields2() throws Exception {
      SerializationContext ctx = createContext();
      String schema = new ProtoSchemaBuilder()
            .fileName("no_fields.proto")
            .addClass(NonStandardPropertyAccessors.class)
            .build(ctx);
      assertTrue(schema.contains("message NonStandardPropertyAccessors"));

      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, new NonStandardPropertyAccessors());
      Object o = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertTrue(o instanceof NonStandardPropertyAccessors);
   }

   static class MessageWithAbstractFieldType {

      static abstract class AbstractType {

         @ProtoField(number = 1, required = true)
         int field1;
      }

      @ProtoField(1)
      AbstractType testField1;
   }

   /**
    * Abstract field types in a message class are not accepted.
    */
   @Test
   public void testAbstractClass() throws Exception {
      exception.expect(ProtoSchemaBuilderException.class);
      exception.expectMessage("The type org.infinispan.protostream.annotations.impl.ProtoSchemaBuilderTest.MessageWithAbstractFieldType.AbstractType of field 'testField1' of org.infinispan.protostream.annotations.impl.ProtoSchemaBuilderTest.MessageWithAbstractFieldType should not be abstract.");

      SerializationContext ctx = createContext();
      ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
      protoSchemaBuilder.fileName("test.proto");
      protoSchemaBuilder.addClass(MessageWithAbstractFieldType.class)
            .build(ctx);
   }

   /**
    * Anonymous classes are not instantiable by our standards, so a meaningful error message is given.
    */
   @Test
   public void testAnonymousClass() throws Exception {
      exception.expect(ProtoSchemaBuilderException.class);
      exception.expectMessage("Local or anonymous classes are not allowed. The class org.infinispan.protostream.annotations.impl.ProtoSchemaBuilderTest$1 must be instantiable using an accessible no-argument constructor.");

      SerializationContext ctx = createContext();
      ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
      protoSchemaBuilder.fileName("test.proto");

      Object msgInstance = new Object() {
         @ProtoField(1)
         String field1;
      };

      protoSchemaBuilder.addClass(msgInstance.getClass())
            .build(ctx);
   }

   /**
    * Local classes are not instantiable by our standards, so a meaningful error message is given.
    */
   @Test
   public void testLocalClass() throws Exception {
      exception.expect(ProtoSchemaBuilderException.class);
      exception.expectMessage("Local or anonymous classes are not allowed. The class org.infinispan.protostream.annotations.impl.ProtoSchemaBuilderTest$1LocalClass must be instantiable using an accessible no-argument constructor.");

      SerializationContext ctx = createContext();
      ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
      protoSchemaBuilder.fileName("test.proto");

      class LocalClass {

         @ProtoField(1)
         String field1;
      }

      protoSchemaBuilder.addClass(LocalClass.class)
            .build(ctx);
   }

   @Test
   public void testGeneration() throws Exception {
      SerializationContext ctx = createContext();
      ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
      protoSchemaBuilder
            .fileName("test.proto")
            .packageName("test_package")
            .addClass(TestClass.class)
            .addClass(TestClass3.class)
            .addClass(Simple.class)
            .build(ctx);

      assertTrue(ctx.canMarshall(TestEnum.class));
      assertTrue(ctx.canMarshall(Simple.class));
      assertTrue(ctx.canMarshall(TestClass.class));
      assertTrue(ctx.canMarshall(TestClass.InnerClass.class));
      assertTrue(ctx.canMarshall(TestClass.InnerClass2.class));
      assertTrue(ctx.canMarshall(TestClass2.class));
      assertTrue(ctx.canMarshall(TestClass3.class));

      assertTrue(ctx.canMarshall("test_package.TestEnumABC"));
      assertTrue(ctx.canMarshall("test_package.Simple"));
      assertTrue(ctx.canMarshall("test_package.TestClass2"));
      assertTrue(ctx.canMarshall("test_package.TestClass3"));
      assertTrue(ctx.canMarshall("test_package.TestClass"));
      assertTrue(ctx.canMarshall("test_package.TestClass.InnerClass"));
      assertTrue(ctx.canMarshall("test_package.TestClass.InnerClass2"));

      Simple simple = new Simple();
      simple.afloat = 3.14f;
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, simple);

      Object unmarshalled = ProtobufUtil.fromWrappedByteArray(ctx, bytes);

      assertTrue(unmarshalled instanceof Simple);
      Simple unwrapped = (Simple) unmarshalled;
      assertEquals(3.14f, unwrapped.afloat, 0.001);

      TestClass testClass = new TestClass();
      testClass.surname = "test";
      testClass.longField = 100L;
      testClass.testClass2 = new TestClass2();
      testClass.testClass2.address = "test address";
      bytes = ProtobufUtil.toWrappedByteArray(ctx, testClass);

      unmarshalled = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertTrue(unmarshalled instanceof TestClass);
      assertEquals("test", ((TestClass) unmarshalled).surname);
      assertEquals(100L, ((TestClass) unmarshalled).longField);
      assertEquals("test address", ((TestClass) unmarshalled).testClass2.address);
   }

   @Test
   public void testGeneration2() throws Exception {
      SerializationContext ctx = createContext();
      ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
      protoSchemaBuilder
            .fileName("test.proto")
            .packageName("test_package")
            .addClass(TestArraysAndCollectionsClass.class)
            .build(ctx);

      assertTrue(ctx.canMarshall(TestArraysAndCollectionsClass.class));
      assertTrue(ctx.canMarshall("test_package.TestArraysAndCollectionsClass"));

      TestArraysAndCollectionsClass testObject = new TestArraysAndCollectionsClass();
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, testObject);

      Object unmarshalled = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertTrue(unmarshalled instanceof TestArraysAndCollectionsClass);
   }

   @Test
   public void testGeneration3() throws Exception {
      SerializationContext ctx = createContext();
      ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
      protoSchemaBuilder
            .fileName("test.proto")
            .packageName("test_package")
            .addClass(TestArraysAndCollectionsClass2.class)
            .build(ctx);

      assertTrue(ctx.canMarshall(TestArraysAndCollectionsClass2.class));
      assertTrue(ctx.canMarshall("test_package.TestArraysAndCollectionsClass2"));

      TestArraysAndCollectionsClass2 testObject = new TestArraysAndCollectionsClass2();
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, testObject);

      Object unmarshalled = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertTrue(unmarshalled instanceof TestArraysAndCollectionsClass2);
   }

   @Test
   public void testTwoFilesGeneration() throws Exception {
      SerializationContext ctx = createContext();

      ProtoSchemaBuilder protoSchemaBuilder1 = new ProtoSchemaBuilder();
      protoSchemaBuilder1
            .fileName("test1.proto")
            .packageName("test_package1")
            .addClass(TestEnum.class)
            .build(ctx);

      assertTrue(ctx.canMarshall(TestEnum.class));
      assertTrue(ctx.canMarshall("test_package1.TestEnumABC"));

      ProtoSchemaBuilder protoSchemaBuilder2 = new ProtoSchemaBuilder();
      protoSchemaBuilder2
            .fileName("test2.proto")
            .packageName("test_package2")
            .addClass(TestClass.class)
            .build(ctx);

      assertTrue(ctx.canMarshall(TestClass.class));
      assertTrue(ctx.canMarshall("test_package2.TestClass"));
      assertTrue(ctx.canMarshall("test_package1.TestEnumABC"));
      assertFalse(ctx.canMarshall("test_package2.TestEnumABC"));
   }

   @Test
   public void testAutoImport() throws Exception {
      SerializationContext ctx = createContext();

      new ProtoSchemaBuilder()
            .fileName("test1.proto")
            .packageName("test_package1")
            .addClass(TestEnum.class)
            .build(ctx);

      assertTrue(ctx.canMarshall(TestEnum.class));
      assertTrue(ctx.canMarshall("test_package1.TestEnumABC"));

      ProtoSchemaBuilder protoSchemaBuilder2 = new ProtoSchemaBuilder();
      protoSchemaBuilder2
            .autoImportClasses(false)
            .fileName("test2.proto")
            .packageName("test_package2")
            .addClass(TestClass.class);

      try {
         // TestClass2 was not added explicitly and is not auto-added because autoImportClasses == false so we expect an exception
         protoSchemaBuilder2.build(ctx);
         fail("ProtoSchemaBuilderException expected");
      } catch (ProtoSchemaBuilderException e) {
         assertTrue(e.getMessage().contains("Found a reference to class org.infinispan.protostream.annotations.impl.testdomain.subpackage.TestClass2 which was not added to the builder and 'autoImportClasses' is disabled."));
      }

      assertFalse(ctx.canMarshall(TestClass.class));
      assertFalse(ctx.canMarshall(TestClass.InnerClass.class));
      assertFalse(ctx.canMarshall(TestClass.InnerClass2.class));
      assertFalse(ctx.canMarshall(TestClass2.class));

      // it must work after explicitly adding TestClass2
      protoSchemaBuilder2.addClass(TestClass2.class)
            .build(ctx);

      assertTrue(ctx.canMarshall(TestClass.class));
      assertTrue(ctx.canMarshall("test_package2.TestClass"));
      assertTrue(ctx.canMarshall(TestClass.InnerClass.class));
      assertTrue(ctx.canMarshall(TestClass.InnerClass2.class));
      assertTrue(ctx.canMarshall(TestClass2.class));
      assertTrue(ctx.canMarshall("test_package1.TestEnumABC"));
      assertFalse(ctx.canMarshall("test_package2.TestEnumABC"));
   }

   public enum TestOverride1 {

      @ProtoEnumValue(number = 0) A
   }

   @ProtoName("TestOverride1")
   public enum TestOverride2 {

      @ProtoEnumValue(number = 0) A
   }

   @Test
   public void testMarshallerOverride1() throws Exception {
      SerializationContext ctx = createContext();

      assertFalse(ctx.canMarshall(TestOverride1.class));
      assertFalse(ctx.canMarshall("package1.TestOverride1"));

      // generate schema and marshaller for TestOverride1, mapping it to package1.TestOverride1 in Protobuf
      new ProtoSchemaBuilder()
            .fileName("test1.proto")
            .packageName("package1")
            .addClass(TestOverride1.class)
            .build(ctx);

      // assert the type is marshallable, check all aspects of this
      assertTrue(ctx.canMarshall(TestOverride1.class));
      assertEquals("package1.TestOverride1", ctx.getMarshaller(TestOverride1.class).getTypeName());
      assertTrue(ctx.canMarshall("package1.TestOverride1"));
      assertEquals(TestOverride1.class, ctx.getMarshaller("package1.TestOverride1").getJavaClass());

      // generate schema and marshaller for TestOverride1 again, mapping it to package2.TestOverride1 in Protobuf
      new ProtoSchemaBuilder()
            .fileName("test2.proto")
            .packageName("package2")
            .addClass(TestOverride1.class)
            .build(ctx);

      // assert the type is marshallable, check all aspects of this
      assertTrue(ctx.canMarshall(TestOverride1.class));
      assertEquals("package2.TestOverride1", ctx.getMarshaller(TestOverride1.class).getTypeName());
      assertTrue(ctx.canMarshall("package2.TestOverride1"));
      assertEquals(TestOverride1.class, ctx.getMarshaller("package2.TestOverride1").getJavaClass());

      // assert the old mapping (TestOverride1 <-> package1.TestOverride1) is gone now
      assertFalse(ctx.canMarshall("package1.TestOverride1"));
   }

   @Test
   public void testMarshallerOverride2() throws Exception {
      SerializationContext ctx = createContext();

      assertFalse(ctx.canMarshall(TestOverride1.class));
      assertFalse(ctx.canMarshall("package1.TestOverride1"));

      // generate schema and marshaller for TestOverride1, mapping it to package1.TestOverride1 in Protobuf
      new ProtoSchemaBuilder()
            .fileName("test1.proto")
            .packageName("package1")
            .addClass(TestOverride1.class)
            .build(ctx);

      // generate schema and marshaller for TestOverride2, mapping it to package2.TestOverride1 in Protobuf
      new ProtoSchemaBuilder()
            .fileName("test2.proto")
            .packageName("package2")
            .addClass(TestOverride2.class)
            .build(ctx);

      // assert the type is marshallable, check all aspects of this
      assertTrue(ctx.canMarshall(TestOverride1.class));
      assertEquals("package1.TestOverride1", ctx.getMarshaller(TestOverride1.class).getTypeName());
      assertTrue(ctx.canMarshall("package1.TestOverride1"));
      assertEquals(TestOverride1.class, ctx.getMarshaller("package1.TestOverride1").getJavaClass());

      assertTrue(ctx.canMarshall(TestOverride2.class));
      assertEquals("package2.TestOverride1", ctx.getMarshaller(TestOverride2.class).getTypeName());
      assertTrue(ctx.canMarshall("package2.TestOverride1"));
      assertEquals(TestOverride2.class, ctx.getMarshaller("package2.TestOverride1").getJavaClass());

      // register marshaller for TestOverride1 again, mapping it to package2.TestOverride1 in Protobuf
      ctx.registerMarshaller(new EnumMarshaller<TestOverride1>() {
         @Override
         public Class<TestOverride1> getJavaClass() {
            return TestOverride1.class;
         }

         @Override
         public String getTypeName() {
            return "package2.TestOverride1";
         }

         @Override
         public TestOverride1 decode(int v) {
            return TestOverride1.A;
         }

         @Override
         public int encode(TestOverride1 e) throws IllegalArgumentException {
            return 1;
         }
      });

      // assert the type is marshallable, check all aspects of this
      assertTrue(ctx.canMarshall(TestOverride1.class));
      assertEquals("package2.TestOverride1", ctx.getMarshaller(TestOverride1.class).getTypeName());
      assertTrue(ctx.canMarshall("package2.TestOverride1"));
      assertEquals(TestOverride1.class, ctx.getMarshaller("package2.TestOverride1").getJavaClass());

      // assert the old type is no longer marshallable
      assertFalse(ctx.canMarshall(TestOverride2.class));

      // assert the old mapping (TestOverride1 <-> package1.TestOverride1) is gone now
      assertFalse(ctx.canMarshall("package1.TestOverride1"));
   }

   @Test
   public void testDocumentation() throws Exception {
      SerializationContext ctx = createContext();

      ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
      String schemaFile = protoSchemaBuilder
            .fileName("test1.proto")
            .packageName("test_package1")
            .addClass(TestEnum.class)
            .addClass(TestClass.class)
            .build(ctx);

      FileDescriptorSource fileDescriptorSource = FileDescriptorSource.fromString("test1.proto", schemaFile);
      Map<String, FileDescriptor> fileDescriptors = new SquareProtoParser(ctx.getConfiguration()).parse(fileDescriptorSource);

      FileDescriptor fd = fileDescriptors.get("test1.proto");
      assertNotNull(fd);

      Map<String, EnumDescriptor> enums = new HashMap<>();
      for (EnumDescriptor e : fd.getEnumTypes()) {
         enums.put(e.getFullName(), e);
      }

      Map<String, Descriptor> messages = new HashMap<>();
      for (Descriptor m : fd.getMessageTypes()) {
         messages.put(m.getFullName(), m);
      }

      EnumDescriptor testEnum = enums.get("test_package1.TestEnumABC");
      assertNotNull(testEnum);

      assertEquals("bla bla bla\nand some more bla\n@TypeId(100777)", testEnum.getDocumentation());
      assertEquals("This should never be read.", testEnum.getValues().get(0).getDocumentation());

      Descriptor testClass = messages.get("test_package1.TestClass");
      assertNotNull(testClass);

      assertEquals("@Indexed()\nbla bla bla\nand some more bla", testClass.getDocumentation());
      assertEquals("The surname, of course", testClass.getFields().get(0).getDocumentation());
   }

   @Test
   public void testUnknownFields() throws Exception {
      SerializationContext ctx = createContext();
      ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
      protoSchemaBuilder
            .fileName("test.proto")
            .packageName("test_package")
            .addClass(Simple.class)
            .build(ctx);

      assertTrue(ctx.canMarshall(Simple.class));
      assertTrue(ctx.canMarshall("test_package.Simple"));

      byte[] msg1 = ProtobufUtil.toByteArray(ctx, new Simple());

      byte[] msg2 = ProtobufUtil.toWrappedByteArray(ctx, 1234);

      // concatenate the two messages to have some unknown fields in the stream
      byte[] concatenatedMsg = new byte[msg1.length + msg2.length];
      System.arraycopy(msg1, 0, concatenatedMsg, 0, msg1.length);
      System.arraycopy(msg2, 0, concatenatedMsg, msg1.length, msg2.length);

      // we should be able to deal with those unknown fields gracefully when unmarshalling
      Object unmarshalled = ProtobufUtil.fromByteArray(ctx, concatenatedMsg, Simple.class);
      assertTrue(unmarshalled instanceof Simple);

      Simple simple = (Simple) unmarshalled;

      // ensure we do have some unknown fields there
      assertFalse(simple.unknownFieldSet.isEmpty());
   }

   static class TestCase_DuplicateEnumValueName {

      public enum E {

         @ProtoEnumValue(number = 0)
         A,

         @ProtoEnumValue(number = 1, name = "A")
         B
      }
   }

   @Test
   public void testDuplicateEnumValueName() throws Exception {
      exception.expect(ProtoSchemaBuilderException.class);
      exception.expectMessage("Found duplicate definition of Protobuf enum constant A on enum constant: org.infinispan.protostream.annotations.impl.ProtoSchemaBuilderTest.TestCase_DuplicateEnumValueName.E.B");

      SerializationContext ctx = createContext();

      new ProtoSchemaBuilder()
            .fileName("test1.proto")
            .packageName("test_package1")
            .addClass(TestCase_DuplicateEnumValueName.E.class)
            .build(ctx);
   }

   @ProtoMessage
   static class MessageWithImportedEnum {

      // this one corresponds to enum sample_bank_account.User.Gender
      @ProtoField(number = 1, defaultValue = "FEMALE")
      public User.Gender gender;
   }

   @Test
   public void testImportedEnum() throws Exception {
      SerializationContext ctx = createContext();
      ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
      protoSchemaBuilder
            .fileName("test.proto")
            .packageName("test_package")
            .addClass(MessageWithImportedEnum.class)
            .build(ctx);

      assertTrue(ctx.canMarshall(MessageWithImportedEnum.class));
      assertTrue(ctx.canMarshall("test_package.MessageWithImportedEnum"));
   }

   @ProtoMessage(name = "User")
   static class AnotherUser {

      @ProtoField(number = 1, defaultValue = "1")
      public byte gender;
   }

   @Test
   public void testReplaceExistingMarshallerWithAnnotations() throws Exception {
      SerializationContext ctx = createContext();

      assertTrue(ctx.canMarshall("sample_bank_account.User"));
      assertTrue(ctx.canMarshall(org.infinispan.protostream.domain.User.class));

      // replace 'sample_bank_account.User' with a new definition and also generate and register a new marshaller for it
      ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
      protoSchemaBuilder
            .fileName("sample_bank_account/bank.proto")
            .packageName("sample_bank_account")
            .addClass(AnotherUser.class)
            .build(ctx);

      assertTrue(ctx.canMarshall("sample_bank_account.User"));
      assertTrue(ctx.canMarshall(AnotherUser.class));
      // this 'sample_bank_account.User' definition does not have a 'name' field
      assertNull(ctx.getMessageDescriptor("sample_bank_account.User").findFieldByName("name"));

      // the old Java type it was mapping to is no longer marshallable
      assertFalse(ctx.canMarshall(org.infinispan.protostream.domain.User.class));
   }

   @Test
   public void testReplaceExistingMarshaller() throws Exception {
      SerializationContext ctx = createContext();

      assertTrue(ctx.canMarshall("sample_bank_account.User"));
      assertTrue(ctx.canMarshall(org.infinispan.protostream.domain.User.class));

      MessageMarshaller<AnotherUser> anotherUserMarshaller = new MessageMarshaller<AnotherUser>() {

         @Override
         public AnotherUser readFrom(ProtoStreamReader reader) throws IOException {
            int gender = reader.readInt("gender");
            AnotherUser anotherUser = new AnotherUser();
            anotherUser.gender = (byte) gender;
            return anotherUser;
         }

         @Override
         public void writeTo(ProtoStreamWriter writer, AnotherUser user) throws IOException {
            writer.writeInt("gender", user.gender);
         }

         @Override
         public String getTypeName() {
            return "sample_bank_account.User";
         }

         @Override
         public Class<AnotherUser> getJavaClass() {
            return AnotherUser.class;
         }
      };
      ctx.registerMarshaller(anotherUserMarshaller);

      assertTrue(ctx.canMarshall("sample_bank_account.User"));
      assertTrue(ctx.canMarshall(AnotherUser.class));
      assertFalse(ctx.canMarshall(org.infinispan.protostream.domain.User.class));
   }

   static class MessageWithAllFieldTypes {

      @ProtoField(number = 1, defaultValue = "A")
      char testField1;

      @ProtoField(number = 2, defaultValue = "A")
      char[] testField2 = {'a', 'b'};

      @ProtoField(number = 3, defaultValue = "A")
      Character testField3 = 'a';

      @ProtoField(number = 4, defaultValue = "A")
      Character[] testField4 = {'a', 'b'};

      @ProtoField(number = 5, defaultValue = "A")
      boolean testField5;

      @ProtoField(number = 6, defaultValue = "true")
      boolean[] testField6 = {true, true};

      @ProtoField(number = 7, defaultValue = "true")
      Boolean testField7 = true;

      @ProtoField(number = 8, defaultValue = "true")
      Boolean[] testField8 = {true, true};

      @ProtoField(number = 9, defaultValue = "100")
      short testField9;

      @ProtoField(number = 10, defaultValue = "100")
      short[] testField10 = {1, 2};

      @ProtoField(number = 11, defaultValue = "100")
      Short testField11 = 1;

      @ProtoField(number = 12, defaultValue = "100")
      Short[] testField12 = {1, 2};

      @ProtoField(number = 13, defaultValue = "100.5")
      float testField13;

      @ProtoField(number = 14, defaultValue = "100.5")
      float[] testField14 = {3.14f, 3.15f};

      @ProtoField(number = 15, defaultValue = "100.5")
      Float testField15 = 3.14f;

      @ProtoField(number = 16, defaultValue = "100.5")
      Float[] testField16 = {3.14f, 3.15f};

      @ProtoField(number = 17, defaultValue = "100.5")
      double testField17;

      @ProtoField(number = 18, defaultValue = "100.5")
      double[] testField18;

      @ProtoField(number = 19, defaultValue = "100.5")
      Double testField19 = 3.14;

      @ProtoField(number = 20, defaultValue = "100.5")
      Double[] testField20 = {3.14, 3.15};

      @ProtoField(number = 21, defaultValue = "100")
      int testField21;

      @ProtoField(number = 22, defaultValue = "100")
      int[] testField22;

      @ProtoField(number = 23, defaultValue = "100")
      Integer testField23 = 1;

      @ProtoField(number = 24, defaultValue = "100")
      Integer[] testField24 = {1, 2};

      @ProtoField(number = 25, defaultValue = "100")
      long testField25;

      @ProtoField(number = 26, defaultValue = "100")
      long[] testField26 = {1, 2};

      @ProtoField(number = 27, defaultValue = "100")
      Long testField27 = 1L;

      @ProtoField(number = 28, defaultValue = "100")
      Long[] testField28 = {1L, 2L};

      @ProtoField(number = 29, defaultValue = "xyz")
      String testField29 = "test";

      @ProtoField(number = 30, defaultValue = "xyz")
      String[] testField30 = {"one", "two"};

      @ProtoField(number = 31, defaultValue = "1")
      Date testField31 = new Date(100);

      @ProtoField(number = 32, defaultValue = "1")
      Date[] testField32 = {new Date(100), new Date(200)};

      @ProtoField(number = 33, defaultValue = "1")
      Instant testField33 = Instant.ofEpochMilli(100);

      @ProtoField(number = 34, defaultValue = "1")
      Instant[] testField34 = {Instant.ofEpochMilli(100), Instant.ofEpochMilli(200)};

      @ProtoField(number = 35, defaultValue = "35")
      byte[] testField35 = {1, 2, 3};

      @ProtoField(number = 36, defaultValue = "36")
      Byte[] testField36 = {1, 2, 3};

      private char testField51;

      @ProtoField(number = 51, defaultValue = "A")
      public char getTestField51() {
         return testField51;
      }

      public void setTestField51(char testField51) {
         this.testField51 = testField51;
      }

      private char[] testField52 = {'a', 'b'};

      @ProtoField(number = 52, defaultValue = "A")
      public char[] getTestField52() {
         return testField52;
      }

      public void setTestField52(char[] testField52) {
         this.testField52 = testField52;
      }

      private Character testField53 = 'a';

      @ProtoField(number = 53, defaultValue = "A")
      public Character getTestField53() {
         return testField53;
      }

      public void setTestField53(Character testField53) {
         this.testField53 = testField53;
      }

      private Character[] testField54 = {'a', 'b'};

      @ProtoField(number = 54, defaultValue = "A")
      public Character[] getTestField54() {
         return testField54;
      }

      public void setTestField54(Character[] testField54) {
         this.testField54 = testField54;
      }

      private boolean testField55;

      @ProtoField(number = 55, defaultValue = "A")
      public boolean isTestField55() {
         return testField55;
      }

      public void setTestField55(boolean testField55) {
         this.testField55 = testField55;
      }

      private boolean[] testField56 = {true, true};

      @ProtoField(number = 56, defaultValue = "true")
      public boolean[] getTestField56() {
         return testField56;
      }

      public void setTestField56(boolean[] testField56) {
         this.testField56 = testField56;
      }

      private Boolean testField57 = true;

      @ProtoField(number = 57, defaultValue = "true")
      public Boolean getTestField57() {
         return testField57;
      }

      public void setTestField57(Boolean testField57) {
         this.testField57 = testField57;
      }

      private Boolean[] testField58 = {true, true};

      @ProtoField(number = 58, defaultValue = "true")
      public Boolean[] getTestField58() {
         return testField58;
      }

      public void setTestField58(Boolean[] testField58) {
         this.testField58 = testField58;
      }

      private short testField59;

      @ProtoField(number = 59, defaultValue = "100")
      public short getTestField59() {
         return testField59;
      }

      public void setTestField59(short testField59) {
         this.testField59 = testField59;
      }

      private short[] testField60 = {1, 2};

      @ProtoField(number = 60, defaultValue = "100")
      public short[] getTestField60() {
         return testField60;
      }

      public void setTestField60(short[] testField60) {
         this.testField60 = testField60;
      }

      private Short testField61 = 1;

      @ProtoField(number = 61, defaultValue = "100")
      public Short getTestField61() {
         return testField61;
      }

      public void setTestField61(Short testField61) {
         this.testField61 = testField61;
      }

      private Short[] testField62 = {1, 2};

      @ProtoField(number = 62, defaultValue = "100")
      public Short[] getTestField62() {
         return testField62;
      }

      public void setTestField62(Short[] testField62) {
         this.testField62 = testField62;
      }

      private float testField63;

      @ProtoField(number = 63, defaultValue = "100.5")
      public float getTestField63() {
         return testField63;
      }

      public void setTestField63(float testField63) {
         this.testField63 = testField63;
      }

      private float[] testField64 = {3.14f, 3.15f};

      @ProtoField(number = 64, defaultValue = "100.5")
      public float[] getTestField64() {
         return testField64;
      }

      public void setTestField64(float[] testField64) {
         this.testField64 = testField64;
      }

      private Float testField65 = 3.14f;

      @ProtoField(number = 65, defaultValue = "100.5")
      public Float getTestField65() {
         return testField65;
      }

      public void setTestField65(Float testField65) {
         this.testField65 = testField65;
      }

      private Float[] testField66 = {3.14f, 3.15f};

      @ProtoField(number = 66, defaultValue = "100.5")
      public Float[] getTestField66() {
         return testField66;
      }

      public void setTestField66(Float[] testField66) {
         this.testField66 = testField66;
      }

      private double testField67;

      @ProtoField(number = 67, defaultValue = "100.5")
      public double getTestField67() {
         return testField67;
      }

      public void setTestField67(double testField67) {
         this.testField67 = testField67;
      }

      private double[] testField68;

      @ProtoField(number = 68, defaultValue = "100.5")
      public double[] getTestField68() {
         return testField68;
      }

      public void setTestField68(double[] testField68) {
         this.testField68 = testField68;
      }

      private Double testField69 = 3.14;

      @ProtoField(number = 69, defaultValue = "100.5")
      public Double getTestField69() {
         return testField69;
      }

      public void setTestField69(Double testField69) {
         this.testField69 = testField69;
      }

      private Double[] testField70 = {3.14, 3.15};

      @ProtoField(number = 70, defaultValue = "100.5")
      public Double[] getTestField70() {
         return testField70;
      }

      public void setTestField70(Double[] testField70) {
         this.testField70 = testField70;
      }

      private int testField71;

      @ProtoField(number = 71, defaultValue = "100")
      public int getTestField71() {
         return testField71;
      }

      public void setTestField71(int testField71) {
         this.testField71 = testField71;
      }

      private int[] testField72;

      @ProtoField(number = 72, defaultValue = "100")
      public int[] getTestField72() {
         return testField72;
      }

      public void setTestField72(int[] testField72) {
         this.testField72 = testField72;
      }

      private Integer testField73 = 1;

      @ProtoField(number = 73, defaultValue = "100")
      public Integer getTestField73() {
         return testField73;
      }

      public void setTestField73(Integer testField73) {
         this.testField73 = testField73;
      }

      private Integer[] testField74 = {1, 2};

      @ProtoField(number = 74, defaultValue = "100")
      public Integer[] getTestField74() {
         return testField74;
      }

      public void setTestField74(Integer[] testField74) {
         this.testField74 = testField74;
      }

      private long testField75;

      @ProtoField(number = 75, defaultValue = "100")
      public long getTestField75() {
         return testField75;
      }

      public void setTestField75(long testField75) {
         this.testField75 = testField75;
      }

      private long[] testField76 = {1, 2};

      @ProtoField(number = 76, defaultValue = "100")
      public long[] getTestField76() {
         return testField76;
      }

      public void setTestField76(long[] testField76) {
         this.testField76 = testField76;
      }

      private Long testField77 = 1L;

      @ProtoField(number = 77, defaultValue = "100")
      public Long getTestField77() {
         return testField77;
      }

      public void setTestField77(Long testField77) {
         this.testField77 = testField77;
      }

      private Long[] testField78 = {1L, 2L};

      @ProtoField(number = 78, defaultValue = "100")
      public Long[] getTestField78() {
         return testField78;
      }

      public void setTestField78(Long[] testField78) {
         this.testField78 = testField78;
      }

      private String testField79 = "test";

      @ProtoField(number = 79, defaultValue = "xyz")
      public String getTestField79() {
         return testField79;
      }

      public void setTestField79(String testField79) {
         this.testField79 = testField79;
      }

      private String[] testField80 = {"one", "two"};

      @ProtoField(number = 80, defaultValue = "xyz")
      public String[] getTestField80() {
         return testField80;
      }

      public void setTestField80(String[] testField80) {
         this.testField80 = testField80;
      }

      private Date testField81 = new Date(100);

      @ProtoField(number = 81, defaultValue = "1")
      public Date getTestField81() {
         return testField81;
      }

      public void setTestField81(Date testField81) {
         this.testField81 = testField81;
      }

      private Date[] testField82 = {new Date(100), new Date(200)};

      @ProtoField(number = 82, defaultValue = "1")
      public Date[] getTestField82() {
         return testField82;
      }

      public void setTestField82(Date[] testField82) {
         this.testField82 = testField82;
      }

      private Instant testField83 = Instant.ofEpochMilli(100);

      @ProtoField(number = 83, defaultValue = "1")
      public Instant getTestField83() {
         return testField83;
      }

      public void setTestField83(Instant testField83) {
         this.testField83 = testField83;
      }

      private Instant[] testField84 = {Instant.ofEpochMilli(100), Instant.ofEpochMilli(200)};

      @ProtoField(number = 84, defaultValue = "1")
      public Instant[] getTestField84() {
         return testField84;
      }

      public void setTestField84(Instant[] testField84) {
         this.testField84 = testField84;
      }

      private byte[] testField85 = {1, 2, 3};

      @ProtoField(number = 85, defaultValue = "85")
      public byte[] getTestField85() {
         return testField85;
      }

      public void setTestField85(byte[] testField85) {
         this.testField85 = testField85;
      }

      private Byte[] testField86 = {1, 2, 3};

      @ProtoField(number = 86, defaultValue = "86")
      public Byte[] getTestField86() {
         return testField86;
      }

      public void setTestField86(Byte[] testField86) {
         this.testField86 = testField86;
      }
   }

   @Test
   public void testAllFieldTypes() throws Exception {
      SerializationContext ctx = createContext();
      new ProtoSchemaBuilder()
            .fileName("test.proto")
            .addClass(MessageWithAllFieldTypes.class)
            .build(ctx);

      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, new MessageWithAllFieldTypes());
      Object o = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertTrue(o instanceof MessageWithAllFieldTypes);
   }

   static class MessageWithRepeatedFields {

      @ProtoField(1001)
      byte[] testField1001;

      @ProtoField(1002)
      Byte[] testField1002;

      @ProtoField(1003)
      List<Byte> testField1003;

      @ProtoField(1)
      int[] testField1;

      @ProtoField(2)
      Integer[] testField2;

      static class MyArrayList extends ArrayList {
      }

      @ProtoField(number = 3, collectionImplementation = MyArrayList.class)
      List<Integer> testField3;

      @ProtoField(4)
      Inner[] testField4;

      @ProtoField(5)
      List<Inner> testField5;

      int[] testField6;

      Integer[] testField7;

      List<Integer> testField8;

      Inner[] testField9;

      List<Inner> testField10;

      @ProtoField(6)
      public int[] getTestField6() {
         return testField6;
      }

      public void setTestField6(int[] testField6) {
         this.testField6 = testField6;
      }

      @ProtoField(7)
      public Integer[] getTestField7() {
         return testField7;
      }

      public void setTestField7(Integer[] testField7) {
         this.testField7 = testField7;
      }

      @ProtoField(8)
      public List<Integer> getTestField8() {
         return testField8;
      }

      public void setTestField8(List<Integer> testField8) {
         this.testField8 = testField8;
      }

      @ProtoField(9)
      public Inner[] getTestField9() {
         return testField9;
      }

      public void setTestField9(Inner[] testField9) {
         this.testField9 = testField9;
      }

      @ProtoField(10)
      public List<Inner> getTestField10() {
         return testField10;
      }

      public void setTestField10(List<Inner> testField10) {
         this.testField10 = testField10;
      }

      static class Inner {

         @ProtoField(number = 1, required = true)
         int intField;
      }
   }

   @Test
   public void testNonNullRepeatedFields() throws Exception {
      SerializationContext ctx = createContext();
      new ProtoSchemaBuilder()
            .fileName("test.proto")
            .addClass(MessageWithRepeatedFields.class)
            .build(ctx);

      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, new MessageWithRepeatedFields());
      MessageWithRepeatedFields o = ProtobufUtil.fromWrappedByteArray(ctx, bytes);

      assertNotNull(o);
      assertNotNull(o.testField1);
      assertEquals(0, o.testField1.length);
      assertNotNull(o.testField2);
      assertEquals(0, o.testField2.length);
      assertNotNull(o.testField3);
      assertEquals(0, o.testField3.size());
      assertNotNull(o.testField4);
      assertEquals(0, o.testField4.length);
      assertNotNull(o.testField5);
      assertEquals(0, o.testField5.size());
      assertNotNull(o.testField6);
      assertEquals(0, o.testField6.length);
      assertNotNull(o.testField7);
      assertEquals(0, o.testField7.length);
      assertNotNull(o.testField8);
      assertEquals(0, o.testField8.size());
      assertNotNull(o.testField9);
      assertEquals(0, o.testField9.length);
      assertNotNull(o.testField10);
      assertEquals(0, o.testField10.size());
   }

   interface ByteBuffer {

      @ProtoField(number = 1, name = "theBytes")
      byte[] getBytes();
   }

   @ProtoReserved(numbers = 42)
   @ProtoReserved(ranges = @Range(from = 55, to = 66))
   @ProtoReserved(names = {"oldBytes", "ancientByteArray"})
   static class ByteBufferImpl implements ByteBuffer {

      private byte[] bytes;

      @Override
      public byte[] getBytes() {
         return bytes;
      }

      public void setBytes(byte[] bytes) {
         this.bytes = bytes;
      }
   }

   static class ByteBufferWrapper {

      private ByteBufferImpl buffer;

      @ProtoField(number = 5, name = "value", javaType = ByteBufferImpl.class)
      public ByteBuffer getBuffer() {
         return buffer;
      }

      public void setBuffer(ByteBuffer buffer) {
         this.buffer = (ByteBufferImpl) buffer;
      }
   }

   @Test
   public void testImplementInterface() throws Exception {
      SerializationContext ctx = createContext();
      new ProtoSchemaBuilder()
            .fileName("test.proto")
            .addClass(ByteBufferWrapper.class)
            .build(ctx);

      ByteBufferWrapper wrapper = new ByteBufferWrapper();
      wrapper.setBuffer(new ByteBufferImpl());
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, wrapper);
      ByteBufferWrapper o = ProtobufUtil.fromWrappedByteArray(ctx, bytes);

      assertNotNull(o);
      assertTrue(o.getBuffer() instanceof ByteBufferImpl);
   }

   static final class RGBColor {

      private final int r;

      private final int g;

      private final int b;

      @ProtoFactory
      public RGBColor(int r, int g, int b) {
         this.r = r;
         this.g = g;
         this.b = b;
      }

      @ProtoField(number = 1, defaultValue = "-1")
      public int getR() {
         return r;
      }

      @ProtoField(number = 2, defaultValue = "-1")
      public int getG() {
         return g;
      }

      @ProtoField(number = 3, defaultValue = "-1")
      public int getB() {
         return b;
      }
   }

   static final class ImmutableColor {

      @ProtoField(number = 1, defaultValue = "1")
      byte r;

      //@ProtoField(number = 1, defaultValue = "1")
      void setR(byte r) {
         this.r = r;
      }

      byte g;

      byte[] b;

      @ProtoField(number = 12, defaultValue = "12")
      final int[] i;

      @ProtoField(number = 13, defaultValue = "13")
      final List<Integer> li;

      //@ProtoFactory
      ImmutableColor(byte r, byte g, byte[] b, int[] i, ArrayList<Integer> li) {
         this.r = r;
         this.g = g;
         this.b = b;
         this.i = i;
         this.li = li;
      }

      @ProtoFactory
      static ImmutableColor make(byte r, byte g, byte[] b, int[] i, List<Integer> li) {
         return new ImmutableColor(r, g, b, i, (ArrayList<Integer>) li);
      }

      byte getG() {
         return g;
      }

      @ProtoField(number = 2, defaultValue = "2")
      void setG(byte g) {
         this.g = g;
      }

      @ProtoField(number = 3, defaultValue = "3")
      byte[] getB() {
         return b;
      }
   }

   static final class AlphaColor {

      private final int r;

      private final int g;

      private final int b;

      private final float transparency;

      @ProtoFactory
      public AlphaColor(int r, int g, int b, float transparency) {
         this.r = r;
         this.g = g;
         this.b = b;
         this.transparency = transparency;
      }

      @ProtoField(number = 1, required = true)
      public int getR() {
         return r;
      }

      @ProtoField(number = 2, required = true)
      public int getG() {
         return g;
      }

      @ProtoField(number = 3, required = true)
      public int getB() {
         return b;
      }

      @ProtoField(number = 4, required = true)
      public float getTransparency() {
         return transparency;
      }
   }

   @Test
   public void testFactoryMethod() throws Exception {
      SerializationContext ctx = createContext();
      String schema = new ProtoSchemaBuilder()
            .fileName("immutable.proto")
            .addClass(RGBColor.class)
            .addClass(ImmutableColor.class)
            .addClass(AlphaColor.class)
            .build(ctx);

      assertTrue(schema.contains("message RGBColor"));
      assertTrue(schema.contains("message ImmutableColor"));
      assertTrue(schema.contains("message AlphaColor"));

      RGBColor color = new RGBColor(55, 66, 77);
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, color);
      RGBColor o = ProtobufUtil.fromWrappedByteArray(ctx, bytes);

      assertNotNull(o);
      assertEquals(55, o.r);
      assertEquals(66, o.g);
      assertEquals(77, o.b);
   }

   static final class OuterClass {
      enum InnerEnum {
         @ProtoEnumValue(number = 0) OPTION_A,
         @ProtoEnumValue(number = 1) OPTION_B
      }
   }

   @Test
   public void testNestedEnum() throws Exception {
      SerializationContext ctx = createContext();
      String schema = new ProtoSchemaBuilder()
            .fileName("inner_enum.proto")
            .addClass(OuterClass.InnerEnum.class)
            .build(ctx);

      assertTrue(schema.contains("enum InnerEnum"));
      assertTrue(ctx.canMarshall(OuterClass.InnerEnum.class));
   }

   static final class GenericMessage {

      @ProtoField(1)
      WrappedMessage field1;

      @ProtoField(2)
      WrappedMessage field2;

      @ProtoField(3)
      WrappedMessage field3;

      @ProtoField(4)
      WrappedMessage field4;

      static final class OtherMessage {

         @ProtoField(1)
         String field1;

         @ProtoFactory
         public OtherMessage(String field1) {
            this.field1 = field1;
         }
      }
   }

   @Test
   public void testGenericMessage() throws Exception {
      SerializationContext ctx = createContext();
      String schema = new ProtoSchemaBuilder()
            .fileName("generic_message.proto")
            .addClass(GenericMessage.class)
            .addClass(GenericMessage.OtherMessage.class)
            .build(ctx);

      assertTrue(schema.contains("message GenericMessage"));

      GenericMessage genericMessage = new GenericMessage();
      genericMessage.field1 = new WrappedMessage(3.1415d);
      genericMessage.field2 = new WrappedMessage("qwerty".getBytes());
      genericMessage.field3 = new WrappedMessage(new WrappedMessage("azerty"));
      genericMessage.field4 = new WrappedMessage(new GenericMessage.OtherMessage("asdfg"));

      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, genericMessage);
      GenericMessage o = ProtobufUtil.fromWrappedByteArray(ctx, bytes);

      assertNotNull(o);
      assertEquals(Double.class, genericMessage.field1.getValue().getClass());
      assertEquals(3.1415d, genericMessage.field1.getValue());
      assertArrayEquals("qwerty".getBytes(), (byte[]) genericMessage.field2.getValue());
      assertEquals("azerty", ((WrappedMessage) genericMessage.field3.getValue()).getValue());
      assertEquals("asdfg", ((GenericMessage.OtherMessage) genericMessage.field4.getValue()).field1);
   }

   static final class ListOfBytes {

      @ProtoField(1)
      List<byte[]> theListOfBytes;
   }

   @Test
   public void testListOfBytes() throws Exception {
      SerializationContext ctx = createContext();
      String schema = new ProtoSchemaBuilder()
            .fileName("test_list_of_bytes.proto")
            .addClass(ListOfBytes.class)
            .build(ctx);

      assertTrue(schema.contains("message ListOfBytes"));

      ListOfBytes listOfBytes = new ListOfBytes();
      listOfBytes.theListOfBytes = new ArrayList<>();
      listOfBytes.theListOfBytes.add(new byte[]{1, 2, 3});

      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, listOfBytes);
      ListOfBytes o = ProtobufUtil.fromWrappedByteArray(ctx, bytes);

      assertNotNull(o);
      assertEquals(1, listOfBytes.theListOfBytes.size());
      assertArrayEquals(new byte[]{1, 2, 3}, listOfBytes.theListOfBytes.get(0));
   }

   /**
    * Demonstrates an entity that has a field of type Map<CustomKey, String>.
    */
   static class CustomMap {

      static class CustomKey {
         @ProtoField(1)
         String key;

         CustomKey() {
         }

         CustomKey(String key) {
            this.key = key;
         }

         @Override
         public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CustomKey customKey = (CustomKey) o;
            return key != null ? key.equals(customKey.key) : customKey.key == null;
         }

         @Override
         public int hashCode() {
            return key != null ? key.hashCode() : 0;
         }
      }

      static class KVPair {

         @ProtoField(1)
         CustomKey key;

         @ProtoField(2)
         String value;

         KVPair() {
         }

         KVPair(Map.Entry<CustomKey, String> entry) {
            this.key = entry.getKey();
            this.value = entry.getValue();
         }
      }

      private Map<CustomKey, String> myMap;

      CustomMap() {
      }

      CustomMap(Map<CustomKey, String> myMap) {
         this.myMap = myMap;
      }

      public Map<CustomKey, String> getMyMap() {
         return myMap;
      }

      @ProtoField(1)
      public List<KVPair> getMapEntries() {
         if (myMap == null) {
            return Collections.emptyList();
         }
         List<KVPair> pairs = new ArrayList<>(myMap.size());
         for (Map.Entry<CustomKey, String> e : myMap.entrySet()) {
            pairs.add(new KVPair(e));
         }
         return pairs;
      }

      public void setMapEntries(List<KVPair> entries) {
         myMap = new HashMap<>();
         entries.forEach(p -> myMap.put(p.key, p.value));
      }
   }

   @Test
   public void testCustomMap() throws Exception {
      SerializationContext ctx = createContext();
      String schema = new ProtoSchemaBuilder()
            .fileName("test_custom_map.proto")
            .addClass(CustomMap.class)
            .build(ctx);

      assertTrue(schema.contains("message CustomMap"));

      Map<CustomMap.CustomKey, String> myMap = new HashMap<>();
      myMap.put(new CustomMap.CustomKey("k"), "v");
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, new CustomMap(myMap));
      CustomMap o = ProtobufUtil.fromWrappedByteArray(ctx, bytes);

      assertNotNull(o);
      assertTrue(o.getMyMap() instanceof HashMap);
      assertEquals("v", o.getMyMap().get(new CustomMap.CustomKey("k")));
   }

   static class Optionals {

      private String field1;

      private OptionalInner field2;

      private OptionalInner[] field3;

      private List<OptionalInner> field4;

      @ProtoField(1)
      public Optional<String> getField1() {
         return Optional.ofNullable(field1);
      }

      public void setField1(String field1) {
         this.field1 = field1;
      }

      @ProtoField(2)
      public Optional<OptionalInner> getField2() {
         return Optional.ofNullable(field2);
      }

      public void setField2(OptionalInner field2) {
         this.field2 = field2;
      }

      @ProtoField(3)
      public Optional<OptionalInner[]> getField3() {
         return Optional.ofNullable(field3);
      }

      public void setField3(OptionalInner[] field3) {
         this.field3 = field3;
      }

      @ProtoField(4)
      public Optional<List<OptionalInner>> getField4() {
         return Optional.ofNullable(field4);
      }

      public void setField4(List<OptionalInner> field4) {
         this.field4 = field4;
      }

      static class OptionalInner {

         @ProtoField(1)
         String theString;
      }
   }

   @Test
   public void testOptional() throws Exception {
      SerializationContext ctx = createContext();
      String schema = new ProtoSchemaBuilder()
            .fileName("test_optional.proto")
            .addClass(Optionals.class)
            .build(ctx);

      assertTrue(schema.contains("message Optionals"));

      Optionals opt = new Optionals();
      opt.field1 = "abc";
      opt.field2 = new Optionals.OptionalInner();
      opt.field2.theString = "xyz";

      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, opt);
      Optionals o = ProtobufUtil.fromWrappedByteArray(ctx, bytes);

      assertNotNull(o);
      assertEquals("abc", o.field1);
      assertNotNull(o.field2);
      assertEquals("xyz", o.field2.theString);
   }

   static abstract class AbstractMessage {

      @ProtoField(1)
      String baseField1;
   }

   static class InnerMessage1 extends AbstractMessage {
   }

   static class OuterMessage1 {

      @ProtoField(1)
      InnerMessage1 inner;
   }

   static class OuterMessage2 {

      static class InnerMessage2 extends AbstractMessage {
         @ProtoField(2)
         String field2;
      }

      @ProtoField(1)
      InnerMessage2 inner;
   }

   static class OuterMessage3 {

      // this class is nested but not referenced from the outer class or the builder, so it does not get included
      static class InnerMessage3 extends AbstractMessage {
         @ProtoField(2)
         String field2;
      }

      @ProtoField(1)
      String field1;
   }

   @Test
   public void testDiscoveryWithoutAutoImport() throws Exception {
      SerializationContext ctx = createContext();

      try {
         new ProtoSchemaBuilder()
               .fileName("DiscoveryWithoutAutoImport.proto")
               .addClass(OuterMessage1.class)
               .autoImportClasses(false)
               .build(ctx);
         fail("ProtoSchemaBuilderException was expected");
      } catch (ProtoSchemaBuilderException e) {
         assertEquals("Found a reference to class org.infinispan.protostream.annotations.impl.ProtoSchemaBuilderTest.InnerMessage1"
               + " which was not added to the builder and 'autoImportClasses' is disabled.", e.getMessage());
      }

      // ensure that it starts working once we turn autoImportClasses on
      String schema = new ProtoSchemaBuilder()
            .fileName("DiscoveryWithAutoImport.proto")
            .addClass(OuterMessage1.class)
            .autoImportClasses(true)
            .build(ctx);

      assertTrue(schema.contains("message OuterMessage1"));
      assertTrue(schema.contains("message InnerMessage1"));
      assertTrue(schema.contains("baseField1"));
   }

   @Test
   public void testNestedDiscoveryWithoutAutoImport() throws Exception {
      SerializationContext ctx = createContext();
      String schema = new ProtoSchemaBuilder()
            .fileName("DiscoveryWithoutAutoImport.proto")
            .addClass(OuterMessage2.class)
            .autoImportClasses(false)  // even with autoImportClasses disabled, InnerMessage2 is still discovered because it's a nested class
            .build(ctx);

      assertTrue(schema.contains("message OuterMessage2"));
      assertTrue(schema.contains("message InnerMessage2"));
      assertTrue(schema.contains("baseField1"));

      schema = new ProtoSchemaBuilder()
            .fileName("DiscoveryWithoutAutoImport.proto")
            .addClass(OuterMessage3.class)
            .autoImportClasses(false)
            .build(ctx);

      assertTrue(schema.contains("message OuterMessage3"));
      assertFalse(schema.contains("message InnerMessage3"));  // InnerMessage3 is a nested class but still not included
      assertFalse(schema.contains("baseField1"));
   }
}
