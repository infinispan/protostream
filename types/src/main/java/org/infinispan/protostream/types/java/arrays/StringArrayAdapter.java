package org.infinispan.protostream.types.java.arrays;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoName;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@ProtoAdapter(String[].class)
@ProtoName("StringArray")
public final class StringArrayAdapter extends AbstractArrayAdapter<String> {

   @ProtoFactory
   public String[] create(int size) {
      return new String[size];
   }
}
