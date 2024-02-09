package org.infinispan.protostream.descriptors;

public interface MessageContainer<T> {
   T addMessage(Descriptor.Builder message);

   String getFullName();
}
