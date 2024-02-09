package org.infinispan.protostream.descriptors;

import java.util.Locale;
import java.util.Objects;

/**
 * @since 15.0
 **/
public interface DataType {

   static DataType create(String type) {
      try {
         return ScalarType.valueOf(type.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
         return new NamedType(Objects.requireNonNull(type));
      }
   }

   static MapType create(DataType keyType, DataType valueType) {
      return new MapType(Objects.requireNonNull(keyType, "keyType"), Objects.requireNonNull(valueType, "valueType"));
   }

   Kind kind();

   enum Kind {
      SCALAR,
      MAP,
      NAMED
   }

   enum ScalarType implements DataType {
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

   final class MapType implements DataType {

      private final DataType keyType;
      private final DataType valueType;

      private MapType(DataType keyType, DataType valueType) {
         this.keyType = keyType;
         this.valueType = valueType;
      }

      @Override
      public Kind kind() {
         return Kind.MAP;
      }

      public DataType keyType() {
         return keyType;
      }

      public DataType valueType() {
         return valueType;
      }
   }

   final class NamedType implements DataType {
      private final String name;

      private NamedType(String name) {
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
         NamedType namedType = (NamedType) o;
         return Objects.equals(name, namedType.name);
      }

      @Override
      public int hashCode() {
         return Objects.hash(name);
      }
   }
}
