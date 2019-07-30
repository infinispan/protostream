package org.infinispan.protostream.annotations.impl.types;

import java.lang.reflect.Modifier;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
public interface XMethod extends XExecutable {

   XClass getReturnType();

   /**
    * Determine the type argument of Optional, if the return is Optional, otherwise just return the return type.
    */
   XClass determineOptionalReturnType();

   default boolean isAbstract() {
      return Modifier.isAbstract(getModifiers());
   }
}
