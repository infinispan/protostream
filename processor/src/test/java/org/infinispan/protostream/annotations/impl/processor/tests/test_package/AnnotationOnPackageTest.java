package org.infinispan.protostream.annotations.impl.processor.tests.test_package;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ServiceLoader;

import org.infinispan.protostream.SerializationContextInitializer;
import org.junit.Test;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
public class AnnotationOnPackageTest {

   @Test
   public void testAnnotationOnPackage() {
      SerializationContextInitializer initializer = null;
      for (SerializationContextInitializer sci : ServiceLoader.load(SerializationContextInitializer.class)) {
         if (sci.getClass().getSimpleName().equals("AnnotationOnPackageTestInitializer")) {
            initializer = sci;
            break;
         }
      }

      assertNotNull(initializer);
      assertEquals("test_schema.proto", initializer.getProtoFileName());
   }
}
