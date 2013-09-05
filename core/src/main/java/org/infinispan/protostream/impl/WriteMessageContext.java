package org.infinispan.protostream.impl;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Descriptors;
import org.infinispan.protostream.MessageContext;

/**
 * @author anistor@redhat.com
 */
final class WriteMessageContext extends MessageContext<WriteMessageContext> {

   final CodedOutputStream out;

   WriteMessageContext(WriteMessageContext parent, String fieldName, Descriptors.Descriptor messageDescriptor, CodedOutputStream out) {
      super(parent, fieldName, messageDescriptor);
      this.out = out;
   }
}
