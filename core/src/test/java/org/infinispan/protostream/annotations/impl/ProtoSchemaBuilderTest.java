package org.infinispan.protostream.annotations.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoEnum;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoField;
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

      assertEquals("bla bla bla\nand some more bla", testEnum.getDocumentation());
      assertEquals("This should never be read.", testEnum.getValues().get(0).getDocumentation());

      Descriptor testClass = messages.get("test_package1.TestClass");
      assertNotNull(testClass);

      assertEquals("@Indexed()\nbla bla bla\nand some more bla", testClass.getDocumentation());
      assertEquals("The surname, of course", testClass.getFields().get(0).getDocumentation());
   }

   static class TestCase_DuplicateEnumValueName {

      @ProtoEnum
      public enum E {

         @ProtoEnumValue(number = 1)
         A,

         @ProtoEnumValue(number = 2, name = "A")
         B
      }
   }

   @Test
   public void testDuplicateEnumValueName() throws Exception {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Enum constant 'A' is already defined in test_package1.E");

      SerializationContext ctx = createContext();

      new ProtoSchemaBuilder()
            .fileName("test1.proto")
            .packageName("test_package1")
            .addClass(TestCase_DuplicateEnumValueName.E.class)
            .build(ctx);
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
      ProtobufUtil.fromWrappedByteArray(ctx, bytes);
   }
}
