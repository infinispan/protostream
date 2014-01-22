package org.infinispan.protostream.impl;

import java.util.AbstractSet;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An efficient implementation of a Set of Integers backed by a BitSet. This class was introduced just for efficiently
 * implementing the deprecated method MessageContext.getSeenFields() backed by a BitSet and will be removed soon.
 *
 * @author anistor@redhat.com
 * @since 1.0
 */
public final class FastIntegerSet extends AbstractSet<Integer> {

   private final BitSet bitSet;

   private static final Iterator<Integer> EMPTY_ITERATOR =
         new Iterator<Integer>() {

            @Override
            public boolean hasNext() {
               return false;
            }

            @Override
            public Integer next() {
               throw new NoSuchElementException();
            }

            @Override
            public void remove() {
               throw new UnsupportedOperationException();
            }
         };

   public FastIntegerSet(BitSet bitSet) {
      this.bitSet = bitSet;
   }

   @Override
   public int size() {
      return bitSet.cardinality();
   }

   @Override
   public boolean isEmpty() {
      return bitSet.isEmpty();
   }

   @Override
   public boolean contains(Object o) {
      int index = ((Integer) o).intValue();
      return bitSet.get(index);
   }

   @Override
   public Iterator<Integer> iterator() {
      if (isEmpty()) {
         return EMPTY_ITERATOR;
      }
      return new Iterator<Integer>() {

         private int current = 0;

         @Override
         public boolean hasNext() {
            if (current == -1) {
               return false;
            }
            current = bitSet.nextSetBit(current);
            return current != -1;
         }

         @Override
         public Integer next() {
            if (current == -1) {
               throw new NoSuchElementException();
            }
            current = bitSet.nextSetBit(current);
            if (current == -1) {
               throw new NoSuchElementException();
            }
            int next = current;
            current++;
            return next;
         }

         @Override
         public void remove() {
            throw new UnsupportedOperationException();
         }
      };
   }

   @Override
   public boolean add(Integer integer) {
      int index = integer.intValue();
      if (bitSet.get(index)) {
         return false;
      }
      bitSet.set(index);
      return true;
   }

   @Override
   public boolean remove(Object o) {
      int index = ((Integer) o).intValue();
      if (bitSet.get(index)) {
         bitSet.clear(index);
         return true;
      }
      return false;
   }

   @Override
   public boolean containsAll(Collection<?> c) {
      boolean containsAll = true;
      for (Object i : c) {
         int index = ((Integer) i).intValue();
         if (!bitSet.get(index)) {
            containsAll = false;
         }
      }
      return containsAll;
   }

   @Override
   public boolean addAll(Collection<? extends Integer> c) {
      boolean modified = false;
      for (Integer i : c) {
         int index = i.intValue();
         if (!bitSet.get(index)) {
            modified = true;
            bitSet.set(index);
         }
      }
      return modified;
   }

   @Override
   public void clear() {
      bitSet.clear();
   }
}
