package org.infinispan.protostream.types.java;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.containers.IndexedElementContainerAdapter;

@ProtoAdapter(Integer[].class)
@ProtoName("IntegerArray")
public class IntegerArrayAdapter implements IndexedElementContainerAdapter<Integer[], Integer> {

   @ProtoFactory
   public Integer[] create(int size) {
      return new Integer[size];
   }

   @Override
   public int getNumElements(Integer[] array) {
      return array.length;
   }

   @Override
   public Integer getElement(Integer[] array, int index) {
      return array[index];
   }

   @Override
   public void setElement(Integer[] array, int index, Integer element) {
      array[index] = element;
   }
}
