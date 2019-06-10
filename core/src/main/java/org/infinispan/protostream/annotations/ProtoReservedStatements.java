package org.infinispan.protostream.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Aggregates multiple {@link ProtoReserved} annotations.
 * <p>
 * This annotation is not explicitly marked {@link java.lang.annotation.Inherited} but annotation processors will scan
 * for occurrences of this annotation in all superclasses and superinterfaces (recursively).
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProtoReservedStatements {

   ProtoReserved[] value();
}
