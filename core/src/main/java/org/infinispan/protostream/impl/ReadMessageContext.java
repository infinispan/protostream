package org.infinispan.protostream.impl;

import com.google.protobuf.CodedInputStream;
import org.infinispan.protostream.MessageContext;

/**
 * @author anistor@redhat.com
 */
final class ReadMessageContext extends MessageContext<ReadMessageContext> {

   final CodedInputStream in;

   final UnknownFieldSetImpl unknownFieldSet = new UnknownFieldSetImpl();

   final MessageMarshallerDelegate<?> marshallerDelegate;

   ReadMessageContext(ReadMessageContext parent, String fieldName, MessageMarshallerDelegate<?> marshallerDelegate, CodedInputStream in) {
      super(parent, fieldName, marshallerDelegate.getMessageDescriptor());
      this.in = in;
      this.marshallerDelegate = marshallerDelegate;
   }
}
