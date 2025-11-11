package org.infinispan.protostream.processor.tests.testdomain;

import java.util.List;
import java.util.Map;

import org.infinispan.protostream.annotations.Proto;
import org.infinispan.protostream.annotations.ProtoEnumValue;

@Proto
public record OuterRecord(String aString, int anInt, InnerEnum anEnum, List<String> things, float[] someFloats, Map<String, String> aMapOfStrings) {

   public enum InnerEnum {
      @ProtoEnumValue(number = 0)
      ZERO,
      @ProtoEnumValue(number = 1)
      ONE,
      @ProtoEnumValue(number = 2)
      TWO
   }
}
