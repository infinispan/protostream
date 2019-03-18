package org.infinispan.protostream.annotations.impl.processor.types;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.infinispan.protostream.annotations.impl.types.XClass;
import org.junit.Test;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
public class MirrorClassFactoryTest {

   @Test
   public void testFromClass() {
      ProcessingEnvironment processingEnvironmentMock = mock(ProcessingEnvironment.class);
      Elements elementsMock = mock(Elements.class);
      Types typesMock = mock(Types.class);
      when(processingEnvironmentMock.getElementUtils()).thenReturn(elementsMock);
      when(processingEnvironmentMock.getTypeUtils()).thenReturn(typesMock);

      TypeElement typeElementMock = mock(TypeElement.class);
      DeclaredType typeMirrorMock = mock(DeclaredType.class);
      when(typeMirrorMock.asElement()).thenReturn(typeElementMock);
      when(typeElementMock.asType()).thenReturn(typeMirrorMock);
      when(typeMirrorMock.getKind()).thenReturn(TypeKind.DECLARED);
      when(typeElementMock.getKind()).thenReturn(ElementKind.CLASS);

      Name nameMock = mock(Name.class);
      when(nameMock.toString()).thenReturn("java.lang.Integer");
      when(typeElementMock.getQualifiedName()).thenReturn(nameMock);

      when(elementsMock.getTypeElement("java.lang.Integer")).thenReturn(typeElementMock);
      when(elementsMock.getBinaryName(typeElementMock)).thenReturn(nameMock);

      MirrorClassFactory typeFactory = new MirrorClassFactory(processingEnvironmentMock);

      XClass integerClass = typeFactory.fromClass(Integer.class);

      assertSame(Integer.class, integerClass.asClass());
      assertFalse(integerClass.isArray());
   }

   @Test
   public void testFromTypeMirror() {
      ProcessingEnvironment processingEnvironmentMock = mock(ProcessingEnvironment.class);
      Elements elementsMock = mock(Elements.class);
      Types typesMock = mock(Types.class);
      when(processingEnvironmentMock.getElementUtils()).thenReturn(elementsMock);
      when(processingEnvironmentMock.getTypeUtils()).thenReturn(typesMock);

      TypeElement typeElementMock = mock(TypeElement.class);
      DeclaredType typeMirrorMock = mock(DeclaredType.class);
      when(typeMirrorMock.asElement()).thenReturn(typeElementMock);
      when(typeElementMock.asType()).thenReturn(typeMirrorMock);
      when(typeMirrorMock.getKind()).thenReturn(TypeKind.DECLARED);
      when(typeElementMock.getKind()).thenReturn(ElementKind.CLASS);

      Name nameMock = mock(Name.class);
      when(nameMock.toString()).thenReturn("java.lang.Integer");
      when(typeElementMock.getQualifiedName()).thenReturn(nameMock);

      when(elementsMock.getTypeElement("java.lang.Integer")).thenReturn(typeElementMock);
      when(elementsMock.getBinaryName(typeElementMock)).thenReturn(nameMock);

      MirrorClassFactory typeFactory = new MirrorClassFactory(processingEnvironmentMock);

      XClass integerClass = typeFactory.fromTypeMirror(typeMirrorMock);

      assertSame(Integer.class, integerClass.asClass());
      assertFalse(integerClass.isArray());
   }
}
