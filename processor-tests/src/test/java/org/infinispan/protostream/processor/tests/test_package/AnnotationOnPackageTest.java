package org.infinispan.protostream.processor.tests.test_package;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ServiceLoader;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.SerializationContextInitializer;
import org.junit.jupiter.api.Test;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
public class AnnotationOnPackageTest {

   @Test
   public void testAnnotationOnPackage() {
      GeneratedSchema generatedSchema = null;
      for (SerializationContextInitializer sci : ServiceLoader.load(SerializationContextInitializer.class)) {
         if (sci.getClass().getSimpleName().equals("AnnotationOnPackageTestInitializer")) {
            generatedSchema = (GeneratedSchema) sci;
            break;
         }
      }

      assertNotNull(generatedSchema);
      assertEquals("test_schema.proto", generatedSchema.getProtoFileName());
   }
}
