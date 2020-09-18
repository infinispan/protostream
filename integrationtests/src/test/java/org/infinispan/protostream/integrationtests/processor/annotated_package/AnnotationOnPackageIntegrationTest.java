package org.infinispan.protostream.integrationtests.processor.annotated_package;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ServiceLoader;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.impl.processor.tests.ReusableInitializer;
import org.junit.Test;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
public class AnnotationOnPackageIntegrationTest {

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
      assertEquals("annotated_package.proto", generatedSchema.getProtoFileName());

      SerializationContext serCtx = ProtobufUtil.newSerializationContext();

      generatedSchema.registerSchema(serCtx);
      generatedSchema.registerMarshallers(serCtx);

      assertTrue(serCtx.canMarshall(TestMessage.class));
   }

   @AutoProtoSchemaBuilder(dependsOn = ReusableInitializer.class, includeClasses = DependentInitializer.C.class, service = true)
   interface DependentInitializer extends SerializationContextInitializer {
      class C {
         @ProtoField(number = 1, required = true)
         boolean flag;
      }
   }

   @Test
   public void testDependsOn() {
      DependentInitializer dependentInitializer = null;
      for (SerializationContextInitializer sci : ServiceLoader.load(SerializationContextInitializer.class)) {
         if (sci instanceof DependentInitializer) {
            dependentInitializer = (DependentInitializer) sci;
            break;
         }
      }

      assertNotNull("DependentInitializer implementation not found by ServiceLoader", dependentInitializer);

      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      dependentInitializer.registerSchema(ctx);
      dependentInitializer.registerMarshallers(ctx);

      assertTrue(ctx.canMarshall(ReusableInitializer.A.class));
      assertTrue(ctx.canMarshall(ReusableInitializer.B.class));
      assertTrue(ctx.canMarshall(DependentInitializer.C.class));
   }
}
