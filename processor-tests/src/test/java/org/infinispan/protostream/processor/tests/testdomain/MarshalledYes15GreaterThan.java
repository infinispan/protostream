package org.infinispan.protostream.processor.tests.testdomain;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

@ProtoTypeId(23)
public class MarshalledYes15GreaterThan {
   private final double[] doubles;
   private final int integer;
   private final String string;

   @ProtoFactory
   public MarshalledYes15GreaterThan(double[] doubles, int integer, String string) {
      this.doubles = doubles;
      this.integer = integer;
      this.string = string;
   }

   @ProtoField(4)
   public double[] getDoubles() {
      return doubles;
   }

   @ProtoField(value = 15, defaultValue = "0")
   public int getInteger() {
      return integer;
   }

   @ProtoField(18)
   public String getString() {
      return string;
   }

   @Override
   public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      MarshalledYes15GreaterThan that = (MarshalledYes15GreaterThan) o;
      return integer == that.integer && Objects.deepEquals(doubles, that.doubles) && Objects.equals(string, that.string);
   }

   @Override
   public int hashCode() {
      return Objects.hash(Arrays.hashCode(doubles), integer, string);
   }

   @Override
   public String toString() {
      return "MarshalledYes15GreaterThan{" +
            "doubles=" + Arrays.toString(doubles) +
            ", integer=" + integer +
            ", string='" + string + '\'' +
            '}';
   }
}
