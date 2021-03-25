package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
public final class BoolValue {

   private final boolean value;

   @ProtoFactory
   public BoolValue(boolean value) {
      this.value = value;
   }

   @ProtoField(value = 1, defaultValue = "false")
   public boolean getValue() {
      return value;
   }
}
