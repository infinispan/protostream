package org.infinispan.protostream.processor.tests.testdomain;

import java.util.Objects;
import java.util.Set;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

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
