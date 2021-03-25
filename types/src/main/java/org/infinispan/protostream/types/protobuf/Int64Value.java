package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
public final class Int64Value {

   private final long value;

   @ProtoFactory
   public Int64Value(long value) {
      this.value = value;
   }

   @ProtoField(value = 1, defaultValue = "0")
   public long getValue() {
      return value;
   }
}
