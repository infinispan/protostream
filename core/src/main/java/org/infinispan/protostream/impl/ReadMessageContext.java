package org.infinispan.protostream.impl;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.Descriptors;
import org.infinispan.protostream.MessageContext;

/**
 * @author anistor@redhat.com
 */
final class ReadMessageContext extends MessageContext<ReadMessageContext> {

   final CodedInputStream in;

   final UnknownFieldSetImpl unknownFieldSet = new UnknownFieldSetImpl();

   ReadMessageContext(ReadMessageContext parent, String fieldName, Descriptors.Descriptor messageDescriptor, CodedInputStream in) {
      super(parent, fieldName, messageDescriptor);
      this.in = in;
   }
}
