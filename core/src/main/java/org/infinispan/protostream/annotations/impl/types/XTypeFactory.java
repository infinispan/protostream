package org.infinispan.protostream.annotations.impl.types;

import javax.lang.model.type.TypeMirror;

/**
 * Factory for XClass implementations based on a given java.lang.Class or a javax.lang.model.type.TypeMirror. The
 * factory must ensure it returns the exact same XClass instance for multiple calls with the same argument, or an
 * argument that represents the same type, either in the form of a java.lang.Class or a
 * javax.lang.model.type.TypeMirror. This ensures that produced XClass instances can be safely compared by reference
 * equality.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
public interface XTypeFactory {

   /**
    * Wraps the given Class in an XClass.
    *
    * @param clazz can be null
    * @return the wrapper XClass, or null iff the actual argument is null
    */
   XClass fromClass(Class<?> clazz);

   /**
    * Wraps the given TypeMirror in an XClass.
    *
    * @param typeMirror can be null
    * @return the wrapper XClass, or null iff the actual argument is null
    */
   XClass fromTypeMirror(TypeMirror typeMirror);
}
