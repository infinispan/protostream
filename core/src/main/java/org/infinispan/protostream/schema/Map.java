package org.infinispan.protostream.schema;

/**
 * @since 5.0
 */
public class Map extends Field {
   private final Type valueType;

   Map(Builder builder) {
      super(builder);
      this.valueType = builder.valueType;
   }

   public Type getValueType() {
      return valueType;
   }

   public static class Builder extends Field.Builder {

      private final Type valueType;

      Builder(FieldContainer parent, Type keyType, Type valueType, String name, int number) {
         super(parent, keyType, name, number, true);
         this.valueType = valueType;
      }

      @Override
      protected Map create() {
         return new Map(this);
      }
   }
}
