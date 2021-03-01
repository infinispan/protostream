package org.infinispan.protostream;

/**
 * This is the base interface of the marshaller hierarchy, exposing common methods for identifying the Java and Protobuf
 * types handled by this marshaller instance. A marshaller handles a single type pair. The marshaller implementation
 * must be stateless and thread-safe.
 *
 * @author anistor@redhat.com
 * @since 1.0
 */
public interface BaseMarshaller<T> {

   /**
    * Returns the Java type handled by this marshaller. This must not change over multiple invocations.
    *
    * @return the Java type.
    */
   Class<? extends T> getJavaClass();

   /**
    * Returns the full name of the message or enum type, defined in a proto file. This must not change over multiple
    * invocations.
    *
    * @return the full name of the message or enum type, defined in a proto file.
    */
   String getTypeName();
}
