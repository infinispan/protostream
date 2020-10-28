package org.infinispan.protostream.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marshalling adapter for a target class or enum that cannot be annotated for various reasons. This class will handle
 * the schema definition and marshalling for the target by declaring a protobuf schema via annotations.
 * <br><b>This class will not become marshallable! It is the target class that becomes marshallable.</b>
 * <p>
 * <em>When used on classes:</em><br>
 * The class bearing this annotation will have an annotated factory method for the marshalled class and annotated
 * accessor methods for each field. These methods can be instance or static methods and their first argument must be
 * the marshalled class. Annotations must be placed only on methods. Direct field annotations are not allowed.
 * <p>
 * This class must be thread-safe and stateless and must have an accessible no argument constructor. A single instance
 * of it will be created per {@link org.infinispan.protostream.SerializationContext}.
 * <p>
 * <em>When used on enums:</em><br>
 * For each enum value in this enum there must exist en identically named enum value in the target enum.
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
