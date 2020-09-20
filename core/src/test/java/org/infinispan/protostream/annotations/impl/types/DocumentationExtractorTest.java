package org.infinispan.protostream.annotations.impl.types;

import static org.junit.Assert.assertEquals;

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
      @ProtoField(1)
      @ProtoDoc("2")
      public String field2;

      @ProtoDocs({@ProtoDoc("1"), @ProtoDoc("2")})
      public String field3;

      @ProtoDocs(@ProtoDoc("1"))
      public String field4;

      @ProtoDoc("")
      @ProtoDoc("1")
      @ProtoDoc("")
      @ProtoDoc("2")
      @ProtoDoc("3")
      @ProtoDoc("")
      @ProtoDoc("")
      public String field5;
   }

   @Test
   public void testSingleDoc1() throws Exception {
      String doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredField("field1").getAnnotationsByType(ProtoDoc.class));
      assertEquals("1", doc);
   }

   @Test
   public void testSingleDoc2() throws Exception {
      String doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredMethod("method1").getAnnotationsByType(ProtoDoc.class));
      assertEquals("1", doc);
   }

   @Test
   public void testMultiDoc1() throws Exception {
      String doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredField("field2").getAnnotationsByType(ProtoDoc.class));
      assertEquals("1\n2", doc);
   }

   @Test
   public void testMultiDoc2() throws Exception {
      String doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredField("field3").getAnnotationsByType(ProtoDoc.class));
      assertEquals("1\n2", doc);
   }

   @Test
   public void testMultiDoc3() throws Exception {
      String doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredField("field4").getAnnotationsByType(ProtoDoc.class));
      assertEquals("1", doc);
   }

   @Test
   public void testMultiDoc4() throws Exception {
      String doc = DocumentationExtractor.getDocumentation(TestDocs.class.getDeclaredField("field5").getAnnotationsByType(ProtoDoc.class));
      assertEquals("1\n\n2\n3", doc);
   }
}
