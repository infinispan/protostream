package org.infinispan.protostream.impl.parser.impl;

import org.infinispan.protostream.AnnotationMetadataCreator;
import org.infinispan.protostream.AnnotationParserException;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.AnnotationElement;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.ExtendDescriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.descriptors.GenericDescriptor;
import org.infinispan.protostream.descriptors.JavaType;
import org.infinispan.protostream.descriptors.Rule;
import org.infinispan.protostream.descriptors.Type;
import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.impl.parser.SquareProtoParser;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DescriptorsTest {

   private final Configuration config = new Configuration.Builder().build();

   @org.junit.Rule
   public ExpectedException exception = ExpectedException.none();

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
      String file =
              " package test;\n" +
                      " import invalid.proto;\n" +
                      " message M {\n" +
                      "    required string a = 1;\n" +
                      "}";

      parseAndResolve(FileDescriptorSource.fromString("dummy.proto", file));
   }

   @Test
   public void testCyclicImport() throws Exception {
      exception.expect(DescriptorParserException.class);
      exception.expectMessage("Possible cyclic import detected at test.proto, import test2.proto");
      String file1 =
              " import test2.proto;\n" +
                      " message M {\n" +
                      "    required string a = 1;\n" +
                      "}";
      String file2 =
              " import test.proto;\n" +
                      " message M2 {\n" +
                      "    required string a = 1;\n" +
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

      parseAndResolve(fileDescriptorSource);
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
            "  message M1 { required string a = 1; }\n"+
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
            "  message M2 { required string a = 1; }\n"+
            "  message M2 { required string a = 1; }\n"+
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
            "  enum E1 { VAL1 = 1; }\n"+
            "  enum E1 { VAL2 = 2; }\n"+
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
                  AnnotationElement.Value value = annotation.getDefaultAttributeValue();
                  return value == null ? null : value.getValue();
               }
            })
            .fieldAnnotation("Bar")
               .attribute(AnnotationElement.Annotation.DEFAULT_ATTRIBUTE)
               .identifierType()
            .annotationMetadataCreator(new AnnotationMetadataCreator<Object, FieldDescriptor>() {
               @Override
               public Object create(FieldDescriptor fieldDescriptor, AnnotationElement.Annotation annotation) {
                  AnnotationElement.Value value = annotation.getDefaultAttributeValue();
                  return value == null ? null : value.getValue();
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
      assertEquals("fooValue", typeX.getParsedAnnotation("Foo"));
      assertEquals("@Bar(barValue)", field1.getDocumentation());
      Map<String, AnnotationElement.Annotation> fieldAnnotations = field1.getAnnotations();
      assertEquals("barValue", fieldAnnotations.get("Bar").getDefaultAttributeValue().getValue());
      assertEquals("barValue", field1.getParsedAnnotation("Bar"));
   }

   @Test
   public void testAnnotationParser() throws Exception {
      Configuration config = new Configuration.Builder()
            .messageAnnotation("Indexed")
            .annotationMetadataCreator(new AnnotationMetadataCreator<Object, Descriptor>() {
               @Override
               public Object create(Descriptor descriptor, AnnotationElement.Annotation annotation) {
                  AnnotationElement.Value value = annotation.getDefaultAttributeValue();
                  if (value == null) {
                     return Boolean.TRUE;
                  }
                  if (Boolean.TRUE.equals(value.getValue())) {
                     return Boolean.TRUE;
                  } else if (Boolean.FALSE.equals(value.getValue())) {
                     return Boolean.FALSE;
                  }
                  throw new AnnotationParserException("Invalid value " + value.getValue());
               }
            })
            .build();

      FileDescriptorSource fileDescriptorSource = FileDescriptorSource.fromResources("/sample_bank_account/bank.proto");
      Map<String, FileDescriptor> descriptors = new SquareProtoParser(config).parseAndResolve(fileDescriptorSource);

      FileDescriptor fileDescriptor = descriptors.get("sample_bank_account/bank.proto");
      List<Descriptor> messageTypes = fileDescriptor.getMessageTypes();

      Descriptor userMessageType = messageTypes.get(0);
      assertEquals("sample_bank_account.User", userMessageType.getFullName());
      assertEquals(Boolean.TRUE, userMessageType.getParsedAnnotation("Indexed"));

      Descriptor accountMessageType = messageTypes.get(1);
      assertEquals("sample_bank_account.Account", accountMessageType.getFullName());
      assertEquals(Boolean.TRUE, accountMessageType.getParsedAnnotation("Indexed"));
   }

   private Map<String, FileDescriptor> parseAndResolve(FileDescriptorSource fileDescriptorSource) {
      return new SquareProtoParser(config).parseAndResolve(fileDescriptorSource);
   }

   private void assertResult(Descriptor descriptor) {
      assertThat(descriptor.getFields()).hasSize(4);
      assertThat(descriptor.findFieldByName("url").getJavaType()).isEqualTo(JavaType.STRING);
      assertThat(descriptor.findFieldByName("title").getRule()).isEqualTo(Rule.OPTIONAL);
      assertThat(descriptor.findFieldByName("i").getType()).isEqualTo(Type.MESSAGE);
      assertThat(descriptor.findFieldByName("i").getMessageType().getName()).isEqualTo("MoreInner");
      assertThat(descriptor.findFieldByNumber(3).getRule()).isEqualTo(Rule.REPEATED);
   }

   private void assertExtensions(List<ExtendDescriptor> extensions) {
      assertThat(extensions).hasSize(1);

      ExtendDescriptor resultExtension = extensions.get(0);
      assertThat(resultExtension.getExtendedMessage().getName()).isEqualTo("MoreInner");
      assertThat(resultExtension.getFileDescriptor()).isNotNull();

      FieldDescriptor barField = resultExtension.getFields().get(0);
      assertThat(barField.getRule()).isEqualTo(Rule.OPTIONAL);
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
      assertThat(queryField.getRule()).isEqualTo(Rule.REQUIRED);
      assertThat(queryField.getType()).isEqualTo(Type.STRING);
      assertThat(queryField.getJavaType()).isEqualTo(JavaType.STRING);
      assertThat(queryField.getFullName()).isEqualTo("org.infinispan.protostream.test.SearchRequest.query");
      assertThat(queryField.getName()).isEqualTo("query");
      assertThat(queryField.getNumber()).isEqualTo(1);
      assertThat(queryField.getMessageType()).isNull();
      assertThat(queryField.getContainingMessage().getName()).isEqualTo("SearchRequest");


      FieldDescriptor pageNumberField = fields.get(1);
      assertThat(pageNumberField.getRule()).isEqualTo(Rule.OPTIONAL);
      assertThat(pageNumberField.getType()).isEqualTo(Type.INT32);
      assertThat(pageNumberField.getJavaType()).isEqualTo(JavaType.INT);
      assertThat(pageNumberField.getName()).isEqualTo("page_number");
      assertThat(pageNumberField.getNumber()).isEqualTo(2);
      assertThat(pageNumberField.hasDefaultValue()).isTrue();
      assertThat(pageNumberField.getDefaultValue()).isEqualTo(10);
      assertThat(pageNumberField.getMessageType()).isNull();

      FieldDescriptor flagField = fields.get(3);
      assertThat(flagField.getRule()).isEqualTo(Rule.REPEATED);
      assertThat(flagField.getType()).isEqualTo(Type.INT32);
      assertThat(flagField.getJavaType()).isEqualTo(JavaType.INT);
      assertThat(flagField.getOptionByName("packed")).isEqualTo("true");

      FieldDescriptor dntField = fields.get(4);
      assertThat(dntField.getRule()).isEqualTo(Rule.OPTIONAL);
      assertThat(dntField.getType()).isEqualTo(Type.ENUM);
      assertThat(dntField.getEnumDescriptor().findValueByName("DONT_CARE").getNumber()).isEqualTo(2);
      assertThat(dntField.getEnumDescriptor().findValueByNumber(1).getName()).isEqualTo("TRACK_FOR_SURE");
      assertThat(dntField.getEnumDescriptor().getFileDescriptor()).isNotNull();
      assertThat(dntField.getFileDescriptor()).isNotNull();
      assertThat(dntField.getJavaType()).isEqualTo(JavaType.ENUM);

      FieldDescriptor reqEnumField = fields.get(5);
      assertThat(reqEnumField.getRule()).isEqualTo(Rule.REQUIRED);
      assertThat(reqEnumField.getType()).isEqualTo(Type.ENUM);
      assertThat(reqEnumField.getJavaType()).isEqualTo(JavaType.ENUM);
      assertThat(reqEnumField.hasDefaultValue()).isFalse();
      assertThat(reqEnumField.getEnumDescriptor()).isNotNull();
      assertThat(reqEnumField.getEnumDescriptor().findValueByNumber(0).getFileDescriptor()).isNotNull();
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
