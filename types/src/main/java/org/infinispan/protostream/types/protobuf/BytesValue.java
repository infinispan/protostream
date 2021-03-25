package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
public final class BytesValue {

   private final byte[] value;

   @ProtoFactory
   public BytesValue(byte[] value) {
      this.value = value;
   }

   @ProtoField(value = 1)
   public byte[] getValue() {
      return value;
   }
}
