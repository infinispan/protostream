package org.infinispan.protostream.types.java.collections;

import java.util.Collection;
import java.util.Iterator;

import org.infinispan.protostream.containers.IterableElementContainerAdapter;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
public abstract class AbstractCollectionAdapter<C extends Collection<E>, E> implements IterableElementContainerAdapter<C, E> {

   public abstract C create(int size);

   @Override
   public final int getNumElements(C c) {
      return c.size();
   }

   @Override
   public final Iterator<E> getElements(C c) {
      return c.iterator();
   }

   @Override
   public final void appendElement(C c, E e) {
      c.add(e);
   }
}
