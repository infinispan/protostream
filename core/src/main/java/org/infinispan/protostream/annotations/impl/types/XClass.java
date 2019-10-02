package org.infinispan.protostream.annotations.impl.types;

import java.lang.reflect.Modifier;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
public interface XClass extends XElement {

   XTypeFactory getFactory();

   /**
    * This is the only place we can get back the java.lang.Class object and should be used very sparingly. Some
    * implementations can throw {@link UnsupportedOperationException}.
    */
   @Deprecated
   Class<?> asClass() throws UnsupportedOperationException;

   String getSimpleName();

   String getCanonicalName();

   String getPackageName();

   boolean isPrimitive();

   boolean isEnum();

   /**
    * Enum constants, for enums only.
    */
   Iterable<? extends XEnumConstant> getEnumConstants();

   boolean isArray();

   /**
    * Array component type, for arrays only.
    */
   XClass getComponentType();

   XClass getEnclosingClass();

   XClass getSuperclass();

   XClass[] getInterfaces();

   boolean isAssignableTo(XClass c);

   XConstructor getDeclaredConstructor(XClass... argTypes);

   Iterable<? extends XConstructor> getDeclaredConstructors();

   Iterable<? extends XMethod> getDeclaredMethods();

   XMethod getMethod(String methodName, XClass... argTypes);

   Iterable<? extends XField> getDeclaredFields();

   /**
    * Is it a local or anonymous class?
    */
   boolean isLocal();

   default boolean isAbstract() {
      return Modifier.isAbstract(getModifiers());
   }

   default boolean isInterface() {
      return Modifier.isInterface(getModifiers());
   }
}
