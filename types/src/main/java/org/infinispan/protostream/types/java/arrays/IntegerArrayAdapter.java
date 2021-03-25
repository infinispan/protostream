package org.infinispan.protostream.types.java.arrays;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoName;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@ProtoAdapter(Integer[].class)
@ProtoName("IntegerArray")
public final class IntegerArrayAdapter extends GenericArrayAdapter<Integer> {

   @ProtoFactory
   public Integer[] create(int size) {
      return new Integer[size];
   }
}
