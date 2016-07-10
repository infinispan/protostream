package org.infinispan.protostream.impl;

import java.io.IOException;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.descriptors.FieldDescriptor;

/**
 * The marshallers (descendants of {@link BaseMarshaller}) do not have a uniform interface, so an extra layer of
 * indirection is used to provide uniformity. A delegate object wraps the real marshaller and delegates actual
 * marshalling to it, taking into account the specific interface differences for each kind of marshaller
 * (EnumMarshaller, MessageMarshaller, RawProtobufMarshaller).
 *
 * @author anistor@redhat.com
 * @since 1.0
 */
public interface BaseMarshallerDelegate<T> {

   /**
    * Gets the wrapped marshaller.
    *
    * @return the wrapped marshaller instance
    */
   BaseMarshaller<T> getMarshaller();

   /**
    * Marshalls an object.
    *
    * @param fieldDescriptor the {@code FieldDescriptor} of the field being marshalled or {@code null} if this is a
    *                        top-level object
    * @param value           the value being marshalled (cannot be {@code null})
    * @param writer          the {@link ProtoStreamWriterImpl} instance to use/re-use, if the specific marshaller type
    *                        needs one; can be {@code null} in which case the delegate has to create a {@link
    *                        ProtoStreamWriterImpl} instance itself based on the {@code out} parameter
    * @param out             the Protobuf tag output stream (cannot be {@code null})
    * @throws IOException if marshalling fails for some reason
    */
   void marshall(FieldDescriptor fieldDescriptor, T value, ProtoStreamWriterImpl writer, RawProtoStreamWriter out) throws IOException;

   /**
    * Unmarshalls an object.
    *
    * @param fieldDescriptor the {@code FieldDescriptor} of the field being unmarshalled or {@code null} if this is a
    *                        top-level object
    * @param reader          the {@link ProtoStreamReaderImpl} instance to use/re-use, if the specific marshaller type
    *                        needs one; can be {@code null} in which case the delegate has to create a {@link
    *                        ProtoStreamReaderImpl} instance itself based on the {@code in} parameter
    * @param in              the Protobuf tag input stream (cannot be {@code null})
    * @throws IOException if unmarshalling fails for some reason
    */
   T unmarshall(FieldDescriptor fieldDescriptor, ProtoStreamReaderImpl reader, RawProtoStreamReader in) throws IOException;
}
