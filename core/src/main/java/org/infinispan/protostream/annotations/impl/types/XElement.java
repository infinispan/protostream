package org.infinispan.protostream.annotations.impl.types;

import java.lang.annotation.Annotation;

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

   /**
    * Returns this element's annotation if present.
    */
   <A extends Annotation> A getAnnotation(Class<A> annotationClass);

   /**
    * Collect and concatenate the description text from the (multiple) {@code @ProtoDoc.value} annotations of the given
    * element.
    */
   String getDocumentation();
}
