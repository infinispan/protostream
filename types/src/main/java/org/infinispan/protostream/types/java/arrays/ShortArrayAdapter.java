package org.infinispan.protostream.types.java.arrays;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.containers.IndexedElementContainerAdapter;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@ProtoAdapter(short[].class)
@ProtoName("ShortArray")
public final class ShortArrayAdapter implements IndexedElementContainerAdapter<short[], Short> {

   @ProtoFactory
   public short[] create(int size) {
      return new short[size];
   }

   @Override
   public int getNumElements(short[] array) {
      return array.length;
   }

   @Override
   public Short getElement(short[] array, int index) {
      return array[index];
   }

   @Override
   public void setElement(short[] array, int index, Short element) {
      array[index] = element;
   }
}
