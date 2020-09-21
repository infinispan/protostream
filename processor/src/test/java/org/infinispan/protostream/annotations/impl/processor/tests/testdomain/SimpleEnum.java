package org.infinispan.protostream.annotations.impl.processor.tests.testdomain;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
@ProtoTypeId(100777)
@ProtoDoc("bla bla bla\nand some more bla")
@ProtoName("SimpleEnumABC")
public enum SimpleEnum {

   @ProtoEnumValue(value = 4, name = "AX")
   A,

   @ProtoEnumValue(value = 2, name = "BX")
   B,

   @ProtoDoc("This should never be read.")
   @ProtoEnumValue(value = 1, name = "CX")
   C
}
