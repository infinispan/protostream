package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

public final class Timestamp {

   private final long seconds;

   private final long nanos;

   @ProtoFactory
   public Timestamp(long seconds, long nanos) {
      this.seconds = seconds;
      this.nanos = nanos;
   }

   @ProtoField(value = 1, defaultValue = "0")
   public long getSeconds() {
      return seconds;
   }

   @ProtoField(value = 2, defaultValue = "0")
   public long getNanos() {
      return nanos;
   }
}
