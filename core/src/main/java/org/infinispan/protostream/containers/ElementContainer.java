package org.infinispan.protostream.containers;

/**
 * Base interface for element containers. Allows automatic handling of repeated elements/children of
 * container/collection-like messages.
 *
 * @author anistor@redhat.com
 * @since 4.4
 */
public interface ElementContainer {

   int getNumElements();
}
