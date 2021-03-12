package org.infinispan.protostream.types.java.arrays;

import org.infinispan.protostream.containers.IndexedElementContainerAdapter;

abstract class GenericArrayAdapter<T> implements IndexedElementContainerAdapter<T[], T> {

   public abstract T[] create(int size);

   @Override
   public final int getNumElements(T[] array) {
      return array.length;
   }

   @Override
   public final T getElement(T[] array, int index) {
      return array[index];
   }

   @Override
   public final void setElement(T[] array, int index, T element) {
      array[index] = element;
   }
}