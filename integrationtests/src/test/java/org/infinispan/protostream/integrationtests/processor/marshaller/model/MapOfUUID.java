package org.infinispan.protostream.integrationtests.processor.marshaller.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.infinispan.protostream.annotations.ProtoField;

public class MapOfUUID {

   @ProtoField(value = 1, mapImplementation = HashMap.class)
   public Map<String, UUID> data;

}
