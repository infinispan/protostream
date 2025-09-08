package org.infinispan.protostream.processor.tests.testdomain;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * This class tests to ensure that the marshaller generated when we have a 15th field as well as another field greater
 * than 15. This is because the marshaller has optimizations for field numbers 15 and less but the transition to
 * higher than 15 fields requires extra processing. See {@link MarshalledYes15GreaterThan} for the opposite when the
 *  * 15th field is not provided.
 */
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
