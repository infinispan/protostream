package org.infinispan.protostream.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Designates the constructor or static method that is used for creating instances of a message class. Cannot be used
 * with instance methods. The method/constructor must not be private and must have a signature containing the exact same
 * number of parameters as the protobuf fields. Each parameter must correspond to a protobuf field, named identically to
 * the name of the Java field/property used for declaring the protobuf field and the type of each parameter must also be
 * identical to the type of the corresponding field/property. The ordering of parameters versus fields is not relevant
 * because they are matched by name (not by position). A single constructor or static method is allowed to have this
 * annotation in a ProtoStream marshallable class. This annotation is not inherited, occurrences in
 * superclass/superinterfaces are ignored for this class.
 *
 * <p>This annotation is meant to support creation of immutable messages. When used in a class, all protobuf fields
 * must be declared with annotations on instance fields or getter methods. Setter are not allowed to be annotated.
 *
 * <p>Classes that do not have a constructor or static method annotated with this annotation must at least have an
 * accessible no-argument constructor to be used for instantiating the class.
 *
 * <p><b>NOTE</b>
 * This annotation can be processed at <em>runtime</em> only if parameter name debug information was emitted by the
 * compiler, ie. the compiler was invoked with the <i>-parameters</i> flag. Usage of this annotation at <em>compile
 * time</em> is possible regardless of that.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProtoFactory {
}
