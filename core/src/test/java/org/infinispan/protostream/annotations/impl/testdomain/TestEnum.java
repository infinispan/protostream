package org.infinispan.protostream.annotations.impl.testdomain;

import org.infinispan.protostream.annotations.ProtoEnum;
import org.infinispan.protostream.annotations.ProtoEnumValue;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
@ProtoEnum(name = "TestEnumABC")
public enum TestEnum {

   @ProtoEnumValue(number = 4, name = "AX")
   A,

   @ProtoEnumValue(number = 2, name = "BX")
   B,

   @ProtoEnumValue(number = 1, name = "CX")
   C
}
