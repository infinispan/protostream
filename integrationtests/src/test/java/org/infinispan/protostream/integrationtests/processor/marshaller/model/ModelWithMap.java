package org.infinispan.protostream.integrationtests.processor.marshaller.model;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.infinispan.protostream.annotations.ProtoField;

public class ModelWithMap {

    private Map<String, Integer> simpleMap;
    private Map<String, SimpleEnum> enumMap;
    private Map<String, UUID> adapterMap;

    @ProtoField(value = 1, mapImplementation = ConcurrentHashMap.class)
    public Map<String, Integer> getSimpleMap() {
        return simpleMap;
    }

    public void setSimpleMap(Map<String, Integer> simpleMap) {
        this.simpleMap = simpleMap;
    }

    @ProtoField(value = 2, mapImplementation = ConcurrentHashMap.class)
    public Map<String, UUID> getAdapterMap() {
        return adapterMap;
    }

    public void setAdapterMap(Map<String, UUID> adapterMap) {
        this.adapterMap = adapterMap;
    }

    @ProtoField(value = 3, mapImplementation = ConcurrentHashMap.class)
    public Map<String, SimpleEnum> getEnumMap() {
        return enumMap;
    }

    public void setEnumMap(Map<String, SimpleEnum> enumMap) {
        this.enumMap = enumMap;
    }
}


