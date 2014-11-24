package org.infinispan.protostream;

/**
 * An interface to be implemented by marshaller objects of type {@link MessageMarshaller}) that are able to handle
 * unknown fields by storing them into an {@link UnknownFieldSet}.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
public interface UnknownFieldSetHandler<T> {

   UnknownFieldSet getUnknownFieldSet(T message);

   void setUnknownFieldSet(T message, UnknownFieldSet unknownFieldSet);
}
