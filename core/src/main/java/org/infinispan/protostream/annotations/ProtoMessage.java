package org.infinispan.protostream.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a Protobuf message type. This annotation is optional.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProtoMessage {

   /**
    * Defines the name of the Protobuf message type. If missing, the Java name will be used for Protobuf too.
    */
   String name() default "";
}
