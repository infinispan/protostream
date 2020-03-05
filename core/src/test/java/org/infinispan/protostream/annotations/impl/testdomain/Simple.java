package org.infinispan.protostream.annotations.impl.testdomain;

import org.infinispan.protostream.UnknownFieldSet;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoUnknownFieldSet;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
@ProtoDoc("@TypeId(100010)")
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

   @ProtoField(number = 315)
   public TestEnum getEnumMethod() {
      return TestEnum.A;
   }

   public void setEnumMethod(TestEnum ignore) {
   }

   //TODO here we have several cases not covered by tests...
/*
   private Integer x;

   @ProtoDoc("he he he")
   public Integer getX() {
      //No suitable getter method found for property 'x' of type int in class org.infinispan.protostream.annotations.impl.testdomain.Simple. The candidate method does not have a suitable return type: public java.lang.Integer org.infinispan.protostream.annotations.impl.testdomain.Simple.getX()
      return x;
   }

   @ProtoDoc("X is unknown")
   @ProtoField(number = 100, required = true, defaultValue = "0")
   public void setX(int x) {
      this.x = x;
   }
*/
/*
   private int y;

   public int getY() {
      return y;
   }

   //todo test boxing type mismatch between getter, setter and field
   @ProtoField(number = 101, required = true, defaultValue = "0")
   public void setY(Integer y) {
      this.y = y;
   }
*/
/*
   private String z;

   public String getZ() {
      return z;
   }

   @ProtoField(number = 102, required = true, defaultValue = "0")
   public void setZ(String z) {
      this.z = z;
   }
*/

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
