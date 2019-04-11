package org.infinispan.protostream.tests.annotated_package;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ServiceLoader;

import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.junit.Test;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
public class AnnotationOnPackageTest {

   @Test
   public void testAnnotationOnPackage() throws Exception {
      SerializationContextInitializer initializer = null;
      for (SerializationContextInitializer sci : ServiceLoader.load(SerializationContextInitializer.class)) {
         if (sci.getClass().getSimpleName().equals("AnnotationOnPackageTestInitializer")) {
            initializer = sci;
            break;
         }
      }

      assertNotNull(initializer);
      assertEquals("annotated_package.proto", initializer.getProtoFileName());

      SerializationContext serCtx = ProtobufUtil.newSerializationContext();

      initializer.registerSchema(serCtx);
      initializer.registerMarshallers(serCtx);

      assertTrue(serCtx.canMarshall(TestMessage.class));
   }
}
