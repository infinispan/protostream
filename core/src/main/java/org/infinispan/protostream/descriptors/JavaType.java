package org.infinispan.protostream.descriptors;

/**
 * Java mappings of the protobuf field types
 *
 * @author gustavonalle
 * @since 2.0
 */
public enum JavaType {
   INT {
      @Override
      Object fromString(String input) {
         return Integer.valueOf(input);
      }
   },
   LONG {
      @Override
      Object fromString(String input) {
         return Long.valueOf(input);
      }
   },
   FLOAT {
      @Override
      Object fromString(String input) {
         return Float.valueOf(input);
      }
   },
   DOUBLE {
      @Override
      Object fromString(String input) {
         return Double.valueOf(input);
      }
   },
   BOOLEAN {
      @Override
      Object fromString(String input) {
         return Boolean.valueOf(input);
      }
   },
   STRING {
      @Override
      Object fromString(String input) {
         return input;
      }
   },
   BYTE_STRING {
      @Override
      Object fromString(String input) {
         return null;
      }
   },
   ENUM {
      @Override
      Object fromString(String input) {
         return null;
      }
   },
   MESSAGE {
      @Override
      Object fromString(String input) {
         return null;
      }
   };

   /**
    * Returns the correspondent java type
    *
    * @param input Input text
    * @return an Object with the correct java type or null in case of composite value
    */
   abstract Object fromString(String input);

   boolean isScalar() {
      return !this.equals(MESSAGE) && !this.equals(ENUM) && !this.equals(BYTE_STRING);
   }

}
