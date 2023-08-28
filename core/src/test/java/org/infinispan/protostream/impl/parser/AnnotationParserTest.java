package org.infinispan.protostream.impl.parser;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.infinispan.protostream.AnnotationParserException;
import org.infinispan.protostream.descriptors.AnnotationElement;
import org.infinispan.protostream.impl.Log;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author anistor@redhat.com
 * @since 2.0
 */
public class AnnotationParserTest {

   private static final Log log = Log.LogFactory.getLog(AnnotationParserTest.class);

   @org.junit.Rule
   public ExpectedException exception = ExpectedException.none();

   private static final String testDoc = "\n" +
         "some garbage  \n" +
         "\n" +
         "more garb#$#$#$#4age\n" +
         "   @XXX.yyy.zzzz(false)@ABC(x = some.class.or.enum, y=45, z = \"333333\")  \"dd\"\n" +
         "\n" +
         "@TestAnnotation2  ccc\n" +
         "\n" +
         "@TestAnnotation3(Store.NO) @TestAnnotation4(x = Store.NO, y = true)\n" +
         "\n" +
         "@TestAnnotation5(store = Store.YES, quickAndDirty = true)   VVV\n" +
         "\n" +
         "sd sfds fdsf df sdf sdf\n" +
         " junk sfdsf sd junk\n" +
         "@Test (\n" +
         "\n" +
         "   x=9, cc = { 34, 0 , {34 ,44}},\n" +
         "   y = true,\n" +
         "   y2 = false,\n" +
         "   y3 = null,\n" +
         "\n" +
         "   b = 45.9,  u = \"\\u0018\",  v ='f',\n" +
         "\n" +
         "   xx = \"axxxxx\\\\xxxxxxxb\",\n" +
         "   _xx = \"\",\n" +
         "\n" +
         "   yy = @Moooo\n" +
         ")\n" +
         "\n" +
         "@XXX.yyy.zz(false)@ABCd\n" +
         "\n" +
         "sss sdsds dew32432432 432   ;'  \n" +
         " @ XXX () abcd fdfd f\n" +
         "\n" +
         "@ XXX2          dfdsf\n";

   private static final String expectedOutput = "@XXX.yyy.zzzz(\n" +
         "   value=false\n" +
         ")\n" +
         "@ABC(\n" +
         "   x=some.class.or.enum,\n" +
         "   y=45,\n" +
         "   z=333333\n" +
         ")\n" +
         "@TestAnnotation2(\n" +
         ")\n" +
         "@TestAnnotation3(\n" +
         "   value=Store.NO\n" +
         ")\n" +
         "@TestAnnotation4(\n" +
         "   x=Store.NO,\n" +
         "   y=true\n" +
         ")\n" +
         "@TestAnnotation5(\n" +
         "   store=Store.YES,\n" +
         "   quickAndDirty=true\n" +
         ")\n" +
         "@Test(\n" +
         "   x=9,\n" +
         "   cc={\n" +
         "      34,\n" +
         "      0,\n" +
         "      {\n" +
         "         34,\n" +
         "         44\n" +
         "      }\n" +
         "   },\n" +
         "   y=true,\n" +
         "   y2=false,\n" +
         "   y3=null,\n" +
         "   b=45.9,\n" +
         "   u=\\u0018,\n" +
         "   v=f,\n" +
         "   xx=axxxxx\\\\xxxxxxxb,\n" +
         "   _xx=,\n" +
         "   yy=@Moooo(\n" +
         "   )\n" +
         ")\n" +
         "@XXX.yyy.zz(\n" +
         "   value=false\n" +
         ")\n" +
         "@ABCd(\n" +
         ")\n" +
         "@XXX(\n" +
         ")\n" +
         "@XXX2(\n" +
         ")\n";

   @Test
   public void testEmptyAnnotations() {
      testAnnotationParsing("", true, "");
   }

   @Test
   public void testDefaultArrayValue() {
      testAnnotationParsing("@Abc({\"a\", \"b\"})", true,
            "@Abc(\n" +
            "   value={\n" +
            "      a,\n" +
            "      b\n" +
            "   }\n" +
            ")\n");
   }

   @Test
   public void testSingleAnnotation() {
      testAnnotationParsing("@Abc()", true, "@Abc(\n)\n");
      testAnnotationParsing("@Abc", true, "@Abc(\n)\n");

      testAnnotationParsing("@Abc()", false, "@Abc(\n)\n");
      testAnnotationParsing("@Abc", false, "@Abc(\n)\n");
   }

   @Test
   public void testMultipleAnnotations() {
      testAnnotationParsing(testDoc, true, expectedOutput);
   }

   @Test
   public void testAnnotationsMustStartOnAnEmptyLine() {
      exception.expect(AnnotationParserException.class);
      exception.expectMessage("Error: 4,7: Annotations must start on an empty line");

      testAnnotationParsing(
            "some garbage  \n" +
                  "\n" +
                  "more garbage\n" +
                  " aaa  @Abc\n" +
                  "\n",
            true,
            "@Abc(\n)\n");
   }

   @Test
   public void testUnexpectedShmoo() {
      exception.expect(AnnotationParserException.class);
      exception.expectMessage("Error: 1,1: Unexpected character: x");

      testAnnotationParsing(
            "xx\n" +
                  "   @Abc  \n" +
                  "\n",
            false,
            "@Abc(\n)\n");
   }

   @Test
   public void testNoShmooExpected() {
      testAnnotationParsing(
            "\n   @Abc(x=\n" +
            "@YYY(a=1))  \n\n",
            false,
            "@Abc(\n" +
                  "   x=@YYY(\n" +
                  "      a=1\n" +
                  "   )\n" +
                  ")\n");
   }

   @Test(expected = AnnotationParserException.class)
   public void testInvalidUnicodeEscape() {
      testAnnotationParsing(
            "\n   @Abc(x=\"\\u000G\")  \n\n",
            false,
            "Should not have parsed");
   }

   private void testAnnotationParsing(String input, boolean expectDocNoise, String expectedOutput) {
      AnnotationParser parser = new AnnotationParser(input, expectDocNoise);
      List<AnnotationElement.Annotation> annotations = parser.parse();
      TreePrinter treePrinter = new TreePrinter();
      treePrinter.printAnnotations(annotations);
      String output = treePrinter.getOutput();
      log.debug(output);
      assertEquals(expectedOutput, output);
   }
}
