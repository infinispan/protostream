package org.infinispan.protostream.types.java.arrays;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.containers.IndexedElementContainerAdapter;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@ProtoAdapter(boolean[].class)
@ProtoName("BooleanArray")
public final class BooleanArrayAdapter implements IndexedElementContainerAdapter<boolean[], Boolean> {

   @ProtoFactory
   public boolean[] create(int size) {
      return new boolean[size];
   }

   @Override
   public int getNumElements(boolean[] array) {
      return array.length;
   }

   @Override
   public Boolean getElement(boolean[] array, int index) {
      return array[index];
   }

   @Override
   public void setElement(boolean[] array, int index, Boolean element) {
      array[index] = element;
   }
}
