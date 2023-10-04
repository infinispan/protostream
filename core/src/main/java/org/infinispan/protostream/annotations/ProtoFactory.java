package org.infinispan.protostream.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An optional annotation that designates the constructor or static method that is used for creating instances of a
 * message class. Cannot be used with instance methods. The method or constructor must not be private and must have a
 * signature containing the exact same number of parameters as the protobuf fields of the corresponding message. Each
 * parameter must correspond to a protobuf field, named identically to the name of the Java field/property used for
 * declaring the protobuf field and the type of each parameter must also be identical to the Java type of the
 * corresponding field/property. The ordering of parameters versus fields is not relevant because they are matched by
 * name, not by position.
 *
 * <p>A <b>single</b> constructor or static method is allowed to have this annotation in a ProtoStream marshallable
 * class.
 *
 * <p>This annotation is not inherited, occurrences in superclass/superinterfaces are ignored for current class.
 *
 * <p>This annotation can be used support creation of immutable messages (that have final fields or do not have setters).
 * In that case all protobuf fields should be declared with annotations on instance fields or getter methods.
 *
 * <p>This annotation is not mandatory, but classes that do not have a constructor or static method annotated with this
 * annotation are expected to have an accessible no-argument constructor instead to be used for instantiating the class.
 *
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProtoFactory {
}
