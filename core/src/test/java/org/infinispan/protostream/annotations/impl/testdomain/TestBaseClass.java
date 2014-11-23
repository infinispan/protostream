package org.infinispan.protostream.annotations.impl.testdomain;

import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;

import java.util.List;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public class TestBaseClass {

   @ProtoField(number = 314, name = "my_enum_field")
   public TestEnum myEnumField;

   @ProtoField(number = 777, collectionImplementation = java.util.LinkedList.class)
   public List<Integer> ints;

   @ProtoField(number = 888)
   public Float floats;

   @ProtoField(number = 999, type = Type.MESSAGE, required = true)
   public float x;

   private String name;

   @ProtoField(number = 111, defaultValue = "red")
   public String color;

   @ProtoField(number = 6)
   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getColor() {
      return color;
   }

   public void setColor(String color) {
      this.color = color;
   }
}
