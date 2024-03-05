package org.infinispan.protostream.integrationtests.compliance.handwritten;

import java.util.Objects;

public class CustomValue {
   int id;
   String s;

   public CustomValue(int id, String s) {
      this.id = id;
      this.s = s;
   }

   @Override
   public String toString() {
      return "CustomValue{" +
            "id=" + id +
            ", s='" + s + '\'' +
            '}';
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      CustomValue that = (CustomValue) o;
      return id == that.id && Objects.equals(s, that.s);
   }

   @Override
   public int hashCode() {
      return Objects.hash(id, s);
   }
}
