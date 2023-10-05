package org.infinispan.protostream.annotations.impl.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.infinispan.custom.annotations.MyCustomAnnotation;
import org.infinispan.protostream.annotations.ProtoComment;
import org.infinispan.protostream.annotations.ProtoComments;
import org.infinispan.protostream.annotations.ProtoField;
import org.junit.Test;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
public class DocumentationExtractorTest {

   public static class TestDocs {

      @ProtoComment("1")
      public String field1;

      @ProtoComment("1")
      public void method1() {
      }

      @ProtoComment("1")
      @ProtoField(1)
      @ProtoComment("2")
      public String field2;

      @ProtoComments({@ProtoComment("1"), @ProtoComment("2")})
      public String field3;

      @ProtoComments(@ProtoComment("1"))
      public String field4;

      @ProtoComment("")
      @ProtoComment("1")
      @ProtoComment("")
      @ProtoComment("2")
      @ProtoComment("3")
      @ProtoComment("")
      @ProtoComment("")
      public String field5;

      @MyCustomAnnotation(name = "field6", someBool = true, someEnum = MyCustomAnnotation.MyEnum.TWO, someLong = -100, someInteger = 100)
      public String field6;

      @MyCustomAnnotation(name = "method2", someBool = true, someEnum = MyCustomAnnotation.MyEnum.TWO, someLong = -100, someInteger = 100)
      public void method2() {
      }

      @ProtoComment("1")
      @MyCustomAnnotation(name = "field7", someBool = true, someEnum = MyCustomAnnotation.MyEnum.TWO, someLong = -100, someInteger = 100)
      @ProtoComment("1")
      public String field7;

      @ProtoComment("1")
      @MyCustomAnnotation(name = "method3", someBool = true, someEnum = MyCustomAnnotation.MyEnum.TWO, someLong = -100, someInteger = 100)
      @ProtoComment("1")
      public void method3() {
      }

      @MyCustomAnnotation
      public String field8;

      @MyCustomAnnotation
      public void method4() {
      }
   }

   @Test
   public void testSingleDoc1() throws Exception {
      String doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredField("field1"), false);
      assertEquals("1", doc);
   }

   @Test
   public void testSingleDoc2() throws Exception {
      String doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredMethod("method1"), false);
      assertEquals("1", doc);
   }

   @Test
   public void testMultiDoc1() throws Exception {
      String doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredField("field2"), false);
      assertEquals("1\n2", doc);
   }

   @Test
   public void testMultiDoc2() throws Exception {
      String doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredField("field3"), false);
      assertEquals("1\n2", doc);
   }

   @Test
   public void testMultiDoc3() throws Exception {
      String doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredField("field4"), false);
      assertEquals("1", doc);
   }

   @Test
   public void testMultiDoc4() throws Exception {
      String doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredField("field5"), false);
      assertEquals("1\n\n2\n3", doc);
   }

   @Test
   public void testCustomFieldAnnotation() throws Exception {
      String doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredField("field6"), true);
      assertTrue(doc, doc.startsWith("@org.infinispan.custom.annotations.MyCustomAnnotation("));
      verifyAnnotation(doc);
      doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredField("field6"), false);
      assertTrue(doc, doc.startsWith("@MyCustomAnnotation("));
      verifyAnnotation(doc);
   }

   private void verifyAnnotation(String doc) {
      assertTrue(doc, doc.contains("name="));
      assertTrue(doc.contains("someBool=true"));
      assertTrue(doc.contains("someLong=-100"));
      assertTrue(doc.contains("someInteger=100"));
      assertTrue(doc.contains("someEnum=TWO"));
   }

   private void verifyAnnotationDefaultValues(String doc) {
      assertTrue(doc.contains("name="));
      assertTrue(doc.contains("someBool=false"));
      assertTrue(doc.contains("someLong=0"));
      assertTrue(doc.contains("someInteger=0"));
      assertTrue(doc.contains("someEnum=ONE"));
   }

   @Test
   public void testCustomMethodAnnotation() throws Exception {
      String doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredMethod("method2"), true);
      assertTrue(doc, doc.startsWith("@org.infinispan.custom.annotations.MyCustomAnnotation("));
      verifyAnnotation(doc);
      doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredMethod("method2"), false);
      assertTrue(doc, doc.startsWith("@MyCustomAnnotation("));
      verifyAnnotation(doc);
   }

   @Test
   public void testMixedCustomFieldAnnotation() throws Exception {
      String doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredField("field7"), true);
      assertTrue(doc, doc.startsWith("1\n1\n@org.infinispan.custom.annotations.MyCustomAnnotation("));
      verifyAnnotation(doc);
      doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredField("field7"), false);
      assertTrue(doc, doc.startsWith("1\n1\n@MyCustomAnnotation("));
      verifyAnnotation(doc);
   }

   @Test
   public void testMixedCustomMethodAnnotation() throws Exception {
      String doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredMethod("method3"), true);
      assertTrue(doc, doc.startsWith("1\n1\n@org.infinispan.custom.annotations.MyCustomAnnotation("));
      verifyAnnotation(doc);
      doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredMethod("method3"), false);
      assertTrue(doc, doc.startsWith("1\n1\n@MyCustomAnnotation("));
      verifyAnnotation(doc);
   }

   @Test
   public void testDefaultFieldAnnotation() throws Exception {
      String doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredField("field8"), true);
      assertTrue(doc, doc.startsWith("@org.infinispan.custom.annotations.MyCustomAnnotation("));
      verifyAnnotationDefaultValues(doc);
      doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredField("field8"), false);
      assertTrue(doc, doc.startsWith("@MyCustomAnnotation("));
      verifyAnnotationDefaultValues(doc);
   }

   @Test
   public void testDefaultMethodAnnotation() throws Exception {
      String doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredMethod("method4"), true);
      assertTrue(doc, doc.startsWith("@org.infinispan.custom.annotations.MyCustomAnnotation("));
      verifyAnnotationDefaultValues(doc);
      doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredMethod("method4"), false);
      assertTrue(doc, doc.startsWith("@MyCustomAnnotation("));
      verifyAnnotationDefaultValues(doc);
   }
}
