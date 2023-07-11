package org.infinispan.protostream.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Aggregates repeated {@link ProtoDoc} annotations.
 *
 * @author anistor@redhat.com
 * @since 4.0
 * @deprecated For ProtoStream documentation comment annotations, annotate directly the {@link ProtoField}s with the annotations to add.
 *  *          For the general text case, there is no replacement.
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Deprecated
public @interface ProtoDocs {

   ProtoDoc[] value();
}
