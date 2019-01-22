package org.infinispan.protostream.annotations.impl;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoDocs;
import org.infinispan.protostream.annotations.ProtoField;
import org.junit.Test;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
public class DocumentationExtractorTest {

   public static class TestDocs {

      @ProtoDoc("1")
      public String field1;

      @ProtoDoc("1")
      public void method1() {
      }

      @ProtoDoc("1")
      @ProtoField(number = 1)
      @ProtoDoc("2")
      public String field2;

      @ProtoDocs({@ProtoDoc("1"), @ProtoDoc("2")})
      public String field3;

      @ProtoDocs(@ProtoDoc("1"))
      public String field4;
   }

   @Test
   public void testSingleDoc1() throws Exception {
      String doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredField("field1"));
      assertEquals("1", doc);
   }

   @Test
   public void testSingleDoc2() throws Exception {
      String doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredMethod("method1", null));
      assertEquals("1", doc);
   }

   @Test
   public void testMultiDoc1() throws Exception {
      Field field2 = TestDocs.class.getDeclaredField("field2");
      String doc = DocumentationExtractor.getDocumentation(field2);
      assertEquals("1\n2", doc);
   }

   @Test
   public void testMultiDoc2() throws Exception {
      String doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredField("field3"));
      assertEquals("1\n2", doc);
   }

   @Test
   public void testMultiDoc3() throws Exception {
      String doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredField("field4"));
      assertEquals("1", doc);
   }
}
