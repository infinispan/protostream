package org.infinispan.protostream.types.java.collections;

import java.util.ArrayList;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@ProtoAdapter(ArrayList.class)
public final class ArrayListAdapter<E> extends AbstractCollectionAdapter<ArrayList<E>, E> {

   @ProtoFactory
   public ArrayList<E> create(int size) {
      return new ArrayList<>(size);
   }
}
