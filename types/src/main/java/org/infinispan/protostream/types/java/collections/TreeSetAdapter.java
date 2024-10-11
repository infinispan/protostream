package org.infinispan.protostream.types.java.collections;

import java.util.TreeSet;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@ProtoAdapter(TreeSet.class)
public final class TreeSetAdapter<E> extends AbstractCollectionAdapter<TreeSet<E>, E> {

   @ProtoFactory
   public TreeSet<E> create(int size) {
      // the size param is useless for a TreeSet
      return new TreeSet<>();
   }
}
