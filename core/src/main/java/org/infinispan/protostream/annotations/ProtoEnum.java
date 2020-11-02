package org.infinispan.protostream.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An optional annotation for specifying the Protobuf enum type name.
 *
 * @author anistor@redhat.com
 * @since 3.0
 * @deprecated replaced by {@link ProtoName}. To be removed in version 5.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Deprecated
public @interface ProtoEnum {

   /**
    * Defines the name of the Protobuf enum type. Must not be fully qualified (ie. no dots allowed).
    * If missing, the Java class name ({@link Class#getSimpleName()}) will be used for Protobuf too.
    */
   String name() default "";
}
