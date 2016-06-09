package org.infinispan.protostream.descriptors;

/**
 * Rules associated with a field
 *
 * @author gustavonalle
 * @since 2.0
 */
public enum Rule {
   REQUIRED,

   OPTIONAL,

   REPEATED,

   /**
    * Indicates a field that is a member of a {@code oneof} element. It is an implicitly optional and non-repeated
    * field.
    */
   ONE_OF
}
