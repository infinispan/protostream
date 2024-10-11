package org.infinispan.protostream.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.ProtobufTagMarshaller;
import org.infinispan.protostream.ProtobufTagMarshaller.ReadContext;
import org.infinispan.protostream.ProtobufTagMarshaller.WriteContext;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContext.MarshallerProvider;
import org.infinispan.protostream.TagReader;
import org.infinispan.protostream.TagWriter;
import org.junit.Test;


public class FullBufferReadTest {

   private SerializationContext createContext() {
      return ProtobufUtil.newSerializationContext();
   }

   @Test
   public void testFullArrayMarshaller() throws Exception {
      SerializationContext ctx = createContext();

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource().addProtoFile("file.proto", file);
      ctx.registerProtoFiles(fileDescriptorSource);

      class MockMarshallerFuncs implements MarshallerFuncs<X> {
         public byte[] actualBytes = null;
         public boolean isInputStream = false;

         @Override
         public X read(ReadContext rc) throws IOException {
            TagReader r = rc.getReader();
            isInputStream = r.isInputStream();
            actualBytes = r.fullBufferArray();
            return null;
         }

         @Override
         public void write(WriteContext wc, X p) throws IOException {
            TagWriter w = wc.getWriter();
            w.writeInt32(1, p.f1);
            w.writeInt64(2, p.f2);
         }
      }
      MockMarshallerFuncs mockMarshallerFuncs = new MockMarshallerFuncs();

      ctx.registerMarshallerProvider(new MockProtobufMarshaller<>(X.class, "test.X", mockMarshallerFuncs));

      byte[] fullMsgBytes = ProtobufUtil.toWrappedByteArray(ctx, new X(1234, 4321L));

      ProtobufUtil.fromWrappedByteArray(ctx, fullMsgBytes);

      assertNotNull(mockMarshallerFuncs.actualBytes);
      assertEquals(6, mockMarshallerFuncs.actualBytes.length);
      assertFalse(mockMarshallerFuncs.isInputStream);

      byte[] expectedBytes = {8, -46, 9, 16, -31, 33};
      assertNotNull(expectedBytes);
      assertTrue(Arrays.equals(mockMarshallerFuncs.actualBytes, expectedBytes));
   }

   @Test
   public void testFullArrayInputStreamMarshaller() throws Exception {
      SerializationContext ctx = createContext();

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource().addProtoFile("file.proto", file);
      ctx.registerProtoFiles(fileDescriptorSource);

      class MockMarshallerFuncs implements MarshallerFuncs<X> {
         public InputStream actualStream = null;
         public boolean isInputStream = false;

         @Override
         public X read(ReadContext rc) throws IOException {
            TagReader r = rc.getReader();
            isInputStream = r.isInputStream();
            actualStream = r.fullBufferInputStream();
            return null;
         }

         @Override
         public void write(WriteContext wc, X p) throws IOException {
            TagWriter w = wc.getWriter();
            w.writeInt32(1, p.f1);
            w.writeInt64(2, p.f2);
         }
      }
      MockMarshallerFuncs mockMarshallerFuncs = new MockMarshallerFuncs();

      ctx.registerMarshallerProvider(new MockProtobufMarshaller<>(X.class, "test.X", mockMarshallerFuncs));

      byte[] fullMsgBytes = ProtobufUtil.toWrappedByteArray(ctx, new X(1234, 4321L));
      InputStream in = new ByteArrayInputStream(fullMsgBytes);

      ProtobufUtil.fromWrappedStream(ctx, in);

      assertNotNull(mockMarshallerFuncs.actualStream);
      assertEquals(6, mockMarshallerFuncs.actualStream.available());
      //assertTrue(mockMarshallerFuncs.isInputStream); // Currently always false - the InputStream appears to be converted to a byte array decoder after WrappedMessage processed

      byte[] actualBytes = new byte[mockMarshallerFuncs.actualStream.available()];
      mockMarshallerFuncs.actualStream.read(actualBytes);

      byte[] expectedBytes = {8, -46, 9, 16, -31, 33};
      assertNotNull(expectedBytes);
      assertTrue(Arrays.equals(actualBytes, expectedBytes));
   }

   @Test
   public void illegalStateExceptionOnMixingReadWithFullBuffer() throws Exception {
      SerializationContext ctx = createContext();

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource().addProtoFile("file.proto", file);
      ctx.registerProtoFiles(fileDescriptorSource);

      class MockMarshallerFuncs implements MarshallerFuncs<X> {
         public byte[] actualBytes = null;

         @Override
         public X read(ReadContext rc) throws IOException {
            TagReader r = rc.getReader();
            r.readTag(); // calling any tag or field read prior to fullBufferArray should call IllegalStateException
            actualBytes = r.fullBufferArray();
            return null;
         }

         @Override
         public void write(WriteContext wc, X p) throws IOException {
            TagWriter w = wc.getWriter();
            w.writeInt32(1, p.f1);
            w.writeInt64(2, p.f2);
         }
      }
      MockMarshallerFuncs mockMarshallerFuncs = new MockMarshallerFuncs();

      ctx.registerMarshallerProvider(new MockProtobufMarshaller<>(X.class, "test.X", mockMarshallerFuncs));

      byte[] fullMsgBytes = ProtobufUtil.toWrappedByteArray(ctx, new X(1234, 4321L));

      try {
         ProtobufUtil.fromWrappedByteArray(ctx, fullMsgBytes);
         fail("IllegalStateException expected");
      } catch (IllegalStateException e) {
         assertEquals("fullBufferArray in marshaller can only be used on an unprocessed buffer", e.getMessage());
         assertNull(mockMarshallerFuncs.actualBytes);
      }
   }

   /**
    * Test Support
    **/

   private String file = "package test;\n" +
         "message X {\n" +
         "   optional int32 f1 = 1;\n" +
         "   optional int64 f2 = 2;\n" +
         "}";

   private class X {
      Integer f1;
      Long f2;

      private X(Integer f1, Long f2) {
         this.f1 = f1;
         this.f2 = f2;
      }
   }

   private interface MarshallerFuncs<P> {
      P read(ReadContext rc) throws IOException;

      void write(WriteContext wc, P p) throws IOException;
   }

   private class MockProtobufMarshaller<P> implements MarshallerProvider {
      private Class<? extends P> clazz;
      private String typeName;
      private MarshallerFuncs<P> marshallerFuncs;

      MockProtobufMarshaller(Class<? extends P> clazz, String typeName, MarshallerFuncs<P> marshallerFuncs) {
         this.clazz = clazz;
         this.typeName = typeName;
         this.marshallerFuncs = marshallerFuncs;
      }

      @Override
      public BaseMarshaller<?> getMarshaller(String typeName) {
         if (typeName.equals(typeName)) {
            return makeMarshaller();
         }
         return null;
      }

      @Override
      public BaseMarshaller<?> getMarshaller(Class<?> javaClass) {
         if (javaClass == clazz) {
            return makeMarshaller();
         }
         return null;
      }

      private BaseMarshaller<?> makeMarshaller() {
         return new ProtobufTagMarshaller<P>() {

            @Override
            public P read(ReadContext ctx) throws IOException {
               return marshallerFuncs.read(ctx);
            }

            @Override
            public void write(WriteContext ctx, P x) throws IOException {
               marshallerFuncs.write(ctx, x);
            }

            @Override
            public Class<? extends P> getJavaClass() {
               return clazz;
            }

            @Override
            public String getTypeName() {
               return typeName;
            }
         };
      }
   }

}
