package org.infinispan.protostream.types.java.collections;

import java.util.HashSet;
import java.util.Iterator;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.containers.IterableElementContainerAdapter;

@ProtoAdapter(HashSet.class)
public class HashSetAdapter<T> implements IterableElementContainerAdapter<HashSet<T>, T> {

   @ProtoFactory
   public HashSet<T> create(int size) {
      return new HashSet<>(size);
   }

   @Override
   public int getNumElements(HashSet<T> set) {
      return set.size();
   }

   @Override
   public Iterator<T> getElements(HashSet<T> set) {
      return set.iterator();
   }

   @Override
   public void appendElement(HashSet<T> set, T element) {
      set.add(element);
   }
}

