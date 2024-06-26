package org.infinispan.protostream.types.java.time;

import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;

@ProtoAdapter(OffsetTime.class)
public class OffsetTimeAdapter {
   @ProtoFactory
   OffsetTime create(LocalTime localTime, ZoneOffset offset) {
      return OffsetTime.of(localTime, offset);
   }

   @ProtoField(number = 1, type = Type.MESSAGE)
   LocalTime getLocalTime(OffsetTime offsetTime) {
      return offsetTime.toLocalTime();
   }

   @ProtoField(number = 2, type = Type.MESSAGE)
   ZoneOffset getOffset(OffsetTime offsetTime) {
      return offsetTime.getOffset();
   }
}
