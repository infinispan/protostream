package org.infinispan.protostream.impl;

import java.util.ArrayList;
import java.util.Iterator;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.containers.IndexedElementContainerAdapter;
import org.infinispan.protostream.containers.IterableElementContainerAdapter;

/**
 * @author anistor@redhat.com
 */
public class ElementContainerTest {

   @ProtoAdapter(int[].class)
   @ProtoName("IntArray")
   public static final class IntArrayAdapter implements IndexedElementContainerAdapter<int[], Integer> {

      @ProtoFactory
      public int[] create(int theSize) {
         return new int[theSize];
      }

      @Override
      public int getNumElements(int[] container) {
         return container.length;
      }

      @Override
      public Integer getElement(int[] container, int index) {
         return container[index];
      }

      @Override
      public void setElement(int[] container, int index, Integer element) {
         container[index] = element;
      }
   }

   @ProtoAdapter(ArrayList.class)
   public static final class ArrayListAdapter1<T> implements IndexedElementContainerAdapter<ArrayList<T>, T> {

      @ProtoFactory
      public ArrayList<T> create(int numElements) {
         return new ArrayList<>(numElements);
      }

      @Override
      public int getNumElements(ArrayList<T> container) {
         return container.size();
      }

      @Override
      public T getElement(ArrayList<T> container, int index) {
         return container.get(index);
      }

      @Override
      public void setElement(ArrayList<T> container, int index, T element) {
         while (container.size() <= index) {
            container.add(null);
         }
         container.set(index, element);
      }
   }

   @ProtoAdapter(ArrayList.class)
   public static final class ArrayListAdapter2<T> implements IterableElementContainerAdapter<ArrayList<T>, T> {

      @ProtoFactory
      public ArrayList<T> create(int size) {
         return new ArrayList<>(size);
      }

      @Override
      public int getNumElements(ArrayList<T> container) {
         return container.size();
      }

      @Override
      public Iterator<T> getElements(ArrayList<T> container) {
         return container.iterator();
      }

      @Override
      public void appendElement(ArrayList<T> container, T element) {
         container.add(element);
      }
   }
}
