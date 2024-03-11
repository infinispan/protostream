package org.infinispan.protostream.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a Protocol Buffers message or enum without having to annotate all fields with {@link ProtoField} or {@link ProtoEnumValue}.
 * <p>
 *   Use this annotation on records or classes with public fields to quickly generate protocol buffers messages.
 *   Fields must be public and they will be assigned incremental numbers based on the declaration order.
 *   It is possible to override the automated defaults for a field by using the {@link ProtoField} annotation.
 * </p>
 * <p>
 *   Use this annotation on Java enums to quickly generate protocol buffer enums.
 *   The enums will use the natural ordinal number of the values.
 *   It is possible to override the automated defaults for an enum value by using the {@link ProtoEnumValue} annotation.
 * </p>
 *
 * @since 5.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Proto {
}
