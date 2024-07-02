package org.infinispan.protostream.integrationtests.processor.marshaller.model;

import org.infinispan.protostream.annotations.Proto;

@Proto
public record SimpleRecord(String string, Integer boxedInt) {
}
