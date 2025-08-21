package org.infinispan.protostream.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;

/**
 * A map-like data structure that uses a copy-on-write array for keys up to a certain threshold 
 * and a HashMap for keys beyond that threshold. This is optimized for integer keys that are 
 * often small and positive, and for scenarios with frequent reads and infrequent writes.
 *
 * @param <V> the type of values stored in the map
 */
public class SmallIntMap<V> {

   private static final int DEFAULT_ARRAY_THRESHOLD = 4096;

   private volatile V[] array;
   private Map<Integer, V> map;

   private final StampedLock lock = new StampedLock();

   public SmallIntMap() {
      this(DEFAULT_ARRAY_THRESHOLD);
   }

   public SmallIntMap(int arrayThreshold) {
      this.array = (V[]) new Object[arrayThreshold];
   }

   /**
    * Associates the specified value with the specified key in this map.
    *
    * @param key   the key with which the specified value is to be associated
    * @param value the value to be associated with the specified key
    * @return the previous value associated with the key, or {@code null} if there was no mapping for the key
    */
   public V put(int key, V value) {
      long stamp = lock.writeLock();
      try {
         if (key >= 0 && key < array.length) {
            V oldValue = array[key];
            if (oldValue == value) {
               return oldValue;
            }
            V[] newArray = Arrays.copyOf(array, array.length);
            newArray[key] = value;
            this.array = newArray;
            return oldValue;
         }
         if (map == null) {
            map = new HashMap<>();
         }
         return map.put(key, value);
      } finally {
         lock.unlockWrite(stamp);
      }
   }

   /**
    * Copies all of the mappings from the specified map to this map.
    *
    * @param m mappings to be stored in this map
    */
   public void putAll(Map<? extends Integer, ? extends V> m) {
      long stamp = lock.writeLock();
      try {
         V[] newArray = null;
         for (Map.Entry<? extends Integer, ? extends V> e : m.entrySet()) {
            int key = e.getKey();
            V value = e.getValue();

            if (key >= 0 && key < array.length) {
               if (newArray == null) {
                  newArray = Arrays.copyOf(array, array.length);
               }
               V oldValue = newArray[key];
               if (oldValue != value) {
                  newArray[key] = value;
               }
            } else {
               if (map == null) {
                  map = new HashMap<>();
               }
               map.put(key, value);
            }
         }
         if (newArray != null) {
            this.array = newArray;
         }
      } finally {
         lock.unlockWrite(stamp);
      }
   }

   /**
    * Returns the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the key.
    *
    * @param key the key whose associated value is to be returned
    * @return the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the key
    */
   public V get(int key) {
      if (key >= 0 && key < array.length) {
         return array[key];
      }
      long stamp = lock.readLock();
      try {
         if (map == null) {
            return null;
         }
         return map.get(key);
      } finally {
         lock.unlockRead(stamp);
      }
   }

   /**
    * Removes the mapping for a key from this map if it is present.
    *
    * @param key the key whose mapping is to be removed from the map
    * @return the previous value associated with the key, or {@code null} if there was no mapping for the key
    */
   public V remove(int key) {
      long stamp = lock.writeLock();
      try {
         if (key >= 0 && key < array.length) {
            V oldValue = array[key];
            if (oldValue == null) {
               return null;
            }
            V[] newArray = Arrays.copyOf(array, array.length);
            newArray[key] = null;
            this.array = newArray;
            return oldValue;
         }
         if (map == null) {
            return null;
         }
         return map.remove(key);
      } finally {
         lock.unlockWrite(stamp);
      }
   }

   /**
    * Removes all of the mappings from this map.
    */
   public void clear() {
      long stamp = lock.writeLock();
      try {
         this.array = (V[]) new Object[array.length];
         if (map != null) {
            map.clear();
         }
      } finally {
         lock.unlockWrite(stamp);
      }
   }
}