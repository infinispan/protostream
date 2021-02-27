package org.infinispan.protostream.annotations.impl.types;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import org.junit.Test;

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
