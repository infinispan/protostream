package org.infinispan.protostream.types.java.arrays;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoName;

@ProtoAdapter(String[].class)
@ProtoName("StringArray")
public class StringArrayAdapter extends GenericArrayAdapter<String> {

   @ProtoFactory
   public String[] create(int size) {
      return new String[size];
   }
}
