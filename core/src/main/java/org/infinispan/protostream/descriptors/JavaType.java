package org.infinispan.protostream.descriptors;

/**
 * Java mappings of the Protobuf field types.
 *
 * @author gustavonalle
 * @author anistor@redhat.com
 * @since 2.0
 */
public enum JavaType {
   INT {
      @Override
      Object fromString(String input) {
         return Integer.valueOf(input);
      }

      @Override
      public String defaultValueAsString() {
         return "0";
      }
   },
   LONG {
      @Override
      Object fromString(String input) {
         return Long.valueOf(input);
      }

      @Override
      public String defaultValueAsString() {
         return "0";
      }
   },
   FLOAT {
      @Override
      Object fromString(String input) {
         return Float.valueOf(input);
      }

      @Override
      public String defaultValueAsString() {
         return "0f";
      }
   },
   DOUBLE {
      @Override
      Object fromString(String input) {
         return Double.valueOf(input);
      }

      @Override
      public String defaultValueAsString() {
         return "0d";
      }
   },
   BOOLEAN {
      @Override
      Object fromString(String input) {
         return Boolean.valueOf(input);
      }

      @Override
      public String defaultValueAsString() {
         return "false";
      }
   },
   STRING {
      @Override
      Object fromString(String input) {
         return input;
      }

      @Override
      public String defaultValueAsString() {
         return "\"\"";
      }
   },
   BYTE_STRING {
      @Override
      Object fromString(String input) {
         return null;
      }

      @Override
      public String defaultValueAsString() {
         return "[]";
      }
   },
   ENUM {
      @Override
      Object fromString(String input) {
         return null;
      }

      @Override
      public String defaultValueAsString() {
         return "null"; //FIXME
      }
   },
   MESSAGE {
      @Override
      Object fromString(String input) {
         return null;
      }

      @Override
      public String defaultValueAsString() {
         return "null";
      }
   },

   MAP {
      @Override
      Object fromString(String input) {
         return null;
      }

      @Override
      public String defaultValueAsString() {
         return "null";
      }
   };

   /**
    * Returns the correspondent java type
    *
    * @param input Input text
    * @return an Object with the correct java type or null in case of composite value
    */
   abstract Object fromString(String input);

   /**
    * Returns the default value as a string according to the Protobuf 3 spec
    * @return a String representing the default value
    */
    public abstract String defaultValueAsString();
}
