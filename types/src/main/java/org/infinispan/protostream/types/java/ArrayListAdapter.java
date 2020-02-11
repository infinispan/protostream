package org.infinispan.protostream.types.java;

import java.util.ArrayList;
import java.util.Iterator;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.containers.IterableElementContainerAdapter;

@ProtoAdapter(ArrayList.class)
public class ArrayListAdapter<T> implements IterableElementContainerAdapter<ArrayList<T>, T> {

   @ProtoFactory
   public ArrayList<?> create(int size) {
      return new ArrayList<>(size);
   }

   @Override
   public int getNumElements(ArrayList<T> list) {
      return list.size();
   }

   @Override
   public Iterator<T> getElements(ArrayList<T> list) {
      return list.iterator();
   }

   @Override
   public void appendElement(ArrayList<T> list, T element) {
      list.add(element);
   }
}
