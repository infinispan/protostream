package org.infinispan.protostream.types.java.arrays;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoName;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@ProtoAdapter(Byte[].class)
@ProtoName("BoxedByteArray")
public final class BoxedByteArrayAdapter extends AbstractArrayAdapter<Byte> {

   @ProtoFactory
   public Byte[] create(int size) {
      return new Byte[size];
   }
}
