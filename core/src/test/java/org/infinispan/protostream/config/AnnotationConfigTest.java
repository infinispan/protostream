package org.infinispan.protostream.config;

import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

/**
 * @author anistor@redhat.com
 */
public class AnnotationConfigTest {

   @org.junit.Rule
   public ExpectedException exception = ExpectedException.none();

   @Test
   public void testWrongDefaultValueType() {
      exception.expect(IllegalArgumentException.class);
      exception.expectMessage("Illegal default value type for attribute 'attr'. Boolean expected.");

      AnnotationAttributeConfig.Builder builder = new Configuration.Builder()
            .messageAnnotation("Xyz")
            .attribute("attr")
            .booleanType()
            .defaultValue(13);  // this is not valid

      builder.build();  // exception expected here
   }

   @Test
   public void testMatchingDefaultValueType() {
      Configuration cfg = new Configuration.Builder()
            .messageAnnotation("Xyz")
            .attribute("attr")
            .booleanType()
            .defaultValue(true)
            .build();
      assertEquals(Boolean.TRUE, cfg.messageAnnotations().get("Xyz").attributes().get("attr").defaultValue());
   }
}
