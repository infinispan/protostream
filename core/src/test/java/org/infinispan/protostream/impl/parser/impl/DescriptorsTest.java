package org.infinispan.protostream.impl.parser.impl;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.infinispan.protostream.AnnotationMetadataCreator;
import org.infinispan.protostream.AnnotationParserException;
import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.AnnotationElement;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.ExtendDescriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.descriptors.GenericDescriptor;
import org.infinispan.protostream.descriptors.JavaType;
import org.infinispan.protostream.descriptors.Label;
import org.infinispan.protostream.descriptors.Type;
import org.infinispan.protostream.impl.parser.SquareProtoParser;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DescriptorsTest {

   private final Configuration config = new Configuration.Builder().build();

   @org.junit.Rule
   public ExpectedException exception = ExpectedException.none();

   @Test
   public void testGroupsAreNotSupported() throws Exception {
      // groups are a deprecated feature and are not supported
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Syntax error in file1.proto at 2:33: expected ';'");

      String file1 = "message TestMessage {\n" +
            "  repeated group TestGroup = 1 {\n" +
            "    required string url = 2;\n" +
            "    optional string title = 3;\n" +
            "  }\n" +
            "}";

      parseAndResolve(FileDescriptorSource.fromString("file1.proto", file1));
   }

   @Test
   public void testInputFromFile() throws Exception {
      String f1 = "org/infinispan/protostream/lib/base.proto";
      String f2 = "org/infinispan/protostream/lib/base2.proto";
      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile(f1, asFile(f1));
      fileDescriptorSource.addProtoFile(f2, asFile(f2));
      Map<String, FileDescriptor> parseResult = parseAndResolve(fileDescriptorSource);
      assertThat(parseResult).isNotEmpty();
   }

   @Test
   public void testInvalidImport() throws Exception {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Import 'invalid.proto' not found");

      String file1 = "package test;\n" +
            "import invalid.proto;\n" +
            "message M {\n" +
            "   required string a = 1;\n" +
            "}";

      parseAndResolve(FileDescriptorSource.fromString("file1.proto", file1));
   }

   @Test
   public void testCyclicImport() throws Exception {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Possible cyclic import detected at test.proto, import test2.proto");

      String file1 = "import test2.proto;\n" +
            "message M {\n" +
            "   required string a = 1;\n" +
            "}";
      String file2 = "import test.proto;\n" +
            " message M2 {\n" +
            "   required string a = 1;\n" +
            "}";

      FileDescriptorSource source = new FileDescriptorSource();
      source.addProtoFile("test.proto", file1);
      source.addProtoFile("test2.proto", file2);

      parseAndResolve(source);
   }

   @Test
   public void testDuplicateImport() throws Exception {
      String file1 = "package test1;\n";

      String file2 = "import \"file1.proto\";\n" +
            "import \"file1.proto\";\n";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("file1.proto", file1);
      fileDescriptorSource.addProtoFile("file2.proto", file2);

      Map<String, FileDescriptor> files = parseAndResolve(fileDescriptorSource);

      FileDescriptor descriptor1 = files.get("file1.proto");
      assertThat(descriptor1.getMessageTypes()).hasSize(0);
      assertThat(descriptor1.getEnumTypes()).hasSize(0);

      FileDescriptor descriptor2 = files.get("file2.proto");
      assertThat(descriptor2.getMessageTypes()).hasSize(0);
      assertThat(descriptor2.getEnumTypes()).hasSize(0);
   }

   @Test
   public void testDuplicateEnumConstantName() throws Exception {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Enum constant 'A' is already defined in test1.E");

      String file1 = "package test1;\n" +
            "enum E {\n" +
            "   A = 1;\n" +
            "   A = 2;\n" +
            "}";
      FileDescriptorSource source = new FileDescriptorSource();
      source.addProtoFile("test.proto", file1);

      parseAndResolve(source);
   }

   @Test
   public void testEnumConstantNameClashesWithEnumTypeName() throws Exception {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Enum constant 'E' clashes with enum type name: test1.E");

      String file1 = "package test1;\n" +
            "enum E {\n" +
            "   A = 1;\n" +
            "   E = 2;\n" +
            "}";
      FileDescriptorSource source = new FileDescriptorSource();
      source.addProtoFile("test.proto", file1);

      parseAndResolve(source);
   }

   @Test
   public void testDuplicateEnumConstantValue() throws Exception {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("java.lang.IllegalStateException: Duplicate tag 1 in test1.E");

      String file1 = "package test1;\n" +
            "enum E {\n" +
            "   A = 1;\n" +
            "   B = 1;\n" +
            "}";
      FileDescriptorSource source = new FileDescriptorSource();
      source.addProtoFile("test.proto", file1);

      parseAndResolve(source);
   }

   @Ignore("see https://issues.jboss.org/browse/IPROTO-14")
   @Test
   public void testAllowAliasOfEnumConstantValue() throws Exception {
      String file1 = "package test1;\n" +
            "enum E {\n" +
            "   option allow_alias = true;\n" +
            "   A = 1;\n" +
            "   B = 1;\n" +
            "}";
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
      assertExtensions(descriptor.getExtensionsTypes());
   }

   @Test
   public void testDuplicateTypeInFile1() throws Exception {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("test.M1 is already defined in test_proto_path/file1.proto");

      String file1 = "package test;\n" +
            "message M1 {\n" +
            "  required string a = 1;\n" +
            "}\n\n" +
            "message M1 {\n" +
            "  required string a = 1;\n" +
            "}\n";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("test_proto_path/file1.proto", file1);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testDuplicateTypeInFile2() throws Exception {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("test.M1 is already defined in test_proto_path/file1.proto");

      String file1 = "package test;\n" +
            "message M1 {\n" +
            "  required string a = 1;\n" +
            "}\n\n" +
            "enum M1 {\n" +
            "  VAL = 1;\n" +
            "}\n";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("test_proto_path/file1.proto", file1);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testNestedMessageWithSameName() throws Exception {
      String file1 = "package test;\n" +
            "message M1 {\n" +
            "  required string a = 1;\n" +
            "  message M1 { required string a = 1; }\n" +
            "}\n";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("file1.proto", file1);

      Map<String, FileDescriptor> descriptors = parseAndResolve(fileDescriptorSource);
      assertEquals(1, descriptors.size());
      assertTrue(descriptors.containsKey("file1.proto"));
      FileDescriptor fd = descriptors.get("file1.proto");
      assertEquals(1, fd.getMessageTypes().size());
   }

   @Test
   public void testDuplicateTypeInMessage1() throws Exception {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("test.M1.M2 is already defined in test_proto_path/file1.proto");

      String file1 = "package test;\n" +
            "message M1 {\n" +
            "  required string a = 1;\n" +
            "  message M2 { required string a = 1; }\n" +
            "  message M2 { required string a = 1; }\n" +
            "}\n";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("test_proto_path/file1.proto", file1);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testDuplicateTypeInMessage2() throws Exception {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("test.M1.E1 is already defined in test_proto_path/file1.proto");

      String file1 = "package test;\n" +
            "message M1 {\n" +
            "  required string a = 1;\n" +
            "  enum E1 { VAL1 = 1; }\n" +
            "  enum E1 { VAL2 = 2; }\n" +
            "}\n";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("test_proto_path/file1.proto", file1);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testDuplicateTypeInPackage1() throws Exception {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Duplicate definition of test.M1 in test_proto_path/file1.proto and test_proto_path/file2.proto");

      String file1 = "package test;\n" +
            "message M1 {\n" +
            "  required string a = 1;\n" +
            "}";

      String file2 = "package test;\n" +
            "message M1 {\n" +
            "  required string b = 2;\n" +
            "}";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("test_proto_path/file1.proto", file1);
      fileDescriptorSource.addProtoFile("test_proto_path/file2.proto", file2);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testDuplicateTypeInPackage2() throws Exception {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Duplicate definition of test.M1 in test_proto_path/file1.proto and test_proto_path/file2.proto");

      String file1 = "package test;\n" +
            "message M1 {\n" +
            "  required string a = 1;\n" +
            "}";

      String file2 = "package test;\n" +
            "enum M1 {\n" +
            "  VAL1 = 1;\n" +
            "}";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("test_proto_path/file1.proto", file1);
      fileDescriptorSource.addProtoFile("test_proto_path/file2.proto", file2);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testNotImportedInSamePackage() throws Exception {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Field type M1 not found");

      String file1 = "package test;\n" +
            "message M1 {\n" +
            "  required string a = 1;\n" +
            "}";

      String file2 = "package test;\n" +
            "message M2 {\n" +
            "  required M1 b = 2;\n" +
            "}";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("file1.proto", file1);
      fileDescriptorSource.addProtoFile("file2.proto", file2);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testNotImportedInAnotherPackage() throws Exception {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Field type test1.M1 not found");

      String file1 = "package test1;\n" +
            "message M1 {\n" +
            "  required string a = 1;\n" +
            "}";

      String file2 = "package test2;\n" +
            "message M2 {\n" +
            "  required test1.M1 b = 2;\n" +
            "}";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("file1.proto", file1);
      fileDescriptorSource.addProtoFile("file2.proto", file2);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testEmptyPackageName() throws Exception {
      // package name cannot be empty
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Syntax error in file1.proto at 1:9: expected a word");

      String file1 = "package ;\n" +
            "message M1 {\n" +
            "  required string a = 1;\n" +
            "}";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("file1.proto", file1);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testDefinitionNameWithDots1() throws Exception {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Definition names should not be qualified : somePackage.M1");

      String file1 = "package test;\n" +
            "message somePackage.M1 {\n" +
            "  required string a = 1;\n" +
            "}";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("file1.proto", file1);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testDefinitionNameWithDots2() throws Exception {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Definition names should not be qualified : somePackage.E1");

      String file1 = "package testPackage;\n" +
            "enum somePackage.E1 {\n" +
            "  VAL = 1;\n" +
            "}";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("file1.proto", file1);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testPublicImport() throws Exception {
      String file1 = "message M1 {\n" +
            "  required string a = 1;\n" +
            "}";

      String file2 = "import public \"file1.proto\";";

      String file3 = "import \"file2.proto\";\n" +
            "message M3 {\n" +
            "  required M1 a = 1;\n" +
            "}";

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
   public void testPrivateImport() throws Exception {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Field type M1 not found");

      String file1 = "message M1 {\n" +
            "  required string a = 1;\n" +
            "}";

      String file2 = "import \"file1.proto\";";

      String file3 = "import \"file2.proto\";\n" +
            "message M3 {\n" +
            "  required M1 a = 1;\n" +
            "}";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("file1.proto", file1);
      fileDescriptorSource.addProtoFile("file2.proto", file2);
      fileDescriptorSource.addProtoFile("file3.proto", file3);

      parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testImportAndPackage() throws Exception {
      String file1 = "package p;\n" +
            "message A {\n" +
            "   optional int32 f1 = 1;\n" +
            "}";

      String file2 = "package org.infinispan;\n" +
            "import \"file1.proto\";\n" +
            "message B {\n" +
            "   required p.A ma = 1;\n" +
            "}";

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
   public void testDocComment() throws Exception {
      String file1 = "package test1;\n" +
            "/**  \n" +
            " *    some doc text \n" +
            "  *    some more doc text \n" +
            "      **/\n\n" +
            "message X {\n" +
            " /**\n" +
            "  * field doc text  \n\n" +
            "  */\n" +
            "  optional int32 field1 = 1;\n" +
            "}\n";

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
      assertEquals("some doc text \n   some more doc text", typeX.getDocumentation());
      assertEquals("field doc text", field1.getDocumentation());
   }

   @Test
   public void testDocAnnotations() throws Exception {
      String file1 = "package test1;\n" +
            "/**  \n" +
            " *  @Foo(fooValue) \n" +
            "  *    some more doc text \n" +
            "      **/\n\n" +
            "message X {\n" +
            " /**\n" +
            "  * @Bar(barValue)  \n\n" +
            "  */\n" +
            "  optional int32 field1 = 1;\n" +
            "}\n";

      Configuration config = new Configuration.Builder()
            .messageAnnotation("Foo")
               .attribute(AnnotationElement.Annotation.DEFAULT_ATTRIBUTE)
               .identifierType()
            .annotationMetadataCreator(new AnnotationMetadataCreator<Object, Descriptor>() {
               @Override
               public Object create(Descriptor descriptor, AnnotationElement.Annotation annotation) {
                  return annotation.getDefaultAttributeValue().getValue();
               }
            })
            .fieldAnnotation("Bar")
               .attribute(AnnotationElement.Annotation.DEFAULT_ATTRIBUTE)
               .identifierType()
            .annotationMetadataCreator(new AnnotationMetadataCreator<Object, FieldDescriptor>() {
               @Override
               public Object create(FieldDescriptor fieldDescriptor, AnnotationElement.Annotation annotation) {
                  return annotation.getDefaultAttributeValue().getValue();
               }
            })
            .build();

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("file1.proto", file1);
      Map<String, FileDescriptor> descriptors = new SquareProtoParser(config).parseAndResolve(fileDescriptorSource);

      assertEquals(1, descriptors.size());
      assertTrue(descriptors.containsKey("file1.proto"));
      Map<String, GenericDescriptor> types = descriptors.get("file1.proto").getTypes();
      Descriptor typeX = (Descriptor) types.get("test1.X");
      assertNotNull(typeX);
      assertEquals(1, typeX.getFields().size());
      FieldDescriptor field1 = typeX.getFields().get(0);
      assertEquals("@Foo(fooValue) \n   some more doc text", typeX.getDocumentation());
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
      Configuration config = new Configuration.Builder()
            .messageAnnotation("Indexed")
            .attribute(AnnotationElement.Annotation.DEFAULT_ATTRIBUTE)
               .booleanType()
               .defaultValue(true)
            .annotationMetadataCreator(new AnnotationMetadataCreator<Boolean, Descriptor>() {
               @Override
               public Boolean create(Descriptor descriptor, AnnotationElement.Annotation annotation) {
                  return (Boolean) annotation.getDefaultAttributeValue().getValue();
               }
            })
            .build();

      FileDescriptorSource fileDescriptorSource = FileDescriptorSource.fromResources("/sample_bank_account/bank.proto");
      Map<String, FileDescriptor> descriptors = new SquareProtoParser(config).parseAndResolve(fileDescriptorSource);

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
   public void testAnnotationParserMissingRequiredAttribute() throws Exception {
      exception.expect(AnnotationParserException.class);
      exception.expectMessage("Attribute 'value' of annotation 'Indexed' on sample_bank_account.Account is required");

      Configuration config = new Configuration.Builder()
            .messageAnnotation("Indexed")
            .attribute(AnnotationElement.Annotation.DEFAULT_ATTRIBUTE)
            .booleanType()
            .build();

      FileDescriptorSource fileDescriptorSource = FileDescriptorSource.fromResources("/sample_bank_account/bank.proto");
      new SquareProtoParser(config).parseAndResolve(fileDescriptorSource);
   }

   @Test
   public void testMultipleAnnotationAttribute() throws Exception {
      Configuration config = new Configuration.Builder()
            .messageAnnotation("Xyz")
            .attribute("attr")
            .booleanType()
            .defaultValue(true)
            .multiple(true)
            .build();

      String testProto = "/** @Xyz(attr = {true, false, true}) */\n" +
            "message M {\n" +
            "  optional int32 field1 = 1; \n" +
            "}\n";

      FileDescriptorSource fileDescriptorSource = FileDescriptorSource.fromString("test.proto", testProto);
      Map<String, FileDescriptor> descriptors = new SquareProtoParser(config).parseAndResolve(fileDescriptorSource);

      FileDescriptor fileDescriptor = descriptors.get("test.proto");
      List<Descriptor> messageTypes = fileDescriptor.getMessageTypes();

      Descriptor messageType = messageTypes.get(0);
      assertEquals("M", messageType.getFullName());
      AnnotationElement.Annotation annotation = messageType.getAnnotations().get("Xyz");
      assertNotNull(annotation);
      AnnotationElement.Value attr = annotation.getAttributeValue("attr");
      assertTrue(attr instanceof AnnotationElement.Array);
      assertTrue(attr.getValue() instanceof List);
      List values = (List) attr.getValue();
      assertEquals(3, values.size());
      assertEquals(true, values.get(0));
      assertEquals(false, values.get(1));
      assertEquals(true, values.get(2));
   }

   @Test
   public void testArrayAnnotationAttributeNormalizing() throws Exception {
      Configuration config = new Configuration.Builder()
            .messageAnnotation("Xyz")
            .attribute("attr")
            .booleanType()
            .defaultValue(true)
            .multiple(true)
            .build();

      String testProto = "/** @Xyz(attr = true) */\n" +
            "message M {\n" +
            "  optional int32 field1 = 1; \n" +
            "}\n";

      FileDescriptorSource fileDescriptorSource = FileDescriptorSource.fromString("test.proto", testProto);
      Map<String, FileDescriptor> descriptors = new SquareProtoParser(config).parseAndResolve(fileDescriptorSource);

      FileDescriptor fileDescriptor = descriptors.get("test.proto");
      List<Descriptor> messageTypes = fileDescriptor.getMessageTypes();

      Descriptor messageType = messageTypes.get(0);
      assertEquals("M", messageType.getFullName());
      AnnotationElement.Annotation annotation = messageType.getAnnotations().get("Xyz");
      assertNotNull(annotation);
      AnnotationElement.Value attr = annotation.getAttributeValue("attr");
      assertTrue(attr instanceof AnnotationElement.Array);
      assertTrue(attr.getValue() instanceof List);
      List values = (List) attr.getValue();
      assertEquals(1, values.size());
      assertEquals(true, values.get(0));
   }

   private Map<String, FileDescriptor> parseAndResolve(FileDescriptorSource fileDescriptorSource) {
      return new SquareProtoParser(config).parseAndResolve(fileDescriptorSource);
   }

   private void assertResult(Descriptor descriptor) {
      assertThat(descriptor.getFields()).hasSize(4);
      assertThat(descriptor.findFieldByName("url").getJavaType()).isEqualTo(JavaType.STRING);
      assertThat(descriptor.findFieldByName("title").getLabel()).isEqualTo(Label.OPTIONAL);
      assertThat(descriptor.findFieldByName("i").getType()).isEqualTo(Type.MESSAGE);
      assertThat(descriptor.findFieldByName("i").getMessageType().getName()).isEqualTo("MoreInner");
      assertThat(descriptor.findFieldByNumber(3).getLabel()).isEqualTo(Label.REPEATED);
   }

   private void assertExtensions(List<ExtendDescriptor> extensions) {
      assertThat(extensions).hasSize(1);

      ExtendDescriptor resultExtension = extensions.get(0);
      assertThat(resultExtension.getExtendedMessage().getName()).isEqualTo("MoreInner");
      assertThat(resultExtension.getFileDescriptor()).isNotNull();

      FieldDescriptor barField = resultExtension.getFields().get(0);
      assertThat(barField.getLabel()).isEqualTo(Label.OPTIONAL);
      assertThat(barField.getJavaType()).isEqualTo(JavaType.FLOAT);
      assertThat(barField.getNumber()).isEqualTo(101);
      assertThat(barField.isPacked()).isTrue();
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
      assertThat(descriptor.getOptions()).hasSize(1);

      assertSearchRequestFields(descriptor.getFields());
   }

   private void assertSearchRequestFields(List<FieldDescriptor> fields) {
      FieldDescriptor queryField = fields.get(0);
      assertThat(queryField.getLabel()).isEqualTo(Label.REQUIRED);
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
      assertThat(pageNumberField.hasDefaultValue()).isTrue();
      assertThat(pageNumberField.getDefaultValue()).isEqualTo(10);
      assertThat(pageNumberField.getMessageType()).isNull();

      FieldDescriptor flagField = fields.get(3);
      assertThat(flagField.getLabel()).isEqualTo(Label.REPEATED);
      assertThat(flagField.getType()).isEqualTo(Type.INT32);
      assertThat(flagField.getJavaType()).isEqualTo(JavaType.INT);
      assertThat(flagField.getOptionByName("packed")).isEqualTo("true");

      FieldDescriptor dntField = fields.get(4);
      assertThat(dntField.getLabel()).isEqualTo(Label.OPTIONAL);
      assertThat(dntField.getType()).isEqualTo(Type.ENUM);
      assertThat(dntField.getEnumType().findValueByName("DONT_CARE").getNumber()).isEqualTo(2);
      assertThat(dntField.getEnumType().findValueByNumber(1).getName()).isEqualTo("TRACK_FOR_SURE");
      assertThat(dntField.getEnumType().getFileDescriptor()).isNotNull();
      assertThat(dntField.getFileDescriptor()).isNotNull();
      assertThat(dntField.getJavaType()).isEqualTo(JavaType.ENUM);

      FieldDescriptor reqEnumField = fields.get(5);
      assertThat(reqEnumField.getLabel()).isEqualTo(Label.REQUIRED);
      assertThat(reqEnumField.getType()).isEqualTo(Type.ENUM);
      assertThat(reqEnumField.getJavaType()).isEqualTo(JavaType.ENUM);
      assertThat(reqEnumField.hasDefaultValue()).isFalse();
      assertThat(reqEnumField.getEnumType()).isNotNull();
      assertThat(reqEnumField.getEnumType().findValueByNumber(0).getFileDescriptor()).isNotNull();
      assertThat(reqEnumField.getOptionByName("deprecated")).isEqualTo("true");

      FieldDescriptor labelField = fields.get(6);
      assertThat(labelField.hasDefaultValue()).isTrue();
      assertThat(labelField.getDefaultValue()).isEqualTo("whatever");

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
      URL resource = this.getClass().getClassLoader().getResource(resourcePath);
      if (resource != null) {
         return new File(resource.getPath());
      }
      return null;
   }
}
