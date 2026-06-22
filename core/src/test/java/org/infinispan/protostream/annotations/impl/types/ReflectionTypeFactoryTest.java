package org.infinispan.protostream.annotations.impl.types;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
public class ReflectionTypeFactoryTest {

   @Test
   public void testFromClass() {
      ReflectionTypeFactory typeFactory = new ReflectionTypeFactory();

      XClass integerClass = typeFactory.fromClass(Integer.class);

      assertSame("java.lang.Integer", integerClass.getName());
      assertFalse(integerClass.isArray());
   }
}
