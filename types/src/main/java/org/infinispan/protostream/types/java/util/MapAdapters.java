package org.infinispan.protostream.types.java.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * ProtoStream adapters for various {@link Map} implementations.
 *
 * @author José Bolina
 * @since 6.0
 */
public final class MapAdapters {

   /**
    * Adapter for {@link HashMap} and immutable Map implementations.
    *
    * @author José Bolina
    * @since 6.0
    */
   @ProtoAdapter(
         value = Map.class,
         subClassNames = {
               "java.util.HashMap",
               "java.util.ImmutableCollections$Map1",
               "java.util.ImmutableCollections$MapN",
         }
   )
   public static class HashMapAdapter<K, V> extends AbstractMapAdapter<K, V, Map<K, V>> {

      @ProtoFactory
      @Override
      public Map<K, V> create(int size) {
         return new HashMap<>(size);
      }
   }

   /**
    * Adapter for {@link ConcurrentHashMap}.
    *
    * @author José Bolina
    * @since 6.0
    */
   @ProtoAdapter(ConcurrentHashMap.class)
   public static class ConcurrentHashMapAdapter<K, V> extends AbstractMapAdapter<K, V, ConcurrentHashMap<K, V>> {

      @ProtoFactory
      @Override
      public ConcurrentHashMap<K, V> create(int size) {
         return new ConcurrentHashMap<>(size);
      }
   }

   /**
    * Adapter for {@link LinkedHashMap}.
    *
    * @author José Bolina
    * @since 6.0
    */
   @ProtoAdapter(LinkedHashMap.class)
   public static class LinkedHashMapAdapter<K, V> extends AbstractMapAdapter<K, V, LinkedHashMap<K, V>> {

      @ProtoFactory
      @Override
      public LinkedHashMap<K, V> create(int size) {
         return new LinkedHashMap<>(size);
      }
   }

   /**
    * Adapter for {@link TreeMap}.
    *
    * @author José Bolina
    * @since 6.0
    */
   @ProtoAdapter(TreeMap.class)
   public static class TreeMapAdapter<K, V> extends AbstractMapAdapter<K, V, TreeMap<K, V>> {

      @ProtoFactory
      @Override
      public TreeMap<K, V> create(int ignore) {
         return new TreeMap<>();
      }
   }

   /**
    * Adapter for {@link WeakHashMap}.
    *
    * @author José Bolina
    * @since 6.0
    */
   @ProtoAdapter(WeakHashMap.class)
   public static class WeakHashMapAdapter<K, V> extends AbstractMapAdapter<K, V, WeakHashMap<K, V>> {

      @ProtoFactory
      @Override
      public WeakHashMap<K, V> create(int size) {
         return new WeakHashMap<>(size);
      }
   }

   /**
    * Adapter for {@link IdentityHashMap}.
    *
    * @author José Bolina
    * @since 6.0
    */
   @ProtoAdapter(IdentityHashMap.class)
   public static class IdentityHashMapAdapter<K, V> extends AbstractMapAdapter<K, V, IdentityHashMap<K, V>> {

      @ProtoFactory
      @Override
      public IdentityHashMap<K, V> create(int size) {
         return new IdentityHashMap<>(size);
      }
   }

   /**
    * Adapter for {@link ConcurrentSkipListMap}.
    *
    * @author José Bolina
    * @since 6.0
    */
   @ProtoAdapter(ConcurrentSkipListMap.class)
   public static class ConcurrentSkipListMapAdapter<K, V> extends AbstractMapAdapter<K, V, ConcurrentSkipListMap<K, V>> {

      @ProtoFactory
      @Override
      public ConcurrentSkipListMap<K, V> create(int ignore) {
         return new ConcurrentSkipListMap<>();
      }
   }

   /**
    * Adapter for {@link Hashtable}.
    *
    * @author José Bolina
    * @since 6.0
    */
   @ProtoAdapter(Hashtable.class)
   public static class HashtableAdapter<K, V> extends AbstractMapAdapter<K, V, Hashtable<K, V>> {

      @ProtoFactory
      @Override
      public Hashtable<K, V> create(int size) {
         return new Hashtable<>(size);
      }
   }

   /**
    * Adapter for {@link Properties}.
    *
    * @author José Bolina
    * @since 6.0
    */
   @ProtoAdapter(
         value = Map.class,
         subClassNames = "java.util.Properties"
   )
   public static class PropertiesAdapter extends AbstractMapAdapter<Object, Object, Map<Object, Object>> {

      @ProtoFactory
      @Override
      public Map<Object, Object> create(int ignore) {
         return new Properties();
      }
   }

   /**
    * Adapter for {@link Collections#emptyMap()}.
    *
    * @author José Bolina
    * @since 6.0
    */
   @ProtoAdapter(
         value = Map.class,
         subClassNames = {
               "java.util.Collections$EmptyMap",
         }
   )
   public static class CollectionsEmptyMap<K, V> {

      @ProtoFactory
      public Map<K, V> create() {
         return Collections.emptyMap();
      }
   }

   /**
    * Adapter for {@link Collections#singletonMap(Object, Object)}.
    *
    * @author José Bolina
    * @since 6.0
    */
   @ProtoAdapter(
         value = Map.class,
         subClassNames = "java.util.Collections$SingletonMap"
   )
   public static class CollectionSingletonMap<K, V> {

      @ProtoFactory
      public Map<K, V> create(AbstractMapAdapter.MapEntryWrapper<? extends K, ? extends V> entry) {
         return Collections.singletonMap(entry.getKey(), entry.getValue());
      }

      @ProtoField(number = 1)
      AbstractMapAdapter.MapEntryWrapper<K, V> getEntry(Map<K, V> map) {
         Iterator<Map.Entry<K, V>> iterator = map.entrySet().iterator();
         return AbstractMapAdapter.entry(iterator.next());
      }
   }
}
