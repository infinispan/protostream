package org.infinispan.protostream.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates the field or java-bean property of type {@link org.infinispan.protostream.UnknownFieldSet} to be used for
 * storing the unknown field set. This is an optional annotation.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtoUnknownFieldSet {
}
