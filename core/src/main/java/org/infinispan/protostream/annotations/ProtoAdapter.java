package org.infinispan.protostream.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// [anistor] TODO We also need a similar mechanism for enums.

/**
 * A marshalling adapter for another class that cannot be annotated. This class will handle marshalling for it by
 * defining its schema via annotations.
 * <p>
 * The class bearing this annotation will have an annotated factory method for the marshalled class and annotated
 * accessor methods for each field. These methods can be instance or static methods and their first argument must be
 * the marshalled class. Annotations must be placed only on methods. Direct field annotation is not allowed.
 * <p>
 * This class must be thread-safe and stateless and must have an accessible no argument constructor. A single instance
 * of it will be created per SerializationContext.
 *
 * @author anistor@redhat.com
 * @since 4.4
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProtoAdapter {

   /**
    * The actual class being marshalled.
    */
   Class<?> value();
}
