package org.infinispan.protostream.types.java.time;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;

/**
 * An adapter for {@link ZonedDateTime}.
 */
@ProtoAdapter(ZonedDateTime.class)
public final class ZonedDateTimeAdapter {

   @ProtoFactory
   ZonedDateTime create(Long epochSecond, Integer nanoAdjustment, String zoneId) {
      return ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSecond, nanoAdjustment), ZoneId.of(zoneId));
   }

   @ProtoField(number = 1, type = Type.UINT64)
   Long getEpochSecond(ZonedDateTime zdt) {
      return zdt.toInstant().getEpochSecond();
   }

   @ProtoField(number = 2, type = Type.UINT32)
   Integer getNanoAdjustment(ZonedDateTime zdt) {
      return zdt.toInstant().getNano();
   }

   @ProtoField(number = 3, type = Type.STRING)
   String getZoneId(ZonedDateTime zdt) {
      return zdt.getZone().getId();
   }
}
