package org.infinispan.protostream.types.protobuf;

import org.infinispan.protostream.annotations.ProtoField;

public class Timestamp {

   private long seconds;

   private long nanos;

   public Timestamp() {
   }

   public Timestamp(long seconds, long nanos) {
      this.seconds = seconds;
      this.nanos = nanos;
   }

   @ProtoField(value = 1, defaultValue = "0")
   public long getSeconds() {
      return seconds;
   }

   public void setSeconds(long seconds) {
      this.seconds = seconds;
   }

   @ProtoField(value = 2, defaultValue = "0")
   public long getNanos() {
      return nanos;
   }

   public void setNanos(long nanos) {
      this.nanos = nanos;
   }
}