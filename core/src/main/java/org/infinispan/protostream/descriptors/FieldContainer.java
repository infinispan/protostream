package org.infinispan.protostream.descriptors;

public interface FieldContainer<T> {
   T addField(FieldDescriptor.Builder field);
}
