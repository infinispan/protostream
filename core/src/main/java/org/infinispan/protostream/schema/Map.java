package org.infinispan.protostream.schema;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @since 5.0
 */
public class Map extends Field {
   private final Type valueType;

   Map(Builder builder, AtomicInteger autoNumber) {
      super(builder, autoNumber);
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
      protected Map create(AtomicInteger autoNumber) {
         return new Map(this, autoNumber);
      }
   }
}
