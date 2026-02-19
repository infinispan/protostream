package org.infinispan.protostream.containers;

import java.util.Map;

/**
 * Container adapter interface for {@link Map} implementations.
 *
 * @author José Bolina
 * @since 6.0
 */
public interface MapElementContainer<K, V> extends IterableElementContainer<Map.Entry<K, V>> { }
