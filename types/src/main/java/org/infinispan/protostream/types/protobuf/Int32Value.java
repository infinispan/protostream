package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
public final class Int32Value {

   private final int value;

   @ProtoFactory
   public Int32Value(int value) {
      this.value = value;
   }

   @ProtoField(value = 1, defaultValue = "0")
   public int getValue() {
      return value;
   }
}
