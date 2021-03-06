package org.infinispan.protostream.types.java.util;

import java.util.UUID;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.protostream.descriptors.Type;

@ProtoTypeId(10005 /* ProtoStreamTypeIds.UUID */)
@ProtoAdapter(UUID.class)
public class UUIDAdapter {

   @ProtoFactory
   UUID create(Long mostSigBits, Long leastSigBits, Long mostSigBitsFixed, Long leastSigBitsFixed) {
      if (mostSigBits == null)
         return new UUID(mostSigBitsFixed, leastSigBitsFixed);

      // Create the UUID using the old fields
      return new UUID(mostSigBits, leastSigBits);
   }

   @ProtoField(number = 1, type = Type.UINT64)
   Long getMostSigBits(UUID uuid) {
      return null;
   }

   @ProtoField(number = 2, type = Type.UINT64)
   Long getLeastSigBits(UUID uuid) {
      return null;
   }

   @ProtoField(number = 3, type = Type.FIXED64, defaultValue = "0")
   Long getMostSigBitsFixed(UUID uuid) {
      return uuid.getMostSignificantBits();
   }

   @ProtoField(number = 4, type = Type.FIXED64, defaultValue = "0")
   Long getLeastSigBitsFixed(UUID uuid) {
      return uuid.getLeastSignificantBits();
   }
}
