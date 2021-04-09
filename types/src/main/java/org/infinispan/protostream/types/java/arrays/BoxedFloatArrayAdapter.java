package org.infinispan.protostream.types.java.arrays;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoName;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@ProtoAdapter(Float[].class)
@ProtoName("BoxedFloatArray")
public final class BoxedFloatArrayAdapter extends AbstractArrayAdapter<Float> {

   @ProtoFactory
   public Float[] create(int size) {
      return new Float[size];
   }
}
