package org.infinispan.protostream.types.java.collections;

import java.util.HashSet;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@ProtoAdapter(HashSet.class)
public final class HashSetAdapter<E> extends AbstractCollectionAdapter<HashSet<E>, E> {

   @ProtoFactory
   public HashSet<E> create(int size) {
      return new HashSet<>(size);
   }
}

