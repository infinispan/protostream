package org.infinispan.protostream.integrationtests;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static javax.tools.StandardLocation.CLASS_OUTPUT;
import static javax.tools.StandardLocation.SOURCE_OUTPUT;
import static org.junit.Assert.assertTrue;

import org.infinispan.protostream.annotations.impl.processor.AutoProtoSchemaBuilderAnnotationProcessor;
import org.junit.Test;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
public class JavacCompilationTest {

   private static final String src1 = "package test;\n" +
         "import org.infinispan.protostream.annotations.ProtoField;\n" +
         "public class TestMessage {\n" +
         "   @ProtoField(number = 1, required = true)\n" +
         "   boolean flag;\n" +
         "}\n";

   private static final String src2 = "package test;\n" +
         "import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;\n" +
         "import org.infinispan.protostream.SerializationContextInitializer;\n" +
         "@AutoProtoSchemaBuilder(schemaFilePath = \"second_initializer\", className = \"TestInitializer\",\n" +
         "         basePackages = {\"org.infinispan.protostream.integrationtests\", \"test\"}, service = true)\n" +
         "public abstract class SecondInitializer implements SerializationContextInitializer {\n" +
         "}\n";

   private static final String src3 = "package test_depends;\n" +
         "import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;\n" +
         "import org.infinispan.protostream.annotations.ProtoField;\n" +
         "import org.infinispan.protostream.SerializationContextInitializer;\n" +
         "import test.TestMessage;\n" +
         "@AutoProtoSchemaBuilder(schemaFilePath = \"/\", dependsOn = test.SecondInitializer.class,\n" +
         "         includeClasses = DependentInitializer.A.class, autoImportClasses = false, service = true)\n" +
         "interface DependentInitializer extends SerializationContextInitializer {\n" +
         "   class A {\n" +
         "      @ProtoField(number = 1, required = true)\n" +
         "      public TestMessage testMessage;\n" +
         "   }\n\n" +
         "   default String getProtoFileName() {\n" +
         "      return null;\n" +
         "   }" +
         "}\n";

   private static final String src4 = "package test_depends;\n" +
         "import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;\n" +
         "import org.infinispan.protostream.SerializationContextInitializer;\n" +
         "@AutoProtoSchemaBuilder(dependsOn = InitializerB.class)\n" +
         "interface InitializerA extends SerializationContextInitializer { }\n" +
         "@AutoProtoSchemaBuilder(dependsOn = InitializerA.class)\n" +
         "public interface InitializerB extends SerializationContextInitializer { }\n";

   private static final String src5 = "package test;\n" +
         "@org.infinispan.protostream.annotations.AutoProtoSchemaBuilder(\"test!\")\n" +
         "public interface BrokenInitializer extends org.infinispan.protostream.SerializationContextInitializer {\n" +
         "}\n";

   private static final String src6 = "package test;\n" +
         "@org.infinispan.protostream.annotations.AutoProtoSchemaBuilder\n" +
         "public interface SimplestInitializer extends org.infinispan.protostream.SerializationContextInitializer {\n" +
         "}\n\n" +
         "abstract class AbstractInnerMessage {\n" +
         "   @org.infinispan.protostream.annotations.ProtoField(number = 1, required = true)\n" +
         "   boolean field1;\n" +
         "}\n\n" +
         "class InnerMessage extends AbstractInnerMessage { }\n\n" +
         "class OuterMessage {\n" +
         "   @org.infinispan.protostream.annotations.ProtoField(number = 1)\n" +
         "   InnerMessage inner;\n" +
         "}\n";

   private static final String src7 = "package test;\n" +
         "@org.infinispan.protostream.annotations.AutoProtoSchemaBuilder\n" +
         "public interface TestEnumInitializer extends org.infinispan.protostream.SerializationContextInitializer {\n" +
         "}\n\n" +
         "class OuterClass {\n" +
         "\n" +
         "   enum InnerEnum {\n" +
         "      @org.infinispan.protostream.annotations.ProtoEnumValue(number = 1) OPTION_A,\n" +
         "      @org.infinispan.protostream.annotations.ProtoEnumValue(number = 2) OPTION_B\n" +
         "   }\n" +
         "}\n";

   @Test
   public void testAnnotationProcessing() {
      Compilation compilation =
            javac().withProcessors(new AutoProtoSchemaBuilderAnnotationProcessor())
                  .compile(JavaFileObjects.forSourceString("TestMessage", src1),
                        JavaFileObjects.forSourceString("SecondInitializer", src2),
                        JavaFileObjects.forSourceString("DependentInitializer", src3));

      assertThat(compilation).succeeded();
      assertTrue(compilation.generatedFile(SOURCE_OUTPUT, "test/TestInitializer.java").isPresent());
      assertTrue(compilation.generatedFile(SOURCE_OUTPUT, "test_depends/DependentInitializerImpl.java").isPresent());
      assertTrue(compilation.generatedFile(CLASS_OUTPUT, "second_initializer/SecondInitializer.proto").isPresent());
      assertTrue(compilation.generatedFile(CLASS_OUTPUT, "DependentInitializer.proto").isPresent());
      assertTrue(compilation.generatedFile(CLASS_OUTPUT, "META-INF/services/org.infinispan.protostream.SerializationContextInitializer").isPresent());
   }

   @Test
   public void testDependsOnCycles() {
      Compilation compilation =
            javac().withProcessors(new AutoProtoSchemaBuilderAnnotationProcessor())
                  .compile(JavaFileObjects.forSourceString("InitializerB", src4));

      assertThat(compilation).failed();
      assertThat(compilation).hadErrorContaining("Illegal recursive dependency on test_depends.InitializerB");
   }

   @Test
   public void testBadBasePackage() {
      Compilation compilation =
            javac().withProcessors(new AutoProtoSchemaBuilderAnnotationProcessor())
                  .compile(JavaFileObjects.forSourceString("BrokenInitializer", src5));

      assertThat(compilation).failed();
      assertThat(compilation).hadErrorContaining("@AutoProtoSchemaBuilder.value contains an invalid package name : \"test!\"");
   }

   @Test
   public void testDiscoveryWithNoAutoImport() {
      Compilation compilation =
            javac().withProcessors(new AutoProtoSchemaBuilderAnnotationProcessor())
                  .compile(JavaFileObjects.forSourceString("SimplestInitializer", src6));

      assertThat(compilation).succeeded();
      assertTrue(compilation.generatedFile(SOURCE_OUTPUT, "test/SimplestInitializerImpl.java").isPresent());
   }

   @Test
   public void testNestedEnum() {
      Compilation compilation =
            javac().withProcessors(new AutoProtoSchemaBuilderAnnotationProcessor())
                  .compile(JavaFileObjects.forSourceString("TestEnumInitializer", src7));

      assertThat(compilation).succeeded();
      assertTrue(compilation.generatedFile(SOURCE_OUTPUT, "test/TestEnumInitializerImpl.java").isPresent());
   }
}
