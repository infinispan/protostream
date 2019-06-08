package org.infinispan.protostream.annotations.impl.types;

import java.lang.reflect.Modifier;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
public interface XMethod extends XExecutable {

   XClass getReturnType();

   default boolean isAbstract() {
      return Modifier.isAbstract(getModifiers());
   }
}
