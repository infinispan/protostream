package org.infinispan.protostream.types.java.arrays;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoName;

@ProtoAdapter(Short[].class)
@ProtoName("ShortArray")
public class ShortArrayAdapter extends GenericArrayAdapter<Short> {

   @ProtoFactory
   public Short[] create(int size) {
      return new Short[size];
   }
}
