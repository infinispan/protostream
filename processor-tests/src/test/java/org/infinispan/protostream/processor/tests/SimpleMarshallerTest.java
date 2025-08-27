package org.infinispan.protostream.processor.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.processor.tests.testdomain.SimpleMarshalledObject;
import org.junit.Test;

public class SimpleMarshallerTest {
   @Test
   public void testSerializeAndDeserialize() throws IOException {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();

      ProtoSchemaTest.TestSerializationContextInitializer serCtxInitializer = new TestSerializationContextInitializerImpl();
      serCtxInitializer.registerSchema(ctx);
      serCtxInitializer.registerMarshallers(ctx);

      assertTrue(ctx.canMarshall(SimpleMarshalledObject.class));

      SimpleMarshalledObject obj = new SimpleMarshalledObject(23, "super-fast", false, new byte[] { 0x1F, 0xA});
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, obj);

      assertEquals(obj, ProtobufUtil.fromWrappedByteArray(ctx, bytes));
   }
}
