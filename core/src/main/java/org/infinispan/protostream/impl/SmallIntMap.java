package org.infinispan.protostream.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A map-like data structure that uses a copy-on-write array for keys up to a certain threshold 
 * and a HashMap for keys beyond that threshold. This is optimized for integer keys that are 
 * often small and positive, and for scenarios with frequent reads and infrequent writes.
 *
 * @param <V> the type of values stored in the map
 */
public class SmallIntMap<V> {

   private static final int DEFAULT_ARRAY_THRESHOLD = 4096;

   private volatile State<V> state;

   private final ReentrantLock lock = new ReentrantLock();

   public SmallIntMap() {
      this(DEFAULT_ARRAY_THRESHOLD);
   }

   public SmallIntMap(int arrayThreshold) {
      this.state = new State<>(allocate(arrayThreshold), null);
   }

   public SmallIntMap(SmallIntMap<V> other) {
      V[] arr = Arrays.copyOf(other.state.array, other.state.array.length);
      Map<Integer, V> map = other.state.map != null ? new HashMap<>(other.state.map) : null;
      this.state = new State<>(arr, map);
   }

   /**
    * Associates the specified value with the specified key in this map.
    *
    * @param key   the key with which the specified value is to be associated
    * @param value the value to be associated with the specified key
    * @return the previous value associated with the key, or {@code null} if there was no mapping for the key
    */
   public V put(int key, V value) {
      lock.lock();
      try {
         State<V> s = state;
         if (key >= 0 && key < s.array.length) {
            V oldValue = s.array[key];
            if (oldValue == value) {
               return oldValue;
            }
            V[] newArray = Arrays.copyOf(s.array, s.array.length);
            newArray[key] = value;
            state = new State<>(newArray, s.map);
            return oldValue;
         }
         Map<Integer, V> newMap = s.map != null ? new HashMap<>(s.map) : new HashMap<>();
         V old = newMap.put(key, value);
         state = new State<>(s.array, newMap);
         return old;
      } finally {
         lock.unlock();
      }
   }

   /**
    * Copies all of the mappings from the specified map to this map.
    *
    * @param m mappings to be stored in this map
    */
   public void putAll(Map<? extends Integer, ? extends V> m) {
      lock.lock();
      try {
         State<V> s = state;
         V[] newArray = null;
         Map<Integer, V> newMap = null;
         for (Map.Entry<? extends Integer, ? extends V> e : m.entrySet()) {
            int key = e.getKey();
            V value = e.getValue();

            if (key >= 0 && key < s.array.length) {
               if (newArray == null) {
                  newArray = Arrays.copyOf(s.array, s.array.length);
               }
               newArray[key] = value;
            } else {
               if (newMap == null) {
                  newMap = s.map != null ? new HashMap<>(s.map) : new HashMap<>();
               }
               newMap.put(key, value);
            }
         }

         state = new State<>(
               newArray != null ? newArray : s.array,
               newMap != null ? newMap : s.map);
      } finally {
         lock.unlock();
      }
   }

   /**
    * Returns the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the key.
    *
    * @param key the key whose associated value is to be returned
    * @return the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the key
    */
   public V get(int key) {
      return state.get(key);
   }

   /**
    * Removes the mapping for a key from this map if it is present.
    *
    * @param key the key whose mapping is to be removed from the map
    * @return the previous value associated with the key, or {@code null} if there was no mapping for the key
    */
   public V remove(int key) {
      lock.lock();
      try {
         State<V> s = state;
         if (key >= 0 && key < s.array.length) {
            V oldValue = s.array[key];
            if (oldValue == null) {
               return null;
            }
            V[] newArray = Arrays.copyOf(s.array, s.array.length);
            newArray[key] = null;
            state = new State<>(newArray, s.map);
            return oldValue;
         }
         Map<Integer, V> newMap = s.map != null ? new HashMap<>(s.map) : new HashMap<>();
         V oldValue = newMap.remove(key);
         state = new State<>(s.array, newMap);
         return oldValue;
      } finally {
         lock.unlock();
      }
   }

   /**
    * Removes all of the mappings from this map.
    */
   public void clear() {
      lock.lock();
      try {
         V[] newArray = allocate(state.array.length);
         Map<Integer, V> newMap = state.map != null
               ? new HashMap<>()
               : null;
         state = new State<>(newArray, newMap);
      } finally {
         lock.unlock();
      }
   }

   private record State<V>(V[] array, Map<Integer, V> map) {
      public V get(int key) {
         if (key >= 0 && key < array.length) {
            return array[key];
         }

         return map != null ? map.get(key) : null;
      }
   }

   @SuppressWarnings("unchecked")
   private static <T> T[] allocate(int size) {
      return (T[]) new Object[size];
   }
}