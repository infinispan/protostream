package org.infinispan.protostream.impl.parser;

import org.infinispan.protostream.descriptors.AnnotationElement;
import org.infinispan.protostream.impl.Log;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author anistor@redhat.com
 * @since 2.0
 */
public class AnnotationParserTest {

   private static final Log log = Log.LogFactory.getLog(AnnotationParserTest.class);

   private static final String testDoc = "\n" +
         "some garbage\n" +
         "\n" +
         "garb#$#$#$#4age @XXX.yyy.zzzz(false)@ABC(x = some.class.or.enum, y=45, z = \"333333\")  \"dd\"\n" +
         "\n" +
         "@IndexedField2  ccc\n" +
         "\n" +
         "@IndexedField3(Store.NO) @IndexedField4(x = Store.NO, y = true)\n" +
         "\n" +
         "@IndexedField5(store = Store.YES, quickAndDirty = true)   VVV\n" +
         "\n" +
         "sd sfds fdsf df sdf sdf\n" +
         " junk sfdsf sd junk @Test (\n" +
         "\n" +
         "   x=9, cc = { 34, 0 , {34 ,44}},\n" +
         "   y = true,\n" +
         "   y2 = false,\n" +
         "   y3 = null,\n" +
         "\n" +
         "   b = 45.9,    v ='f',\n" +
         "\n" +
         "   xx = \"axxxxx\\\\xxxxxxxb\",\n" +
         "   _xx = \"\",\n" +
         "\n" +
         "   yy = @Moooo\n" +
         ")\n" +
         "\n" +
         "@XXX.yyy.zz(false)@ABCd\n" +
         "\n" +
         "sss sdsds dew32432432 432   ;'  @ XXX () abcd fdfd f\n" +
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
         "@IndexedField2(\n" +
         ")\n" +
         "@IndexedField3(\n" +
         "   value=Store.NO\n" +
         ")\n" +
         "@IndexedField4(\n" +
         "   x=Store.NO,\n" +
         "   y=true\n" +
         ")\n" +
         "@IndexedField5(\n" +
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
      testAnnotationParsing("", "");
   }

   @Test
   public void testSingleAnnotation() {
      testAnnotationParsing("@Indexed()", "@Indexed(\n)\n");
   }

   @Test
   public void testMultipleAnnotations() {
      testAnnotationParsing(testDoc, expectedOutput);
   }

   private void testAnnotationParsing(String input, String expected) {
      AnnotationParser parser = new AnnotationParser(input);
      Map<String, AnnotationElement.Annotation> annotations = parser.parse();
      TreePrinter treePrinter = new TreePrinter();
      treePrinter.printAnnotations(annotations);
      String output = treePrinter.getOutput();
      log.debug(output);
      assertEquals(expected, output);
   }
}
