package org.infinispan.protostream.descriptors;

public interface EnumContainer<T> {
   T addEnum(EnumDescriptor.Builder enumDescriptor);
}
