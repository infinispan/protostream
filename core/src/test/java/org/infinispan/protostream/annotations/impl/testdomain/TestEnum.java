package org.infinispan.protostream.annotations.impl.testdomain;

import org.infinispan.protostream.annotations.ProtoComment;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
@ProtoTypeId(100777)
@ProtoComment("bla bla bla\nand some more bla")
@ProtoName("TestEnumABC")
public enum TestEnum {

   @ProtoEnumValue(number = 4, name = "AX")
   A() {
      @Override
      public boolean matches() {
         return false;
      }
   },

   @ProtoEnumValue(number = 2, name = "BX")
   B() {
      @Override
      public boolean matches() {
         return false;
      }
   },

   @ProtoComment("This should never be read.")
   @ProtoEnumValue(number = 1, name = "CX")
   C() {
      @Override
      public boolean matches() {
         return false;
      }
   };

   public abstract boolean matches();
}
