package org.infinispan.protostream.schema;

public interface ReservedNumbers {
   boolean get(long i);

   int size();

   boolean isEmpty();

   int nextSetBit(int fromIndex);

   int nextClearBit(int fromIndex);
}
