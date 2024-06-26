package org.infinispan.protostream.types.java.time;

import java.time.LocalDate;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;

@ProtoAdapter(LocalDate.class)
public class LocalDateAdapter {
   @ProtoFactory
   LocalDate create(Long epochDay) {
      return LocalDate.ofEpochDay(epochDay);
   }

   @ProtoField(number = 1, type = Type.UINT64)
   Long getEpochDay(LocalDate localDate) {
      return localDate.toEpochDay();
   }
}
