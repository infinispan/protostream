package org.infinispan.protostream.integrationtests.processor.marshaller.model;

import java.util.HashMap;
import java.util.Map;

import org.infinispan.protostream.annotations.ProtoField;

public class MapOfMapOfUUID {

   @ProtoField(value = 1, mapImplementation = HashMap.class)
   public Map<String, String> data1;

   @ProtoField(2)
   public MapOfUUID data2;

   @ProtoField(3)
   public SimpleEnum data3;

}
