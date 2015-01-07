package org.infinispan.protostream.impl;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.descriptors.FieldDescriptor;

import java.io.IOException;

/**
 * @author anistor@redhat.com
 * @since 1.0
 */
interface BaseMarshallerDelegate<T> {

   BaseMarshaller<T> getMarshaller();

   void marshall(String fieldName, FieldDescriptor fieldDescriptor, T value, ProtoStreamWriterImpl writer, RawProtoStreamWriter out) throws IOException;

   T unmarshall(String fieldName, FieldDescriptor fieldDescriptor, ProtoStreamReaderImpl reader, RawProtoStreamReader in) throws IOException;
}
