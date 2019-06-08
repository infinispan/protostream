package org.infinispan.protostream.annotations.impl.processor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * All generated classes will have this annotation indicating the FQNs of classes that are the origins of this generated
 * code, as an aid when rebuilding incrementally. This annotation is internal to the generator and should never be used
 * by users. Its retention policy makes it unavailable at runtime.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface OriginatingClasses {

   /**
    * Origin class FQNs.
    */
   String[] value();
}
