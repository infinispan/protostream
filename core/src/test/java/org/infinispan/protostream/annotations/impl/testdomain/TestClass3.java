package org.infinispan.protostream.annotations.impl.testdomain;

import org.infinispan.protostream.UnknownFieldSet;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoUnknownFieldSet;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public class TestClass3 {

   private TestClass.InnerClass2 inner2;

   private UnknownFieldSet unknownFieldSet;

   @ProtoField(1)
   public TestClass.InnerClass2 getInner2() {
      return inner2;
   }

   public void setInner2(TestClass.InnerClass2 inner2) {
      this.inner2 = inner2;
   }

   public UnknownFieldSet getUnknownFieldSet() {
      return unknownFieldSet;
   }

   @ProtoUnknownFieldSet
   public void setUnknownFieldSet(UnknownFieldSet unknownFieldSet) {
      this.unknownFieldSet = unknownFieldSet;
   }
}
