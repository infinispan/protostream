package org.infinispan.protostream.processor.tests.testdomain;

import java.util.Objects;
import java.util.Set;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * This class tests to ensure that the marshaller generated when we don't have a 15th field as well as another field
 * greater than 15. This is because the marshaller has optimizations for field numbers 15 and less but the transition to
 * higher than 15 fields requires extra processing. See {@link MarshalledYes15GreaterThan} for the opposite when the
 * 15th field is provided.
 */
@ProtoTypeId(24)
public class MarshalledNo15GreaterThan {
   private final Set<SimpleEnum> enums;
   private final String string;

   @ProtoFactory
   public MarshalledNo15GreaterThan(Set<SimpleEnum> enums, String string) {
      this.enums = enums;
      this.string = string;
   }

   @ProtoField(14)
   public Set<SimpleEnum> getEnums() {
      return enums;
   }

   @ProtoField(18)
   public String getString() {
      return string;
   }

   @Override
   public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      MarshalledNo15GreaterThan that = (MarshalledNo15GreaterThan) o;
      return Objects.equals(enums, that.enums) && Objects.equals(string, that.string);
   }

   @Override
   public int hashCode() {
      return Objects.hash(enums, string);
   }

   @Override
   public String toString() {
      return "SimpleMarshalledObject{" +
            "enums=" + enums +
            ", string='" + string + '\'' +
            '}';
   }
}
