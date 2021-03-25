package org.infinispan.protostream.types.java.arrays;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoName;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@ProtoAdapter(Object[].class)
@ProtoName("ObjectArray")
public final class ObjectArrayAdapter extends GenericArrayAdapter<Object> {

   @ProtoFactory
   public Object[] create(int size) {
      return new Object[size];
   }
}