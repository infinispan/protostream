package org.infinispan.protostream.annotations.impl.processor.tests.testdomain;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

public class Inheritance {
   public static class ParentType {
      final String message;

      @ProtoFactory
      public ParentType(String message) {
         this.message = message;
      }

      @ProtoField(1)
      public String getMessage() {
         return message;
      }
   }

   public static class ChildType extends ParentType {
      @ProtoFactory
      public ChildType(String message) {
         super(message);
      }
   }

   public static class Parent {
      final ParentType field;

      @ProtoFactory
      public Parent(ParentType field) {
         this.field = field;
      }

      @ProtoField(1)
      ParentType getField() {
         return field;
      }
   }

   public static class Child extends Parent {
      @ProtoFactory
      public Child(ChildType field) {
         super(field);
      }

      @ProtoField(1)
      ChildType getField() {
         return (ChildType) super.getField();
      }
   }
}
