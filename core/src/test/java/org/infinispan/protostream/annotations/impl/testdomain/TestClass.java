package org.infinispan.protostream.annotations.impl.testdomain;

import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoEnum;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.impl.testdomain.subpackage.TestClass2;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
@ProtoDoc("@Indexed()\nbla bla bla\nand some more bla")
public class TestClass extends TestBaseClass implements TestBaseInterface /*, TestBaseInterface2 */, TestBaseInterface3 {

   //@ProtoField(number = 315)
   public NonProtobufEnum nonProtobufEnum;

   private int age;

   private Integer height;

   @ProtoField(number = 4, required = true)
   @ProtoDoc("The surname, of course")
   public String surname;

   @ProtoField(number = 66)
   public TestClass2 testClass2;

   @ProtoField(number = 76)
   public InnerClass inner;

   @ProtoField(number = 77)
   public InnerClass2 inner2;

   @ProtoField(number = 88)
   public WrappedMessage wm;

   @ProtoField(number = 1000, defaultValue = "23")
   public double longField;

   @ProtoField(number = 1001, defaultValue = "3.14")
   public double doubleField;

   @ProtoField(number = 1002, defaultValue = "3.14")
   public double floatField;

   public TestClass() {
   }

   @Override
   public int getAge() {
      return age;
   }

   public void setAge(int age) {
      this.age = age;
   }

   @Override
   public Integer getHeight() {
      return height;
   }

   public void setHeight(Integer height) {
      this.height = height;
   }

   @ProtoDoc("InnerClass documentation")
   public static class InnerClass {

      @ProtoDoc("some field documentation")
      @ProtoField(number = 42, required = true)
      public int innerAttribute;
   }

   public static class InnerClass2 {

      @ProtoField(number = 42, required = true)
      public int innerAttribute;
   }
}
