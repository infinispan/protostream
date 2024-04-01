package org.infinispan.protostream.types.java;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.impl.Log;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@RunWith(Parameterized.class)
public class TypesMarshallingTest {

    private static final Log log = Log.LogFactory.getLog(MethodHandles.lookup().lookupClass());

    private final MarshallingMethod method;
    private final ImmutableSerializationContext context;

    public TypesMarshallingTest(MarshallingMethod method) {
        this.method = method;
        context = newContext();
    }

    @Parameterized.Parameters
    public static Object[][] marshallingMethods() {
        return Arrays.stream(MarshallingMethodType.values())
                .map(t -> new Object[]{t})
                .toArray(Object[][]::new);
    }

    @Test
    public void testUUID() throws IOException {
        method.marshallAndUnmarshallTest(UUID.randomUUID(), context);
    }

    @Test
    public void testBitSet() throws IOException {
        var bytes = new byte[ThreadLocalRandom.current().nextInt(64)];
        ThreadLocalRandom.current().nextBytes(bytes);
        method.marshallAndUnmarshallTest(BitSet.valueOf(bytes), context);
    }

    @Test
    public void testBigDecimal() throws IOException {
        method.marshallAndUnmarshallTest(BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(-256, 256)), context);
    }

    @Test
    public void testBigInteger() throws IOException {
        method.marshallAndUnmarshallTest(BigInteger.valueOf(ThreadLocalRandom.current().nextInt()), context);
    }

    @FunctionalInterface
    public interface MarshallingMethod {
        void marshallAndUnmarshallTest(Object original, ImmutableSerializationContext ctx) throws IOException;
    }

    private static ImmutableSerializationContext newContext() {
        var ctx = ProtobufUtil.newSerializationContext();
        register(new CommonTypesSchema(), ctx);
        register(new CommonContainerTypesSchema(), ctx);
        return ctx;
    }

    private static void register(GeneratedSchema schema, SerializationContext context) {
        schema.registerMarshallers(context);
        schema.registerSchema(context);
    }

    enum MarshallingMethodType implements MarshallingMethod {
        WRAPPED_MESSAGE {
            @Override
            public void marshallAndUnmarshallTest(Object original, ImmutableSerializationContext ctx) throws IOException {
                var bytes = ProtobufUtil.toWrappedByteArray(ctx, original, 512);
                var copy = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
                log.debugf("Wrapped Message: bytes length=%s, original=%s, copy=%s", bytes.length, original, copy);
                Assert.assertEquals(original, copy);
            }
        },
        INPUT_STREAM {
            @Override
            public void marshallAndUnmarshallTest(Object original, ImmutableSerializationContext ctx) throws IOException {
                var baos = new ByteArrayOutputStream(512);
                ProtobufUtil.writeTo(ctx, baos, original);
                var bais = new ByteArrayInputStream(baos.toByteArray());
                var copy = ProtobufUtil.readFrom(ctx, bais, original.getClass());
                log.debugf("Input Stream: bytes length=%s, original=%s, copy=%s", baos.size(), original, copy);
                Assert.assertEquals(original, copy);
            }
        },
        BYTE_ARRAY {
            @Override
            public void marshallAndUnmarshallTest(Object original, ImmutableSerializationContext ctx) throws IOException {
                var baos = new ByteArrayOutputStream(512);
                ProtobufUtil.writeTo(ctx, baos, original);
                var copy = ProtobufUtil.fromByteArray(ctx, baos.toByteArray(), original.getClass());
                log.debugf("Byte Array: bytes length=%s, original=%s, copy=%s", baos.size(), original, copy);
                Assert.assertEquals(original, copy);
            }
        },
        JSON {
            @Override
            public void marshallAndUnmarshallTest(Object original, ImmutableSerializationContext ctx) throws IOException {
                var bytes = ProtobufUtil.toWrappedByteArray(ctx, original, 512);

                var json = ProtobufUtil.toCanonicalJSON(ctx, bytes);
                var jsonBytes = ProtobufUtil.fromCanonicalJSON(ctx, new StringReader(json));

                var copy = ProtobufUtil.fromWrappedByteArray(ctx, jsonBytes);

                log.debugf("JSON: JSON bytes length=%s, JSON String=%s, original=%s, copy=%s", jsonBytes.length, json, original, copy);
                Assert.assertEquals(original, copy);
            }
        }
    }

}
