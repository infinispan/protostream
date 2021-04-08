package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@AutoProtoSchemaBuilder(
      schemaFileName = "wrappers.proto",
      schemaFilePath = "/protostream/google/protobuf",
      schemaPackageName = "google.protobuf",
      includeClasses = {
            WrappersSchema.DoubleValue.class,
            WrappersSchema.FloatValue.class,
            WrappersSchema.Int64Value.class,
            WrappersSchema.UInt64Value.class,
            WrappersSchema.Int32Value.class,
            WrappersSchema.UInt32Value.class,
            WrappersSchema.BoolValue.class,
            WrappersSchema.StringValue.class,
            WrappersSchema.BytesValue.class
      }
)
public interface WrappersSchema extends GeneratedSchema {

   final class StringValue {

      private final String value;

      @ProtoFactory
      public StringValue(String value) {
         this.value = value;
      }

      @ProtoField(value = 1)
      public String getValue() {
         return value;
      }
   }

   final class BoolValue {

      private final boolean value;

      @ProtoFactory
      public BoolValue(boolean value) {
         this.value = value;
      }

      @ProtoField(value = 1, defaultValue = "false")
      public boolean getValue() {
         return value;
      }
   }

   final class BytesValue {

      private final byte[] value;

      @ProtoFactory
      public BytesValue(byte[] value) {
         this.value = value;
      }

      @ProtoField(value = 1)
      public byte[] getValue() {
         return value;
      }
   }

   final class DoubleValue {

      private final double value;

      @ProtoFactory
      public DoubleValue(double value) {
         this.value = value;
      }

      @ProtoField(value = 1, defaultValue = "0")
      public double getValue() {
         return value;
      }
   }

   final class FloatValue {

      private final float value;

      @ProtoFactory
      public FloatValue(float value) {
         this.value = value;
      }

      @ProtoField(value = 1, defaultValue = "0")
      public float getValue() {
         return value;
      }
   }

   final class Int32Value {

      private final int value;

      @ProtoFactory
      public Int32Value(int value) {
         this.value = value;
      }

      @ProtoField(value = 1, defaultValue = "0")
      public int getValue() {
         return value;
      }
   }

   final class UInt32Value {

      private final int value;

      @ProtoFactory
      public UInt32Value(int value) {
         this.value = value;
      }

      @ProtoField(value = 1, defaultValue = "0")
      public int getValue() {
         return value;
      }
   }

   final class Int64Value {

      private final long value;

      @ProtoFactory
      public Int64Value(long value) {
         this.value = value;
      }

      @ProtoField(value = 1, defaultValue = "0")
      public long getValue() {
         return value;
      }
   }

   final class UInt64Value {

      private final long value;

      @ProtoFactory
      public UInt64Value(long value) {
         this.value = value;
      }

      @ProtoField(value = 1, defaultValue = "0")
      public long getValue() {
         return value;
      }
   }
}
