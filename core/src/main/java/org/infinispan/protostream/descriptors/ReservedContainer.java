package org.infinispan.protostream.descriptors;

public interface ReservedContainer<T> {
   T addReserved(int number);

   T addReserved(int from, int to);

   T addReserved(String name);
}
