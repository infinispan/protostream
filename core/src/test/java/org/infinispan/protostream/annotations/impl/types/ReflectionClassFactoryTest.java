package org.infinispan.protostream.annotations.impl.types;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import org.junit.Test;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
public class ReflectionClassFactoryTest {

   @Test
   public void testFromClass() {
      ReflectionClassFactory typeFactory = new ReflectionClassFactory();

      XClass integerClass = typeFactory.fromClass(Integer.class);

      assertSame("java.lang.Integer", integerClass.getName());
      assertFalse(integerClass.isArray());
   }
}
