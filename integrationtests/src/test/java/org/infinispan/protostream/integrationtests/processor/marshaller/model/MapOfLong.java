package org.infinispan.protostream.integrationtests.processor.marshaller.model;

import java.util.HashMap;
import java.util.Map;

import org.infinispan.protostream.annotations.ProtoField;

public class MapOfLong {

   @ProtoField(value = 1, mapImplementation = HashMap.class)
   public Map<String, Long> data;

}
