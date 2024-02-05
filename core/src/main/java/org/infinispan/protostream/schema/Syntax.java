package org.infinispan.protostream.schema;

/**
 * @since 5.0
 */
public enum Syntax {
   PROTO2,
   PROTO3;

   public static Syntax fromString(String syntax) {
      return Syntax.valueOf(syntax.toUpperCase());
   }


   @Override
   public String toString() {
      return name().toLowerCase();
   }
}