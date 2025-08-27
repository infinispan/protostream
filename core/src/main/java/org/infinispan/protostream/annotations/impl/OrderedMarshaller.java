package org.infinispan.protostream.annotations.impl;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An optional private annotation for specifying that a marshaller generated for a given class, usually denoted by
 * also including the {@link org.infinispan.protostream.annotations.ProtoTypeId} annotation on it, will use a condensed
 * marshaller explicitly optimized for writing and reading fields in order. This marshaller also limits the class
 * to only having up to 15 proto fields and only supports single values (no array, sequence, map etc).
 *
 * @since 6.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OrderedMarshaller {
}
