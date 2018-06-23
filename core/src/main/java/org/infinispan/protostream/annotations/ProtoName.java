package org.infinispan.protostream.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An optional annotation for specifying the Protobuf message or enum type name. If missing, the Java class name will be
 * used for Protobuf too.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProtoName {

   /**
    * Defines the name of the Protobuf message type.
    */
   String value();
}
