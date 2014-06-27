package org.infinispan.protostream.descriptors;

/**
 * Represents any option in a proto file
 *
 * @author gustavonalle
 * @since 2.0
 */
public final class Option {

   private final String name;
   private final Object value;

   public Option(String name, Object value) {
      this.name = name;
      this.value = value;
   }

   public String getName() {
      return name;
   }

   public Object getValue() {
      return value;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Option option = (Option) o;

      if (!name.equals(option.name)) return false;
      if (!value.equals(option.value)) return false;

      return true;
   }

   @Override
   public int hashCode() {
      int result = name.hashCode();
      result = 31 * result + value.hashCode();
      return result;
   }
}
