package org.infinispan.protostream.impl;

import org.infinispan.protostream.MessageContext;
import org.infinispan.protostream.RawProtoStreamWriter;

/**
 * @author anistor@redhat.com
 */
final class WriteMessageContext extends MessageContext<WriteMessageContext> {

   final RawProtoStreamWriter out;

   final MessageMarshallerDelegate<?> marshallerDelegate;

   WriteMessageContext(WriteMessageContext parent, String fieldName, MessageMarshallerDelegate<?> marshallerDelegate, RawProtoStreamWriter out) {
      super(parent, fieldName, marshallerDelegate.getMessageDescriptor());
      this.out = out;
      this.marshallerDelegate = marshallerDelegate;
   }
}
