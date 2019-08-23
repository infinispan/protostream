package org.infinispan.protostream.annotations.impl.processor.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.annotations.ProtoReserved;
import org.infinispan.protostream.annotations.ProtoReserved.Range;
import org.infinispan.protostream.annotations.impl.processor.tests.testdomain.SimpleClass;
import org.infinispan.protostream.annotations.impl.processor.tests.testdomain.SimpleEnum;
import org.junit.Test;

public class AutoProtoSchemaBuilderTest {

   @ProtoDoc("This is the documentation")
   @ProtoName("NoteMsg")
   public static class Note {

      @ProtoField(number = 1, required = true)
      boolean flag;

      private String text;

      @ProtoField(number = 2)
      public String getText() {
         return text;
      }

      void setText(String text) {
         this.text = text;
      }
   }

   @ProtoReserved(42)
   @ProtoReserved(ranges = @Range(from = 55))
   @ProtoReserved(names = {"oldBytes", "ancientByteArray"})
   interface ByteBuffer {

      @ProtoField(number = 1, name = "theBytes")
      byte[] getBytes();
   }

   @ProtoReserved(ranges = @Range(from = 45, to = 54))
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

   static class X {

      @ProtoField(number = 1, name = "__someByte")
      public byte[] someBytes;

      @ProtoField(number = 2, name = "__someShorts")
      public short[] someShorts;

      @ProtoField(number = 3, name = "__someFloats")
      public float[] someFloats;

      @ProtoField(number = 4, name = "__someInts")
      public List<Integer> someInts;

      private ByteBufferImpl buffer;

      @ProtoField(number = 5, name = "value", javaType = ByteBufferImpl.class)
      public ByteBuffer getBuffer() {
         return buffer;
      }

      public void setBuffer(ByteBuffer buffer) {
         this.buffer = (ByteBufferImpl) buffer;
      }
   }

   // test inheritance of java properties and annotations
   public static class EmbeddedMetadata {

      private String version;

      @ProtoField(number = 1)
      public String getVersion() {
         return version;
      }

      public void setVersion(String version) {
         this.version = version;
      }

      public static class EmbeddedLifespanExpirableMetadata extends EmbeddedMetadata {

         @ProtoField(number = 2, defaultValue = "-1")
         long lifespan;
      }
   }

   @AutoProtoSchemaBuilder(schemaFileName = "TestFile.proto", schemaFilePath = "org/infinispan/protostream/generated_schemas", schemaPackageName = "firstTestPackage",
         service = true,
         includeClasses = {
               Note.class,
               SimpleClass.class,
               SimpleClass.class, // duplicates are handled nicely
               ByteBufferImpl.class,
//               EmbeddedMetadata.class,
               EmbeddedMetadata.EmbeddedLifespanExpirableMetadata.class,
               SimpleEnum.class,
//               String.class,
               X.class
         }
   )
   interface TestSerializationContextInitializer extends SerializationContextInitializer {
   }

   @Test
   public void testGeneratedInitializer() throws Exception {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();

      TestSerializationContextInitializer serCtxInitializer = new TestSerializationContextInitializerImpl();
      serCtxInitializer.registerSchema(ctx);
      serCtxInitializer.registerMarshallers(ctx);

      assertTrue(ctx.canMarshall(Note.class));
      assertTrue(ctx.canMarshall("firstTestPackage.NoteMsg"));
      ProtobufUtil.toWrappedByteArray(ctx, new Note());

      assertTrue(ctx.canMarshall(SimpleClass.class));
   }

   @AutoProtoSchemaBuilder(schemaFilePath = "second_initializer", className = "TestInitializer", autoImportClasses = true,
         basePackages = "org.infinispan.protostream.annotations.impl.processor", service = true)
   abstract static class SecondInitializer implements SerializationContextInitializer {
      SecondInitializer() {
      }
   }

   @Test
   public void testGeneratedService() {
      SecondInitializer initializer = null;
      for (SerializationContextInitializer sci : ServiceLoader.load(SerializationContextInitializer.class)) {
         if (sci instanceof SecondInitializer) {
            initializer = (SecondInitializer) sci;
            break;
         }
      }

      assertNotNull("SecondInitializer implementation not found by ServiceLoader", initializer);
      assertEquals("org.infinispan.protostream.annotations.impl.processor.tests.TestInitializer", initializer.getClass().getName());
   }

   @Test
   public void testLocalAnnotatedClassesAreSkipped() {
      // Standard Java annotation processors do not process the bodies of methods, so LocalInitializer is never seen by our AP and no code is generated for it, and that is OK.
      // If we ever decide to process method bodies we should probably study the approach used by "The Checker Framework" (https://checkerframework.org).
      @AutoProtoSchemaBuilder(className = "NeverEverGenerated",
            basePackages = "org.infinispan.protostream.annotations.impl.processor", service = true)
      abstract class LocalInitializer implements SerializationContextInitializer {
      }

      for (SerializationContextInitializer sci : ServiceLoader.load(SerializationContextInitializer.class)) {
         if (sci.getClass().getSimpleName().equals("NeverEverGenerated")) {
            fail("Local classes should not be processed by AutoProtoSchemaBuilderAnnotationProcessor.");
         }
      }
   }

   // Using a fully implemented initializer as a base is not the usual use case but some users might need this and we do support it.
   @AutoProtoSchemaBuilder(className = "NonAbstractInitializerImpl", autoImportClasses = true,
         basePackages = "org.infinispan.protostream.annotations.impl.processor", service = true)
   static class NonAbstractInitializer implements SerializationContextInitializer {

      @Override
      public String getProtoFileName() {
         return null;
      }

      @Override
      public String getProtoFile() {
         return null;
      }

      @Override
      public void registerSchema(SerializationContext serCtx) {
      }

      @Override
      public void registerMarshallers(SerializationContext serCtx) {
      }
   }

   @Test
   public void testNonAbstractInitializer() {
      boolean found = false;
      for (SerializationContextInitializer sci : ServiceLoader.load(SerializationContextInitializer.class)) {
         if (sci.getClass().getSimpleName().equals("NonAbstractInitializerImpl")) {
            found = true;
            break;
         }
      }
      assertTrue("Non-abstract initializers must be supported", found);
   }

   @AutoProtoSchemaBuilder(dependsOn = ReusableInitializer.class, includeClasses = DependentInitializer.C.class, service = true)
   public interface DependentInitializer extends SerializationContextInitializer {
      class C {
         @ProtoField(number = 1, required = true)
         public boolean flag;

         @ProtoField(number = 2)
         public ReusableInitializer.A a;
      }
   }

   @Test
   public void testDependsOn() throws Exception {
      DependentInitializer dependentInitializer = null;
      for (SerializationContextInitializer sci : ServiceLoader.load(SerializationContextInitializer.class)) {
         if (sci instanceof DependentInitializer) {
            dependentInitializer = (DependentInitializer) sci;
            break;
         }
      }

      assertNotNull("DependentInitializer implementation not found by ServiceLoader", dependentInitializer);

      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      dependentInitializer.registerSchema(ctx);
      dependentInitializer.registerMarshallers(ctx);

      assertTrue(ctx.canMarshall(ReusableInitializer.A.class));
      assertTrue(ctx.canMarshall(ReusableInitializer.B.class));
      assertTrue(ctx.canMarshall(DependentInitializer.C.class));
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

   @AutoProtoSchemaBuilder(includeClasses = MessageWithAllFieldTypes.class)
   interface AllFieldTypesInitializer extends SerializationContextInitializer {
   }

   @Test
   public void testAllFieldTypes() throws Exception {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      AllFieldTypesInitializer serCtxInitializer = new AllFieldTypesInitializerImpl();
      serCtxInitializer.registerSchema(ctx);
      serCtxInitializer.registerMarshallers(ctx);

      assertTrue(ctx.canMarshall(MessageWithAllFieldTypes.class));
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, new MessageWithAllFieldTypes());
      Object o = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertTrue(o instanceof MessageWithAllFieldTypes);
   }

   static class MessageWithRepeatedFields {

      @ProtoField(number = 1001)
      byte[] testField1001;

      @ProtoField(number = 1002)
      Byte[] testField1002;

      @ProtoField(number = 1003)
      List<Byte> testField1003;

      @ProtoField(number = 1)
      int[] testField1;

      @ProtoField(number = 2)
      Integer[] testField2;

      static class MyArrayList extends ArrayList {
      }

      @ProtoField(number = 3, collectionImplementation = MyArrayList.class)
      List<Integer> testField3;

      @ProtoField(number = 4)
      Inner[] testField4;

      @ProtoField(number = 5)
      List<Inner> testField5;

      int[] testField6;

      Integer[] testField7;

      List<Integer> testField8;

      Inner[] testField9;

      List<Inner> testField10;

      @ProtoField(number = 6)
      public int[] getTestField6() {
         return testField6;
      }

      public void setTestField6(int[] testField6) {
         this.testField6 = testField6;
      }

      @ProtoField(number = 7)
      public Integer[] getTestField7() {
         return testField7;
      }

      public void setTestField7(Integer[] testField7) {
         this.testField7 = testField7;
      }

      @ProtoField(number = 8)
      public List<Integer> getTestField8() {
         return testField8;
      }

      public void setTestField8(List<Integer> testField8) {
         this.testField8 = testField8;
      }

      @ProtoField(number = 9)
      public Inner[] getTestField9() {
         return testField9;
      }

      public void setTestField9(Inner[] testField9) {
         this.testField9 = testField9;
      }

      @ProtoField(number = 10)
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

   @AutoProtoSchemaBuilder(includeClasses = {MessageWithRepeatedFields.class, MessageWithRepeatedFields.Inner.class})
   interface NonNullRepeatedFieldsInitializer extends SerializationContextInitializer {
   }

   @Test
   public void testNonNullRepeatedFields() throws Exception {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      NonNullRepeatedFieldsInitializer serCtxInitializer = new NonNullRepeatedFieldsInitializerImpl();
      serCtxInitializer.registerSchema(ctx);
      serCtxInitializer.registerMarshallers(ctx);

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
      public void setR(byte r) {
         this.r = r;
      }

      byte g;

      byte[] b;

      @ProtoField(number = 12, defaultValue = "12")
      List<Integer> i;

      @ProtoField(number = 13, defaultValue = "13")
      int[] j;

      public void setG(byte g) {
         this.g = g;
      }

      public void setB(byte[] b) {
         this.b = b;
      }

      public void setI(List<Integer> i) {
         this.i = i;
      }

      public void setJ(int[] j) {
         this.j = j;
      }

      private ImmutableColor(byte r, byte g, byte[] b, List<Integer> i, int[] j) {
         this.r = r;
         this.g = g;
         this.b = b;
         this.i = i;
         this.j = j;
      }

      @ProtoFactory
      static ImmutableColor make(byte r, byte g, byte[] b, List<Integer> i, int[] j) {
         return new ImmutableColor(r, g, b, i, j);
      }

      @ProtoField(number = 2, defaultValue = "2")
      byte getG() {
         return g;
      }

      @ProtoField(number = 3, defaultValue = "3")
      byte[] getB() {
         return b;
      }
   }

   @AutoProtoSchemaBuilder(includeClasses = {RGBColor.class, ImmutableColor.class})
   interface ImmutableMessageTestInitializer extends SerializationContextInitializer {
   }

   @Test
   public void testFactoryMethod() throws Exception {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      ImmutableMessageTestInitializer serCtxInitializer = new ImmutableMessageTestInitializerImpl();
      serCtxInitializer.registerSchema(ctx);
      serCtxInitializer.registerMarshallers(ctx);

      assertTrue(serCtxInitializer.getProtoFile().contains("message RGBColor"));
      assertTrue(serCtxInitializer.getProtoFile().contains("message ImmutableColor"));

      RGBColor color = new RGBColor(55, 66, 77);
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, color);
      RGBColor o = ProtobufUtil.fromWrappedByteArray(ctx, bytes);

      assertNotNull(o);
      assertEquals(55, o.r);
      assertEquals(66, o.g);
      assertEquals(77, o.b);
   }

   /**
    * Demonstrates that a class with no fields is legal (but a warning is logged).
    */
   @ProtoName("NoFields")
   static class NoProtoFields {
   }

   @Test
   public void testNoAnnotatedFields() throws Exception {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      TestInitializer serCtxInitializer = new TestInitializer();
      serCtxInitializer.registerSchema(ctx);
      serCtxInitializer.registerMarshallers(ctx);

      assertTrue(serCtxInitializer.getProtoFile().contains("message NoFields {\n}\n"));

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
   public void testNonStandardPropertyAccessors() throws Exception {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      TestInitializer serCtxInitializer = new TestInitializer();
      serCtxInitializer.registerSchema(ctx);
      serCtxInitializer.registerMarshallers(ctx);

      assertTrue(serCtxInitializer.getProtoFile().contains("message NonStandardPropertyAccessors"));

      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, new NonStandardPropertyAccessors());
      Object o = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertTrue(o instanceof NonStandardPropertyAccessors);
   }

   /**
    * Demonstrates an entity that has a field of type Map<CustomKey, String>.
    */
   static class CustomMap {

      static class CustomKey {
         @ProtoField(number = 1)
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

         @ProtoField(number = 1)
         CustomKey key;

         @ProtoField(number = 2)
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

      @ProtoField(number = 1)
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
      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      TestInitializer serCtxInitializer = new TestInitializer();
      serCtxInitializer.registerSchema(ctx);
      serCtxInitializer.registerMarshallers(ctx);

      assertTrue(serCtxInitializer.getProtoFile().contains("message CustomMap"));

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

      @ProtoField(number = 1)
      public Optional<String> getField1() {
         return Optional.ofNullable(field1);
      }

      public void setField1(String field1) {
         this.field1 = field1;
      }

      @ProtoField(number = 2)
      public Optional<OptionalInner> getField2() {
         return Optional.ofNullable(field2);
      }

      public void setField2(OptionalInner field2) {
         this.field2 = field2;
      }

      @ProtoField(number = 3)
      public Optional<OptionalInner[]> getField3() {
         return Optional.ofNullable(field3);
      }

      public void setField3(OptionalInner[] field3) {
         this.field3 = field3;
      }

      @ProtoField(number = 4)
      public Optional<List<OptionalInner>> getField4() {
         return Optional.ofNullable(field4);
      }

      public void setField4(List<OptionalInner> field4) {
         this.field4 = field4;
      }

      static class OptionalInner {

         @ProtoField(number = 1)
         String theString;
      }
   }

   @Test
   public void testOptional() throws Exception {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      TestInitializer serCtxInitializer = new TestInitializer();
      serCtxInitializer.registerSchema(ctx);
      serCtxInitializer.registerMarshallers(ctx);

      assertTrue(serCtxInitializer.getProtoFile().contains("message Optionals"));

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

   //todo warnings logged to log4j during generation do not end up in compiler's message log

   //todo provide a sensible value() alias for all @ProtoXyz annotations

   //todo test enum with members and without

   //todo check if the classes to be generated do not already exist in sourcepath, handwritten

   //todo translate all ProtoSchemaBuilderTest tests into AutoProtoSchemaBuilderTest
   //todo think about inherited methods/fields/annotations !
   //todo test protostream with intersection types
   //todo test protostream with getters, setter and constructors declaring thrown exceptions
   //todo test protostream with getters/setter with compatible but not identical types than the expected ones, ie boxing, derived classes
   //todo test protostream with derived class and superclass field/method hiding
   //todo record the precise location/element where an error is reported
   //todo detect misplaced annotations and complain
   //todo marshallers should throw a more specific exception (instead of IOException) for data encoding errors, required fields, and other validation errors that are not really IO failures
}
