package org.infinispan.protostream.integrationtests.processor.annotated_package;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.ServiceLoader;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoSchema;
import org.infinispan.protostream.annotations.ProtoSyntax;
import org.infinispan.protostream.domain.User;
import org.infinispan.protostream.integrationtests.processor.UserSerializationContextInitializer;
import org.infinispan.protostream.processor.tests.ReusableInitializer;
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

   @Test
   public void testUserWithLotsOfFields() throws IOException {
      SerializationContext serCtx = ProtobufUtil.newSerializationContext();

      UserSerializationContextInitializer.INSTANCE.registerSchema(serCtx);
      UserSerializationContextInitializer.INSTANCE.registerMarshallers(serCtx);

      User user = new User();
      user.setName("T");
      user.setSurname("DL");
      user.setSalutation("Helpful");

      byte[] userBytes = ProtobufUtil.toWrappedByteArray(serCtx, user);

      String json = ProtobufUtil.toCanonicalJSON(serCtx, userBytes, true);
      byte[] jsonBytes = ProtobufUtil.fromCanonicalJSON(serCtx, new StringReader(json));

      assertArrayEquals(userBytes, jsonBytes);
   }

   @ProtoSchema(dependsOn = ReusableInitializer.class, includeClasses = DependentInitializer.C.class, syntax = ProtoSyntax.PROTO3)
   interface DependentInitializer extends SerializationContextInitializer {
      class C {
         @ProtoField(number = 1)
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
