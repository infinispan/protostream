package org.infinispan.protostream.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An optional annotation for specifying the Protobuf message type name.
 *
 * @author anistor@redhat.com
 * @since 3.0
 * @deprecated replaced by {@link ProtoName}. To be removed in version 5.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProtoMessage {

   /**
    * Defines the name of the Protobuf message type. Must not be fully qualified. If missing, the Java class name
    * ({@link Class#getSimpleName()}) will be used for Protobuf too.
    */
   String name() default "";
}
