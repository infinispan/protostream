package org.infinispan.protostream.types.java.util;

import java.util.Iterator;
import java.util.Map;

import org.infinispan.protostream.containers.MapElementContainerAdapter;

/**
 * Base adapter for {@link Map} implementations.
 *
 * @author José Bolina
 * @since 6.0
 */
public abstract class AbstractMapAdapter<K, V, M extends Map<K, V>> implements MapElementContainerAdapter<K, V, M> {

   abstract public M create(int size);

   @Override
   public Iterator<Map.Entry<K, V>> getElements(M container) {
      return AbstractMapAdapter.toIterator(container);
   }

   public static <K, V> MapEntryWrapper<K, V> entry(Map.Entry<K, V> entry) {
      if (entry instanceof AbstractMapAdapter.MapEntryWrapper) {
         return (AbstractMapAdapter.MapEntryWrapper<K, V>) entry;
      }
      return new MapEntryWrapper<>(entry);
   }

   static <K, V> Iterator<Map.Entry<K, V>> toIterator(Map<K, V> map) {
      Iterator<Map.Entry<K, V>> delegate = map.entrySet().iterator();
      return new Iterator<>() {

         @Override
         public boolean hasNext() {
            return delegate.hasNext();
         }

         @Override
         public Map.Entry<K, V> next() {
            Map.Entry<K, V> entry = delegate.next();
            // Wrap entries in our MapEntryWrapper to provide a consistent serializable type
            return MapEntryWrapper.create(entry);
         }
      };
   }

   @Override
   public final int getNumElements(M container) {
      return container.size();
   }

   /**
    * Wrapper for {@link Map.Entry} instances.
    * <p>
    * This wrapper is necessary because there are several implementations of the {@link Map.Entry} interface
    * across different map types (e.g., HashMap.Node, TreeMap.Entry, etc.). Instead of creating a separate
    * ProtoStream adapter for each implementation, we wrap all entries in this single, consistent type that
    * can be serialized uniformly.
    *
    * @param <K> the type of the entry key
    * @param <V> the type of the entry value
    */
   public static class MapEntryWrapper<K, V> implements Map.Entry<K, V> {
      private final Map.Entry<K, V> entry;

      private MapEntryWrapper(Map.Entry<K, V> entry) {
         this.entry = entry;
      }

      public static <K, V> Map.Entry<K, V> create(K key, V value) {
         return new MapEntryWrapper<>(Map.entry(key, value));
      }

      public static <K, V> Map.Entry<K, V> create(Map.Entry<K, V> entry) {
         return new MapEntryWrapper<>(entry);
      }

      @Override
      public K getKey() {
         return entry.getKey();
      }

      @Override
      public V getValue() {
         return entry.getValue();
      }

      @Override
      public V setValue(V v) {
         return entry.setValue(v);
      }
   }
}
