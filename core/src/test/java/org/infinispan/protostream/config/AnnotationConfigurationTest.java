package org.infinispan.protostream.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.infinispan.protostream.descriptors.AnnotationElement;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author anistor@redhat.com
 */
public class AnnotationConfigurationTest {

   @org.junit.Rule
   public ExpectedException exception = ExpectedException.none();

   @Test
   public void testNullDefaultValue() {
      exception.expect(IllegalArgumentException.class);
      exception.expectMessage("Default value cannot be null");

      Configuration.builder()
            .annotationsConfig()
            .annotation("Xyz", AnnotationElement.AnnotationTarget.MESSAGE)
            .attribute("elem1")
            .type(AnnotationElement.AttributeType.BOOLEAN)
            .defaultValue(null);  // exception expected here
   }

   @Test
   public void testWrongDefaultValueType() {
     exception.expect(IllegalArgumentException.class);
     exception.expectMessage("Illegal default value type for annotation element 'elem1'. Boolean expected.");

      AnnotationAttributeConfiguration.Builder builder = Configuration.builder()
            .annotationsConfig()
            .annotation("Xyz", AnnotationElement.AnnotationTarget.MESSAGE)
            .attribute("elem1")
            .type(AnnotationElement.AttributeType.BOOLEAN)
            .defaultValue(13);  // this is not valid

      builder.build();  // exception expected here
   }

   @Test
   public void testMatchingDefaultValueType() {
      Configuration cfg = Configuration.builder()
            .annotationsConfig()
            .annotation("Xyz", AnnotationElement.AnnotationTarget.MESSAGE)
            .attribute("elem1")
            .type(AnnotationElement.AttributeType.BOOLEAN)
            .defaultValue(true)
            .build();
      assertEquals(Boolean.TRUE, cfg.annotationsConfig().annotations().get("Xyz").attributes().get("elem1").defaultValue());
   }

   @Test
   public void testAttributeNameMustNotBeEmpty() {
      exception.expect(IllegalArgumentException.class);
      exception.expectMessage("'' is not a valid annotation element name");

      Configuration.builder()
            .annotationsConfig()
            .annotation("Xyz", AnnotationElement.AnnotationTarget.MESSAGE)
            .metadataCreator(null)
            .attribute("elem1")
            .type(AnnotationElement.AttributeType.STRING)
            .attribute("");
   }

   @Test
   public void testRepeatableAnnotation() {
      Configuration cfg = Configuration.builder()
            .annotationsConfig()
            .annotation("Inner", AnnotationElement.AnnotationTarget.MESSAGE)
            .repeatable("Outer")
            .attribute("elem1")
            .type(AnnotationElement.AttributeType.BOOLEAN)
            .defaultValue(true)
            .build();

      assertEquals(Boolean.TRUE, cfg.annotationsConfig().annotations().get("Inner").attributes().get("elem1").defaultValue());
      AnnotationConfiguration outer = cfg.annotationsConfig().annotations().get("Outer");
      assertEquals(AnnotationElement.AttributeType.ANNOTATION, outer.attributes().get("value").type());
      assertTrue(outer.attributes().get("value").multiple());
      assertEquals(Collections.singleton("Inner"), outer.attributes().get("value").allowedValues());
   }

   /**
    * Test an annotation configuration similar to Infinispan's use case.
    */
   @Test
   public void testComplexConfiguration() {
      Configuration.builder()
            .annotationsConfig()
               .annotation("Indexed", AnnotationElement.AnnotationTarget.MESSAGE)
                  .attribute("index")
                     .type(AnnotationElement.AttributeType.STRING)
                     .defaultValue("")
               .annotation("Analyzer", AnnotationElement.AnnotationTarget.MESSAGE, AnnotationElement.AnnotationTarget.FIELD)
                  .attribute("definition")
                     .type(AnnotationElement.AttributeType.STRING)
                     .defaultValue("")
               .annotation("Field", AnnotationElement.AnnotationTarget.FIELD)
                  .repeatable("Fields")
                  .attribute("name")
                     .type(AnnotationElement.AttributeType.STRING)
                     .defaultValue("")
                  .attribute("index")
                     .type(AnnotationElement.AttributeType.IDENTIFIER)
                     .allowedValues("Index.YES", "Index.NO")
                     .defaultValue("Index.YES")
                  .attribute("boost")
                     .type(AnnotationElement.AttributeType.FLOAT)
                     .defaultValue(1.0f)
                  .attribute("analyze")
                     .type(AnnotationElement.AttributeType.IDENTIFIER)
                     .allowedValues("Analyze.YES", "Analyze.NO")
                     .defaultValue("Analyze.NO")
                  .attribute("store")
                     .type(AnnotationElement.AttributeType.IDENTIFIER)
                     .allowedValues("Store.YES", "Store.NO")
                     .defaultValue("Store.NO")
                  .attribute("analyzer")
                     .type(AnnotationElement.AttributeType.ANNOTATION)
                     .allowedValues("Analyzer")
                     .defaultValue("@Analyzer(definition=\"\")")
                  .attribute("indexNullAs")
                     .type(AnnotationElement.AttributeType.STRING)
                     .defaultValue("__DO_NOT_INDEX_NULL__")
                  .annotation("SortableField", AnnotationElement.AnnotationTarget.FIELD)
                     .repeatable("SortableFields")
            .build();
   }
}
