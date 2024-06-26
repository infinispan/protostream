package org.infinispan.protostream.types.java.time;

import java.time.Month;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoName;

@ProtoAdapter(Month.class)
@ProtoName("Month")
public enum MonthAdapter {
   @ProtoEnumValue
   JANUARY,
   @ProtoEnumValue(value = 1)
   FEBRUARY,
   @ProtoEnumValue(value = 2)
   MARCH,
   @ProtoEnumValue(value = 3)
   APRIL,
   @ProtoEnumValue(value = 4)
   MAY,
   @ProtoEnumValue(value = 5)
   JUNE,
   @ProtoEnumValue(value = 6)
   JULY,
   @ProtoEnumValue(value = 7)
   AUGUST,
   @ProtoEnumValue(value = 8)
   SEPTEMBER,
   @ProtoEnumValue(value = 9)
   OCTOBER,
   @ProtoEnumValue(value = 10)
   NOVEMBER,
   @ProtoEnumValue(value = 11)
   DECEMBER;
}
