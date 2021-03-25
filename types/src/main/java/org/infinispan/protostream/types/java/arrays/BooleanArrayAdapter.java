package org.infinispan.protostream.types.java.arrays;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoName;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@ProtoAdapter(Boolean[].class)
@ProtoName("BooleanArray")
public final class BooleanArrayAdapter extends GenericArrayAdapter<Boolean> {

   @ProtoFactory
   public Boolean[] create(int size) {
      return new Boolean[size];
   }
}
