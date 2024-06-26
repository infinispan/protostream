package org.infinispan.protostream.types.java.time;

import java.time.MonthDay;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;

@ProtoAdapter(MonthDay.class)
public class MonthDayAdapter {
   @ProtoFactory
   MonthDay create(Integer month, Integer dayOfMonth) {
      return MonthDay.of(month, dayOfMonth);
   }
   @ProtoField(number = 1, type = Type.INT32)
   Integer getMonth(MonthDay monthDay) {
      return monthDay.getMonthValue();
   }

   @ProtoField(number = 2, type = Type.INT32)
   Integer getDayOfMonth(MonthDay monthDay) {
      return monthDay.getDayOfMonth();
   }
}
