package org.infinispan.protostream.impl.parser;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.infinispan.protostream.test.AbstractProtoStreamTest.PROTO3_SYNTAX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.core.StringStartsWith;
import org.infinispan.protostream.AnnotationParserException;
import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.AnnotationElement;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.descriptors.GenericDescriptor;
import org.infinispan.protostream.descriptors.JavaType;
import org.infinispan.protostream.descriptors.Label;
import org.infinispan.protostream.descriptors.ResolutionContext;
import org.infinispan.protostream.descriptors.Type;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DescriptorsTest {

   private final Configuration config = Configuration.builder().build();

   @org.junit.Rule
   public ExpectedException exception = ExpectedException.none();

   @Test
   public void testGroupsAreNotSupported() {
      // groups are a deprecated feature and are not supported
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Syntax error in file1.proto at 3:32: unexpected label: {");

      String file1 = """
            syntax = "proto3";
            message TestMessage {
              repeated group TestGroup = 1 {
                string url = 2;
                string title = 3;
              }
            }""";

      parseAndResolve(FileDescriptorSource.fromString("file1.proto", file1));
   }

   @Test
   public void testInputFromDiskFile() throws Exception {
      String f1 = "org/infinispan/protostream/lib/base.proto";
      String f2 = "org/infinispan/protostream/lib/base2.proto";
      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile(f1, asFile(f1));
      fileDescriptorSource.addProtoFile(f2, asFile(f2));
      Map<String, FileDescriptor> parseResult = parseAndResolve(fileDescriptorSource);
      assertThat(parseResult).isNotEmpty();
   }

   @Test
   public void testInputFromString() {
      String file1 = """
            syntax = "proto3";
            package test1;
            enum E1 {
               V1 = 1;
               V2 = 2;
            }
            message M {
               int32 f = 1;
               enum E2 {
                  V1 = 1;
                  V2 = 2;
               }
            }""";
      FileDescriptorSource source = new FileDescriptorSource();
      source.addProtoFile("test.proto", file1);

      Map<String, FileDescriptor> files = parseAndResolve(source);
      FileDescriptor fileDescriptor = files.get("test.proto");
      assertThat(fileDescriptor.getMessageTypes()).hasSize(1);
      assertThat(fileDescriptor.getMessageTypes().get(0).getEnumTypes()).hasSize(1);
      assertThat(fileDescriptor.getEnumTypes()).hasSize(1);
   }

   @Test
   public void testInvalidImport() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Syntax error in file1.proto at 3:20: unexpected label: invalid.proto");

      String file1 = """
            syntax = "proto3";
            package test;
            import invalid.proto;
            message M {
               string a = 1;
            }""";

      parseAndResolve(FileDescriptorSource.fromString("file1.proto", file1));
   }

   @Test
   public void testCyclicImport() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Cyclic import detected at test1.proto, import test2.proto");

      String file1 = """
            syntax = "proto3";
            import "test2.proto";
            message M {
               string a = 1;
            }""";
      String file2 = """
            import "test1.proto";
             message M2 {
               string a = 1;
            }""";

      FileDescriptorSource source = new FileDescriptorSource();
      source.addProtoFile("test1.proto", file1);
      source.addProtoFile("test2.proto", file2);

      parseAndResolve(source);
   }

   @Test
   public void testIndirectlyImportSameFile() {
      String file1 = """
            syntax = "proto3";
            import public "test2.proto";
            import public "test3.proto";message M1 { M4 a = 1; }
            """;
      String file2 = "import public \"test4.proto\";";
      String file3 = "import public \"test4.proto\";";
      String file4 = "message M4 { string a = 1; }";

      FileDescriptorSource source = new FileDescriptorSource();
      source.addProtoFile("test1.proto", file1);
      source.addProtoFile("test2.proto", file2);
      source.addProtoFile("test3.proto", file3);
      source.addProtoFile("test4.proto", file4);

      Map<String, FileDescriptor> files = parseAndResolve(source);

      FileDescriptor descriptor1 = files.get("test1.proto");
      assertThat(descriptor1.getMessageTypes()).hasSize(1);
   }

   @Test
   public void testDuplicateImport() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Duplicate import : file1.proto");

      String file1 = """
            syntax = "proto3";
            package test1;
            """;

      String file2 = """
            syntax = "proto3";
            import "file1.proto";
            import "file1.proto";
            """;

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("file1.proto", file1);
      fileDescriptorSource.addProtoFile("file2.proto", file2);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testEnumConstantNameClashesWithEnumTypeName() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Enum constant 'E' clashes with enum type name: test1.E");

      String file1 = """
            syntax = "proto3";
            package test1;
            enum E {
               A = 1;
               E = 2;
            }""";
      FileDescriptorSource source = new FileDescriptorSource();
      source.addProtoFile("test.proto", file1);

      parseAndResolve(source);
   }

   @Test
   public void testDuplicateEnumConstantName() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Enum constant 'A' is already defined in test1.E");

      String file1 = """
            syntax = "proto3";
            package test1;
            enum E {
               A = 1;
               A = 2;
            }""";
      FileDescriptorSource source = new FileDescriptorSource();
      source.addProtoFile("test.proto", file1);

      parseAndResolve(source);
   }

   @Test
   public void testEnumConstantNameClashesWithContainingEnumTypeName() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Enum constant 'E' clashes with enum type name: test1.E");

      String file1 = """
            syntax = "proto3";
            package test1;
            enum E {
               A = 1;
               E = 2;
            }""";
      FileDescriptorSource source = new FileDescriptorSource();
      source.addProtoFile("test.proto", file1);

      parseAndResolve(source);
   }

   @Test
   public void testDuplicateEnumConstantValue() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("IPROTO000013: Error while parsing 'test.proto': Duplicate tag 1 in test1.E");

      String file1 = """
            syntax = "proto3";
            package test1;
            enum E {
               A = 1;
               B = 1;
            }""";
      FileDescriptorSource source = new FileDescriptorSource();
      source.addProtoFile("test.proto", file1);

      parseAndResolve(source);
   }

   @Test
   public void testAllowAliasOfEnumConstantValue() {
      String file1 = """
            syntax = "proto3";
            package test1;
            enum E {
               option allow_alias = true;
               A = 1;
               B = 1;
            }""";
      FileDescriptorSource source = new FileDescriptorSource();
      source.addProtoFile("test.proto", file1);

      Map<String, FileDescriptor> descriptors = parseAndResolve(source);
      FileDescriptor descriptor = descriptors.get("test.proto");
      assertThat(descriptor.getEnumTypes()).hasSize(1);
      EnumDescriptor enumDescriptor = descriptor.getEnumTypes().get(0);
      assertThat(enumDescriptor.getName()).isEqualTo("E");
      assertThat(enumDescriptor.getValues().size()).isEqualTo(2);
      assertThat(enumDescriptor.getValues().get(0).getName()).isEqualTo("A");
      assertThat(enumDescriptor.getValues().get(0).getNumber()).isEqualTo(1);
      assertThat(enumDescriptor.getValues().get(1).getName()).isEqualTo("B");
      assertThat(enumDescriptor.getValues().get(1).getNumber()).isEqualTo(1);
   }

   @Test
   public void testTransform() throws Exception {
      FileDescriptorSource fileDescriptorSource = FileDescriptorSource.fromResources(
            "org/infinispan/protostream/test/message.proto",
            "org/infinispan/protostream/lib/base.proto",
            "org/infinispan/protostream/lib/base2.proto");

      Map<String, FileDescriptor> files = parseAndResolve(fileDescriptorSource);

      FileDescriptor descriptor = files.get("org/infinispan/protostream/test/message.proto");

      assertThat(descriptor.getMessageTypes()).hasSize(3);
      assertThat(descriptor.getEnumTypes()).hasSize(1);
      assertThat(descriptor.getOptions()).hasSize(2);

      assertSearchRequest(descriptor.getMessageTypes().get(0));
      assertTopLevelEnum(descriptor.getEnumTypes().iterator().next());
      assertSearchResponse(descriptor.getMessageTypes().get(1));
      assertResult(descriptor.getMessageTypes().get(2));
   }

   @Test
   public void testDuplicateTypeInFile1() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Duplicate definition of 'test.M1' in test_proto_path/file1.proto");

      String file1 = """
            syntax = "proto3";
            package test;
            message M1 {
              string a = 1;
            }
            message M1 {
              string a = 1;
            }
            """;

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("test_proto_path/file1.proto", file1);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testDuplicateTypeInFile2() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Duplicate definition of 'test.M1' in test_proto_path/file1.proto");

      String file1 = """
            syntax = "proto3";
            package test;
            message M1 {
              string a = 1;
            }
            enum M1 {
              VAL = 1;
            }
            """;

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("test_proto_path/file1.proto", file1);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testDuplicateTypeInFile3() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Duplicate definition of 'test.E1' in test_proto_path/file1.proto");

      String file1 = """
            syntax = "proto3";
            package test;
            enum E1 {
              VAL = 1;
            }
            enum E1 {
              VAL = 1;
            }
            """;

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("test_proto_path/file1.proto", file1);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testNestedMessageWithSameName() {
      String file1 = """
            syntax = "proto3";
            package test;
            message M1 {
              string a = 1;
              message M1 { string a = 1; }
            }
            """;

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("file1.proto", file1);

      Map<String, FileDescriptor> descriptors = parseAndResolve(fileDescriptorSource);
      assertEquals(1, descriptors.size());
      assertTrue(descriptors.containsKey("file1.proto"));
      FileDescriptor fd = descriptors.get("file1.proto");
      assertEquals(1, fd.getMessageTypes().size());
   }

   @Test
   public void testDuplicateTypeInMessage1() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Duplicate definition of 'test.M1.M2' in test_proto_path/file1.proto");

      String file1 = """
            syntax = "proto3";
            package test;
            message M1 {
              string a = 1;
              message M2 { string a = 1; }
              message M2 { string b = 1; }
            }
            """;

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("test_proto_path/file1.proto", file1);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testDuplicateTypeInMessage2() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Duplicate definition of 'test.M1.E1' in test_proto_path/file1.proto");

      String file1 = """
            syntax = "proto3";
            package test;
            message M1 {
              string a = 1;
              enum E1 { VAL1 = 1; }
              enum E1 { VAL2 = 2; }
            }
            """;

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("test_proto_path/file1.proto", file1);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testDuplicateTypeInMessage3() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Duplicate definition of 'test.M1.E1' in test_proto_path/file1.proto");

      String file1 = """
            syntax = "proto3";
            package test;
            message M1 {
              string a = 1;
              message E1 { string a = 1; }
              enum E1 { VAL1 = 1; }
            }
            """;

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("test_proto_path/file1.proto", file1);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testDuplicateTypeInPackage1() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Duplicate definition of test.M1 in test_proto_path/file1.proto and test_proto_path/file2.proto");

      String file1 = """
            syntax = "proto3";
            package test;
            message M1 {
              string a = 1;
            }""";

      String file2 = """
            syntax = "proto3";
            package test;
            message M1 {
              string b = 2;
            }""";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("test_proto_path/file1.proto", file1);
      fileDescriptorSource.addProtoFile("test_proto_path/file2.proto", file2);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testDuplicateTypeInPackage2() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Duplicate definition of test.M1 in test_proto_path/file1.proto and test_proto_path/file2.proto");

      String file1 = """
            syntax = "proto3";
            package test;
            message M1 {
              string a = 1;
            }""";

      String file2 = """
            syntax = "proto3";
            package test;
            enum M1 {
              VAL1 = 1;
            }""";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("test_proto_path/file1.proto", file1);
      fileDescriptorSource.addProtoFile("test_proto_path/file2.proto", file2);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testDuplicateTypeIdInSameFile() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Duplicate type id 100010 for type test1.M2. Already used by test1.M1");

      String file1 = PROTO3_SYNTAX + "package test1;\n" +
            "/**@TypeId(100010)*/\n" +
            "message M1 {\n" +
            "   string a = 1;\n" +
            "}" +
            "/**@TypeId(100010)*/\n" +
            "message M2 {\n" +
            "   string b = 1;\n" +
            "}";

      FileDescriptorSource source = new FileDescriptorSource();
      source.addProtoFile("file1.proto", file1);

      parseAndResolve(source);
   }

   @Test
   public void testDuplicateTypeIdInImportedFile() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Duplicate type id 100010 for type test2.M2. Already used by test1.M1");

      String file1 = PROTO3_SYNTAX + "package test1;\n" +
            "/**@TypeId(100010)*/\n" +
            "message M1 {\n" +
            "   string a = 1;\n" +
            "}";

      String file2 = PROTO3_SYNTAX + "package test2;\n" +
            "import \"file1.proto\";\n" +
            "/**@TypeId(100010)*/\n" +
            "message M2 {\n" +
            "   string b = 1;\n" +
            "}";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("file1.proto", file1);
      fileDescriptorSource.addProtoFile("file2.proto", file2);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testNotImportedInSamePackage() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Failed to resolve type of field \"test.M2.b\" in \"file2.proto\". Type not found : M1");

      String file1 = PROTO3_SYNTAX + "package test;\n" +
            "message M1 {\n" +
            "  string a = 1;\n" +
            "}";

      String file2 = PROTO3_SYNTAX + "package test;\n" +
            "message M2 {\n" +
            "  M1 b = 2;\n" +
            "}";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("file1.proto", file1);
      fileDescriptorSource.addProtoFile("file2.proto", file2);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testNotImportedInAnotherPackage() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Failed to resolve type of field \"test2.M2.b\" in \"file2.proto\". Type not found : test1.M1");

      String file1 = PROTO3_SYNTAX + "package test1;\n" +
            "message M1 {\n" +
            "  string a = 1;\n" +
            "}";

      String file2 = PROTO3_SYNTAX + "package test2;\n" +
            "message M2 {\n" +
            "  test1.M1 b = 2;\n" +
            "}";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("file1.proto", file1);
      fileDescriptorSource.addProtoFile("file2.proto", file2);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testEmptyPackageName() {
      // package name cannot be empty
      exception.expect(DescriptorParserException.class);
      exception.expectMessage(StringStartsWith.startsWith("IPROTO000013"));

      String file1 = """
            syntax = "proto3";
            package ;
            message M1 {
              string a = 1;
            }""";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("file1.proto", file1);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testDefinitionNameWithDots1() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Syntax error in file1.proto at 3:22: unexpected label: somePackage.M1");

      String file1 = """
            syntax = "proto3";
            package test;
            message somePackage.M1 {
              string a = 1;
            }""";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("file1.proto", file1);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testDefinitionNameWithDots2() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Syntax error in file1.proto at 3:19: unexpected label: somePackage.E1");

      String file1 = """
            syntax = "proto3";
            package testPackage;
            enum somePackage.E1 {
              VAL = 1;
            }""";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("file1.proto", file1);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testPublicImport() {
      String file1 = """
            syntax = "proto3";
            message M1 {
              string a = 1;
            }""";

      String file2 = "syntax = \"proto3\";\nimport public \"file1.proto\";";

      String file3 = """
            syntax = "proto3";
            import "file2.proto";
            message M3 {
              M1 a = 1;
            }""";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("file1.proto", file1);
      fileDescriptorSource.addProtoFile("file2.proto", file2);
      fileDescriptorSource.addProtoFile("file3.proto", file3);

      Map<String, FileDescriptor> descriptors = parseAndResolve(fileDescriptorSource);
      assertEquals(3, descriptors.size());
      assertTrue(descriptors.containsKey("file1.proto"));
      assertTrue(descriptors.containsKey("file2.proto"));
      assertTrue(descriptors.containsKey("file3.proto"));
      assertTrue(descriptors.get("file1.proto").getTypes().containsKey("M1"));
      assertTrue(descriptors.get("file2.proto").getTypes().isEmpty());
      assertTrue(descriptors.get("file3.proto").getTypes().containsKey("M3"));
   }

   @Test
   public void testPrivateImport() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Failed to resolve type of field \"M3.a\" in \"file3.proto\". Type not found : M1");

      String file1 = """
            syntax = "proto3";
            message M1 {
              string a = 1;
            }""";

      String file2 = "syntax = \"proto3\";\nimport \"file1.proto\";";

      String file3 = """
            syntax = "proto3";
            import "file2.proto";
            message M3 {
              M1 a = 1;
            }""";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("file1.proto", file1);
      fileDescriptorSource.addProtoFile("file2.proto", file2);
      fileDescriptorSource.addProtoFile("file3.proto", file3);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testImportAndPackage() {
      String file1 = """
            syntax = "proto3";
            package p;
            message A {
               int32 f1 = 1;
            }""";

      String file2 = """
            syntax = "proto3";
            package org.infinispan;
            import "file1.proto";
            message B {
               p.A ma = 1;
            }""";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("file1.proto", file1);
      fileDescriptorSource.addProtoFile("file2.proto", file2);

      Map<String, FileDescriptor> descriptors = parseAndResolve(fileDescriptorSource);
      assertEquals(2, descriptors.size());
      assertTrue(descriptors.containsKey("file1.proto"));
      assertTrue(descriptors.containsKey("file2.proto"));
      assertTrue(descriptors.get("file1.proto").getTypes().containsKey("p.A"));
      assertTrue(descriptors.get("file2.proto").getTypes().containsKey("org.infinispan.B"));
   }

   @Test
   public void testDocComment() {
      String file1 = """
            syntax = "proto3";
            package test1;
            /** \s
             *    some doc text\s
              *    some more doc text\s
                  **/

            message X {
             /**
              * field doc text \s

              */
              int32 field1 = 1;
            }
            """;

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("file1.proto", file1);
      Map<String, FileDescriptor> descriptors = parseAndResolve(fileDescriptorSource);

      assertEquals(1, descriptors.size());
      assertTrue(descriptors.containsKey("file1.proto"));
      Map<String, GenericDescriptor> types = descriptors.get("file1.proto").getTypes();
      Descriptor typeX = (Descriptor) types.get("test1.X");
      assertNotNull(typeX);
      assertEquals(1, typeX.getFields().size());
      FieldDescriptor field1 = typeX.getFields().get(0);
      assertEquals("some doc text \nsome more doc text", typeX.getDocumentation());
      assertEquals("field doc text", field1.getDocumentation());
   }

   @Test
   public void testDocAnnotations() {
      String file1 = """
            syntax = "proto3";
            package test1;
            /** \s
             *  @Foo(fooValue)\s
              *    some more doc text\s
                  **/

            message X {
             /**
              * @Bar(barValue) \s

              */
              int32 field1 = 1;
            }
            """;

      Configuration config = Configuration.builder().annotationsConfig()
            .annotation("Foo", AnnotationElement.AnnotationTarget.MESSAGE)
            .attribute(AnnotationElement.Annotation.VALUE_DEFAULT_ATTRIBUTE)
            .type(AnnotationElement.AttributeType.IDENTIFIER)
            .metadataCreator((descriptor, annotation) -> annotation.getDefaultAttributeValue().getValue())
            .annotation("Bar", AnnotationElement.AnnotationTarget.FIELD)
            .attribute(AnnotationElement.Annotation.VALUE_DEFAULT_ATTRIBUTE)
            .type(AnnotationElement.AttributeType.IDENTIFIER)
            .metadataCreator((fieldDescriptor, annotation) -> annotation.getDefaultAttributeValue().getValue())
            .build();

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("file1.proto", file1);
      Map<String, FileDescriptor> descriptors = parseAndResolve(fileDescriptorSource, config);

      assertEquals(1, descriptors.size());
      assertTrue(descriptors.containsKey("file1.proto"));
      Map<String, GenericDescriptor> types = descriptors.get("file1.proto").getTypes();
      Descriptor typeX = (Descriptor) types.get("test1.X");
      assertNotNull(typeX);
      assertEquals(1, typeX.getFields().size());
      FieldDescriptor field1 = typeX.getFields().get(0);
      assertEquals("@Foo(fooValue) \nsome more doc text", typeX.getDocumentation());
      Map<String, AnnotationElement.Annotation> typeAnnotations = typeX.getAnnotations();
      assertEquals("fooValue", typeAnnotations.get("Foo").getDefaultAttributeValue().getValue());
      assertEquals("fooValue", typeX.getProcessedAnnotation("Foo"));
      assertEquals("@Bar(barValue)", field1.getDocumentation());
      Map<String, AnnotationElement.Annotation> fieldAnnotations = field1.getAnnotations();
      assertEquals("barValue", fieldAnnotations.get("Bar").getDefaultAttributeValue().getValue());
      assertEquals("barValue", field1.getProcessedAnnotation("Bar"));
   }

   @Test
   public void testAnnotationParser() throws Exception {
      Configuration config = Configuration.builder().annotationsConfig()
            .annotation("Indexed", AnnotationElement.AnnotationTarget.MESSAGE)
            .attribute(AnnotationElement.Annotation.VALUE_DEFAULT_ATTRIBUTE)
            .type(AnnotationElement.AttributeType.BOOLEAN)
            .defaultValue(true)
            .metadataCreator((descriptor, annotation) -> annotation.getDefaultAttributeValue().getValue())
            .build();

      FileDescriptorSource fileDescriptorSource = FileDescriptorSource.fromResources("sample_bank_account/bank.proto");
      Map<String, FileDescriptor> descriptors = parseAndResolve(fileDescriptorSource, config);

      FileDescriptor fileDescriptor = descriptors.get("sample_bank_account/bank.proto");
      List<Descriptor> messageTypes = fileDescriptor.getMessageTypes();

      Descriptor userMessageType = messageTypes.get(0);
      assertEquals("sample_bank_account.User", userMessageType.getFullName());
      assertEquals(Boolean.TRUE, userMessageType.getProcessedAnnotation("Indexed"));

      Descriptor accountMessageType = messageTypes.get(1);
      assertEquals("sample_bank_account.Account", accountMessageType.getFullName());
      assertEquals(Boolean.TRUE, accountMessageType.getProcessedAnnotation("Indexed"));
   }

   @Test
   public void testDuplicateAnnotation() {
      exception.expect(AnnotationParserException.class);
      exception.expectMessage("Error: 1,8: duplicate annotation definition \"Field\"");

      Configuration config = Configuration.builder().annotationsConfig()
            .annotation("Field", AnnotationElement.AnnotationTarget.FIELD)
            .attribute(AnnotationElement.Annotation.VALUE_DEFAULT_ATTRIBUTE)
            .type(AnnotationElement.AttributeType.BOOLEAN)
            .defaultValue(true)
            .build();

      String testProto = """
            syntax = "proto3";
            message M {
              /** @Field @Field */
              int32 field1 = 1;\s
            }""";

      FileDescriptorSource fileDescriptorSource = FileDescriptorSource.fromString("test.proto", testProto);
      Map<String, FileDescriptor> descriptors = parseAndResolve(fileDescriptorSource, config);

      //todo [anistor] this is waaay too lazy
      descriptors.get("test.proto").getMessageTypes().get(0).getFields().get(0).getAnnotations();
   }

   @Test
   public void testUndefinedAnnotation() {
      Configuration config = Configuration.builder().annotationsConfig()
            .annotation("Field", AnnotationElement.AnnotationTarget.FIELD)
            .attribute(AnnotationElement.Annotation.VALUE_DEFAULT_ATTRIBUTE)
            .type(AnnotationElement.AttributeType.BOOLEAN)
            .defaultValue(true)
            .build();

      String testProto = """
            syntax = "proto3";
            message M {
              /** @SomeAnnotation(x=777, y="YES") @Field */
              int32 field1 = 1;\s
            }""";

      FileDescriptorSource fileDescriptorSource = FileDescriptorSource.fromString("test.proto", testProto);
      Map<String, FileDescriptor> descriptors = parseAndResolve(fileDescriptorSource, config);

      //todo [anistor] The processing of annotations is waaay too lazy
      AnnotationElement.Annotation someAnnotation = descriptors.get("test.proto").getMessageTypes().get(0).getFields().get(0).getAnnotations().get("SomeAnnotation");
      assertNull(someAnnotation);

      AnnotationElement.Annotation fieldAnnotation = descriptors.get("test.proto").getMessageTypes().get(0).getFields().get(0).getAnnotations().get("Field");
      assertNotNull(fieldAnnotation);
      assertEquals("Field", fieldAnnotation.getName());
   }

   @Test
   public void testDuplicateUndefinedAnnotation() {
      Configuration config = Configuration.builder().annotationsConfig()
            .annotation("Field", AnnotationElement.AnnotationTarget.FIELD)
            .attribute(AnnotationElement.Annotation.VALUE_DEFAULT_ATTRIBUTE)
            .type(AnnotationElement.AttributeType.BOOLEAN)
            .defaultValue(true)
            .build();

      String testProto = """
            syntax = "proto3";
            message M {
              /** @SomeAnnotation @SomeAnnotation @Field */
              int32 field1 = 1;\s
            }""";

      FileDescriptorSource fileDescriptorSource = FileDescriptorSource.fromString("test.proto", testProto);
      Map<String, FileDescriptor> descriptors = parseAndResolve(fileDescriptorSource, config);

      //todo [anistor] The processing of annotations is waaay too lazy
      AnnotationElement.Annotation someAnnotation = descriptors.get("test.proto").getMessageTypes().get(0).getFields().get(0).getAnnotations().get("SomeAnnotation");

      // 'SomeAnnotation' annotation is not defined, but we accept it silently
      assertNull(someAnnotation);

      AnnotationElement.Annotation fieldAnnotation = descriptors.get("test.proto").getMessageTypes().get(0).getFields().get(0).getAnnotations().get("Field");
      assertNotNull(fieldAnnotation);
      assertEquals("Field", fieldAnnotation.getName());
   }

   @Test
   public void testBrokenUndefinedAnnotation() {
      exception.expect(AnnotationParserException.class);
      exception.expectMessage("Error: 2,23: ')' expected");

      Configuration config = Configuration.builder().annotationsConfig()
            .annotation("Field", AnnotationElement.AnnotationTarget.FIELD)
            .attribute(AnnotationElement.Annotation.VALUE_DEFAULT_ATTRIBUTE)
            .type(AnnotationElement.AttributeType.BOOLEAN)
            .defaultValue(true)
            .build();

      String testProto = """
            syntax = "proto3";
            message M {
              /** Here we have an annotation that fails to parse
              @SomeAnnotation(777 @Field */
              int32 field1 = 1;\s
            }""";

      FileDescriptorSource fileDescriptorSource = FileDescriptorSource.fromString("test.proto", testProto);
      Map<String, FileDescriptor> descriptors = parseAndResolve(fileDescriptorSource, config);

      //todo [anistor] The processing of annotations is waaay too lazy
      descriptors.get("test.proto").getMessageTypes().get(0).getFields().get(0).getAnnotations();
   }

   @Test
   public void testRepeatedAnnotation() {
      Configuration config = Configuration.builder().annotationsConfig()
            .annotation("Field", AnnotationElement.AnnotationTarget.FIELD)
            .repeatable("Fields")
            .attribute(AnnotationElement.Annotation.VALUE_DEFAULT_ATTRIBUTE)
            .type(AnnotationElement.AttributeType.BOOLEAN)
            .defaultValue(true)
            .build();

      String testProto = """
            syntax = "proto3";
            message M {
              /** @Field @Field */
              int32 field1 = 1;
            }""";

      FileDescriptorSource fileDescriptorSource = FileDescriptorSource.fromString("test.proto", testProto);
      Map<String, FileDescriptor> descriptors = parseAndResolve(fileDescriptorSource, config);

      Map<String, AnnotationElement.Annotation> annotations = descriptors.get("test.proto").getMessageTypes().get(0).getFields().get(0).getAnnotations();
      assertFalse(annotations.containsKey("Field"));
      assertTrue(annotations.containsKey("Fields"));
      List<AnnotationElement.Annotation> innerAnnotations = (List<AnnotationElement.Annotation>) annotations.get("Fields").getDefaultAttributeValue().getValue();
      assertEquals(2, innerAnnotations.size());
   }

   @Test
   public void testAnnotationTarget() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Annotation '@Field()' cannot be applied to message types.");

      Configuration config = Configuration.builder().annotationsConfig()
            .annotation("Field", AnnotationElement.AnnotationTarget.FIELD)
            .attribute(AnnotationElement.Annotation.VALUE_DEFAULT_ATTRIBUTE)
            .type(AnnotationElement.AttributeType.BOOLEAN)
            .defaultValue(true)
            .build();

      String testProto = """
            syntax = "proto3";
            /** @Field */
            message M {
              int32 field1 = 1;
            }""";

      FileDescriptorSource fileDescriptorSource = FileDescriptorSource.fromString("test.proto", testProto);
      parseAndResolve(fileDescriptorSource, config);
   }

   @Test
   public void testMultipleAnnotationAttribute() {
      Configuration config = Configuration.builder().annotationsConfig()
            .annotation("Xyz", AnnotationElement.AnnotationTarget.MESSAGE)
            .attribute("elem1")
            .type(AnnotationElement.AttributeType.BOOLEAN)
            .defaultValue(true)
            .multiple(true)
            .build();

      String testProto = """
            syntax = "proto3";
            /** @Xyz(elem1 = {true, false, true}) */
            message M {
              int32 field1 = 1;
            }
            """;

      FileDescriptorSource fileDescriptorSource = FileDescriptorSource.fromString("test.proto", testProto);
      Map<String, FileDescriptor> descriptors = parseAndResolve(fileDescriptorSource, config);

      FileDescriptor fileDescriptor = descriptors.get("test.proto");
      List<Descriptor> messageTypes = fileDescriptor.getMessageTypes();

      Descriptor messageType = messageTypes.get(0);
      assertEquals("M", messageType.getFullName());
      AnnotationElement.Annotation annotation = messageType.getAnnotations().get("Xyz");
      assertNotNull(annotation);
      AnnotationElement.Value attr = annotation.getAttributeValue("elem1");
      assertTrue(attr instanceof AnnotationElement.Array);
      assertTrue(attr.getValue() instanceof List);
      List values = (List) attr.getValue();
      assertEquals(3, values.size());
      assertEquals(true, values.get(0));
      assertEquals(false, values.get(1));
      assertEquals(true, values.get(2));
   }

   @Test
   public void testArrayAnnotationAttributeNormalizing() {
      Configuration config = Configuration.builder().annotationsConfig()
            .annotation("Xyz", AnnotationElement.AnnotationTarget.MESSAGE)
            .attribute("elem1")
            .type(AnnotationElement.AttributeType.BOOLEAN)
            .defaultValue(true)
            .multiple(true)
            .build();

      String testProto = """
            syntax = "proto3";
            /** @Xyz(elem1 = true) */
            message M {
              int32 field1 = 1;\s
            }
            """;

      FileDescriptorSource fileDescriptorSource = FileDescriptorSource.fromString("test.proto", testProto);
      Map<String, FileDescriptor> descriptors = parseAndResolve(fileDescriptorSource, config);

      FileDescriptor fileDescriptor = descriptors.get("test.proto");
      List<Descriptor> messageTypes = fileDescriptor.getMessageTypes();

      Descriptor messageType = messageTypes.get(0);
      assertEquals("M", messageType.getFullName());
      AnnotationElement.Annotation annotation = messageType.getAnnotations().get("Xyz");
      assertNotNull(annotation);
      AnnotationElement.Value attr = annotation.getAttributeValue("elem1");
      assertTrue(attr instanceof AnnotationElement.Array);
      assertTrue(attr.getValue() instanceof List);
      List values = (List) attr.getValue();
      assertEquals(1, values.size());
      assertEquals(true, values.get(0));
   }

   @Test
   public void testDuplicateOptionInFile() {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("test_proto_path/file1.proto: Option \"custom_option\" was already set.");

      String file1 = """
            syntax = "proto3";
            package test;
            option custom_option = true;
            option custom_option = true;
            """;

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("test_proto_path/file1.proto", file1);

      parseAndResolve(fileDescriptorSource);
   }

   private Map<String, FileDescriptor> parseAndResolve(FileDescriptorSource fileDescriptorSource, Configuration config) {
      // parse the input
      ProtostreamProtoParser protoParser = new ProtostreamProtoParser(config);
      Map<String, FileDescriptor> fileDescriptorMap = protoParser.parse(fileDescriptorSource);

      // resolve imports and types
      ResolutionContext resolutionContext = new ResolutionContext(null, fileDescriptorMap,
            new HashMap<>(), new HashMap<>(), new HashMap<>());
      resolutionContext.resolve();

      return fileDescriptorMap;
   }

   private Map<String, FileDescriptor> parseAndResolve(FileDescriptorSource fileDescriptorSource) {
      return parseAndResolve(fileDescriptorSource, config);
   }

   private void assertResult(Descriptor descriptor) {
      assertThat(descriptor.getFields()).hasSize(4);
      assertThat(descriptor.findFieldByName("url").getJavaType()).isEqualTo(JavaType.STRING);
      assertThat(descriptor.findFieldByName("title").getLabel()).isEqualTo(Label.OPTIONAL);
      assertThat(descriptor.findFieldByName("i").getType()).isEqualTo(Type.MESSAGE);
      assertThat(descriptor.findFieldByName("i").getMessageType().getName()).isEqualTo("MoreInner");
      assertThat(descriptor.findFieldByNumber(3).getLabel()).isEqualTo(Label.REPEATED);
   }

   private void assertSearchResponse(Descriptor descriptor) {
      assertThat(descriptor.getName()).isEqualTo("SearchResponse");
      assertThat(descriptor.getFullName()).isEqualTo("org.infinispan.protostream.test.SearchResponse");
      assertThat(descriptor.getFields()).hasSize(3);
      assertThat(descriptor.getEnumTypes()).isEmpty();
      FieldDescriptor resultField = descriptor.getFields().get(0);
      assertThat(resultField.getName()).isEqualTo("result");
      assertThat(resultField.getType()).isEqualTo(Type.MESSAGE);

      FieldDescriptor baseField = descriptor.getFields().get(1);
      assertThat(baseField.getName()).isEqualTo("base");
      assertThat(baseField.getType()).isEqualTo(Type.MESSAGE);
   }

   private void assertSearchRequest(Descriptor descriptor) {
      assertThat(descriptor.getName()).isEqualTo("SearchRequest");
      assertThat(descriptor.getFullName()).isEqualTo("org.infinispan.protostream.test.SearchRequest");
      assertThat(descriptor.getFields()).hasSize(8);
      assertThat(descriptor.getEnumTypes()).hasSize(1);
      assertThat(descriptor.getNestedTypes()).hasSize(1);
      assertThat(descriptor.getOptions()).hasSize(0);

      assertSearchRequestFields(descriptor.getFields());
   }

   private void assertSearchRequestFields(List<FieldDescriptor> fields) {
      FieldDescriptor queryField = fields.get(0);
      assertThat(queryField.getType()).isEqualTo(Type.STRING);
      assertThat(queryField.getJavaType()).isEqualTo(JavaType.STRING);
      assertThat(queryField.getFullName()).isEqualTo("org.infinispan.protostream.test.SearchRequest.query");
      assertThat(queryField.getName()).isEqualTo("query");
      assertThat(queryField.getNumber()).isEqualTo(1);
      assertThat(queryField.getMessageType()).isNull();
      assertThat(queryField.getContainingMessage().getName()).isEqualTo("SearchRequest");

      FieldDescriptor pageNumberField = fields.get(1);
      assertThat(pageNumberField.getLabel()).isEqualTo(Label.OPTIONAL);
      assertThat(pageNumberField.getType()).isEqualTo(Type.INT32);
      assertThat(pageNumberField.getJavaType()).isEqualTo(JavaType.INT);
      assertThat(pageNumberField.getName()).isEqualTo("page_number");
      assertThat(pageNumberField.getNumber()).isEqualTo(2);
      assertThat(pageNumberField.getMessageType()).isNull();

      FieldDescriptor flagField = fields.get(3);
      assertThat(flagField.getLabel()).isEqualTo(Label.REPEATED);
      assertThat(flagField.getType()).isEqualTo(Type.INT32);
      assertThat(flagField.getJavaType()).isEqualTo(JavaType.INT);
      assertThat(flagField.getOptionByName("packed").getValue()).isEqualTo("true");

      FieldDescriptor dntField = fields.get(4);
      assertThat(dntField.getLabel()).isEqualTo(Label.OPTIONAL);
      assertThat(dntField.getType()).isEqualTo(Type.ENUM);
      assertThat(dntField.getEnumType().findValueByName("DONT_CARE").getNumber()).isEqualTo(2);
      assertThat(dntField.getEnumType().findValueByNumber(1).getName()).isEqualTo("TRACK_FOR_SURE");
      assertThat(dntField.getEnumType().getFileDescriptor()).isNotNull();
      assertThat(dntField.getFileDescriptor()).isNotNull();
      assertThat(dntField.getJavaType()).isEqualTo(JavaType.ENUM);

      FieldDescriptor reqEnumField = fields.get(5);
      assertThat(reqEnumField.getType()).isEqualTo(Type.ENUM);
      assertThat(reqEnumField.getJavaType()).isEqualTo(JavaType.ENUM);
      assertThat(reqEnumField.hasDefaultValue()).isFalse();
      assertThat(reqEnumField.getEnumType()).isNotNull();
      assertThat(reqEnumField.getEnumType().findValueByNumber(0).getFileDescriptor()).isNotNull();
      assertThat(reqEnumField.getEnumType().findValueByNumber(0).getContainingEnum()).isEqualTo(reqEnumField.getEnumType());
      assertThat(reqEnumField.getOptionByName("deprecated").getValue()).isEqualTo("true");

      FieldDescriptor typedField = fields.get(7);
      Descriptor messageType = typedField.getMessageType();
      assertThat(messageType.getFullName()).isEqualTo("org.infinispan.protostream.test.SearchRequest.Inner");
      assertThat(messageType.getContainingType().getName()).isEqualTo("SearchRequest");

      assertThat(typedField.getType()).isEqualTo(Type.MESSAGE);
      assertThat(typedField.getFullName()).isEqualTo("org.infinispan.protostream.test.SearchRequest.typed");
   }

   private void assertTopLevelEnum(EnumDescriptor topLevelEnum) {
      assertEquals("aEnum", topLevelEnum.getName());
      assertEquals(2, topLevelEnum.getValues().size());
      assertEquals("VAL0", topLevelEnum.getValues().get(0).getName());
      assertEquals(0, topLevelEnum.getValues().get(0).getNumber());
      assertEquals("VAL1", topLevelEnum.getValues().get(1).getName());
      assertEquals(1, topLevelEnum.getValues().get(1).getNumber());
   }

   private File asFile(String resourcePath) {
      URL resource = getClass().getClassLoader().getResource(resourcePath);
      if (resource != null) {
         return new File(resource.getPath());
      }
      return null;
   }
}
