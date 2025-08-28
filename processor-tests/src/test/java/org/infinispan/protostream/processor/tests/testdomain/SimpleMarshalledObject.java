package org.infinispan.protostream.processor.tests.testdomain;

import java.util.Arrays;
import java.util.Objects;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.protostream.annotations.impl.OrderedMarshaller;

@ProtoTypeId(23)
@OrderedMarshaller
public class SimpleMarshalledObject {
   private final int integer;
   private final String string;
   private final boolean bool;
   private final byte[] bytes;
   private final SimpleEnum simpleEnum;
   private final long aLong;

   @ProtoFactory
   public SimpleMarshalledObject(int integer, String string, boolean bool, byte[] bytes, SimpleEnum simpleEnum, long aLong) {
      this.integer = integer;
      this.string = string;
      this.bool = bool;
      this.bytes = bytes;
      this.simpleEnum = simpleEnum;
      this.aLong = aLong;
   }

   @ProtoField(value = 1, defaultValue = "0")
   public int getInteger() {
      return integer;
   }

   @ProtoField(8)
   public String getString() {
      return string;
   }

   @ProtoField(value = 3, defaultValue = "false")
   public boolean isBool() {
      return bool;
   }

   @ProtoField(4)
   public byte[] getBytes() {
      return bytes;
   }

   // This doesn't work.. seems like a different bug
   @ProtoField(14)
   public SimpleEnum getSimpleEnum() {
      return simpleEnum;
   }

   @ProtoField(value = 18, defaultValue = "0")
   public long getALong() {
      return aLong;
   }

   @Override
   public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      SimpleMarshalledObject that = (SimpleMarshalledObject) o;
      return integer == that.integer && bool == that.bool && Objects.equals(string, that.string) &&
            Objects.deepEquals(bytes, that.bytes)/* && simpleEnum == that.simpleEnum*/;
   }

   @Override
   public int hashCode() {
      return Objects.hash(integer, string, bool, Arrays.hashCode(bytes)/*, simpleEnum*/);
   }

   @Override
   public String toString() {
      return "SimpleMarshalledObject{" +
            "integer=" + integer +
            ", string='" + string + '\'' +
            ", bool=" + bool +
            ", bytes=" + Arrays.toString(bytes) +
//            ", simpleEnum=" + simpleEnum +
            '}';
   }
}
