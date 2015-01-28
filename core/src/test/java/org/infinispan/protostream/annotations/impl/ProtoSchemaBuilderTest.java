package org.infinispan.protostream.annotations.impl;

import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
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
import org.infinispan.protostream.impl.parser.SquareProtoParser;
import org.infinispan.protostream.test.AbstractProtoStreamTest;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public class ProtoSchemaBuilderTest extends AbstractProtoStreamTest {

   @org.junit.Rule
   public ExpectedException exception = ExpectedException.none();

   @Test
   @Ignore
   public void testMain() throws Exception {
      String[] args = {
            "-s", "sample_bank_account/bank.proto=sample-domain-definition/src/main/resources/sample_bank_account/bank.proto",
            "-m", "org.infinispan.protostream.domain.marshallers.UserMarshaller",
            "-m", "org.infinispan.protostream.domain.marshallers.GenderMarshaller",
            "-f", "test.proto",
            "-p", "my_package",
            "org.infinispan.protostream.domain.Note"
      };
      ProtoSchemaBuilder.main(args);
   }

   @Test
   public void testNullFileName() throws Exception {
      exception.expect(ProtoSchemaBuilderException.class);
      exception.expectMessage("fileName cannot be null");

      SerializationContext ctx = createContext();
      ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
      protoSchemaBuilder.addClass(Simple.class).build(ctx);
   }

   @Test
   public void testNoAnnotations() throws Exception {
      exception.expect(ProtoSchemaBuilderException.class);
      exception.expectMessage("Class java.lang.Object does not have any @ProtoField annotated fields. The class should be either annotated or it should have a custom marshaller");

      SerializationContext ctx = createContext();
      ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
      protoSchemaBuilder.fileName("test.proto");
      protoSchemaBuilder.addClass(Object.class).build(ctx);
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
      testClass.testClass2 = new TestClass2();
      testClass.testClass2.address = "test address";
      bytes = ProtobufUtil.toWrappedByteArray(ctx, testClass);

      unmarshalled = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertTrue(unmarshalled instanceof TestClass);
      assertEquals("test", ((TestClass) unmarshalled).surname);
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
      assertFalse(ctx.canMarshall("test_package2.TestEnumABC"));
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

      Map<String, EnumDescriptor> enums = new HashMap<String, EnumDescriptor>();
      for (EnumDescriptor e : fd.getEnumTypes()) {
         enums.put(e.getFullName(), e);
      }

      Map<String, Descriptor> messages = new HashMap<String, Descriptor>();
      for (Descriptor m : fd.getMessageTypes()) {
         messages.put(m.getFullName(), m);
      }

      EnumDescriptor testEnum = enums.get("test_package1.TestEnumABC");
      assertNotNull(testEnum);

      assertEquals("bla bla bla\nand some more bla", testEnum.getDocumentation());
      assertEquals("This should never be read.", testEnum.getValues().get(0).getDocumentation());

      Descriptor testClass = messages.get("test_package1.TestClass");
      assertNotNull(testClass);

      assertEquals("@Indexed()\nbla bla bla\nand some more bla", testClass.getDocumentation());
      assertEquals("The surname, of course", testClass.getFields().get(0).getDocumentation());
   }
}
