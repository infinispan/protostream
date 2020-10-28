package org.infinispan.protostream.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a Protobuf enum value. This annotation can only be applied to members of a Java enum.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProtoEnumValue {

   /**
    * Alias for {@link #number()}.
    */
   int value() default 0;

   /**
    * The Protocol Buffers tag number. Alias for {@link #value()} (mutually exclusive).
    */
   int number() default 0;

   /**
    * The name of the Protobuf enum value. If missing, the Java name will be used for Protobuf too.
    */
   String name() default "";
}
