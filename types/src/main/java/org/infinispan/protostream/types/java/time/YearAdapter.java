package org.infinispan.protostream.types.java.time;

import java.time.Year;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;

@ProtoAdapter(Year.class)
public class YearAdapter {
   @ProtoFactory
   Year create(Integer year) {
      return Year.of(year);
   }
   @ProtoField(number = 1, type = Type.INT32)
   Integer getYear(Year year) {
      return year.getValue();
   }
}
