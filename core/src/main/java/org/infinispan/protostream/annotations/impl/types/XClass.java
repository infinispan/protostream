package org.infinispan.protostream.annotations.impl.types;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
public interface XClass extends XElement {

   UnifiedTypeFactory getFactory();

   /**
    * This is the only place we can get back the java.lang.Class and should be used very sparingly. Abuse will void the
    * warranty.
    */
   Class<?> asClass();

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

   Iterable<? extends XMethod> getDeclaredMethods();

   XMethod getMethod(String methodName, XClass... argTypes);

   Iterable<? extends XField> getDeclaredFields();

   /**
    * Is it a local or anonymous class?
    */
   boolean isLocal();
}
