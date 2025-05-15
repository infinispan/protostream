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

   XEnumConstant getEnumConstant(String name);

   boolean isArray();

   /**
    * Array component type, for arrays only.
    */
   XClass getComponentType();

   XClass getEnclosingClass();

   XClass getSuperclass();

   /**
    * Directly implemented interfaces.
    */
   XClass[] getInterfaces();

   /**
    * Gets the actual type params of an interface implemented directly by this class or all its supers.
    */
   String[] getGenericInterfaceParameterTypes(Class<?> c);

   boolean isAssignableTo(XClass c);

   /**
    * Should only be used with class literals. Any other type of usage should be considered suspect.
    */
   default boolean isAssignableTo(Class<?> c) {
      XClass xc = getFactory().fromClass(c);
      return isAssignableTo(xc);
   }

   XConstructor getDeclaredConstructor(XClass... argTypes);

   Iterable<? extends XConstructor> getDeclaredConstructors();

   Iterable<? extends XMethod> getDeclaredMethods();

   XMethod getMethod(String methodName, XClass... argTypes);

   Iterable<? extends XField> getDeclaredFields();

   Iterable<? extends XRecordComponent> getRecordComponents();

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

   default boolean isRecord() {
      return isAssignableTo(Record.class);
   }
}
