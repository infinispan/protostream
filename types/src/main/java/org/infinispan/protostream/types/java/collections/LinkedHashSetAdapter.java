package org.infinispan.protostream.types.java.collections;

import java.util.LinkedHashSet;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@ProtoAdapter(LinkedHashSet.class)
public class LinkedHashSetAdapter<E> extends AbstractCollectionAdapter<LinkedHashSet<E>, E> {

   @ProtoFactory
   public LinkedHashSet<E> create(int size) {
      // the size param is useless for a LinkedHashSet
      return new LinkedHashSet<>();
   }
}
