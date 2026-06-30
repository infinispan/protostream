package org.infinispan.protostream.containers;

import java.util.Map;

/**
 * Container adapter interface for {@link Map} implementations.
 *
 * @author José Bolina
 * @since 6.0
 */
public interface MapElementContainerAdapter<K, V, M extends Map<K, V>> extends IterableElementContainerAdapter<M, Map.Entry<K, V>> {

   @Override
   default void appendElement(M container, Map.Entry<K, V> element) {
      container.put(element.getKey(), element.getValue());
   }
}
