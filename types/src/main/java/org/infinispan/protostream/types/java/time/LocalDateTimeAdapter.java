package org.infinispan.protostream.types.java.time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;

@ProtoAdapter(LocalDateTime.class)
public class LocalDateTimeAdapter {
   @ProtoFactory
   LocalDateTime create(LocalDate localDate, LocalTime localTime) {
      return LocalDateTime.of(localDate, localTime);
   }

   @ProtoField(number = 1, type = Type.MESSAGE)
   LocalDate getLocalDate(LocalDateTime localDateTime) {
      return localDateTime.toLocalDate();
   }

   @ProtoField(number = 2, type = Type.MESSAGE)
   LocalTime getLocalTime(LocalDateTime localDateTime) {
      return localDateTime.toLocalTime()  ;
   }
}
