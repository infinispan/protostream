package org.infinispan.protostream.impl;

import org.infinispan.protostream.MessageContext;
import org.infinispan.protostream.RawProtoStreamReader;

/**
 * @author anistor@redhat.com
 */
final class ReadMessageContext extends MessageContext<ReadMessageContext> {

   final RawProtoStreamReader in;

   final UnknownFieldSetImpl unknownFieldSet = new UnknownFieldSetImpl();

   final MessageMarshallerDelegate<?> marshallerDelegate;

   ReadMessageContext(ReadMessageContext parent, String fieldName, MessageMarshallerDelegate<?> marshallerDelegate, RawProtoStreamReader in) {
      super(parent, fieldName, marshallerDelegate.getMessageDescriptor());
      this.in = in;
      this.marshallerDelegate = marshallerDelegate;
   }
}
