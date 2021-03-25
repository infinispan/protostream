package org.infinispan.protostream.types.java.arrays;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoName;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@ProtoAdapter(Long[].class)
@ProtoName("LongArray")
public final class LongArrayAdapter extends GenericArrayAdapter<Long> {

   @ProtoFactory
   public Long[] create(int size) {
      return new Long[size];
   }
}
