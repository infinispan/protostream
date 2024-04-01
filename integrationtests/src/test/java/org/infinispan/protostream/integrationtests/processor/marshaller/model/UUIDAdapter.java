package org.infinispan.protostream.integrationtests.processor.marshaller.model;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoReserved;
import org.infinispan.protostream.descriptors.Type;

import java.util.UUID;

@ProtoAdapter(UUID.class)
@ProtoReserved({100, 99})
public class UUIDAdapter {

   @ProtoFactory
   UUID create(long mostSigBits, long leastSigBits) {
      return new UUID(mostSigBits, leastSigBits);
   }

   @ProtoField(number = 1, type = Type.UINT64)
   long getMostSigBits(UUID uuid) {
      return uuid.getMostSignificantBits();
   }

   @ProtoField(number = 2, type = Type.UINT64)
   long getLeastSigBits(UUID uuid) {
      return uuid.getLeastSignificantBits();
   }
}
