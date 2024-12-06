package org.infinispan.protostream.annotations.impl.processor.types;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.infinispan.protostream.annotations.impl.processor.AutoProtoSchemaBuilderAnnotationProcessor;
import org.infinispan.protostream.annotations.impl.types.DocumentationExtractor;
import org.infinispan.protostream.annotations.impl.types.ReflectionTypeFactory;
import org.infinispan.protostream.annotations.impl.types.XClass;
import org.infinispan.protostream.annotations.impl.types.XConstructor;
import org.infinispan.protostream.annotations.impl.types.XEnumConstant;
import org.infinispan.protostream.annotations.impl.types.XField;
import org.infinispan.protostream.annotations.impl.types.XMethod;
import org.infinispan.protostream.annotations.impl.types.XTypeFactory;
import org.infinispan.protostream.impl.Log;

/**
 * Implementation relying primarily on {@link javax.lang.model.type.TypeMirror}, but also capable to use reflection
 * similarly to {@link ReflectionTypeFactory}.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
public final class MirrorTypeFactory implements XTypeFactory {

   private static final Log log = Log.LogFactory.getLog(MirrorTypeFactory.class);

   private static final XClass[] EMPTY_XCLASS_ARRAY = new XClass[0];

   private final Elements elements;

   private final Types types;

   private final Map<String, XClass> classCache = new HashMap<>();

   private final MirrorPrimitiveType voidType;

   private final MirrorPrimitiveType booleanType;

   private final MirrorPrimitiveType byteType;

   private final MirrorPrimitiveType shortType;

   private final MirrorPrimitiveType intType;

   private final MirrorPrimitiveType longType;

   private final MirrorPrimitiveType charType;

   private final MirrorPrimitiveType floatType;

   private final MirrorPrimitiveType doubleType;

   private final boolean fullyQualifiedAnnotations;

   public MirrorTypeFactory(ProcessingEnvironment processingEnv) {
      elements = processingEnv.getElementUtils();
      types = processingEnv.getTypeUtils();
      fullyQualifiedAnnotations = Boolean.parseBoolean(processingEnv.getOptions().get(AutoProtoSchemaBuilderAnnotationProcessor.FULLY_QUALIFIED_ANNOTATIONS));
      voidType = new MirrorPrimitiveType(void.class, types.getNoType(TypeKind.VOID));
      booleanType = new MirrorPrimitiveType(boolean.class, types.getPrimitiveType(TypeKind.BOOLEAN));
      byteType = new MirrorPrimitiveType(byte.class, types.getPrimitiveType(TypeKind.BYTE));
      shortType = new MirrorPrimitiveType(short.class, types.getPrimitiveType(TypeKind.SHORT));
      intType = new MirrorPrimitiveType(int.class, types.getPrimitiveType(TypeKind.INT));
      longType = new MirrorPrimitiveType(long.class, types.getPrimitiveType(TypeKind.LONG));
      charType = new MirrorPrimitiveType(char.class, types.getPrimitiveType(TypeKind.CHAR));
      floatType = new MirrorPrimitiveType(float.class, types.getPrimitiveType(TypeKind.FLOAT));
      doubleType = new MirrorPrimitiveType(double.class, types.getPrimitiveType(TypeKind.DOUBLE));
   }

   @Override
   public XClass fromClass(Class<?> c) {
      if (c == null) {
         return null;
      }

      if (c == byte[].class) {
         XClass componentType = byteType;
         String fqn = "[" + componentType.getName();
         return classCache.computeIfAbsent(fqn, k -> new MirrorArray(componentType));
      }
      if (c == void.class) {
         return voidType;
      }
      if (c == boolean.class) {
         return booleanType;
      }
      if (c == byte.class) {
         return byteType;
      }
      if (c == short.class) {
         return shortType;
      }
      if (c == int.class) {
         return intType;
      }
      if (c == long.class) {
         return longType;
      }
      if (c == char.class) {
         return charType;
      }
      if (c == float.class) {
         return floatType;
      }
      if (c == double.class) {
         return doubleType;
      }

      String typeName = c.getCanonicalName();
      if (typeName == null) {
         typeName = c.getName();
      }
      TypeElement typeElement = elements.getTypeElement(typeName);
      if (typeElement == null) {
         // this should never happen because once we have a java.lang.Class instance we should always be able to obtain its TypeElement
         throw new IllegalStateException("Type not found : " + typeName);
      }
      return fromTypeMirror(typeElement.asType());
   }

   /**
    * Wraps the given TypeMirror in an XClass. Implementation must be idempotent. The returned value must be
    * reference-identical.
    *
    * @param typeMirror can be null
    * @return the wrapper XClass, or null iff the actual argument is null
    */
   public XClass fromTypeMirror(TypeMirror typeMirror) {
      if (typeMirror == null) {
         return null;
      }

      switch (typeMirror.getKind()) {
         case ERROR:
            throw new IllegalStateException("Unresolved type : " + typeMirror.toString());
         case VOID:
            return voidType;
         case BOOLEAN:
            return booleanType;
         case BYTE:
            return byteType;
         case SHORT:
            return shortType;
         case INT:
            return intType;
         case LONG:
            return longType;
         case CHAR:
            return charType;
         case FLOAT:
            return floatType;
         case DOUBLE:
            return doubleType;
         case DECLARED: {
            DeclaredType declaredType = (DeclaredType) typeMirror;
            String fqn = ((TypeElement) declaredType.asElement()).getQualifiedName().toString();
            return classCache.computeIfAbsent(fqn, k -> new MirrorClass(declaredType));
         }
         case ARRAY: {
            XClass componentType = fromTypeMirror(((ArrayType) typeMirror).getComponentType());
            String fqn = "[" + componentType.getName();
            return classCache.computeIfAbsent(fqn, k -> new MirrorArray(componentType));
         }
         default:
            throw new IllegalStateException("Unexpected type kind : " + typeMirror.getKind());
      }
   }

   /**
    * Translate the given element modifiers from javax.lang.model.element.Modifier to java.lang.reflect.Modifier.
    */
   private static int getModifiersOfElement(Element element) {
      int modifiers = 0;
      for (Modifier mod : element.getModifiers()) {
         switch (mod) {
            case DEFAULT:
               // lost in translation
               break;
            case PUBLIC:
               modifiers |= java.lang.reflect.Modifier.PUBLIC;
               break;
            case PROTECTED:
               modifiers |= java.lang.reflect.Modifier.PROTECTED;
               break;
            case PRIVATE:
               modifiers |= java.lang.reflect.Modifier.PRIVATE;
               break;
            case ABSTRACT:
               modifiers |= java.lang.reflect.Modifier.ABSTRACT;
               break;
            case STATIC:
               modifiers |= java.lang.reflect.Modifier.STATIC;
               break;
            case FINAL:
               modifiers |= java.lang.reflect.Modifier.FINAL;
               break;
            case TRANSIENT:
               modifiers |= java.lang.reflect.Modifier.TRANSIENT;
               break;
            case VOLATILE:
               modifiers |= java.lang.reflect.Modifier.VOLATILE;
               break;
            case SYNCHRONIZED:
               modifiers |= java.lang.reflect.Modifier.SYNCHRONIZED;
               break;
            case NATIVE:
               modifiers |= java.lang.reflect.Modifier.NATIVE;
               break;
            case STRICTFP:
               modifiers |= java.lang.reflect.Modifier.STRICT;
               break;
         }
      }
      return modifiers;
   }

   private TypeMirror getTypeMirror(String typeName) {
      if ("void".equals(typeName)) {
         return types.getNoType(TypeKind.VOID);
      }
      if ("boolean".equals(typeName)) {
         return types.getPrimitiveType(TypeKind.BOOLEAN);
      }
      if ("char".equals(typeName)) {
         return types.getPrimitiveType(TypeKind.CHAR);
      }
      if ("byte".equals(typeName)) {
         return types.getPrimitiveType(TypeKind.BYTE);
      }
      if ("short".equals(typeName)) {
         return types.getPrimitiveType(TypeKind.SHORT);
      }
      if ("int".equals(typeName)) {
         return types.getPrimitiveType(TypeKind.INT);
      }
      if ("long".equals(typeName)) {
         return types.getPrimitiveType(TypeKind.LONG);
      }
      if ("float".equals(typeName)) {
         return types.getPrimitiveType(TypeKind.FLOAT);
      }
      if ("double".equals(typeName)) {
         return types.getPrimitiveType(TypeKind.DOUBLE);
      }
      TypeElement typeElement = elements.getTypeElement(typeName);
      if (typeElement == null) {
         throw new IllegalStateException("Type not found : " + typeName);
      }
      return typeElement.asType();
   }

   /**
    * A primitive type or void.
    */
   private final class MirrorPrimitiveType implements XClass, HasModelElement {

      private final Class<?> clazz;

      private final TypeMirror primitiveType;

      MirrorPrimitiveType(Class<?> clazz, TypeMirror primitiveType) {
         this.clazz = clazz;
         this.primitiveType = primitiveType;
      }

      @Override
      public XTypeFactory getFactory() {
         return MirrorTypeFactory.this;
      }

      @Override
      public Class<?> asClass() {
         throw new UnsupportedOperationException();
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
         return null;
      }

      @Override
      public boolean isPrimitive() {
         return clazz != void.class;
      }

      @Override
      public boolean isEnum() {
         return false;
      }

      @Override
      public Iterable<? extends XEnumConstant> getEnumConstants() {
         throw new IllegalStateException(getName() + " is not an enum");
      }

      @Override
      public XEnumConstant getEnumConstant(String name) {
         throw new IllegalStateException(getName() + " is not an enum");
      }

      @Override
      public boolean isArray() {
         return false;
      }

      @Override
      public XClass getComponentType() {
         throw new IllegalStateException(getName() + " is not an array");
      }

      @Override
      public XClass getEnclosingClass() {
         return null;
      }

      @Override
      public XClass getSuperclass() {
         return null;
      }

      @Override
      public XClass[] getInterfaces() {
         return EMPTY_XCLASS_ARRAY;
      }

      @Override
      public String[] getGenericInterfaceParameterTypes(Class<?> c) {
         return null;
      }

      @Override
      public boolean isAssignableTo(XClass other) {
         if (this == other) {
            return true;
         }
         String secondTypeName = other.getCanonicalName();
         TypeMirror secondType = getTypeMirror(secondTypeName);
         return types.isSameType(primitiveType, secondType);
      }

      @Override
      public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
         return null;
      }

      @Override
      public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
         return null;
      }

      @Override
      public String getDocumentation() {
         return null;
      }

      @Override
      public int getModifiers() {
         return java.lang.reflect.Modifier.PUBLIC | java.lang.reflect.Modifier.FINAL;
      }

      @Override
      public XConstructor getDeclaredConstructor(XClass... argTypes) {
         return null;
      }

      @Override
      public Iterable<? extends XConstructor> getDeclaredConstructors() {
         return Collections.emptyList();
      }

      @Override
      public Iterable<? extends XMethod> getDeclaredMethods() {
         return Collections.emptyList();
      }

      @Override
      public XMethod getMethod(String methodName, XClass... argTypes) {
         return null;
      }

      @Override
      public Iterable<? extends XField> getDeclaredFields() {
         return Collections.emptyList();
      }

      @Override
      public boolean isLocal() {
         return false;
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         }
         if (!(obj instanceof MirrorPrimitiveType other)) {
            return false;
         }
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

      @Override
      public Element getElement() {
         return types.asElement(primitiveType);
      }
   }

   /**
    * Only for declared types, not for primitives, arrays, or void.
    */
   private final class MirrorClass implements XClass, HasModelElement {

      private final DeclaredType typeMirror;

      private final TypeElement typeElement;

      private final Map<VariableElement, MirrorEnumConstant> enumConstants;

      private final Map<ExecutableElement, MirrorConstructor> constructorCache = new HashMap<>();

      private final Map<ExecutableElement, MirrorMethod> methodCache = new HashMap<>();

      private final Map<VariableElement, MirrorField> fieldCache = new HashMap<>();

      private final int modifiers;

      MirrorClass(DeclaredType typeMirror) {
         this.typeMirror = typeMirror;
         typeElement = (TypeElement) typeMirror.asElement();

         if (typeElement.getKind() == ElementKind.ENUM) {
            int[] ordinal = {0}; // assign ordinals in natural order
            enumConstants = typeElement.getEnclosedElements().stream()
                  .filter(e -> e.getKind() == ElementKind.ENUM_CONSTANT)
                  .map(e -> (VariableElement) e)
                  .collect(Collectors.toMap(
                        e -> e,
                        e -> new MirrorEnumConstant(this, e, ordinal[0]++),
                        (k, v) -> {
                           throw new IllegalStateException("Elements must be distinct");
                        },
                        LinkedHashMap::new)
                  );
         } else {
            enumConstants = null;
         }

         modifiers = getModifiersOfElement(typeElement);
      }

      @Override
      public XTypeFactory getFactory() {
         return MirrorTypeFactory.this;
      }

      @Override
      public Class<?> asClass() {
         throw new UnsupportedOperationException();
      }

      @Override
      public String getName() {
         return elements.getBinaryName(typeElement).toString();
      }

      @Override
      public String getSimpleName() {
         return typeElement.getSimpleName().toString();
      }

      @Override
      public String getCanonicalName() {
         return typeElement.getQualifiedName().toString();
      }

      @Override
      public String getPackageName() {
         PackageElement packageElement = elements.getPackageOf(typeElement);
         return packageElement.isUnnamed() ? null : packageElement.getQualifiedName().toString();
      }

      @Override
      public boolean isPrimitive() {
         return false;
      }

      @Override
      public boolean isEnum() {
         return enumConstants != null;
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
         for (VariableElement v : enumConstants.keySet()) {
            if (name.equals(v.getSimpleName().toString())) {
               return enumConstants.get(v);
            }
         }
         return null;
      }

      @Override
      public boolean isArray() {
         return false;
      }

      @Override
      public XClass getComponentType() {
         throw new IllegalStateException(getName() + " is not an array");
      }

      @Override
      public XClass getEnclosingClass() {
         Element enclosingElement = typeElement.getEnclosingElement();
         return enclosingElement.getKind() == ElementKind.CLASS ? fromTypeMirror(enclosingElement.asType()) : null;
      }

      @Override
      public XClass getSuperclass() {
         TypeMirror superclass = typeElement.getSuperclass();
         return superclass.getKind() == TypeKind.DECLARED ? fromTypeMirror(superclass) : null;
      }

      @Override
      public XClass[] getInterfaces() {
         List<? extends TypeMirror> interfaces = typeElement.getInterfaces();
         XClass[] xclasses = new XClass[interfaces.size()];
         for (int i = 0; i < xclasses.length; i++) {
            xclasses[i] = fromTypeMirror(interfaces.get(i));
         }
         return xclasses;
      }

      @Override
      public String[] getGenericInterfaceParameterTypes(Class<?> c) {
         String name = c.getCanonicalName();
         if (name == null) {
            name = c.getName();
         }
         TypeMirror i = types.erasure(elements.getTypeElement(name).asType());
         return getGenericInterfaceParameterTypes(typeMirror, i);
      }

      private String[] getGenericInterfaceParameterTypes(TypeMirror typeMirror, TypeMirror i) {
         if (types.isAssignable(typeMirror, i)) {
            if (types.isSameType(types.erasure(typeMirror), i)) {
               return ((DeclaredType) typeMirror).getTypeArguments().stream()
                     .map((TypeMirror t) -> {
                        if (t instanceof TypeVariable) {
                           t = ((TypeVariable) t).getUpperBound();
                        }
                        return t.toString();
                     })
                     .toArray(String[]::new);
            }
            for (TypeMirror s : types.directSupertypes(typeMirror)) {
               String[] params = getGenericInterfaceParameterTypes(s, i);
               if (params != null) {
                  return params;
               }
            }
         }
         return null;
      }

      @Override
      public boolean isAssignableTo(XClass c) {
         if (this == c) {
            return true;
         }
         if (c.isPrimitive()) {
            // a non-primitive cannot be assignable to a primitive
            return false;
         }
         TypeMirror secondType = getTypeMirror(c.getCanonicalName());
         return types.isAssignable(types.erasure(typeMirror), types.erasure(secondType));
      }

      @Override
      public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
         return typeElement.getAnnotation(annotationClass);
      }

      @Override
      public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
         return typeElement.getAnnotationsByType(annotationClass);
      }

      @Override
      public String getDocumentation() {
         return DocumentationExtractor.getDocumentation(typeElement, fullyQualifiedAnnotations);
      }

      @Override
      public XMethod getMethod(String methodName, XClass... argTypes) {
         return cacheMethod(findExecutableElement(false, methodName, argTypes));
      }

      private ExecutableElement findExecutableElement(boolean isConstructor, String name, XClass[] argTypes) {
         Predicate<ExecutableElement> argFilter = e -> {
            if (e.getParameters().size() != argTypes.length) {
               return false;
            }
            List<? extends VariableElement> parameters = e.getParameters();
            for (int i = 0; i < argTypes.length; i++) {
               if (argTypes[i] != fromTypeMirror(parameters.get(i).asType())) {
                  return false;
               }
            }
            return true;
         };

         List<? extends Element> members = isConstructor ? typeElement.getEnclosedElements() : elements.getAllMembers(typeElement);
         return (ExecutableElement) members.stream()
               .filter(e -> isConstructor ? e.getKind() == ElementKind.CONSTRUCTOR : (e.getKind() == ElementKind.METHOD && name.equals(e.getSimpleName().toString())))
               .filter(e -> argFilter.test((ExecutableElement) e))
               .findFirst().orElse(null);
      }

      @Override
      public Iterable<? extends XField> getDeclaredFields() {
         return typeElement.getEnclosedElements().stream()
               .filter(e -> e.getKind() == ElementKind.FIELD)
               .map(e -> cacheField((VariableElement) e))
               .collect(Collectors.toList());
      }

      @Override
      public int getModifiers() {
         return modifiers;
      }

      @Override
      public boolean isLocal() {
         NestingKind nestingKind = typeElement.getNestingKind();
         return nestingKind == NestingKind.LOCAL || nestingKind == NestingKind.ANONYMOUS;
      }

      @Override
      public boolean isInterface() {
         return typeElement.getKind().isInterface();
      }

      @Override
      public XConstructor getDeclaredConstructor(XClass... argTypes) {
         return cacheConstructor(findExecutableElement(true, null, argTypes));
      }

      @Override
      public Iterable<? extends XConstructor> getDeclaredConstructors() {
         return typeElement.getEnclosedElements().stream()
               .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
               .map(e -> cacheConstructor((ExecutableElement) e))
               .collect(Collectors.toList());
      }

      @Override
      public Iterable<? extends XMethod> getDeclaredMethods() {
         return typeElement.getEnclosedElements().stream()
               .filter(e -> e.getKind() == ElementKind.METHOD)
               .map(e -> cacheMethod((ExecutableElement) e))
               .collect(Collectors.toList());
      }

      private MirrorMethod cacheMethod(ExecutableElement method) {
         if (method == null) {
            return null;
         }

         MirrorClass declaringClass = (MirrorClass) fromTypeMirror(method.getEnclosingElement().asType());
         MirrorMethod xmethod = declaringClass.methodCache.get(method);
         if (xmethod == null) {
            xmethod = new MirrorMethod(declaringClass, method);
            declaringClass.methodCache.put(method, xmethod);
         }
         return xmethod;
      }

      private MirrorConstructor cacheConstructor(ExecutableElement ctor) {
         if (ctor == null) {
            return null;
         }
         MirrorConstructor xctor = constructorCache.get(ctor);
         if (xctor == null) {
            xctor = new MirrorConstructor(this, ctor);
            constructorCache.put(ctor, xctor);
         }
         return xctor;
      }

      private MirrorField cacheField(VariableElement field) {
         MirrorClass declaringClass = (MirrorClass) fromTypeMirror(field.getEnclosingElement().asType());
         MirrorField xfield = declaringClass.fieldCache.get(field);
         if (xfield == null) {
            XEnumConstant enumConstant = field.getKind() == ElementKind.ENUM_CONSTANT ? declaringClass.enumConstants.get(field) : null;
            xfield = new MirrorField(declaringClass, field, enumConstant);
            declaringClass.fieldCache.put(field, xfield);
         }
         return xfield;
      }

      @Override
      public Element getElement() {
         return typeElement;
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         }
         if (!(obj instanceof XClass other)) {
            return false;
         }
         return getName().equals(other.getName());
      }

      @Override
      public int hashCode() {
         return getName().hashCode();
      }

      @Override
      public String toString() {
         return typeMirror.toString();
      }
   }

   private final class MirrorEnumConstant implements XEnumConstant {

      private final XClass declaringClass;
      private final VariableElement e;
      private final int ordinal;
      private final int modifiers;

      MirrorEnumConstant(XClass declaringClass, VariableElement e, int ordinal) {
         this.declaringClass = declaringClass;
         this.e = e;
         this.ordinal = ordinal;
         this.modifiers = getModifiersOfElement(e);
      }

      @Override
      public int getOrdinal() {
         return ordinal;
      }

      @Override
      public String getName() {
         return e.getSimpleName().toString();
      }

      @Override
      public int getModifiers() {
         return modifiers;
      }

      @Override
      public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
         return e.getAnnotation(annotationClass);
      }

      @Override
      public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
         return e.getAnnotationsByType(annotationClass);
      }

      @Override
      public String getDocumentation() {
         return DocumentationExtractor.getDocumentation(e, fullyQualifiedAnnotations);
      }

      @Override
      public XClass getDeclaringClass() {
         return declaringClass;
      }
   }

   private final class MirrorArray implements XClass, HasModelElement {

      private final XClass componentType;

      MirrorArray(XClass componentType) {
         this.componentType = componentType;
      }

      @Override
      public XTypeFactory getFactory() {
         return MirrorTypeFactory.this;
      }

      @Override
      public Class<?> asClass() {
         throw new UnsupportedOperationException();
      }

      @Override
      public String getName() {
         return "[" + componentType.getName();
      }

      @Override
      public String getSimpleName() {
         return componentType.getSimpleName() + "[]";
      }

      @Override
      public String getCanonicalName() {
         String canonicalName = componentType.getCanonicalName();
         return canonicalName != null ? canonicalName + "[]" : null;
      }

      @Override
      public String getPackageName() {
         return componentType.getPackageName();
      }

      @Override
      public boolean isPrimitive() {
         return false;
      }

      @Override
      public boolean isEnum() {
         return false;
      }

      @Override
      public Iterable<? extends XEnumConstant> getEnumConstants() {
         throw new IllegalStateException(getName() + " is not an enum");
      }

      @Override
      public XEnumConstant getEnumConstant(String name) {
         throw new IllegalStateException(getName() + " is not an enum");
      }

      @Override
      public boolean isArray() {
         return true;
      }

      @Override
      public XClass getComponentType() {
         return componentType;
      }

      @Override
      public XClass getEnclosingClass() {
         return null;
      }

      @Override
      public XClass getSuperclass() {
         return null;
      }

      @Override
      public XClass[] getInterfaces() {
         return EMPTY_XCLASS_ARRAY;
      }

      @Override
      public String[] getGenericInterfaceParameterTypes(Class<?> c) {
         return null;
      }

      @Override
      public boolean isAssignableTo(XClass c) {
         if (this == c) {
            return true;
         }
         if (!c.isArray()) {
            return false;
         }
         return componentType.isAssignableTo(c.getComponentType());
      }

      @Override
      public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
         return null;
      }

      @Override
      public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
         return null;
      }

      @Override
      public String getDocumentation() {
         return null;
      }

      @Override
      public XMethod getMethod(String methodName, XClass... argTypes) {
         return null;
      }

      @Override
      public Iterable<? extends XField> getDeclaredFields() {
         return null;
      }

      @Override
      public int getModifiers() {
         return java.lang.reflect.Modifier.PUBLIC | java.lang.reflect.Modifier.FINAL;
      }

      @Override
      public boolean isLocal() {
         return false;
      }

      @Override
      public XConstructor getDeclaredConstructor(XClass... argTypes) {
         return null;
      }

      @Override
      public Iterable<? extends XConstructor> getDeclaredConstructors() {
         return Collections.emptyList();
      }

      @Override
      public Iterable<? extends XMethod> getDeclaredMethods() {
         return Collections.emptyList();
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         }
         if (!(obj instanceof XClass other)) {
            return false;
         }
         return other.isArray() && getName().equals(other.getName());
      }

      @Override
      public int hashCode() {
         return getName().hashCode();
      }

      @Override
      public String toString() {
         return "[" + componentType.toString();
      }

      @Override
      public Element getElement() {
         return ((HasModelElement) componentType).getElement();
      }
   }

   private final class MirrorMethod implements XMethod, HasModelElement {

      private final MirrorClass declaringClass;

      private final ExecutableElement executableElement;

      private final int modifiers;

      MirrorMethod(MirrorClass declaringClass, ExecutableElement executableElement) {
         this.declaringClass = declaringClass;
         this.executableElement = executableElement;
         this.modifiers = getModifiersOfElement(executableElement);
      }

      @Override
      public XClass getReturnType() {
         return fromTypeMirror(executableElement.getReturnType());
      }

      @Override
      public XClass determineRepeatedElementType() {
         XClass returnType = determineOptionalReturnType();
         if (returnType.isArray()) {
            return returnType.getComponentType();
         }
         if (returnType.isAssignableTo(Collection.class)) {
            List<? extends TypeMirror> typeArguments = ((DeclaredType) unwrapOptionalReturnType()).getTypeArguments();
            if (typeArguments.size() == 1) {
               TypeMirror arg = typeArguments.get(0);
               return fromTypeMirror(arg);
            }
         }
         if (returnType.isAssignableTo(Map.class)) {
            List<? extends TypeMirror> typeArguments = ((DeclaredType) unwrapOptionalReturnType()).getTypeArguments();
            if (typeArguments.size() == 2) {
               return fromTypeMirror(typeArguments.get(1));
            }
         }
         throw log.notRepeatableField(declaringClass.getName(), executableElement.getSimpleName().toString());
      }

      @Override
      public XClass getTypeArgument(int i) {
         List<? extends TypeMirror> typeArguments = ((DeclaredType) unwrapOptionalReturnType()).getTypeArguments();
         return fromTypeMirror(typeArguments.get(i));
      }

      @Override
      public XClass determineOptionalReturnType() {
         return fromTypeMirror(unwrapOptionalReturnType());
      }

      private TypeMirror unwrapOptionalReturnType() {
         if (getReturnType() == fromClass(Optional.class)) {
            return ((DeclaredType) executableElement.getReturnType()).getTypeArguments().get(0);
         }
         return executableElement.getReturnType();
      }

      @Override
      public int getParameterCount() {
         return executableElement.getParameters().size();
      }

      @Override
      public String[] getParameterNames() {
         return executableElement.getParameters().stream().map(p -> p.getSimpleName().toString()).toArray(String[]::new);
      }

      @Override
      public XClass[] getParameterTypes() {
         return executableElement.getParameters().stream().map(p -> fromTypeMirror(p.asType())).toArray(XClass[]::new);
      }

      @Override
      public String getName() {
         return executableElement.getSimpleName().toString();
      }

      @Override
      public int getModifiers() {
         return modifiers;
      }

      @Override
      public XClass getDeclaringClass() {
         return declaringClass;
      }

      @Override
      public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
         return executableElement.getAnnotation(annotationClass);
      }

      @Override
      public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
         return executableElement.getAnnotationsByType(annotationClass);
      }

      @Override
      public String getDocumentation() {
         return DocumentationExtractor.getDocumentation(executableElement, fullyQualifiedAnnotations);
      }

      @Override
      public Element getElement() {
         return executableElement;
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         }
         if (obj == null || obj.getClass() != MirrorMethod.class) {
            return false;
         }
         return executableElement.equals(((MirrorMethod) obj).executableElement);
      }

      @Override
      public int hashCode() {
         return executableElement.hashCode();
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder();
         for (Modifier m : Modifier.values()) {
            if (executableElement.getModifiers().contains(m)) {
               sb.append(m).append(' ');
            }
         }
         sb.append(executableElement.getReturnType())
               .append(' ').append(executableElement.getEnclosingElement().getSimpleName())
               .append('.').append(executableElement);
         return sb.toString();
      }

      @Override
      public String toGenericString() {
         return toString();
      }
   }

   private final class MirrorConstructor implements XConstructor {

      private final MirrorClass c;

      private final ExecutableElement executableElement;

      private final int modifiers;

      MirrorConstructor(MirrorClass c, ExecutableElement executableElement) {
         this.c = c;
         this.executableElement = executableElement;
         this.modifiers = getModifiersOfElement(executableElement);
      }

      @Override
      public int getParameterCount() {
         return executableElement.getParameters().size();
      }

      @Override
      public String[] getParameterNames() {
         return executableElement.getParameters().stream().map(p -> p.getSimpleName().toString()).toArray(String[]::new);
      }

      @Override
      public XClass[] getParameterTypes() {
         return executableElement.getParameters().stream().map(p -> fromTypeMirror(p.asType())).toArray(XClass[]::new);
      }

      @Override
      public String getName() {
         return executableElement.getSimpleName().toString();
      }

      @Override
      public int getModifiers() {
         return modifiers;
      }

      @Override
      public XClass getDeclaringClass() {
         return c;
      }

      @Override
      public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
         return executableElement.getAnnotation(annotationClass);
      }

      @Override
      public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
         return executableElement.getAnnotationsByType(annotationClass);
      }

      @Override
      public String getDocumentation() {
         // no @ProtoDoc allowed on constructors
         return null;
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         }
         if (obj == null || obj.getClass() != MirrorConstructor.class) {
            return false;
         }
         return executableElement.equals(((MirrorConstructor) obj).executableElement);
      }

      @Override
      public int hashCode() {
         return executableElement.hashCode();
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder();
         for (Modifier m : Modifier.values()) {
            if (executableElement.getModifiers().contains(m)) {
               sb.append(m).append(' ');
            }
         }
         sb.append(executableElement);
         return sb.toString();
      }

      @Override
      public String toGenericString() {
         return toString();
      }
   }

   private final class MirrorField implements XField {

      private final MirrorClass c;

      private final VariableElement field;

      private final XEnumConstant enumConstant;

      private final int modifiers;

      MirrorField(MirrorClass c, VariableElement field, XEnumConstant enumConstant) {
         this.c = c;
         this.field = field;
         this.enumConstant = enumConstant;
         this.modifiers = getModifiersOfElement(field);
      }

      @Override
      public XClass getType() {
         return fromTypeMirror(field.asType());
      }

      @Override
      public XClass determineRepeatedElementType() {
         if (getType().isArray()) {
            return getType().getComponentType();
         }
         if (getType().isAssignableTo(Collection.class)) {
            List<? extends TypeMirror> typeArguments = ((DeclaredType) field.asType()).getTypeArguments();
            if (typeArguments.size() == 1) {
               TypeMirror arg = typeArguments.get(0);
               return fromTypeMirror(arg);
            }
         }
         if (getType().isAssignableTo(Map.class)) {
            List<? extends TypeMirror> typeArguments = ((DeclaredType) field.asType()).getTypeArguments();
            if (typeArguments.size() == 2) {
               return fromTypeMirror(typeArguments.get(1));
            }
         }
         throw log.notRepeatableField(c.getName(), field.getSimpleName().toString());
      }

      @Override
      public XClass getTypeArgument(int i) {
         List<? extends TypeMirror> typeArguments = ((DeclaredType) field.asType()).getTypeArguments();
         return fromTypeMirror(typeArguments.get(i));
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
         return field.getSimpleName().toString();
      }

      @Override
      public int getModifiers() {
         return modifiers;
      }

      @Override
      public XClass getDeclaringClass() {
         return c;
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
      public String getDocumentation() {
         return DocumentationExtractor.getDocumentation(field, fullyQualifiedAnnotations);
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         }
         if (obj == null || obj.getClass() != MirrorField.class) {
            return false;
         }
         return field.equals(((MirrorField) obj).field);
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
