package org.infinispan.protostream.containers;

import java.util.Iterator;

/**
 * A container that allows sequential access. Appending at the end is allowed.
 *
 * @author anistor@redhat.com
 * @since 4.4
 */
public interface IterableElementContainerAdapter<C, E> extends ElementContainerAdapter<C> {

   Iterator<E> getElements(C container);

   void appendElement(C container, E element);
}
