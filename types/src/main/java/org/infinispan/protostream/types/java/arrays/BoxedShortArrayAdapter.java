package org.infinispan.protostream.types.java.arrays;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoName;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@ProtoAdapter(Short[].class)
@ProtoName("BoxedShortArray")
public final class BoxedShortArrayAdapter extends AbstractArrayAdapter<Short> {

   @ProtoFactory
   public Short[] create(int size) {
      return new Short[size];
   }
}
