package org.infinispan.protostream.schema;

public interface ReservedNumbers extends Iterable<Long> {
   boolean get(long i);

   int size();

   boolean isEmpty();

   int nextSetBit(int fromIndex);

   int nextClearBit(int fromIndex);
}
