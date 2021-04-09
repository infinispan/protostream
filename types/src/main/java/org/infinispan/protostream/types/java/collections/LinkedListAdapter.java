package org.infinispan.protostream.types.java.collections;

import java.util.LinkedList;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@ProtoAdapter(LinkedList.class)
public class LinkedListAdapter<E> extends AbstractCollectionAdapter<LinkedList<E>, E> {

   @ProtoFactory
   public LinkedList<E> create(int size) {
      // the size param is useless for a LinkedList
      return new LinkedList<>();
   }
}
