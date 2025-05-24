package org.infinispan.protostream.processor.tests.testdomain;

import org.infinispan.custom.annotations.MyCustomAnnotation;
import org.infinispan.custom.annotations.TypeId;
import org.infinispan.protostream.UnknownFieldSet;
import org.infinispan.protostream.annotations.ProtoComment;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoUnknownFieldSet;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
@TypeId(100010)
public class SimpleClass {

   @ProtoComment("Simple is elegant")
   @ProtoField(number = 1111)
   @MyCustomAnnotation(name = "simpleClassField", someBool = true, someEnum = MyCustomAnnotation.MyEnum.TWO, someLong = -100, someInteger = 100)
   public SimpleClass simpleClass;

   @ProtoField(number = 1, defaultValue = "0.0")
   public float afloat;

   @ProtoField(number = 2)
   public Integer anInteger;

   @ProtoField(number = 314, name = "my_enum_field", defaultValue = "AX")
   public SimpleEnum myEnumField;

   @ProtoField(number = 400, name ="my_record")
   public SimpleRecord rec;

   //TODO here we have several cases not covered by tests...
/*
   private Integer x;

   @ProtoDoc("he he he")
   public Integer getX() {
      //No suitable getter method found for property 'x' of type int in class org.infinispan.protostream.annotations.impl.testdomain.Simple. The candidate method does not have a suitable return type: public java.lang.Integer org.infinispan.protostream.annotations.impl.testdomain.Simple.getX()
      return x;
   }

   @ProtoDoc("X is unknown")
   @ProtoField(number = 100)
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
   @ProtoField(number = 101)
   public void setY(Integer y) {
      this.y = y;
   }
*/
/*
   private String z;

   public String getZ() {
      return z;
   }

   @ProtoField(number = 102)
   public void setZ(String z) {
      this.z = z;
   }
*/

   private Float width = 0.71f;

   public Float getWidth() {
      return width;
   }

   @ProtoField(number = 103, defaultValue = "0.71")
   @MyCustomAnnotation(name = "setWidthMethod", someBool = true, someEnum = MyCustomAnnotation.MyEnum.TWO, someLong = -100, someInteger = 100)
   public void setWidth(Float width) {
      this.width = width;
   }

   @ProtoUnknownFieldSet
   public UnknownFieldSet unknownFieldSet;
}
