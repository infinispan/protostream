package org.infinispan.protostream.annotations.impl.testdomain.subpackage;

import org.infinispan.protostream.UnknownFieldSet;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoUnknownFieldSet;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public class TestClass2 {

   @ProtoField(number = 3)
   public String address;

   private UnknownFieldSet unknownFieldSet;

   @ProtoUnknownFieldSet
   public UnknownFieldSet getUnknownFieldSet() {
      return unknownFieldSet;
   }

   public void setUnknownFieldSet(UnknownFieldSet unknownFieldSet) {
      this.unknownFieldSet = unknownFieldSet;
   }
}
