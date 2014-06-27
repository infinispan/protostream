package org.infinispan.protostream.impl;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.descriptors.FieldDescriptor;

import java.io.IOException;

/**
 * @author anistor@redhat.com
 * @since 1.0
 */
interface BaseMarshallerDelegate<T> {

   BaseMarshaller<T> getMarshaller();

   void marshall(String fieldName, FieldDescriptor fieldDescriptor, T value, ProtoStreamWriterImpl writer, CodedOutputStream out) throws IOException;

   T unmarshall(String fieldName, FieldDescriptor fieldDescriptor, ProtoStreamReaderImpl reader, CodedInputStream in) throws IOException;
}
