package org.infinispan.protostream.types.java;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.containers.IndexedElementContainerAdapter;

@ProtoAdapter(int[].class)
@ProtoName("IntArray")
public class IntArrayAdapter implements IndexedElementContainerAdapter<int[], Integer> {

   @ProtoFactory
   public int[] create(int size) {
      return new int[size];
   }

   @Override
   public int getNumElements(int[] array) {
      return array.length;
   }

   @Override
   public Integer getElement(int[] array, int index) {
      return array[index];
   }

   @Override
   public void setElement(int[] array, int index, Integer element) {
      array[index] = element;
   }
}
