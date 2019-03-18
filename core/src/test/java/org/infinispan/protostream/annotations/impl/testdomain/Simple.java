package org.infinispan.protostream.annotations.impl.testdomain;

import org.infinispan.protostream.UnknownFieldSet;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoUnknownFieldSet;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
@ProtoDoc("@TypeId(10)")
public class Simple {

   @ProtoDoc("Simple is elegant")
   @ProtoField(number = 1111)
   public Simple simple;

   @ProtoField(number = 1, required = true, defaultValue = "0.0")
   public float afloat;

   @ProtoField(number = 2)
   public Integer anInteger;

   @ProtoField(number = 314, name = "my_enum_field", defaultValue = "AX")
   public TestEnum myEnumField;

   private Float width = 0.71f;

   public Float getWidth() {
      return width;
   }

   @ProtoField(number = 103, required = true, defaultValue = "0.71")
   public void setWidth(Float width) {
      this.width = width;
   }

   @ProtoUnknownFieldSet
   public UnknownFieldSet unknownFieldSet;
}
