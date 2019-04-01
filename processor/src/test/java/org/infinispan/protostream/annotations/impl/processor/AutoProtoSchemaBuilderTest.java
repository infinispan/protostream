package org.infinispan.protostream.annotations.impl.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.impl.testdomain.Simple;
import org.infinispan.protostream.annotations.impl.testdomain.TestEnum;
import org.infinispan.protostream.test.AbstractProtoStreamTest;
import org.junit.Test;

public class AutoProtoSchemaBuilderTest extends AbstractProtoStreamTest {

   @ProtoDoc("This is the only info we have")
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

   interface ByteBuffer {

      @ProtoField(number = 1, name = "theBytes")
      byte[] getBytes();
   }

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

      @ProtoField(number = 4, name = "__someInts", collectionImplementation = ArrayList.class)
      //todo [anistor] provide some sensible defaults for collectionImplementation if not specified
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

   @AutoProtoSchemaBuilder(filePath = "org/infinispan.protostream/generated_schemas", fileName = "TestFile.proto",
         packageName = "firstTestPackage",
         service = true,
         classes = {
               Note.class,
               Simple.class,
               Simple.class, // duplicates are handled nicely
               ByteBufferImpl.class,
//               EmbeddedMetadata.class,
               EmbeddedMetadata.EmbeddedLifespanExpirableMetadata.class,
               TestEnum.class,
//               String.class,
               X.class
         }
   )
   interface TestSerializationContextInitializer extends SerializationContextInitializer {
   }

   @Test
   public void testGeneratedInitializer() throws Exception {
      SerializationContext ctx = createContext();

      TestSerializationContextInitializer serCtxInitializer = new TestSerializationContextInitializerImpl();
      serCtxInitializer.registerSchema(ctx);
      serCtxInitializer.registerMarshallers(ctx);

      assertTrue(ctx.canMarshall(Note.class));
      ProtobufUtil.toWrappedByteArray(ctx, new Note());

      assertTrue(ctx.canMarshall(Simple.class));
      ProtobufUtil.toWrappedByteArray(ctx, new Simple());
   }

   @AutoProtoSchemaBuilder(filePath = "second_initializer", fileName = "TestInitializer.proto", className = "TestInitializer", service = true)
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
      assertEquals("org.infinispan.protostream.annotations.impl.processor.TestInitializer", initializer.getClass().getName());
   }

   @Test
   public void testLocalAnnotatedClassesAreSkipped() {

      // Standard Java annotation processors do not process the bodies of methods, so LocalInitializer is never seen by our AP and no code is generated for it, and that is OK.
      // If we ever decide to process method bodies we should probably study the approach used by "The Checker Framework" (https://checkerframework.org).
      @AutoProtoSchemaBuilder(fileName = "LocalInitializer.proto", className = "NeverEverGenerated", service = true)
      abstract class LocalInitializer implements SerializationContextInitializer {
      }

      for (SerializationContextInitializer sci : ServiceLoader.load(SerializationContextInitializer.class)) {
         if (sci.getClass().getSimpleName().equals("NeverEverGenerated")) {
            fail("Local classes should not be processed by AutoProtoSchemaBuilderAnnotationProcessor.");
         }
      }
   }

   // this is not the normal use case but some users might need this too and we support it
   @AutoProtoSchemaBuilder(fileName = "NonAbstractInitializer.proto", className = "NonAbstractInitializerImpl", service = true)
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
      public void registerSchema(SerializationContext serCtx) throws IOException {
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
