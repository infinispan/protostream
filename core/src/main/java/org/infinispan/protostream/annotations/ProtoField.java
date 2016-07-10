package org.infinispan.protostream.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;

import org.infinispan.protostream.descriptors.Type;

/**
 * Defines a Protocol Buffers message field. A class must have at least one field/property annotated with {@link
 * ProtoField} in order to be considered a Protocol Buffers message type.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtoField {

   /**
    * The Protocol Buffers tag number.
    */
   int number();

   Type type() default Type.MESSAGE;

   boolean required() default false;

   String name() default "";

   String defaultValue() default "";

   Class<?> javaType() default void.class;

   Class<? extends Collection> collectionImplementation() default Collection.class;
}
