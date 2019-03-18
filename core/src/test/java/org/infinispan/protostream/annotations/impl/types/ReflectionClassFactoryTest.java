package org.infinispan.protostream.annotations.impl.types;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import javax.lang.model.type.DeclaredType;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
public class ReflectionClassFactoryTest {

   @Rule
   public ExpectedException expectedException = ExpectedException.none();

   @Test
   public void testFromClass() {
      ReflectionClassFactory typeFactory = new ReflectionClassFactory();

      XClass integerClass = typeFactory.fromClass(Integer.class);

      assertSame(Integer.class, integerClass.asClass());
      assertFalse(integerClass.isArray());
   }

   @Test
   public void testFromTypeMirror() {
      expectedException.expect(UnsupportedOperationException.class);
      expectedException.expectMessage("javax.lang.model type mirrors are only supported at compile time. Please use reflection during run time.");

      ReflectionClassFactory typeFactory = new ReflectionClassFactory();

      DeclaredType typeMirrorMock = mock(DeclaredType.class);

      typeFactory.fromTypeMirror(typeMirrorMock);
   }
}
