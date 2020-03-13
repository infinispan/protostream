package org.infinispan.protostream.annotations.impl.testdomain;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoEnum;
import org.infinispan.protostream.annotations.ProtoEnumValue;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
@ProtoDoc("bla bla bla\nand some more bla")
@ProtoEnum(name = "TestEnumABC")
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

   @ProtoDoc("This should never be read.")
   @ProtoEnumValue(number = 1, name = "CX")
   C() {
      @Override
      public boolean matches() {
         return false;
      }
   };

   public abstract boolean matches();
}
