package org.infinispan.protostream.annotations.impl.testdomain;

import java.util.UUID;

import org.infinispan.protostream.annotations.ProtoBridgeFor;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;

@ProtoBridgeFor(UUID.class)
public class UUIDBridge {

   @ProtoFactory
   UUID create(long mostSigBits, long leastSigBits) {
      return new UUID(mostSigBits, leastSigBits);
   }

   @ProtoField(number = 1, type = Type.UINT64, defaultValue = "0")
   long getMostSigBits(UUID uuid) {
      return uuid.getMostSignificantBits();
   }

   @ProtoField(number = 2, type = Type.UINT64, defaultValue = "0")
   long getLeastSigBits(UUID uuid) {
      return uuid.getLeastSignificantBits();
   }
}
