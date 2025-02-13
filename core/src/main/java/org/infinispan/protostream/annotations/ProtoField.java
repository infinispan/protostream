package org.infinispan.protostream.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Map;

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
@Documented
public @interface ProtoField {

   /**
    * Alias for {@link #number}.
    */
   int value() default 0;

   /**
    * The Protocol Buffers tag number.
    */
   int number() default 0;

   /**
    * The Protobuf type of the field. If not specified, then the field type will be inferred based on the Java property
    * type.
    */
   Type type() default Type.MESSAGE;

   /**
    * Marks the field as required. This is valid only when using the Protocol Buffers 2 syntax.
    * @deprecated avoid using this
    */
   @Deprecated(since = "5.0", forRemoval = true)
   boolean required() default false;

   /**
    * The name of the field. If not specified, then the name of the Java property is used instead.
    */
   String name() default "";

   /**
    * The default value to assign to this field if it is found <em>missing</em> in the Protobuf data stream <em>during
    * reading</em>.
    * <p>
    * The value is given in the form of a string that must obey correct syntax (as defined by Protobuf spec.) in order
    * to be parsed into the actual value which may be of a different type than string.
    * <p>
    * This value has no significance during writing of a {@code null} field, ie. it does not get written into the data
    * stream as a substitute for the missing value. It is expected that the reader of this Protobuf stream will do that
    * when reading it back. This is valid only when using the Protocol Buffers 2 syntax.
    */
   String defaultValue() default "";

   /**
    * The actual concrete and instantiable Java type. This Class should be assignable to the property type. It should be
    * used when the type is an interface or abstract class in order to designate the actual concrete class that must be
    * instantiated by the marshaling layer when reading this from a data stream.
    */
   Class<?> javaType() default void.class;

   /**
    * The {@link java.util.Collection} to be utilised when unmarshalling the field from a data stream. This Class should
    * only be specified if the Java property type is a {@link java.util.Collection}, {@link java.lang.Iterable} or
    * {@link java.util.stream.Stream} and must be assignable to the property type. It should be used when
    * the field's type an interface or an abstract class in order to designate the actual concrete class
    * that must be instantiated by the marshaling layer.
    */
   Class<? extends Collection> collectionImplementation() default Collection.class;

   /**
    * The actual concrete and instantiable implementation type of the Map. This Class should only be specified if
    * the Java property type is really a Map and must be assignable to the property type. It should be used when
    * the type of the Map is an interface or an abstract class in order to designate the actual concrete class
    * that must be instantiated by the marshaling layer when reading this from a data stream.
    */
   Class<? extends Map> mapImplementation() default Map.class;

   String oneof() default "";
}
