package org.infinispan.protostream.annotations.impl;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoMessage;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.annotations.ProtoSchemaBuilderException;
import org.infinispan.protostream.annotations.ProtoUnknownFieldSet;
import org.infinispan.protostream.annotations.impl.types.XClass;
import org.infinispan.protostream.annotations.impl.types.XConstructor;
import org.infinispan.protostream.annotations.impl.types.XExecutable;
import org.infinispan.protostream.annotations.impl.types.XField;
import org.infinispan.protostream.annotations.impl.types.XMember;
import org.infinispan.protostream.annotations.impl.types.XMethod;
import org.infinispan.protostream.annotations.impl.types.XTypeFactory;
import org.infinispan.protostream.containers.IndexedElementContainer;
import org.infinispan.protostream.containers.IndexedElementContainerAdapter;
import org.infinispan.protostream.containers.IterableElementContainer;
import org.infinispan.protostream.containers.IterableElementContainerAdapter;
import org.infinispan.protostream.descriptors.JavaType;
import org.infinispan.protostream.descriptors.Type;
import org.infinispan.protostream.impl.Log;

/**
 * A {@link ProtoTypeMetadata} for a message type created based on annotations during the current execution of {@link
 * org.infinispan.protostream.annotations.ProtoSchemaBuilder}.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
public class ProtoMessageTypeMetadata extends ProtoTypeMetadata {

   private static final Log log = Log.LogFactory.getLog(ProtoMessageTypeMetadata.class);

   private final BaseProtoSchemaGenerator protoSchemaGenerator;

   protected final XTypeFactory typeFactory;

   private SortedMap<Integer, ProtoFieldMetadata> fieldsByNumber = null;

   private Map<String, ProtoFieldMetadata> fieldsByName = null;

   private final XClass annotatedClass;

   private final boolean isAdapter;

   private final boolean isIndexedContainer;

   private final boolean isIterableContainer;

   private XExecutable factory;

   private XField unknownFieldSetField;

   private XMethod unknownFieldSetGetter;

   private XMethod unknownFieldSetSetter;

   private final Map<XClass, ProtoTypeMetadata> innerTypes = new HashMap<>();

   protected ProtoMessageTypeMetadata(BaseProtoSchemaGenerator protoSchemaGenerator, XClass annotatedClass, XClass javaClass) {
      super(getProtoName(annotatedClass, javaClass), javaClass);
      this.protoSchemaGenerator = protoSchemaGenerator;
      this.annotatedClass = annotatedClass;
      this.typeFactory = annotatedClass.getFactory();
      this.isAdapter = javaClass != annotatedClass;
      this.isIndexedContainer = annotatedClass.isAssignableTo(isAdapter ? IndexedElementContainerAdapter.class : IndexedElementContainer.class);
      this.isIterableContainer = annotatedClass.isAssignableTo(isAdapter ? IterableElementContainerAdapter.class : IterableElementContainer.class);

      checkInstantiability();

      validateName();
   }

   private static String getProtoName(XClass annotatedClass, XClass javaClass) {
      ProtoName annotation = annotatedClass.getAnnotation(ProtoName.class);
      ProtoMessage protoMessageAnnotation = annotatedClass.getAnnotation(ProtoMessage.class);
      if (annotation != null) {
         if (protoMessageAnnotation != null) {
            throw new ProtoSchemaBuilderException("@ProtoMessage annotation cannot be used together with @ProtoName: " + annotatedClass.getName());
         }
         return annotation.value().isEmpty() ? javaClass.getSimpleName() : annotation.value();
      }
      return protoMessageAnnotation == null || protoMessageAnnotation.name().isEmpty() ? javaClass.getSimpleName() : protoMessageAnnotation.name();
   }

   @Override
   public XClass getAnnotatedClass() {
      return annotatedClass;
   }

   @Override
   public boolean isAdapter() {
      return isAdapter;
   }

   public boolean isIndexedContainer() {
      return isIndexedContainer;
   }

   public boolean isIterableContainer() {
      return isIterableContainer;
   }

   public boolean isContainer() {
      return isIterableContainer || isIndexedContainer;
   }

   public XExecutable getFactory() {
      scanMemberAnnotations();
      return factory;
   }

   public XField getUnknownFieldSetField() {
      scanMemberAnnotations();
      return unknownFieldSetField;
   }

   public XMethod getUnknownFieldSetGetter() {
      scanMemberAnnotations();
      return unknownFieldSetGetter;
   }

   public XMethod getUnknownFieldSetSetter() {
      scanMemberAnnotations();
      return unknownFieldSetSetter;
   }

   public SortedMap<Integer, ProtoFieldMetadata> getFields() {
      scanMemberAnnotations();
      return fieldsByNumber;
   }

   protected void addInnerType(ProtoTypeMetadata typeMetadata) {
      innerTypes.put(typeMetadata.getJavaClass(), typeMetadata);
   }

   @Override
   public void generateProto(IndentWriter iw) {
      scanMemberAnnotations();  //todo [anistor] need to have a better place for this call

      iw.append("\n\n");
      appendDocumentation(iw, getDocumentation());
      iw.append("message ").append(name);
      if (BaseProtoSchemaGenerator.generateSchemaDebugComments) {
         iw.append(" /* ").append(getJavaClassName()).append(" */");
      }
      iw.append(" {\n");
      iw.inc();

      ReservedProcessor reserved = new ReservedProcessor();
      reserved.scan(annotatedClass);

      for (int memberNumber : fieldsByNumber.keySet()) {
         ProtoFieldMetadata field = fieldsByNumber.get(memberNumber);
         XClass where = reserved.checkReserved(memberNumber);
         if (where != null) {
            throw new ProtoSchemaBuilderException("Protobuf field number " + memberNumber + " of field " + field.getLocation() +
                  " conflicts with 'reserved' statement in " + where.getCanonicalName());
         }
         where = reserved.checkReserved(field.getName());
         if (where != null) {
            throw new ProtoSchemaBuilderException("Protobuf field number " + memberNumber + " of field " + field.getLocation() +
                  " conflicts with 'reserved' statement in " + where.getCanonicalName());
         }
      }

      reserved.generate(iw);

      for (ProtoTypeMetadata t : innerTypes.values()) {
         t.generateProto(iw);
      }

      for (ProtoFieldMetadata f : fieldsByNumber.values()) {
         f.generateProto(iw);
      }

      iw.dec();
      iw.append("}\n");
   }

   @Override
   public final boolean isEnum() {
      return false;
   }

   @Override
   public final ProtoEnumValueMetadata getEnumMemberByName(String name) {
      throw new IllegalStateException(getJavaClassName() + " is not an enum");
   }

   @Override
   public void scanMemberAnnotations() {
      if (fieldsByNumber == null) {
         // All the fields discovered in this class hierarchy, the key is their number.
         // We use a TreeMap to ensure ascending order by field number.
         fieldsByNumber = new TreeMap<>();

         // all the fields discovered in this class hierarchy, by name
         fieldsByName = new HashMap<>();

         discoverFields(annotatedClass, new HashSet<>());
         if (fieldsByNumber.isEmpty()) {
            //todo avoid this warning in case where not necessary
            // TODO [anistor] remove the "The class should be either annotated or it should have a custom marshaller" part after MessageMarshaller is removed in 5
            log.warnf("Class %s does not have any @ProtoField annotated members. The class should be either annotated or it should have a custom marshaller.", getAnnotatedClassName());
         }

         // If we have a factory method / constructor, we must ensure its parameters match the declared fields
         if (factory != null) {
            String factoryKind = factory instanceof XConstructor ? "constructor" : (factory.isStatic() ? "static method" : "method");
            XClass[] parameterTypes = factory.getParameterTypes();
            int startPos = 0;
            if (isIndexedContainer || isIterableContainer) {
               if (parameterTypes.length == 0 || parameterTypes[0] != typeFactory.fromClass(int.class)) {
                  throw new ProtoSchemaBuilderException("@ProtoFactory annotated " + factoryKind
                        + " signature mismatch. The first parameter is expected to be of type 'int' : "
                        + factory.toGenericString());
               }
               startPos = 1;
            }
            String[] parameterNames = factory.getParameterNames();
            if (parameterNames.length != fieldsByNumber.size() + startPos) {
               throw new ProtoSchemaBuilderException("@ProtoFactory annotated " + factoryKind
                     + " signature mismatch. Expected " + (fieldsByNumber.size() + startPos) + " parameters but found "
                     + parameterNames.length + " : " + factory.toGenericString());
            }
            for (; startPos < parameterNames.length; startPos++) {
               String parameterName = parameterNames[startPos];
               ProtoFieldMetadata fieldMetadata = getFieldByPropertyName(parameterName);
               if (fieldMetadata == null) {
                  throw new ProtoSchemaBuilderException("@ProtoFactory annotated " + factoryKind
                        + " signature mismatch. The parameter '" + parameterName
                        + "' does not match any field : " + factory.toGenericString());
               }
               XClass parameterType = parameterTypes[startPos];
               boolean paramTypeMismatch = false;
               if (fieldMetadata.isArray()) {
                  if (!parameterType.isArray() || parameterType.getComponentType() != fieldMetadata.getJavaType()) {
                     paramTypeMismatch = true;
                  }
               } else if (fieldMetadata.isRepeated()) {
                  if (!fieldMetadata.getCollectionImplementation().isAssignableTo(parameterType)) {
                     paramTypeMismatch = true;
                  }
                  // todo [anistor] also check the collection's type parameter
               } else if (fieldMetadata.getJavaType() != parameterType) {
                  paramTypeMismatch = true;
               }
               if (paramTypeMismatch) {
                  throw new ProtoSchemaBuilderException("@ProtoFactory annotated " + factoryKind
                        + " signature mismatch: " + factory.toGenericString() + ". The parameter '"
                        + parameterName + "' does not match the type from the field definition.");
               }
            }
         }
      }
   }

   private ProtoFieldMetadata getFieldByPropertyName(String propName) {
      for (ProtoFieldMetadata field : fieldsByNumber.values()) {
         if (propName.equals(field.getPropertyName())) {
            return field;
         }
      }
      return null;
   }

   /**
    * Ensure we either have a suitable constructor or a factory method.
    */
   private void checkInstantiability() {
      // ensure the class is not abstract
      if (annotatedClass.isAbstract() || annotatedClass.isInterface()) {
         throw new ProtoSchemaBuilderException("Abstract classes are not allowed: " + getAnnotatedClassName());
      }
      // ensure it is not a local or anonymous class
      if (annotatedClass.isLocal()) {
         throw new ProtoSchemaBuilderException("Local or anonymous classes are not allowed. The class " + getAnnotatedClassName() + " must be instantiable using an accessible no-argument constructor.");
      }
      // ensure the class is not a non-static inner class
      if (annotatedClass.getEnclosingClass() != null && !annotatedClass.isStatic()) {
         throw new ProtoSchemaBuilderException("Non-static inner classes are not allowed. The class " + getAnnotatedClassName() + " must be instantiable using an accessible no-argument constructor.");
      }

      for (XConstructor c : annotatedClass.getDeclaredConstructors()) {
         if (c.getAnnotation(ProtoFactory.class) != null) {
            if (factory != null) {
               throw new ProtoSchemaBuilderException("Found more than one @ProtoFactory annotated method / constructor : " + c);
            }
            if (c.isPrivate()) {
               throw new ProtoSchemaBuilderException("@ProtoFactory annotated constructor must not be private: " + c);
            }
            factory = c;
         }
      }

      for (XMethod m : annotatedClass.getDeclaredMethods()) {
         if (m.getAnnotation(ProtoFactory.class) != null) {
            if (factory != null) {
               throw new ProtoSchemaBuilderException("Found more than one @ProtoFactory annotated method / constructor : " + m);
            }
            if (!isAdapter && !m.isStatic()) {
               throw new ProtoSchemaBuilderException("@ProtoFactory annotated method must be static: " + m);
            }
            if (m.isPrivate()) {
               throw new ProtoSchemaBuilderException("@ProtoFactory annotated method must not be private: " + m);
            }
            if (m.getReturnType() != javaClass) {
               throw new ProtoSchemaBuilderException("@ProtoFactory annotated method has wrong return type: " + m);
            }
            factory = m;
         }
      }

      if (factory == null) {
         if (isAdapter || isContainer()) {
            throw new ProtoSchemaBuilderException("The class " + getJavaClassName() +
                  " must be instantiable using an accessible @ProtoFactory annotated method defined by " + getAnnotatedClassName());
         }
         // If no factory method/constructor was found we need to ensure the class has an accessible no-argument constructor
         XConstructor ctor = javaClass.getDeclaredConstructor();
         if (ctor == null || ctor.isPrivate()) {
            throw new ProtoSchemaBuilderException("The class " + getJavaClassName() + " must be instantiable using an accessible no-argument constructor.");
         }
      }
   }

   private void discoverFields(XClass clazz, Set<XClass> examinedClasses) {
      if (!examinedClasses.add(clazz)) {
         // avoid re-examining classes due to multiple interface inheritance
         return;
      }

      if (clazz.getSuperclass() != null) {
         discoverFields(clazz.getSuperclass(), examinedClasses);
      }
      for (XClass i : clazz.getInterfaces()) {
         discoverFields(i, examinedClasses);
      }

      for (XField field : clazz.getDeclaredFields()) {
         if (field.getAnnotation(ProtoUnknownFieldSet.class) != null) {
            if (isAdapter) {
               throw new ProtoSchemaBuilderException("No ProtoStream annotations should be present on fields when @ProtoAdapter is present on a class : " + clazz.getCanonicalName() + '.' + field);
            }
            if (unknownFieldSetField != null || unknownFieldSetGetter != null || unknownFieldSetSetter != null) {
               throw new ProtoSchemaBuilderException("The @ProtoUnknownFieldSet annotation should not occur more than once in a class and its superclasses and superinterfaces : " + clazz.getCanonicalName() + '.' + field);
            }
            unknownFieldSetField = field;
         } else {
            ProtoField annotation = field.getAnnotation(ProtoField.class);
            if (annotation != null) {
               if (isAdapter) {
                  throw new ProtoSchemaBuilderException("No ProtoStream annotations should be present on fields when @ProtoAdapter is present on a class : " + clazz.getCanonicalName() + '.' + field);
               }
               if (field.isStatic()) {
                  throw new ProtoSchemaBuilderException("Static fields cannot be @ProtoField annotated: " + clazz.getCanonicalName() + '.' + field);
               }
               if (factory == null && field.isFinal()) { //todo [anistor] maybe allow this
                  throw new ProtoSchemaBuilderException("Final fields cannot be @ProtoField annotated: " + clazz.getCanonicalName() + '.' + field);
               }
               if (field.isPrivate()) {
                  throw new ProtoSchemaBuilderException("Private fields cannot be @ProtoField annotated: " + clazz.getCanonicalName() + '.' + field);
               }
               int number = getNumber(annotation, field);
               String fieldName = annotation.name();
               if (fieldName.isEmpty()) {
                  fieldName = field.getName();
               }

               Type protobufType = annotation.type();
               if (field.getType() == typeFactory.fromClass(byte[].class) && protobufType == Type.MESSAGE) {
                  // MESSAGE is the default and stands for 'undefined', we can override it with a better default
                  protobufType = Type.BYTES;
               }
               boolean isArray = isArray(field.getType(), protobufType);
               boolean isRepeated = isRepeated(field.getType(), protobufType);
               boolean isRequired = annotation.required();
               if (isRepeated && isRequired) {
                  throw new ProtoSchemaBuilderException("Repeated field '" + fieldName + "' of " + clazz.getCanonicalName() + " cannot be marked required.");
               }
               XClass javaType = getJavaTypeFromAnnotation(annotation);
               if (javaType == typeFactory.fromClass(void.class)) {
                  javaType = isRepeated ? field.determineRepeatedElementType() : field.getType();
               }
               if (javaType == typeFactory.fromClass(byte[].class) && protobufType == Type.MESSAGE) {
                  // MESSAGE is the default and stands for 'undefined', we can override it with a better default
                  protobufType = Type.BYTES;
               }
               if (!javaType.isArray() && !javaType.isPrimitive() && javaType.isAbstract() && !javaType.isEnum()) {
                  throw new ProtoSchemaBuilderException("The type " + javaType.getCanonicalName() + " of field '" + fieldName + "' of " + clazz.getCanonicalName() + " should not be abstract.");
               }

               protobufType = getProtobufType(javaType, protobufType);

               Object defaultValue = getDefaultValue(clazz, fieldName, javaType, protobufType, annotation.defaultValue());

               if (!isRequired && !isRepeated && javaType.isPrimitive() && defaultValue == null) {
                  throw new ProtoSchemaBuilderException("Primitive field '" + fieldName + "' of " + clazz.getCanonicalName() + " is not nullable so it should be either marked required or should have a default value.");
               }

               XClass collectionImplementation = getCollectionImplementation(clazz, field.getType(), getCollectionImplementationFromAnnotation(annotation), fieldName, isRepeated);
               if (isArray) {
                  collectionImplementation = typeFactory.fromClass(ArrayList.class);
               }

               ProtoTypeMetadata protoTypeMetadata = null;
               if (protobufType.getJavaType() == JavaType.ENUM || protobufType.getJavaType() == JavaType.MESSAGE) {
                  protoTypeMetadata = protoSchemaGenerator.scanAnnotations(javaType);
               }
               ProtoFieldMetadata fieldMetadata = new ProtoFieldMetadata(number, fieldName, javaType, collectionImplementation,
                     protobufType, protoTypeMetadata, isRequired, isRepeated, isArray, defaultValue, field);

               ProtoFieldMetadata existing = fieldsByNumber.get(number);
               if (existing != null) {
                  throw new ProtoSchemaBuilderException("Duplicate field number definition. Found two field definitions with number " + number + ": in "
                        + fieldMetadata.getLocation() + " and in " + existing.getLocation());
               }
               existing = fieldsByName.get(fieldMetadata.getName());
               if (existing != null) {
                  throw new ProtoSchemaBuilderException("Duplicate field name definition. Found two field definitions with name '" + fieldMetadata.getName() + "': in "
                        + fieldMetadata.getLocation() + " and in " + existing.getLocation());
               }

               checkReserved(fieldMetadata);
               fieldsByNumber.put(fieldMetadata.getNumber(), fieldMetadata);
               fieldsByName.put(fieldName, fieldMetadata);
            }
         }
      }

      for (XMethod method : clazz.getDeclaredMethods()) {
         if (method.getAnnotation(ProtoUnknownFieldSet.class) != null) {
            if (unknownFieldSetField != null || unknownFieldSetGetter != null || unknownFieldSetSetter != null) {
               throw new ProtoSchemaBuilderException("The @ProtoUnknownFieldSet annotation should not occur more than once in a class and its superclasses and superinterfaces : " + method);
            }
            String propertyName;
            if (method.getReturnType() == typeFactory.fromClass(void.class)) {
               // this method is expected to be a setter
               if (method.getName().startsWith("set") && method.getName().length() > 3) {
                  propertyName = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
               } else {
                  throw new ProtoSchemaBuilderException("Illegal setter method signature: " + method);
               }
               if (isAdapter && method.getParameterTypes().length != 2 || !isAdapter && method.getParameterTypes().length != 1) {
                  throw new ProtoSchemaBuilderException("Illegal setter method signature: " + method);
               }
               //TODO [anistor] also check setter args
               unknownFieldSetSetter = method;
               unknownFieldSetGetter = findGetter(propertyName, method.getParameterTypes()[0]);
            } else {
               // this method is expected to be a getter
               if (method.getName().startsWith("get") && method.getName().length() > 3) {
                  propertyName = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
               } else if (method.getName().startsWith("is") && method.getName().length() > 2) {
                  propertyName = Character.toLowerCase(method.getName().charAt(2)) + method.getName().substring(3);
               } else {
                  throw new ProtoSchemaBuilderException("Illegal getter method signature: " + method);
               }
               if (isAdapter && method.getParameterTypes().length != 1 || !isAdapter && method.getParameterTypes().length != 0) {
                  throw new ProtoSchemaBuilderException("Illegal getter method signature: " + method);
               }
               //TODO [anistor] also check getter args
               unknownFieldSetGetter = method;
               unknownFieldSetSetter = findSetter(propertyName, unknownFieldSetGetter.getReturnType());
            }
         } else {
            ProtoField annotation = method.getAnnotation(ProtoField.class);
            if (annotation != null) {
               if (method.isPrivate()) {
                  throw new ProtoSchemaBuilderException("Private methods cannot be @ProtoField annotated: " + method);
               }
               if (!isAdapter && method.isStatic()) {
                  throw new ProtoSchemaBuilderException("Static methods cannot be @ProtoField annotated: " + method);
               }
               String propertyName;
               XMethod getter;
               XMethod setter;
               XClass getterReturnType;
               // we can have the annotation present on either getter or setter but not both
               if (method.getReturnType() == typeFactory.fromClass(void.class)) {
                  // this method is expected to be a setter
                  if (method.getName().startsWith("set") && method.getName().length() >= 4) {
                     propertyName = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
                  } else {
                     // not a standard java-beans setter, use the whole name as property name
                     propertyName = method.getName();
                  }
                  if (isAdapter && method.getParameterTypes().length != 2 || !isAdapter && method.getParameterTypes().length != 1) {
                     throw new ProtoSchemaBuilderException("Illegal setter method signature: " + method);
                  }
                  //TODO [anistor] also check setter args
                  setter = method;
                  getter = findGetter(propertyName, method.getParameterTypes()[0]);
                  getterReturnType = getter.getReturnType();
                  if (getterReturnType == typeFactory.fromClass(Optional.class)) {
                     getterReturnType = getter.determineOptionalReturnType();
                  }
               } else {
                  // this method is expected to be a getter
                  if (method.getName().startsWith("get") && method.getName().length() >= 4) {
                     propertyName = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
                  } else if (method.getName().startsWith("is") && method.getName().length() >= 3) {
                     propertyName = Character.toLowerCase(method.getName().charAt(2)) + method.getName().substring(3);
                  } else {
                     // not a standard java-beans getter
                     propertyName = method.getName();
                  }
                  if (isAdapter && method.getParameterTypes().length != 1 || !isAdapter && method.getParameterTypes().length != 0) {
                     throw new ProtoSchemaBuilderException("Illegal setter method signature: " + method);
                  }
                  //TODO [anistor] also check getter args
                  getter = method;
                  getterReturnType = getter.getReturnType();
                  if (getterReturnType == typeFactory.fromClass(Optional.class)) {
                     getterReturnType = getter.determineOptionalReturnType();
                  }
                  setter = factory == null ? findSetter(propertyName, getterReturnType) : null;
               }

               int number = getNumber(annotation, method);

               String fieldName = annotation.name();
               if (fieldName.isEmpty()) {
                  fieldName = propertyName;
               }

               Type protobufType = annotation.type();
               if (getterReturnType == typeFactory.fromClass(byte[].class) && protobufType == Type.MESSAGE) {
                  // MESSAGE is the default and stands for 'undefined', we can override it with a better default
                  protobufType = Type.BYTES;
               }
               boolean isArray = isArray(getterReturnType, protobufType);
               boolean isRepeated = isRepeated(getterReturnType, protobufType);
               boolean isRequired = annotation.required();
               if (isRepeated && isRequired) {
                  throw new ProtoSchemaBuilderException("Repeated field '" + fieldName + "' of " + clazz.getCanonicalName() + " cannot be marked required.");
               }
               XClass javaType = getJavaTypeFromAnnotation(annotation);
               if (javaType == typeFactory.fromClass(void.class)) {
                  javaType = isRepeated ? getter.determineRepeatedElementType() : getterReturnType;
               }
               if (javaType == typeFactory.fromClass(byte[].class) && protobufType == Type.MESSAGE) {
                  // MESSAGE is the default and stands for 'undefined', we can override it with a better default
                  protobufType = Type.BYTES;
               }
               if (!javaType.isArray() && !javaType.isPrimitive() && javaType.isAbstract() && !javaType.isEnum()) {
                  throw new ProtoSchemaBuilderException("The type " + javaType.getCanonicalName() + " of field '" + fieldName + "' of " + clazz.getCanonicalName() + " should not be abstract.");
               }

               protobufType = getProtobufType(javaType, protobufType);

               Object defaultValue = getDefaultValue(clazz, fieldName, javaType, protobufType, annotation.defaultValue());

               if (!isRequired && !isRepeated && javaType.isPrimitive() && defaultValue == null) {
                  throw new ProtoSchemaBuilderException("Primitive field '" + fieldName + "' of " + clazz.getCanonicalName() + " is not nullable so it should be either marked required or should have a default value.");
               }

               XClass collectionImplementation = getCollectionImplementation(clazz, getterReturnType, getCollectionImplementationFromAnnotation(annotation), fieldName, isRepeated);
               if (isArray) {
                  collectionImplementation = typeFactory.fromClass(ArrayList.class);
               }

               ProtoTypeMetadata protoTypeMetadata = null;
               if (protobufType.getJavaType() == JavaType.ENUM || protobufType.getJavaType() == JavaType.MESSAGE) {
                  protoTypeMetadata = protoSchemaGenerator.scanAnnotations(javaType);
               }

               ProtoFieldMetadata fieldMetadata = new ProtoFieldMetadata(number, fieldName, javaType, collectionImplementation,
                     protobufType, protoTypeMetadata, isRequired, isRepeated, isArray, defaultValue,
                     propertyName, method, getter, setter);

               ProtoFieldMetadata existing = fieldsByNumber.get(number);
               if (existing != null) {
                  throw new ProtoSchemaBuilderException("Duplicate field definition. Found two field definitions with number " + number + ": in "
                        + fieldMetadata.getLocation() + " and in " + existing.getLocation());
               }
               existing = fieldsByName.get(fieldMetadata.getName());
               if (existing != null) {
                  throw new ProtoSchemaBuilderException("Duplicate field definition. Found two field definitions with name '" + fieldMetadata.getName() + "': in "
                        + fieldMetadata.getLocation() + " and in " + existing.getLocation());
               }

               checkReserved(fieldMetadata);
               fieldsByNumber.put(number, fieldMetadata);
               fieldsByName.put(fieldName, fieldMetadata);
            }
         }
      }
   }

   private static int getNumber(ProtoField annotation, XMember member) {
      int number = annotation.number();
      if (number == 0) {
         number = annotation.value();
      } else if (annotation.value() != 0) {
         throw new ProtoSchemaBuilderException("@ProtoField.number() and value() are mutually exclusive: " + member);
      }
      if (number < 1) {
         throw new ProtoSchemaBuilderException("Protobuf field numbers specified by @ProtoField.number() or value() must be greater than 0: " + member);
      }
      return number;
   }

   private static void checkReserved(ProtoFieldMetadata fieldMetadata) {
      if (fieldMetadata.getNumber() >= 19000 && fieldMetadata.getNumber() <= 19999) {
         throw new ProtoSchemaBuilderException("Field numbers 19000 through 19999 are reserved for internal use by the protobuf specification: "
               + fieldMetadata.getLocation());
      }
      // TODO [anistor] IPROTO-98 also check reserved numbers and names
   }

   protected XClass getCollectionImplementationFromAnnotation(ProtoField annotation) {
      return typeFactory.fromClass(annotation.collectionImplementation());
   }

   protected XClass getJavaTypeFromAnnotation(ProtoField annotation) {
      return typeFactory.fromClass(annotation.javaType());
   }

   /**
    * Parses the value from string form (coming from proto schema) to an actual Java instance value, according to its
    * type.
    */
   private Object getDefaultValue(XClass clazz, String fieldName, XClass fieldType, Type protobufType, String defaultValue) {
      if (defaultValue == null || defaultValue.isEmpty()) {
         return null;
      }
      if (fieldType == typeFactory.fromClass(String.class)) {
         return defaultValue;
      }
      if (fieldType.isEnum()) {
         ProtoTypeMetadata protoEnumTypeMetadata = protoSchemaGenerator.scanAnnotations(fieldType);
         ProtoEnumValueMetadata enumVal = protoEnumTypeMetadata.getEnumMemberByName(defaultValue);
         if (enumVal == null) {
            throw new ProtoSchemaBuilderException("Invalid default value for field '" + fieldName + "' of Java type " + fieldType.getCanonicalName() + " from class " + clazz.getCanonicalName() + ": " + defaultValue + " is not a member of " + protoEnumTypeMetadata.getFullName() + " enum");
         }
         return enumVal;
      }
      if (fieldType == typeFactory.fromClass(Character.class) || fieldType == typeFactory.fromClass(char.class)) {
         if (defaultValue.length() > 1) {
            throw new ProtoSchemaBuilderException("Invalid default value for field '" + fieldName + "' of Java type " + fieldType.getCanonicalName() + " from class " + clazz.getCanonicalName() + ": " + defaultValue);
         }
         return defaultValue.charAt(0);
      }
      if (fieldType == typeFactory.fromClass(Boolean.class) || fieldType == typeFactory.fromClass(boolean.class)) {
         return Boolean.valueOf(defaultValue);
      }
      try {
         if (fieldType == typeFactory.fromClass(Integer.class) || fieldType == typeFactory.fromClass(int.class)) {
            int v = parseInt(defaultValue);
            if (v < 0 && protobufType.isUnsigned()) {
               throw new ProtoSchemaBuilderException("Field '" + fieldName + "' of unsigned Protobuf type " + protobufType + " from class " + clazz.getCanonicalName() + " does not allow a negative default value : " + defaultValue);
            }
            return v;
         }
         if (fieldType == typeFactory.fromClass(Long.class) || fieldType == typeFactory.fromClass(long.class)) {
            long v = parseLong(defaultValue);
            if (v < 0 && protobufType.isUnsigned()) {
               throw new ProtoSchemaBuilderException("Field '" + fieldName + "' of unsigned Protobuf type " + protobufType + " from class " + clazz.getCanonicalName() + " does not allow a negative default value : " + defaultValue);
            }
            return v;
         }
         if (fieldType == typeFactory.fromClass(Short.class) || fieldType == typeFactory.fromClass(short.class)) {
            int v = parseInt(defaultValue);
            if (v < 0 && protobufType.isUnsigned()) {
               throw new ProtoSchemaBuilderException("Field '" + fieldName + "' of unsigned Protobuf type " + protobufType + " from class " + clazz.getCanonicalName() + " does not allow a negative default value : " + defaultValue);
            }
            if (v < Short.MIN_VALUE || v > Short.MAX_VALUE) {
               throw new NumberFormatException("Value out of range for \"" + protobufType + "\": \"" + defaultValue);
            }
            return (short) v;
         }
         if (fieldType == typeFactory.fromClass(Double.class) || fieldType == typeFactory.fromClass(double.class)) {
            return Double.valueOf(defaultValue);
         }
         if (fieldType == typeFactory.fromClass(Float.class) || fieldType == typeFactory.fromClass(float.class)) {
            return Float.valueOf(defaultValue);
         }
         if (fieldType == typeFactory.fromClass(Byte.class) || fieldType == typeFactory.fromClass(byte.class)) {
            int v = parseInt(defaultValue);
            if (v < 0 && protobufType.isUnsigned()) {
               throw new ProtoSchemaBuilderException("Field '" + fieldName + "' of unsigned Protobuf type " + protobufType + " from class " + clazz.getCanonicalName() + " does not allow a negative default value : " + defaultValue);
            }
            if (v < Byte.MIN_VALUE || v > Byte.MAX_VALUE) {
               throw new NumberFormatException("Value out of range for \"" + protobufType + "\": \"" + defaultValue);
            }
            return (byte) v;
         }
         if (fieldType.isAssignableTo(Date.class)) {
            return Long.parseUnsignedLong(defaultValue);
         }
         if (fieldType.isAssignableTo(Instant.class)) {
            return Long.parseUnsignedLong(defaultValue);
         }
      } catch (NumberFormatException e) {
         throw new ProtoSchemaBuilderException("Invalid default value for field '" + fieldName + "' of Java type " + fieldType.getCanonicalName() + " from class " + clazz.getCanonicalName() + ": " + defaultValue, e);
      }
      if (protobufType == Type.BYTES) {
         if (fieldType == typeFactory.fromClass(byte[].class)) {
            return cescape(defaultValue);
         } else {
            throw new ProtoSchemaBuilderException("Invalid default value for field '" + fieldName + "' of Java type " + fieldType.getCanonicalName() + " from class " + clazz.getCanonicalName() + ": " + defaultValue);
         }
      }

      throw new ProtoSchemaBuilderException("No default value is allowed for field '" + fieldName + "' of Java type " + fieldType.getCanonicalName() + " from class " + clazz.getCanonicalName());
   }

   /**
    * C-style escaping using 3 digit octal escapes ({@code "\xxx"}) for all non-ASCII chars.
    */
   static byte[] cescape(String s) {
      return cescape(s.getBytes(StandardCharsets.UTF_8));
   }

   static byte[] cescape(byte[] bytes) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length);
      for (byte b : bytes) {
         int ub = Byte.toUnsignedInt(b);
         if (ub < 32 || ub > 127) {  // printable 7-bit chars are fine, the rest of all non-US-ASCII chars get escaped !
            baos.write('\\');
            baos.write('0' + ((ub >> 6) & 0x7));  // 3 digit octal code
            baos.write('0' + ((ub >> 3) & 0x7));
            baos.write('0' + (ub & 0x7));
         } else {
            baos.write(ub);
         }
      }
      return baos.toByteArray();
   }

   private static long parseLong(String value) {
      if (value == null) {
         throw new IllegalArgumentException("value argument cannot be null");
      }
      if (value.isEmpty()) {
         throw new NumberFormatException("Empty input string");
      }
      String s = value;
      boolean isNegative = false;

      if (s.charAt(0) == '-') {
         isNegative = true;
         s = s.substring(1);
      }

      int radix;
      if (s.length() > 1 && s.charAt(0) == '0') {
         if (s.length() > 2 && (s.charAt(1) == 'x' || s.charAt(1) == 'X')) {
            radix = 16;
            s = s.substring(2);
         } else {
            radix = 8;
            s = s.substring(1);
         }
      } else {
         radix = 10;
      }

      try {
         long v = Long.parseUnsignedLong(s, radix);
         return isNegative ? -v : v;
      } catch (NumberFormatException e) {
         throw new NumberFormatException("For input string: \"" + value + "\"");
      }
   }

   private static int parseInt(String value) {
      if (value == null) {
         throw new IllegalArgumentException("value argument cannot be null");
      }
      if (value.isEmpty()) {
         throw new NumberFormatException("Empty input string");
      }
      String s = value;
      boolean isNegative = false;

      if (s.charAt(0) == '-') {
         isNegative = true;
         s = s.substring(1);
      }

      int radix;
      if (s.length() > 1 && s.charAt(0) == '0') {
         if (s.length() > 2 && (s.charAt(1) == 'x' || s.charAt(1) == 'X')) {
            radix = 16;
            s = s.substring(2);
         } else {
            radix = 8;
            s = s.substring(1);
         }
      } else {
         radix = 10;
      }

      try {
         int v = Integer.parseUnsignedInt(s, radix);
         return isNegative ? -v : v;
      } catch (NumberFormatException e) {
         throw new NumberFormatException("For input string: \"" + value + "\"");
      }
   }

   private XClass getCollectionImplementation(XClass clazz, XClass fieldType, XClass configuredCollection, String fieldName, boolean isRepeated) {
      XClass collectionImplementation;

      XClass javaUtilCollectionClass = typeFactory.fromClass(Collection.class);
      if (isRepeated && !fieldType.isArray()) {
         collectionImplementation = configuredCollection;
         if (collectionImplementation == javaUtilCollectionClass) {   // default
            if (fieldType == typeFactory.fromClass(Set.class)) {
               collectionImplementation = typeFactory.fromClass(HashSet.class);
            } else if (fieldType == typeFactory.fromClass(List.class) || fieldType == typeFactory.fromClass(Collection.class)) {
               collectionImplementation = typeFactory.fromClass(ArrayList.class);
            } else {
               collectionImplementation = fieldType;
            }
         }
         if (!collectionImplementation.isAssignableTo(javaUtilCollectionClass)) {
            throw new ProtoSchemaBuilderException("The collection class of repeated field '" + fieldName + "' of " + clazz.getCanonicalName() + " must implement java.util.Collection.");
         }
         if (collectionImplementation.isAbstract()) {
            throw new ProtoSchemaBuilderException("The collection class (" + collectionImplementation.getCanonicalName() + ") of repeated field '" + fieldName + "' of " + clazz.getCanonicalName() + " must not be abstract. Please specify an appropriate class in collectionImplementation member.");
         }
         XConstructor ctor = collectionImplementation.getDeclaredConstructor();
         if (ctor == null || ctor.isPrivate()) {
            throw new ProtoSchemaBuilderException("The collection class ('" + collectionImplementation.getCanonicalName() + "') of repeated field '"
                  + fieldName + "' of " + clazz.getCanonicalName() + " must have a public no-argument constructor.");
         }
         if (!collectionImplementation.isAssignableTo(fieldType)) {
            throw new ProtoSchemaBuilderException("The collection implementation class ('" + collectionImplementation.getCanonicalName() + "') of repeated field '"
                  + fieldName + "' of " + clazz.getCanonicalName() + " is not assignable to this field's type.");
         }
      } else {
         if (configuredCollection != javaUtilCollectionClass) {
            throw new ProtoSchemaBuilderException("Specifying the collection implementation class is only allowed for collection (repeated) fields: '" + fieldName + "' of " + clazz.getCanonicalName());
         }
         collectionImplementation = null;
      }

      return collectionImplementation;
   }

   private Type getProtobufType(XClass javaType, Type declaredType) {
      switch (declaredType) {
         case MESSAGE:
            // MESSAGE means either 'unspecified' or MESSAGE
            if (javaType.isEnum()) {
               ProtoTypeMetadata m = protoSchemaGenerator.scanAnnotations(javaType);
               if (!m.isEnum()) {
                  throw new ProtoSchemaBuilderException(javaType.getCanonicalName() + " is not a Protobuf marshallable enum type");
               }
               return Type.ENUM;
            } else if (javaType == typeFactory.fromClass(String.class)) {
               return Type.STRING;
            } else if (javaType == typeFactory.fromClass(Double.class) || javaType == typeFactory.fromClass(double.class)) {
               return Type.DOUBLE;
            } else if (javaType == typeFactory.fromClass(Float.class) || javaType == typeFactory.fromClass(float.class)) {
               return Type.FLOAT;
            } else if (javaType == typeFactory.fromClass(Long.class) || javaType == typeFactory.fromClass(long.class)) {
               return Type.INT64;
            } else if (javaType == typeFactory.fromClass(Integer.class) || javaType == typeFactory.fromClass(int.class) ||
                  javaType == typeFactory.fromClass(Short.class) || javaType == typeFactory.fromClass(short.class) ||
                  javaType == typeFactory.fromClass(Byte.class) || javaType == typeFactory.fromClass(byte.class) ||
                  javaType == typeFactory.fromClass(Character.class) || javaType == typeFactory.fromClass(char.class)) {
               return Type.INT32;
            } else if (javaType == typeFactory.fromClass(Boolean.class) || javaType == typeFactory.fromClass(boolean.class)) {
               return Type.BOOL;
            } else if (javaType.isAssignableTo(Date.class)) {
               return Type.FIXED64;
            } else if (javaType.isAssignableTo(Instant.class)) {
               return Type.FIXED64;
            } else {
               ProtoTypeMetadata m = protoSchemaGenerator.scanAnnotations(javaType);
               if (m.isEnum()) {
                  throw new ProtoSchemaBuilderException(javaType.getCanonicalName() + " is not a Protobuf marshallable message type");
               }
            }
            break;
         case ENUM:
            if (!javaType.isEnum()) {
               throw new ProtoSchemaBuilderException(javaType.getCanonicalName() + " is not a Protobuf marshallable enum type");
            }
            break;
         case GROUP:
            ProtoTypeMetadata m = protoSchemaGenerator.scanAnnotations(javaType);
            if (m.isEnum()) {
               throw new ProtoSchemaBuilderException(javaType.getCanonicalName() + " is not a Protobuf marshallable message type");
            }
            break;
         case STRING:
            if (javaType != typeFactory.fromClass(String.class))
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getCanonicalName() + " vs " + declaredType);
            break;
         case BYTES:
            if (javaType != typeFactory.fromClass(byte[].class))
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getCanonicalName() + " vs " + declaredType);
            break;
         case DOUBLE:
            if (javaType != typeFactory.fromClass(Double.class) && javaType != typeFactory.fromClass(double.class))
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getCanonicalName() + " vs " + declaredType);
            break;
         case FLOAT:
            if (javaType != typeFactory.fromClass(Float.class) && javaType != typeFactory.fromClass(float.class))
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getCanonicalName() + " vs " + declaredType);
            break;
         case BOOL:
            if (javaType != typeFactory.fromClass(Boolean.class) && javaType != typeFactory.fromClass(boolean.class))
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getCanonicalName() + " vs " + declaredType);
            break;
         case INT32:
         case UINT32:
         case FIXED32:
         case SFIXED32:
         case SINT32:
            if (javaType != typeFactory.fromClass(Integer.class) && javaType != typeFactory.fromClass(int.class)
                  && javaType != typeFactory.fromClass(Short.class) && javaType != typeFactory.fromClass(short.class)
                  && javaType != typeFactory.fromClass(Byte.class) && javaType != typeFactory.fromClass(byte.class))
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getCanonicalName() + " vs " + declaredType);
            break;
         case INT64:
         case UINT64:
         case FIXED64:
         case SFIXED64:
         case SINT64:
            if (javaType != typeFactory.fromClass(Long.class) && javaType != typeFactory.fromClass(long.class)
                  && !javaType.isAssignableTo(Date.class) && !javaType.isAssignableTo(Instant.class))
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getCanonicalName() + " vs " + declaredType);
            break;
      }
      return declaredType;
   }

   private boolean isArray(XClass javaType, Type type) {
      if (type == Type.BYTES && javaType == typeFactory.fromClass(byte[].class)) {
         // A byte[] mapped to BYTES needs special handling. This will not be mapped to a repeatable field.
         return false;
      }
      return javaType.isArray();
   }

   private boolean isRepeated(XClass javaType, Type type) {
      if (type == Type.BYTES && javaType == typeFactory.fromClass(byte[].class)) {
         // A byte[] mapped to BYTES needs special handling. This will not be mapped to a repeatable field.
         return false;
      }
      return javaType.isArray() || javaType.isAssignableTo(Collection.class);
   }

   private XMethod findGetter(String propertyName, XClass propertyType) {
      boolean isBoolean = propertyType == typeFactory.fromClass(boolean.class) || propertyType == typeFactory.fromClass(Boolean.class);
      String methodName = (isBoolean ? "is" : "get") + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);

      if (isAdapter) {
         // lookup a java-bean style method first
         XMethod getter = annotatedClass.getMethod(methodName);
         if (getter == null && isBoolean) {
            // retry with 'get' instead of 'is'
            methodName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
            getter = annotatedClass.getMethod(methodName, javaClass);
         }
         if (getter == null) {
            // try the property name directly
            getter = annotatedClass.getMethod(propertyName, javaClass);
         }
         if (getter == null) {
            throw new ProtoSchemaBuilderException("No getter method found for property '" + propertyName
                  + "' of type " + propertyType.getCanonicalName() + " in class " + getAnnotatedClassName());
         }
         XClass returnType = getter.getReturnType();
         if (returnType == typeFactory.fromClass(Optional.class)) {
            returnType = getter.determineOptionalReturnType();
         }
         if (returnType != propertyType) {
            throw new ProtoSchemaBuilderException("No suitable getter method found for property '" + propertyName
                  + "' of type " + propertyType.getCanonicalName() + " in class " + getAnnotatedClassName()
                  + ". The candidate method does not have a suitable return type: " + getter);
         }
         return getter;
      } else {
         // lookup a java-bean style method first
         XMethod getter = javaClass.getMethod(methodName);
         if (getter == null && isBoolean) {
            // retry with 'get' instead of 'is'
            methodName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
            getter = javaClass.getMethod(methodName);
         }
         if (getter == null) {
            // try the property name directly
            getter = javaClass.getMethod(propertyName);
         }
         if (getter == null) {
            throw new ProtoSchemaBuilderException("No getter method found for property '" + propertyName
                  + "' of type " + propertyType.getCanonicalName() + " in class " + javaClass.getCanonicalName());
         }
         XClass returnType = getter.getReturnType();
         if (returnType == typeFactory.fromClass(Optional.class)) {
            returnType = getter.determineOptionalReturnType();
         }
         if (returnType != propertyType) {
            throw new ProtoSchemaBuilderException("No suitable getter method found for property '" + propertyName
                  + "' of type " + propertyType.getCanonicalName() + " in class " + javaClass.getCanonicalName()
                  + ". The candidate method does not have a suitable return type: " + getter);
         }
         return getter;
      }
   }

   private XMethod findSetter(String propertyName, XClass propertyType) {
      String methodName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
      if (isAdapter) {
         // lookup a java-bean style method first
         XMethod setter = annotatedClass.getMethod(methodName, javaClass, propertyType);
         if (setter == null) {
            // try the property name directly
            setter = annotatedClass.getMethod(propertyName, javaClass, propertyType);
         }
         if (setter == null) {
            throw new ProtoSchemaBuilderException("No setter method found for property '" + propertyName
                  + "' of type " + propertyType.getCanonicalName() + " in class " + getAnnotatedClassName());
         }
         if (setter.getReturnType() != typeFactory.fromClass(void.class)) {
            throw new ProtoSchemaBuilderException("No suitable setter method found for property '" + propertyName
                  + "' of type " + propertyType.getCanonicalName() + " in class " + getAnnotatedClassName()
                  + ". The candidate method does not have a suitable return type: " + setter);
         }
         return setter;
      } else {
         // lookup a java-bean style method first
         XMethod setter = javaClass.getMethod(methodName, propertyType);
         if (setter == null) {
            // try the property name directly
            setter = javaClass.getMethod(propertyName, propertyType);
         }
         if (setter == null) {
            throw new ProtoSchemaBuilderException("No setter method found for property '" + propertyName
                  + "' of type " + propertyType.getCanonicalName() + " in class " + getJavaClassName());
         }
         if (setter.getReturnType() != typeFactory.fromClass(void.class)) {
            throw new ProtoSchemaBuilderException("No suitable setter method found for property '" + propertyName
                  + "' of type " + propertyType.getCanonicalName() + " in class " + getJavaClassName()
                  + ". The candidate method does not have a suitable return type: " + setter);
         }
         return setter;
      }
   }

   @Override
   public String toString() {
      return "ProtoMessageTypeMetadata{" +
            "name='" + name + '\'' +
            ", javaClass=" + javaClass +
            ", annotatedClass=" + annotatedClass +
            ", isAdapter=" + isAdapter +
            ", fieldsByNumber=" + fieldsByNumber +
            ", factory=" + factory +
            ", unknownFieldSetField=" + unknownFieldSetField +
            ", unknownFieldSetGetter=" + unknownFieldSetGetter +
            ", unknownFieldSetSetter=" + unknownFieldSetSetter +
            ", innerTypes=" + innerTypes +
            '}';
   }
}
