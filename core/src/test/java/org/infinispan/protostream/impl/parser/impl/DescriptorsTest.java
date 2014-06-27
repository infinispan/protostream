package org.infinispan.protostream.impl.parser.impl;

import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.ExtendDescriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.FileDescriptor;
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

public class DescriptorsTest {

   @org.junit.Rule
   public ExpectedException exception = ExpectedException.none();


   @Test
   public void testInputFromFile() throws Exception {
      File f1 = asFile("org/infinispan/protostream/lib/base.proto");
      File f2 = asFile("org/infinispan/protostream/lib/base2.proto");
      FileDescriptorSource fileDescriptorSource = FileDescriptorSource.fromFiles(f1, f2);
      Map<String, FileDescriptor> parseResult = new SquareProtoParser().parse(fileDescriptorSource);
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

      new SquareProtoParser().parse(FileDescriptorSource.fromString("dummy.proto", file));

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

      new SquareProtoParser().parse(source);

   }

   @Test
   public void testTransform() throws Exception {

      FileDescriptorSource fileDescriptorSource = FileDescriptorSource.fromResources(
              "org/infinispan/protostream/test/message.proto",
              "org/infinispan/protostream/lib/base.proto",
              "org/infinispan/protostream/lib/base2.proto");

      Map<String, FileDescriptor> files = new SquareProtoParser().parse(fileDescriptorSource);

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