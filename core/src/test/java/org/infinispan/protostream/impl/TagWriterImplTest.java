package org.infinispan.protostream.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.TagReader;
import org.infinispan.protostream.TagWriter;
import org.infinispan.protostream.descriptors.WireType;
import org.junit.Test;

/**
 * @author anistor@redhat.com
 */
public class TagWriterImplTest {

   private static final Log log = Log.LogFactory.getLog(TagWriterImplTest.class);
   private static final long SEED = System.nanoTime();
   private static final int MAX_BYTE_ARRAY_SIZE = 512;

   @Test
   public void testByteArrayEncodeAndDecode() throws Exception {
      byte[] buffer = new byte[MAX_BYTE_ARRAY_SIZE];
      doTest(new Factory() {
         @Override
         public TagWriter newWriter(SerializationContext ctx) {
            return TagWriterImpl.newInstance(ctx, buffer);
         }

         @Override
         public TagReader newReader(SerializationContext ctx) {
            return TagReaderImpl.newInstance(ctx, buffer);
         }
      });
   }

   @Test
   public void testOutputStreamEncodeAndDecode() throws Exception {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(MAX_BYTE_ARRAY_SIZE);
      doTest(new Factory() {
         @Override
         public TagWriter newWriter(SerializationContext ctx) {
            return TagWriterImpl.newInstance(ctx, baos);
         }

         @Override
         public TagReader newReader(SerializationContext ctx) {
            return TagReaderImpl.newInstance(ctx, new ByteArrayInputStream(baos.toByteArray()));
         }
      });
   }

   private void doTest(Factory factory) throws IOException {
      log.infof("SEED is %s", SEED);
      Random random = new Random(SEED);
      Data data = new Data(random);
      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      TagWriter writer = factory.newWriter(ctx);
      int tag = 0;
      // bool
      writer.writeBool(++tag, data.b);
      // ints
      writer.writeInt32(++tag, data.i);
      writer.writeSInt32(++tag, data.i);
      writer.writeUInt32(++tag, Math.abs(data.i));
      // longs
      writer.writeInt64(++tag, data.l);
      writer.writeSInt64(++tag, data.l);
      writer.writeUInt64(++tag, Math.abs(data.l));
      // double
      writer.writeDouble(++tag, data.d);
      // float
      writer.writeFloat(++tag, data.f);
      // string
      writer.writeString(++tag, data.s);
      // byte(s)
      writer.writeBytes(++tag, data.bytes);
      writer.writeBytes(++tag, data.bytes, 1, 2);
      writer.flush();

      TagReader reader = factory.newReader(ctx);
      tag = 0;
      // bool
      checkFieldNumber(++tag, reader);
      assertEquals(data.b, reader.readBool());
      // ints
      checkFieldNumber(++tag, reader);
      assertEquals(data.i, reader.readInt32());
      checkFieldNumber(++tag, reader);
      assertEquals(data.i, reader.readSInt32());
      checkFieldNumber(++tag, reader);
      assertEquals(Math.abs(data.i), reader.readUInt32());
      // longs
      checkFieldNumber(++tag, reader);
      assertEquals(data.l, reader.readInt64());
      checkFieldNumber(++tag, reader);
      assertEquals(data.l, reader.readSInt64());
      checkFieldNumber(++tag, reader);
      assertEquals(Math.abs(data.l), reader.readUInt64());
      // double
      checkFieldNumber(++tag, reader);
      assertEquals(data.d, reader.readDouble(), 1);
      // float
      checkFieldNumber(++tag, reader);
      assertEquals(data.f, reader.readFloat(), 1);
      // string
      checkFieldNumber(++tag, reader);
      assertEquals(data.s, reader.readString());
      // byte(s)
      checkFieldNumber(++tag, reader);
      assertArrayEquals(data.bytes, reader.readByteArray());
      checkFieldNumber(++tag, reader);
      assertArrayEquals(new byte[]{data.bytes[1], data.bytes[2]}, reader.readByteArray());
   }

   private void checkFieldNumber(int expected, TagReader reader) throws IOException {
      assertEquals(expected, WireType.getTagFieldNumber(reader.readTag()));
   }

   /**
    * Creates a writer and stores the byte[] generated. Creates a reader from that byte[]
    */
   private interface Factory {
      TagWriter newWriter(SerializationContext ctx);

      TagReader newReader(SerializationContext ctx) throws IOException;
   }

   private static class Data {
      final boolean b;
      final int i;
      final long l;
      final double d;
      final float f;
      final String s;
      final byte[] bytes;

      private Data(Random random) {
         b = random.nextBoolean();
         i = random.nextInt();
         l = random.nextLong();
         d = random.nextDouble();
         f = random.nextFloat();
         s = random.ints('a', 'z')
               .limit(10)
               .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
               .toString();
         bytes = new byte[15];
         random.nextBytes(bytes);

      }
   }

}