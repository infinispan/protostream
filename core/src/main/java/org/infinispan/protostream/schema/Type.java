package org.infinispan.protostream.schema;

import java.util.Locale;
import java.util.Objects;

/**
 * @since 5.0
 */
public interface Type {
   static Type create(String type) {
      try {
         return Type.Scalar.valueOf(type.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
         return new Type.Named(Objects.requireNonNull(type));
      }
   }

   static Type.MapType create(Type keyType, Type valueType) {
      return new Type.MapType(Objects.requireNonNull(keyType, "keyType"), Objects.requireNonNull(valueType, "valueType"));
   }

   Type.Kind kind();

   enum Kind {
      SCALAR,
      MAP,
      NAMED
   }

   enum Scalar implements Type {
      ANY,
      BOOL,
      BYTES,
      DOUBLE,
      FLOAT,
      FIXED32,
      FIXED64,
      INT32,
      INT64,
      SFIXED32,
      SFIXED64,
      SINT32,
      SINT64,
      STRING,
      UINT32,
      UINT64;

      @Override
      public Kind kind() {
         return Kind.SCALAR;
      }

      @Override
      public String toString() {
         return name().toLowerCase(Locale.US);
      }
   }

   final class MapType implements Type {

      private final Type keyType;
      private final Type valueType;

      private MapType(Type keyType, Type valueType) {
         this.keyType = keyType;
         this.valueType = valueType;
      }

      @Override
      public Kind kind() {
         return Kind.MAP;
      }

      public Type keyType() {
         return keyType;
      }

      public Type valueType() {
         return valueType;
      }
   }

   final class Named implements Type {
      private final String name;

      private Named(String name) {
         this.name = name;
      }

      public String name() {
         return name;
      }

      @Override
      public Kind kind() {
         return Kind.NAMED;
      }

      @Override
      public String toString() {
         return name;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         Named namedType = (Named) o;
         return Objects.equals(name, namedType.name);
      }

      @Override
      public int hashCode() {
         return Objects.hash(name);
      }
   }
}
