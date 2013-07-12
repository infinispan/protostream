package org.infinispan.protostream.impl;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.Descriptors;

import java.util.HashSet;
import java.util.Set;

/**
 * @author anistor@redhat.com
 */
final class ReadMessageContext extends MessageContext<ReadMessageContext> {

   final CodedInputStream in;

   final Set<Descriptors.FieldDescriptor> readFields;

   final UnknownFieldSetImpl unknownFieldSet = new UnknownFieldSetImpl();

   ReadMessageContext(ReadMessageContext parent, Descriptors.Descriptor messageDescriptor, CodedInputStream in) {
      super(parent, messageDescriptor);
      this.in = in;
      readFields = new HashSet<Descriptors.FieldDescriptor>(fieldDescriptors.size());
   }
}
