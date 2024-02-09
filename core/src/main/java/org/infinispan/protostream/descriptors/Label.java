package org.infinispan.protostream.descriptors;

/**
 * Rules associated with a field
 *
 * @author gustavonalle
 * @since 2.0
 */
public enum Label {

   OPTIONAL,

   REPEATED,

   REQUIRED,

   /**
    * Indicates a field that is a member of a {@code oneof} element. It is an implicitly optional and non-repeated
    * field.
    */
   ONE_OF;

   public static Label fromString(String label, FileDescriptor.Syntax syntax) {
      switch (label) {
         case "repeated":
            return REPEATED;
         case "optional":
            return OPTIONAL;
         case "oneof":
            return ONE_OF;
         case "required": {
            if (syntax != FileDescriptor.Syntax.PROTO2) throw new IllegalArgumentException("'required' fields are not allowed with syntax " + syntax);
            else return REQUIRED;
         }
         default:
            throw new IllegalArgumentException(label);
      }
   }
}
