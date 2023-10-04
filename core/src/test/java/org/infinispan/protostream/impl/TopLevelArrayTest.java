package org.infinispan.protostream.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Objects;

import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.junit.Test;

/**
 * Try defining a protobuf type that wraps a primitive or message array and marshall it as top level object,
 * using both manual and annotation based marshallers.
 *
 * @author anistor@redhat.com
 */
public class TopLevelArrayTest {

   @Test
   public void testCustomIntArrayMarshalling() throws Exception {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();

      String schema = "syntax = \"proto3\";\n" +
            "package test_array_wrapper;\n" +
            "/** @TypeId(75000) */\n" +
            "message MyIntegerArray {\n" +
            "   repeated int32 value = 1;\n" +
            "}\n";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource()
            .addProtoFile("test_array_wrapper.proto", schema);
      ctx.registerProtoFiles(fileDescriptorSource);

      ctx.registerMarshaller(new MessageMarshaller<int[]>() {

         @Override
         public int[] readFrom(ProtoStreamReader reader) throws IOException {
            return reader.readInts("value");
         }

         @Override
         public void writeTo(ProtoStreamWriter writer, int[] value) throws IOException {
            writer.writeInts("value", value);
         }

         @Override
         public Class<int[]> getJavaClass() {
            return int[].class;
         }

         @Override
         public String getTypeName() {
            return "test_array_wrapper.MyIntegerArray";
         }
      });

      assertTrue(ctx.canMarshall(int[].class));

      int[] dataIn = {3, 1, 4, 1, 5};
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, dataIn);

      Object dataOut = ProtobufUtil.fromWrappedByteArray(ctx, bytes);

      assertTrue(dataOut instanceof int[]);
      assertArrayEquals(dataIn, (int[]) dataOut);
   }

   @ProtoAdapter(int[].class)
   @ProtoTypeId(75000)
   @ProtoName("MyIntegerArray")
   public static final class IntegerArrayAdapter {

      @ProtoFactory
      public int[] create(int[] value) {
         return value;
      }

      @ProtoField(1)
      public int[] getValue(int[] value) {
         return value;
      }
   }

   @ProtoTypeId(75001)
   static class MyMessage {

      @ProtoField(1)
      public String field1;

      @ProtoFactory
      public MyMessage(String field1) {
         this.field1 = field1;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         MyMessage myMessage = (MyMessage) o;
         return Objects.equals(field1, myMessage.field1);
      }

      @Override
      public int hashCode() {
         return Objects.hash(field1);
      }
   }

   @Test
   public void testCustomMessageArrayMarshalling() throws Exception {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();

      String schema = "syntax = \"proto2\";\n" +
            "package test_array_wrapper;\n" +
            "/** @TypeId(75000) */\n" +
            "message MyMessageArray {\n" +
            "   repeated MyMessage value = 1;\n" +
            "}\n" +
            "/** @TypeId(75001) */\n" +
            "message MyMessage {\n" +
            "   optional string field1 = 1;\n" +
            "}\n";

      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource()
            .addProtoFile("test_array_wrapper.proto", schema);
      ctx.registerProtoFiles(fileDescriptorSource);

      ctx.registerMarshaller(new MessageMarshaller<MyMessage[]>() {

         @Override
         public MyMessage[] readFrom(ProtoStreamReader reader) throws IOException {
            return reader.readArray("value", MyMessage.class);
         }

         @Override
         public void writeTo(ProtoStreamWriter writer, MyMessage[] value) throws IOException {
            writer.writeArray("value", value, MyMessage.class);
         }

         @Override
         public Class<MyMessage[]> getJavaClass() {
            return MyMessage[].class;
         }

         @Override
         public String getTypeName() {
            return "test_array_wrapper.MyMessageArray";
         }
      });

      ctx.registerMarshaller(new MessageMarshaller<MyMessage>() {

         @Override
         public MyMessage readFrom(ProtoStreamReader reader) throws IOException {
            String field1 = reader.readString("field1");
            return new MyMessage(field1);
         }

         @Override
         public void writeTo(ProtoStreamWriter writer, MyMessage value) throws IOException {
            writer.writeString("field1", value.field1);
         }

         @Override
         public Class<MyMessage> getJavaClass() {
            return MyMessage.class;
         }

         @Override
         public String getTypeName() {
            return "test_array_wrapper.MyMessage";
         }
      });

      assertTrue(ctx.canMarshall(MyMessage[].class));

      MyMessage[] dataIn = {new MyMessage("1"), new MyMessage("2"), new MyMessage("3")};
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, dataIn);

      Object dataOut = ProtobufUtil.fromWrappedByteArray(ctx, bytes);

      assertTrue(dataOut instanceof MyMessage[]);
      assertArrayEquals(dataIn, (MyMessage[]) dataOut);
   }

   @ProtoAdapter(MyMessage[].class)
   @ProtoTypeId(75000)
   @ProtoName("MyMessageArray")
   public static final class MyMessageArrayAdapter {

      @ProtoFactory
      public MyMessage[] create(MyMessage[] value) {
         return value;
      }

      @ProtoField(1)
      public MyMessage[] getValue(MyMessage[] value) {
         return value;
      }
   }
}
