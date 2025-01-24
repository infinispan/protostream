package org.infinispan.protostream.annotations.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * All generated classes will have this annotation indicating the FQNs of classes that are the origins of this generated
 * code, as an aid when recompiling incrementally. This annotation is internal to the generator and should never be used
 * by users. Its retention policy makes it unavailable at runtime.
 *
 * @author anistor@redhat.com
 * @since 4.3.5
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface OriginatingClasses {

   /**
    * Origin classes. Do not use {@link javax.lang.model.element.Element#getAnnotation(Class)} to access this.
    * Use {@link javax.lang.model.element.Element#getAnnotationMirrors()} instead, to avoid resolution of possibly
    * no longer existent classes during incremental compilation and prevent issues with javac failing miserably with
    * a completely unrelated ClassCastException in such cases.
    */
   @SuppressWarnings("unused")
   Class<?>[] value();
}
