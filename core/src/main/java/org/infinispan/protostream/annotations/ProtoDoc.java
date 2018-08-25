package org.infinispan.protostream.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The documentation text of the generated message type, enum type or field. You can put here human readable text and
 * also ProtoStream documentation comment annotations. This annotation can be repeated and all the text will be
 * collected together in the order of appearance.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(ProtoDocs.class)
public @interface ProtoDoc {

   /**
    * The documentation text (human readable and also ProtoStream documentation comment annotations).
    */
   String value();
}
