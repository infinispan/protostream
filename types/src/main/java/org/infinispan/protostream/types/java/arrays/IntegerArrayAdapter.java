package org.infinispan.protostream.types.java.arrays;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoName;

@ProtoAdapter(Integer[].class)
@ProtoName("IntegerArray")
public class IntegerArrayAdapter extends GenericArrayAdapter<Integer> {

   @ProtoFactory
   public Integer[] create(int size) {
      return new Integer[size];
   }
}
