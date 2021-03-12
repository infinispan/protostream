package org.infinispan.protostream.types.java.arrays;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoName;

@ProtoAdapter(Double[].class)
@ProtoName("DoubleArray")
public class DoubleArrayAdapter extends GenericArrayAdapter<Double> {

   @ProtoFactory
   public Double[] create(int size) {
      return new Double[size];
   }
}
