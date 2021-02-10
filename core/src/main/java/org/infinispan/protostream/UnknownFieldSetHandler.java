package org.infinispan.protostream;

/**
 * An interface to be implemented by marshaller objects of type {@link MessageMarshaller}) that are able to handle
 * unknown fields by storing them into an {@link UnknownFieldSet}. {@link RawProtobufMarshaller} are expected to handle
 * unknown fields manually and cannot benefit from automatic handling by implementing this interface.
 *
 * @author anistor@redhat.com
 * @since 3.0
 * @deprecated since 4.3.1. This will be removed together with {@link MessageMarshaller}
 */
@Deprecated
public interface UnknownFieldSetHandler<T> {

   /**
    * Extract the {@link UnknownFieldSet} that was previously attached to a message during unmarshalling.
    *
    * @return the UnknownFieldSet or {@code null}
    */
   UnknownFieldSet getUnknownFieldSet(T message);

   /**
    * Attach a non-empty {@link UnknownFieldSet} to a message that was newly unmarshalled.
    */
   void setUnknownFieldSet(T message, UnknownFieldSet unknownFieldSet);
}
