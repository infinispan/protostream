package org.infinispan.protostream.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An optional annotation for specifying the a numeric type identifier for a Protobuf message or enum type. This numeric
 * identifier must be globally unique so it can be used to identify the type instead of the fully qualified name.
 * <p>
 * This Java annotations results in a protostream documentation annotation 'TypeId' being added to the generated proto
 * schema.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProtoTypeId {

   /**
    * Defines the type id of the Protobuf message or enum type. This optional annotation defines a unique integer type
    * identifier for a protobuf definition. This can be used alternatively instead of the fully qualified type name
    * during marshalling to save bandwidth. The type id must not be negative.
    * <p>
    * Values in the range 0..65535 (inclusive) are reserved for internal use by Protostream and other projects from the
    * Infinispan organisation and should not be used by application developers.
    */
   int value();
}
