package org.infinispan.protostream.annotations.impl.types;

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
    * Wraps the given Class in an XClass. Implementation must be idempotent. The returned value must be
    * reference-identical.
    *
    * @param clazz can be null
    * @return the wrapper XClass, or null iff the actual argument is null
    */
   XClass fromClass(Class<?> clazz);
}
