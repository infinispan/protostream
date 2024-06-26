package org.infinispan.protostream.types.java.time;

import java.time.LocalTime;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;

@ProtoAdapter(LocalTime.class)
public class LocalTimeAdapter {
   @ProtoFactory
   LocalTime create(Long nanoOfDay) {
      return LocalTime.ofNanoOfDay(nanoOfDay);
   }
   @ProtoField(number = 1, type = Type.UINT64)
   Long getNanoOfDay(LocalTime localTime) {
      return localTime.toNanoOfDay();
   }
}
