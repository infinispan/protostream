package org.infinispan.protostream.types.java.time;

import java.time.ZoneId;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;

@ProtoAdapter(value = ZoneId.class, subClassNames = {
      "java.time.ZoneRegion"
})
public class ZoneIdAdapter {
   @ProtoFactory
   ZoneId create(String zoneId) {
      return ZoneId.of(zoneId);
   }

   @ProtoField(number = 1, type = Type.STRING)
   String getZoneId(ZoneId zid) {
      return zid.getId();
   }
}
