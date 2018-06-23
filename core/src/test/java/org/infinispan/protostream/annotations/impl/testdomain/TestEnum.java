package org.infinispan.protostream.annotations.impl.testdomain;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoName;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
@ProtoDoc("bla bla bla\nand some more bla")
@ProtoName("TestEnumABC")
public enum TestEnum {

   @ProtoEnumValue(number = 4, name = "AX")
   A,

   @ProtoEnumValue(number = 2, name = "BX")
   B,

   @ProtoDoc("This should never be read.")
   @ProtoEnumValue(number = 1, name = "CX")
   C
}
