package org.infinispan.protostream.annotations.impl.types;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;

/**
 * A Java Program element.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
public interface XElement {

   String getName();

   /**
    * The modifiers, as per java.lang.reflect.Modifier.
    */
   int getModifiers();

   default boolean isStatic() {
      return Modifier.isStatic(getModifiers());
   }

   default boolean isFinal() {
      return Modifier.isFinal(getModifiers());
   }

   default boolean isPublic() {
      return Modifier.isPublic(getModifiers());
   }

   default boolean isPrivate() {
      return Modifier.isPrivate(getModifiers());
   }

   /**
    * Returns this element's annotation if present.
    */
   <A extends Annotation> A getAnnotation(Class<A> annotationClass);

   <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass);

   /**
    * Collect and concatenate the description text from the (multiple) {@code @ProtoDoc.value} annotations of the
    * element.
    */
   String getDocumentation();
}
