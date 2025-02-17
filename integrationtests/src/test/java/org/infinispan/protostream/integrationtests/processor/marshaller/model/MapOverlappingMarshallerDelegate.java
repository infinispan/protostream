package org.infinispan.protostream.integrationtests.processor.marshaller.model;

import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

public class MapOverlappingMarshallerDelegate {
   @ProtoField(2)
   BigInteger bigInteger;

   @ProtoField(3)
   Map<Integer, UUID> uuidMap;

   @ProtoFactory
   public MapOverlappingMarshallerDelegate(BigInteger bigInteger, Map<Integer, UUID> uuidMap) {
      this.bigInteger = bigInteger;
      this.uuidMap = uuidMap;
   }

   @Override
   public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      MapOverlappingMarshallerDelegate that = (MapOverlappingMarshallerDelegate) o;
      return Objects.equals(bigInteger, that.bigInteger) && Objects.equals(uuidMap, that.uuidMap);
   }

   @Override
   public int hashCode() {
      return Objects.hash(bigInteger, uuidMap);
   }
}
