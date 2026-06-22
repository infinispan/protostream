package org.infinispan.protostream.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.infinispan.protostream.descriptors.AnnotationElement;
import org.junit.jupiter.api.Test;

/**
 * @author anistor@redhat.com
 */
public class AnnotationConfigurationTest {

   @Test
   public void testNullDefaultValue() {
      var ex = assertThrows(IllegalArgumentException.class, () -> {
         Configuration.builder()
               .annotationsConfig()
               .annotation("Xyz", AnnotationElement.AnnotationTarget.MESSAGE)
               .attribute("elem1")
               .type(AnnotationElement.AttributeType.BOOLEAN)
               .defaultValue(null);  // exception expected here
      });
      assertTrue(ex.getMessage().contains("Default value cannot be null"));
   }

   @Test
   public void testWrongDefaultValueType() {
      var ex = assertThrows(IllegalArgumentException.class, () -> {
         AnnotationAttributeConfiguration.Builder builder = Configuration.builder()
               .annotationsConfig()
               .annotation("Xyz", AnnotationElement.AnnotationTarget.MESSAGE)
               .attribute("elem1")
               .type(AnnotationElement.AttributeType.BOOLEAN)
               .defaultValue(13);  // this is not valid

         builder.build();  // exception expected here
      });
      assertTrue(ex.getMessage().contains("Illegal default value type for annotation element 'elem1'. Boolean expected."));
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
      var ex = assertThrows(IllegalArgumentException.class, () -> {
         Configuration.builder()
               .annotationsConfig()
               .annotation("Xyz", AnnotationElement.AnnotationTarget.MESSAGE)
               .metadataCreator(null)
               .attribute("elem1")
               .type(AnnotationElement.AttributeType.STRING)
               .attribute("");
      });
      assertTrue(ex.getMessage().contains("'' is not a valid annotation element name"));
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
