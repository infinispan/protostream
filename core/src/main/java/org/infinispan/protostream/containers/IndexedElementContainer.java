package org.infinispan.protostream.containers;

/**
 * A container that allows random access by position.
 *
 * @author anistor@redhat.com
 * @since 4.4
 */
public interface IndexedElementContainer<E> extends ElementContainer {

   E getElement(int index);

   void setElement(int index, E element);
}
