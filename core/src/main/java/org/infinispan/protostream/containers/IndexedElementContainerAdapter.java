package org.infinispan.protostream.containers;

/**
 * A container adapter that allows random access by position.
 *
 * @author anistor@redhat.com
 * @since 4.4
 */
public interface IndexedElementContainerAdapter<C, E> extends ElementContainerAdapter<C> {

   E getElement(C container, int index);

   void setElement(C container, int index, E element);
}
