package org.infinispan.protostream.types.java.time;

import java.time.Period;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;

@ProtoAdapter(Period.class)
public class PeriodAdapter {
   @ProtoFactory
   Period create(Integer years, Integer months, Integer days) {
      return Period.of(years, months, days);
   }

   @ProtoField(number = 1, type = Type.INT32)
   Integer getYears(Period period) {
      return period.getYears();
   }

   @ProtoField(number = 2, type = Type.INT32)
   Integer getMonths(Period period) {
      return period.getMonths();
   }

   @ProtoField(number = 3, type = Type.INT32)
   Integer getDays(Period period) {
      return period.getDays();
   }
}
