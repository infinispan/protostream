package org.infinispan.protostream.annotations.impl.processor.tests.testdomain;

import java.util.List;
import java.util.Map;

import org.infinispan.protostream.annotations.Proto;

@Proto
public record SimpleRecord(String aString, int anInt, SimpleEnum anEnum, List<String> things, float[] someFloats, Map<String, String> aMapOfStrings) {
}
