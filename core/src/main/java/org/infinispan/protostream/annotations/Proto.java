package org.infinispan.protostream.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a Protocol Buffers message without having to annotate all fields with {@link ProtoField}.
 * Use this annotation to quickly generate messages from records or classes with public fields.
 * Fields must be public and they will be assigned incremental numbers based on the declaration order.
 * It is possible to override the automated defaults for a field by using the {@link ProtoField} annotation.
 *
 * @since 5.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Proto {
}
