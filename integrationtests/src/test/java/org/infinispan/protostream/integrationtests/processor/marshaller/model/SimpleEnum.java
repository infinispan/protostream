package org.infinispan.protostream.integrationtests.processor.marshaller.model;

import org.infinispan.protostream.annotations.Proto;
import org.infinispan.protostream.annotations.ProtoTypeId;

@ProtoTypeId(111111) // large enough so it does not conflict
@Proto
public enum SimpleEnum {

   FIRST, SECOND

}
