package org.infinispan.protostream.annotations.impl.testdomain;

import org.infinispan.custom.annotations.Indexed;
import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.annotations.ProtoComment;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoReserved;
import org.infinispan.protostream.annotations.ProtoReserved.Range;
import org.infinispan.protostream.annotations.impl.testdomain.subpackage.TestClass2;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
@Indexed
@ProtoComment("\nbla bla bla\nand some more bla")
@ProtoReserved(numbers = {17, 42, 13}, ranges = @Range(from = 7777))
@ProtoReserved(names = {"XX", "XY"})
public class TestClass extends TestBaseClass implements TestBaseInterface /*, TestBaseInterface2 */, TestBaseInterface3 {

   //@ProtoField(number = 315)
   public NonProtobufEnum nonProtobufEnum;

   private int age;

   private Integer height;

   @ProtoField(number = 4, required = true)
   @ProtoComment("The surname, of course")
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
   public long longField;

   @ProtoField(number = 1001, defaultValue = "3.14")
   public double doubleField;

   @ProtoField(number = 1002, defaultValue = "3.14")
   public float floatField;

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

   @ProtoComment("InnerClass documentation")
   public static class InnerClass {

      @ProtoComment("some field documentation")
      @ProtoField(number = 42, required = true)
      public int innerAttribute;
   }

   public static class InnerClass2 {

      @ProtoField(number = 42, required = true)
      public int innerAttribute;
   }
}
