package org.infinispan.protostream.types.java.arrays;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoName;

@ProtoAdapter(Float[].class)
@ProtoName("FloatArray")
public class FloatArrayAdapter extends GenericArrayAdapter<Float> {

   @ProtoFactory
   public Float[] create(int size) {
      return new Float[size];
   }
}
