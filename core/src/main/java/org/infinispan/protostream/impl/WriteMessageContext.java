package org.infinispan.protostream.impl;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Descriptors;

import java.util.HashSet;
import java.util.Set;

/**
 * @author anistor@redhat.com
 */
final class WriteMessageContext extends MessageContext<WriteMessageContext> {

   final CodedOutputStream out;

   final Set<Descriptors.FieldDescriptor> writtenFields;

   WriteMessageContext(WriteMessageContext parent, Descriptors.Descriptor messageDescriptor, CodedOutputStream out) {
      super(parent, messageDescriptor);
      this.out = out;
      writtenFields = new HashSet<Descriptors.FieldDescriptor>(fieldDescriptors.size());
   }
}
