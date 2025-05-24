package org.infinispan.protostream.processor.tests.testdomain;

import org.infinispan.protostream.annotations.ProtoComment;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
@ProtoTypeId(100777)
@ProtoComment("bla bla bla\nand some more bla")
@ProtoName("SimpleEnumABC")
public enum SimpleEnum {

   @ProtoEnumValue(value = 0, name = "AX")
   A,

   @ProtoEnumValue(value = 2, name = "BX")
   B,

   @ProtoComment("This should never be read.")
   @ProtoEnumValue(value = 1, name = "CX")
   C
}
