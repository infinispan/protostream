package org.infinispan.custom.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MyCustomAnnotation {
   String name() default "";
   int someInteger() default 0;
   long someLong() default 0;
   boolean someBool() default false;
   MyEnum someEnum() default MyEnum.ONE;

   public static enum MyEnum {
      ONE,
      TWO
   }
}
