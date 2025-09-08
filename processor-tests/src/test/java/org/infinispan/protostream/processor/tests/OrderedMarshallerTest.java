package org.infinispan.protostream.processor.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.EnumSet;

import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.processor.tests.testdomain.MarshalledNo15GreaterThan;
import org.infinispan.protostream.processor.tests.testdomain.MarshalledYes15GreaterThan;
import org.infinispan.protostream.processor.tests.testdomain.SimpleEnum;
import org.junit.Test;

public class OrderedMarshallerTest {
   @Test
   public void testNoField15HasOneGreaterThan15() throws IOException {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();

      ProtoSchemaTest.TestSerializationContextInitializer serCtxInitializer = new TestSerializationContextInitializerImpl();
      serCtxInitializer.registerSchema(ctx);
      serCtxInitializer.registerMarshallers(ctx);

      assertTrue(ctx.canMarshall(MarshalledNo15GreaterThan.class));

      MarshalledNo15GreaterThan obj = new MarshalledNo15GreaterThan(EnumSet.of(SimpleEnum.A, SimpleEnum.C), "foo");
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, obj);

      assertEquals(obj, ProtobufUtil.fromWrappedByteArray(ctx, bytes));
   }

   @Test
   public void testYesField15HasOneGreaterThan15() throws IOException {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();

      ProtoSchemaTest.TestSerializationContextInitializer serCtxInitializer = new TestSerializationContextInitializerImpl();
      serCtxInitializer.registerSchema(ctx);
      serCtxInitializer.registerMarshallers(ctx);

      assertTrue(ctx.canMarshall(MarshalledYes15GreaterThan.class));

      MarshalledYes15GreaterThan obj = new MarshalledYes15GreaterThan(new double[] { 1.3, 5, -1231.121}, 23, "foo");
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, obj);

      assertEquals(obj, ProtobufUtil.fromWrappedByteArray(ctx, bytes));
   }
}
