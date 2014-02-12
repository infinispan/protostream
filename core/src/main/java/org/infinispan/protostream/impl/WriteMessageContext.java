package org.infinispan.protostream.impl;

import com.google.protobuf.CodedOutputStream;
import org.infinispan.protostream.MessageContext;

/**
 * @author anistor@redhat.com
 */
final class WriteMessageContext extends MessageContext<WriteMessageContext> {

   final CodedOutputStream out;

   final MessageMarshallerDelegate<?> marshallerDelegate;

   WriteMessageContext(WriteMessageContext parent, String fieldName, MessageMarshallerDelegate<?> marshallerDelegate, CodedOutputStream out) {
      super(parent, fieldName, marshallerDelegate.getMessageDescriptor());
      this.out = out;
      this.marshallerDelegate = marshallerDelegate;
   }
}
