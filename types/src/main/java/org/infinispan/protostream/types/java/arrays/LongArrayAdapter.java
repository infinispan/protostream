package org.infinispan.protostream.types.java.arrays;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.containers.IndexedElementContainerAdapter;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@ProtoAdapter(long[].class)
@ProtoName("LongArray")
public final class LongArrayAdapter implements IndexedElementContainerAdapter<long[], Long> {

   @ProtoFactory
   public long[] create(int size) {
      return new long[size];
   }

   @Override
   public int getNumElements(long[] array) {
      return array.length;
   }

   @Override
   public Long getElement(long[] array, int index) {
      return array[index];
   }

   @Override
   public void setElement(long[] array, int index, Long element) {
      array[index] = element;
   }
}
