package org.infinispan.protostream.impl;

import java.io.IOException;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.ProtobufTagMarshaller;
import org.infinispan.protostream.descriptors.FieldDescriptor;

/**
 * The marshallers (descendants of {@link BaseMarshaller}) do not have a uniform interface, so an extra layer of
 * indirection is used to provide uniformity. A delegate object wraps the real marshaller and delegates actual
 * marshalling to it, taking into account the specific interface differences for each kind of marshaller
 * (EnumMarshaller, MessageMarshaller, ProtoStreamMarshaller).
 *
 * @author anistor@redhat.com
 * @since 1.0
 */
public abstract class BaseMarshallerDelegate<T> {

   /**
    * Gets the wrapped marshaller.
    *
    * @return the wrapped marshaller instance
    */
   public abstract BaseMarshaller<T> getMarshaller();

   /**
    * Marshalls an object.
    *
    * @param ctx             operation context
    * @param fieldDescriptor the {@code FieldDescriptor} of the field being marshalled or {@code null} if this is a
    *                        top-level object
    * @param value           the value being marshalled (cannot be {@code null})
    * @throws IOException if marshalling fails for some reason
    */
   public abstract void marshall(ProtobufTagMarshaller.WriteContext ctx, FieldDescriptor fieldDescriptor, T value) throws IOException;

   /**
    * Unmarshalls an object.
    *
    * @param ctx             operation context
    * @param fieldDescriptor the {@code FieldDescriptor} of the field being unmarshalled or {@code null} if this is a
    *                        top-level object
    * @throws IOException if unmarshalling fails for some reason
    */
   public abstract T unmarshall(ProtobufTagMarshaller.ReadContext ctx, FieldDescriptor fieldDescriptor) throws IOException;
}
