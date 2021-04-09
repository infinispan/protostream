package org.infinispan.protostream.types.java.arrays;

import org.infinispan.protostream.containers.IndexedElementContainerAdapter;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
public abstract class AbstractArrayAdapter<E> implements IndexedElementContainerAdapter<E[], E> {

   public abstract E[] create(int size);

   @Override
   public final int getNumElements(E[] array) {
      return array.length;
   }

   @Override
   public final E getElement(E[] array, int index) {
      return array[index];
   }

   @Override
   public final void setElement(E[] array, int index, E element) {
      array[index] = element;
   }
}