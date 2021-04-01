package org.infinispan.protostream.annotations.impl.types;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.infinispan.protostream.annotations.ProtoDoc;

//todo [anistor] revisit this and improve handling of generic types based on ideas from
// https://github.com/XDean/Java-EX/blob/297b21df13ebe25e991ec3741af21b3592e92375/src/main/java/xdean/jex/util/reflect/GenericUtil.java

/**
 * Implementation relying on reflection.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
public final class ReflectionTypeFactory implements XTypeFactory {

   private final Map<Class<?>, XClass> classCache = new HashMap<>();

   public ReflectionTypeFactory() {
   }

   @Override
   public XClass fromClass(Class<?> c) {
      if (c == null) {
         return null;
      }

      XClass xclass = classCache.get(c);
      if (xclass == null) {
         xclass = new ReflectionClass(c);
         classCache.put(c, xclass);
      }
      return xclass;
   }

   private static Class<?> determineCollectionElementType(Type genericType) {
      if (genericType instanceof ParameterizedType) {
         Type[] actualTypeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
         if (actualTypeArguments.length == 1) {
            Type typeArg = actualTypeArguments[0];
            if (typeArg instanceof Class) {
               return (Class<?>) typeArg;
            }
            if (typeArg instanceof TypeVariable) {
               return (Class<?>) ((TypeVariable<?>) typeArg).getBounds()[0];
            }
            return (Class<?>) ((ParameterizedType) typeArg).getRawType();
         }
      } else if (genericType instanceof Class) {
         Class<?> c = (Class<?>) genericType;
         if (c.getGenericSuperclass() != null && Collection.class.isAssignableFrom(c.getSuperclass())) {
            Class<?> x = determineCollectionElementType(c.getGenericSuperclass());
            if (x != null) {
               return x;
            }
         }
         for (Type t : c.getGenericInterfaces()) {
            if (t instanceof Class && Collection.class.isAssignableFrom((Class<?>) t)
                  || t instanceof ParameterizedType && Collection.class.isAssignableFrom((Class<?>) ((ParameterizedType) t).getRawType())) {
               Class<?> x = determineCollectionElementType(t);
               if (x != null) {
                  return x;
               }
            }
         }
      }
      return null;
   }

   private final class ReflectionClass implements XClass {

      private final Class<?> clazz;

      private final Map<Field, ReflectionEnumConstant> enumConstants;

      private final Map<Constructor<?>, ReflectionConstructor> constructorCache = new HashMap<>();

      private final Map<Method, ReflectionMethod> methodCache = new HashMap<>();

      private final Map<Field, ReflectionField> fieldCache = new HashMap<>();

      ReflectionClass(Class<?> clazz) {
         this.clazz = clazz;

         if (clazz.isEnum()) {
            enumConstants = new LinkedHashMap<>();
            for (Field f : clazz.getDeclaredFields()) {
               if (f.isEnumConstant()) {
                  Enum<?> e;
                  try {
                     f.setAccessible(true);
                     e = (Enum<?>) f.get(clazz);
                  } catch (IllegalAccessException iae) {
                     // this is never going to happen, enum constants are always accessible
                     throw new IllegalStateException("Failed to access enum constant field", iae);
                  }
                  enumConstants.put(f, new ReflectionEnumConstant(this, e, f));
               }
            }
         } else {
            enumConstants = null;
         }
      }

      @Override
      public XTypeFactory getFactory() {
         return ReflectionTypeFactory.this;
      }

      @Override
      public Class<?> asClass() {
         return clazz;
      }

      @Override
      public String getName() {
         return clazz.getName();
      }

      @Override
      public String getSimpleName() {
         return clazz.getSimpleName();
      }

      @Override
      public String getCanonicalName() {
         return clazz.getCanonicalName();
      }

      @Override
      public String getPackageName() {
         return clazz.getPackage().getName();
      }

      @Override
      public boolean isPrimitive() {
         return clazz.isPrimitive();
      }

      @Override
      public boolean isEnum() {
         return clazz.isEnum();
      }

      @Override
      public boolean isArray() {
         return clazz.isArray();
      }

      @Override
      public XClass getComponentType() {
         if (!clazz.isArray()) {
            throw new IllegalStateException(getName() + " is not an array");
         }
         return fromClass(clazz.getComponentType());
      }

      @Override
      public XClass getEnclosingClass() {
         return fromClass(clazz.getEnclosingClass());
      }

      @Override
      public XClass getSuperclass() {
         return fromClass(clazz.getSuperclass());
      }

      @Override
      public XClass[] getInterfaces() {
         Class<?>[] interfaces = clazz.getInterfaces();
         XClass[] xInterfaces = new XClass[interfaces.length];
         for (int i = 0; i < interfaces.length; i++) {
            xInterfaces[i] = fromClass(interfaces[i]);
         }
         return xInterfaces;
      }

      //todo [anistor] Needs further testing! ATM this is not used because we're generating 'type-erased' code.
      @Override
      public String[] getGenericInterfaceParameterTypes(Class<?> c) {
         for (Class<?> i : clazz.getInterfaces()) {
            if (c.isAssignableFrom(i)) {
               TypeVariable<? extends Class<?>>[] typeParameters = i.getTypeParameters();
               String[] params = new String[typeParameters.length];
               int j = 0;
               for (TypeVariable<?> t : typeParameters) {
                  params[j++] = t.getGenericDeclaration().toString();
               }
               return params;
            }
         }
         XClass superclass = getSuperclass();
         return superclass != null ? superclass.getGenericInterfaceParameterTypes(c) : null;
      }

      @Override
      public boolean isAssignableTo(XClass other) {
         if (this == other) {
            return true;
         }
         return other.asClass().isAssignableFrom(clazz);
      }

      @Override
      public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
         return clazz.getAnnotation(annotationClass);
      }

      @Override
      public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
         return clazz.getAnnotationsByType(annotationClass);
      }

      @Override
      public String getProtoDocs() {
         return DocumentationExtractor.getDocumentation(clazz.getAnnotationsByType(ProtoDoc.class));
      }

      @Override
      public int getModifiers() {
         return clazz.getModifiers();
      }

      private ReflectionMethod cacheMethod(Method method) {
         if (method == null) {
            return null;
         }
         ReflectionClass declaringClass = (ReflectionClass) fromClass(method.getDeclaringClass());
         ReflectionMethod xmethod = declaringClass.methodCache.get(method);
         if (xmethod == null) {
            xmethod = new ReflectionMethod(declaringClass, method);
            declaringClass.methodCache.put(method, xmethod);
         }
         return xmethod;
      }

      private ReflectionConstructor cacheConstructor(Constructor<?> ctor) {
         if (ctor == null) {
            return null;
         }
         ReflectionConstructor xctor = constructorCache.get(ctor);
         if (xctor == null) {
            xctor = new ReflectionConstructor(this, ctor);
            constructorCache.put(ctor, xctor);
         }
         return xctor;
      }

      private ReflectionField cacheField(Field field) {
         ReflectionClass declaringClass = (ReflectionClass) fromClass(field.getDeclaringClass());
         ReflectionField xfield = declaringClass.fieldCache.get(field);
         if (xfield == null) {
            XEnumConstant enumConstant = field.isEnumConstant() ? declaringClass.enumConstants.get(field) : null;
            xfield = new ReflectionField(declaringClass, field, enumConstant);
            declaringClass.fieldCache.put(field, xfield);
         }
         return xfield;
      }

      @Override
      public XConstructor getDeclaredConstructor(XClass... xArgTypes) {
         Class<?>[] argTypes = argsAsClass(xArgTypes);
         Constructor<?> ctor;
         try {
            ctor = clazz.getDeclaredConstructor(argTypes);
         } catch (NoSuchMethodException e) {
            return null;
         }
         return cacheConstructor(ctor);
      }

      @Override
      public Iterable<? extends XConstructor> getDeclaredConstructors() {
         List<XConstructor> constructors = new ArrayList<>();
         for (Constructor<?> c : clazz.getDeclaredConstructors()) {
            constructors.add(cacheConstructor(c));
         }
         return constructors;
      }

      @Override
      public Iterable<? extends XMethod> getDeclaredMethods() {
         List<XMethod> methods = new ArrayList<>();
         for (Method m : clazz.getDeclaredMethods()) {
            methods.add(cacheMethod(m));
         }
         return methods;
      }

      @Override
      public XMethod getMethod(String methodName, XClass... xArgTypes) {
         Method method = findMethod(clazz, methodName, argsAsClass(xArgTypes));
         return method == null ? null : cacheMethod(method);
      }

      /**
       * Finds a method given its name and argument types in this class, superclass and all superinterfaces.
       */
      private Method findMethod(Class<?> clazz, String methodName, Class<?>[] argTypes) {
         Method method = null;
         try {
            method = clazz.getDeclaredMethod(methodName, argTypes);
         } catch (NoSuchMethodException e) {
            if (clazz.getSuperclass() != null) {
               method = findMethod(clazz.getSuperclass(), methodName, argTypes);
            }
            if (method == null) {
               for (Class<?> i : clazz.getInterfaces()) {
                  method = findMethod(i, methodName, argTypes);
                  if (method != null) {
                     break;
                  }
               }
            }
         }
         return method;
      }

      private Class<?>[] argsAsClass(XClass[] xArgTypes) {
         Class<?>[] argTypes = null;
         if (xArgTypes != null) {
            argTypes = new Class[xArgTypes.length];
            for (int i = 0; i < xArgTypes.length; i++) {
               argTypes[i] = xArgTypes[i].asClass();
            }
         }
         return argTypes;
      }

      @Override
      public boolean isLocal() {
         return clazz.getEnclosingMethod() != null || clazz.getEnclosingConstructor() != null;
      }

      @Override
      public Iterable<? extends XField> getDeclaredFields() {
         List<XField> fields = new ArrayList<>();
         for (Field f : clazz.getDeclaredFields()) {
            fields.add(cacheField(f));
         }
         return fields;
      }

      @Override
      public Iterable<? extends XEnumConstant> getEnumConstants() {
         if (enumConstants != null) {
            return enumConstants.values();
         }
         throw new IllegalStateException(getName() + " is not an enum");
      }

      @Override
      public XEnumConstant getEnumConstant(String name) {
         if (enumConstants == null) {
            throw new IllegalStateException(getName() + " is not an enum");
         }
         for (Field f : enumConstants.keySet()) {
            if (name.equals(f.getName())) {
               return enumConstants.get(f);
            }
         }
         return null;
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         }
         if (!(obj instanceof ReflectionClass)) {
            return false;
         }
         ReflectionClass other = (ReflectionClass) obj;
         return clazz == other.clazz;
      }

      @Override
      public int hashCode() {
         return clazz.hashCode();
      }

      @Override
      public String toString() {
         return clazz.toString();
      }
   }

   private static final class ReflectionEnumConstant implements XEnumConstant {

      private final XClass declaringClass;
      private final Enum<?> e;
      private final Field f;

      private ReflectionEnumConstant(XClass declaringClass, Enum<?> e, Field f) {
         this.declaringClass = declaringClass;
         this.e = e;
         this.f = f;
      }

      @Override
      public int getOrdinal() {
         return e.ordinal();
      }

      @Override
      public String getName() {
         return e.name();
      }

      @Override
      public int getModifiers() {
         return f.getModifiers();
      }

      @Override
      public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
         return f.getAnnotation(annotationClass);
      }

      @Override
      public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
         return f.getAnnotationsByType(annotationClass);
      }

      @Override
      public String getProtoDocs() {
         return DocumentationExtractor.getDocumentation(f.getAnnotationsByType(ProtoDoc.class));
      }

      @Override
      public XClass getDeclaringClass() {
         return declaringClass;
      }
   }

   private final class ReflectionMethod implements XMethod {

      private final ReflectionClass declaringClass;

      private final Method method;

      ReflectionMethod(ReflectionClass declaringClass, Method method) {
         this.declaringClass = declaringClass;
         this.method = method;
      }

      @Override
      public XClass getReturnType() {
         return fromClass(method.getReturnType());
      }

      @Override
      public XClass determineRepeatedElementType() {
         Class<?> returnType = unwrapOptionalReturnType();
         if (returnType.isArray()) {
            return fromClass(returnType.getComponentType());
         }
         if (Collection.class.isAssignableFrom(returnType)) {
            Class<?> c = determineCollectionElementType(unwrapOptionalReturnTypeGeneric());
            if (c == null) {
               throw new IllegalStateException("Failed to determine element type of collection class " + c);
            }
            return fromClass(c);
         }
         throw new IllegalStateException("Not a repeatable field");
      }

      @Override
      public XClass determineOptionalReturnType() {
         return fromClass(unwrapOptionalReturnType());
      }

      private Class<?> unwrapOptionalReturnType() {
         Type t = unwrapOptionalReturnTypeGeneric();
         if (t instanceof ParameterizedType) {
            t = ((ParameterizedType) t).getRawType();
         }
         return (Class<?>) t;
      }

      private Type unwrapOptionalReturnTypeGeneric() {
         if (Optional.class == method.getReturnType()) {
            return ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
         }
         return method.getGenericReturnType();
      }

      @Override
      public int getParameterCount() {
         return method.getParameterCount();
      }

      @Override
      public String[] getParameterNames() {
         Parameter[] parameters = method.getParameters();
         String[] parameterNames = new String[parameters.length];
         for (int i = 0; i < parameters.length; i++) {
            if (!parameters[i].isNamePresent()) {
               throw new IllegalStateException("Method parameter names are not present in compiled class file: "
                     + method.getDeclaringClass().getName() + ". Please enable them at compile time.");
            }
            parameterNames[i] = parameters[i].getName();
         }
         return parameterNames;
      }

      @Override
      public XClass[] getParameterTypes() {
         Class<?>[] paramTypes = method.getParameterTypes();
         XClass[] xparamTypes = new XClass[paramTypes.length];
         for (int i = 0; i < paramTypes.length; i++) {
            xparamTypes[i] = fromClass(paramTypes[i]);
         }
         return xparamTypes;
      }

      @Override
      public String getName() {
         return method.getName();
      }

      @Override
      public int getModifiers() {
         return method.getModifiers();
      }

      @Override
      public XClass getDeclaringClass() {
         return declaringClass;
      }

      @Override
      public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
         return method.getAnnotation(annotationClass);
      }

      @Override
      public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
         return method.getAnnotationsByType(annotationClass);
      }

      @Override
      public String getProtoDocs() {
         return DocumentationExtractor.getDocumentation(method.getAnnotationsByType(ProtoDoc.class));
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         }
         if (obj == null || obj.getClass() != ReflectionMethod.class) {
            return false;
         }
         return method.equals(((ReflectionMethod) obj).method);
      }

      @Override
      public int hashCode() {
         return method.hashCode();
      }

      @Override
      public String toString() {
         return method.toString();
      }

      @Override
      public String toGenericString() {
         return method.toGenericString();
      }
   }

   private final class ReflectionConstructor implements XConstructor {

      private final ReflectionClass declaringClass;

      private final Constructor<?> constructor;

      ReflectionConstructor(ReflectionClass declaringClass, Constructor<?> constructor) {
         this.declaringClass = declaringClass;
         this.constructor = constructor;
      }

      @Override
      public int getParameterCount() {
         return constructor.getParameterCount();
      }

      @Override
      public String[] getParameterNames() {
         Parameter[] parameters = constructor.getParameters();
         String[] parameterNames = new String[parameters.length];
         for (int i = 0; i < parameters.length; i++) {
            if (!parameters[i].isNamePresent()) {
               throw new IllegalStateException("Method parameter names are not present in compiled class file: "
                     + constructor.getDeclaringClass().getName() + ". Please enable them at compile time.");
            }
            parameterNames[i] = parameters[i].getName();
         }
         return parameterNames;
      }

      @Override
      public XClass[] getParameterTypes() {
         Class<?>[] parameterTypes = constructor.getParameterTypes();
         XClass[] xparameterTypes = new XClass[parameterTypes.length];
         for (int i = 0; i < parameterTypes.length; i++) {
            xparameterTypes[i] = fromClass(parameterTypes[i]);
         }
         return xparameterTypes;
      }

      @Override
      public String getName() {
         return constructor.getName();
      }

      @Override
      public int getModifiers() {
         return constructor.getModifiers();
      }

      @Override
      public XClass getDeclaringClass() {
         return declaringClass;
      }

      @Override
      public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
         return constructor.getAnnotation(annotationClass);
      }

      @Override
      public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
         return constructor.getAnnotationsByType(annotationClass);
      }

      @Override
      public String getProtoDocs() {
         // no @ProtoDoc allowed on constructors
         return null;
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         }
         if (obj == null || obj.getClass() != ReflectionConstructor.class) {
            return false;
         }
         return constructor.equals(((ReflectionConstructor) obj).constructor);
      }

      @Override
      public int hashCode() {
         return constructor.hashCode();
      }

      @Override
      public String toString() {
         return constructor.toString();
      }

      @Override
      public String toGenericString() {
         return constructor.toGenericString();
      }
   }

   private final class ReflectionField implements XField {

      private final ReflectionClass declaringClass;

      private final Field field;

      private final XEnumConstant enumConstant;

      ReflectionField(ReflectionClass declaringClass, Field field, XEnumConstant enumConstant) {
         this.declaringClass = declaringClass;
         this.field = field;
         this.enumConstant = enumConstant;
      }

      @Override
      public XClass getType() {
         return fromClass(field.getType());
      }

      @Override
      public XClass determineRepeatedElementType() {
         if (field.getType().isArray()) {
            return fromClass(field.getType().getComponentType());
         }
         if (Collection.class.isAssignableFrom(field.getType())) {
            Class<?> c = determineCollectionElementType(field.getGenericType());
            if (c == null) {
               throw new IllegalStateException("Failed to determine element type of collection class " + c);
            }
            return fromClass(c);
         }
         throw new IllegalStateException("Not a repeatable field");
      }

      @Override
      public boolean isEnumConstant() {
         return enumConstant != null;
      }

      @Override
      public XEnumConstant asEnumConstant() {
         if (enumConstant != null) {
            return enumConstant;
         }
         throw new IllegalStateException(getName() + " is not an enum constant");
      }

      @Override
      public String getName() {
         return field.getName();
      }

      @Override
      public int getModifiers() {
         return field.getModifiers();
      }

      @Override
      public XClass getDeclaringClass() {
         return declaringClass;
      }

      @Override
      public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
         return field.getAnnotation(annotationClass);
      }

      @Override
      public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
         return field.getAnnotationsByType(annotationClass);
      }

      @Override
      public String getProtoDocs() {
         return DocumentationExtractor.getDocumentation(field.getAnnotationsByType(ProtoDoc.class));
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         }
         if (obj == null || obj.getClass() != ReflectionField.class) {
            return false;
         }
         return field.equals(((ReflectionField) obj).field);
      }

      @Override
      public int hashCode() {
         return field.hashCode();
      }

      @Override
      public String toString() {
         return field.toString();
      }
   }
}
