package org.infinispan.protostream.containers;

import java.util.Iterator;

/**
 * A container that allows sequential access. Appending at the end is allowed.
 *
 * @author anistor@redhat.com
 * @since 4.4
 */
public interface IterableElementContainer<E> extends ElementContainer {

   Iterator<E> getIterator();

   void appendElement(E element);
}
