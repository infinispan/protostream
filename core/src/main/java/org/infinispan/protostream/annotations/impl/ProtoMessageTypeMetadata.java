package org.infinispan.protostream.annotations.impl;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.lang.model.type.MirroredTypeException;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoMessage;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.annotations.ProtoSchemaBuilderException;
import org.infinispan.protostream.annotations.ProtoUnknownFieldSet;
import org.infinispan.protostream.annotations.impl.types.UnifiedTypeFactory;
import org.infinispan.protostream.annotations.impl.types.XClass;
import org.infinispan.protostream.annotations.impl.types.XConstructor;
import org.infinispan.protostream.annotations.impl.types.XExecutable;
import org.infinispan.protostream.annotations.impl.types.XField;
import org.infinispan.protostream.annotations.impl.types.XMethod;
import org.infinispan.protostream.descriptors.JavaType;
import org.infinispan.protostream.descriptors.Type;

/**
 * A {@link ProtoTypeMetadata} for a message type created based on annotations during the current execution of {@link
 * org.infinispan.protostream.annotations.ProtoSchemaBuilder}.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
public final class ProtoMessageTypeMetadata extends ProtoTypeMetadata {

   private final BaseProtoSchemaGenerator protoSchemaGenerator;

   private final UnifiedTypeFactory typeFactory;

   private SortedMap<Integer, ProtoFieldMetadata> fieldsByNumber = null;

   private Map<String, ProtoFieldMetadata> fieldsByName = null;

   private XExecutable factory;

   private XField unknownFieldSetField;

   private XMethod unknownFieldSetGetter;

   private XMethod unknownFieldSetSetter;

   private final Map<XClass, ProtoTypeMetadata> innerTypes = new HashMap<>();

   ProtoMessageTypeMetadata(BaseProtoSchemaGenerator protoSchemaGenerator, XClass messageClass) {
      super(getProtoName(messageClass), messageClass);
      this.protoSchemaGenerator = protoSchemaGenerator;
      this.typeFactory = messageClass.getFactory();

      checkInstantiability();
   }

   private static String getProtoName(XClass messageClass) {
      ProtoName annotation = messageClass.getAnnotation(ProtoName.class);
      ProtoMessage protoMessageAnnotation = messageClass.getAnnotation(ProtoMessage.class);
      if (annotation != null) {
         if (protoMessageAnnotation != null) {
            throw new ProtoSchemaBuilderException("@ProtoMessage annotation cannot be used together with @ProtoName: " + messageClass.getName());
         }
         return annotation.value().isEmpty() ? messageClass.getSimpleName() : annotation.value();
      }
      return protoMessageAnnotation == null || protoMessageAnnotation.name().isEmpty() ? messageClass.getSimpleName() : protoMessageAnnotation.name();
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
      reserved.scan(javaClass);

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
   public boolean isEnum() {
      return false;
   }

   @Override
   public ProtoEnumValueMetadata getEnumMemberByName(String name) {
      throw new IllegalStateException(javaClass.getCanonicalName() + " is not an enum");
   }

   @Override
   public void scanMemberAnnotations() {
      if (fieldsByNumber == null) {
         // All the fields discovered in this class hierarchy, the key is their number.
         // We use a TreeMap to ensure ascending order by field number.
         fieldsByNumber = new TreeMap<>();

         // all the fields discovered in this class hierarchy, by name
         fieldsByName = new HashMap<>();

         discoverFields(javaClass, new HashSet<>());
         if (fieldsByNumber.isEmpty()) {
            throw new ProtoSchemaBuilderException("Class " + javaClass.getCanonicalName() + " does not have any @ProtoField annotated members. The class should be either annotated or it should have a custom marshaller.");
         }

         // if we have a factory method or constructor, ensure its params match the declared fields
         if (factory != null) {
            String[] parameterNames = factory.getParameterNames();
            if (parameterNames.length != fieldsByNumber.size()) {
               throw new ProtoSchemaBuilderException("@ProtoFactory annotated static method or constructor signature mismatch. Expected "
                     + fieldsByNumber.size() + " parameters but found " + parameterNames.length + " : " + factory.toGenericString());
            }
            XClass[] parameterTypes = factory.getParameterTypes();
            for (int i = 0; i < parameterNames.length; i++) {
               String parameterName = parameterNames[i];
               ProtoFieldMetadata fieldMetadata = getFieldByPropertyName(parameterName);
               if (fieldMetadata == null) {
                  throw new ProtoSchemaBuilderException("@ProtoFactory annotated static method or constructor signature mismatch. The parameter '"
                        + parameterName + "' does not match any field : " + factory.toGenericString());
               }
               XClass parameterType = parameterTypes[i];
               if (fieldMetadata.isArray()) {
                  if (!parameterType.isArray()) {
                     throw new ProtoSchemaBuilderException("@ProtoFactory annotated static method or constructor signature mismatch. The parameter '" + parameterName + "' does not match the field definition: " + factory.toGenericString());
                  }
                  if (parameterType.getComponentType() != fieldMetadata.getJavaType()) {
                     throw new ProtoSchemaBuilderException("@ProtoFactory annotated static method or constructor signature mismatch. The parameter '" + parameterName + "' does not match the field definition: " + factory.toGenericString());
                  }
               } else if (fieldMetadata.isRepeated()) {
                  // todo [anistor] check collection type parameter also
                  if (!fieldMetadata.getCollectionImplementation().isAssignableTo(parameterType)) {
                     throw new ProtoSchemaBuilderException("@ProtoFactory annotated static method or constructor signature mismatch. The parameter '" + parameterName + "' does not match the field definition: " + factory.toGenericString());
                  }
               } else {
                  if (!fieldMetadata.getJavaType().isAssignableTo(parameterType)) {
                     throw new ProtoSchemaBuilderException("@ProtoFactory annotated static method or constructor signature mismatch. The parameter '" + parameterName + "' does not match the field definition: " + factory.toGenericString());
                  }
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
    * Ensure we have a proper constructor or factory method.
    */
   private void checkInstantiability() {
      // ensure the class is not abstract
      if (javaClass.isAbstract() || javaClass.isInterface()) {
         throw new ProtoSchemaBuilderException("Abstract classes are not allowed: " + getJavaClassName());
      }
      // ensure it is not a local or anonymous class
      if (javaClass.isLocal()) {
         throw new ProtoSchemaBuilderException("Local or anonymous classes are not allowed. The class " + getJavaClassName() + " must be instantiable using an accessible no-argument constructor.");
      }
      // ensure the class is not a non-static inner class
      if (javaClass.getEnclosingClass() != null && !javaClass.isStatic()) {
         throw new ProtoSchemaBuilderException("Non-static inner classes are not allowed. The class " + getJavaClassName() + " must be instantiable using an accessible no-argument constructor.");
      }

      for (XConstructor c : javaClass.getDeclaredConstructors()) {
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
      for (XMethod m : javaClass.getDeclaredMethods()) {
         if (m.getAnnotation(ProtoFactory.class) != null) {
            if (factory != null) {
               throw new ProtoSchemaBuilderException("Found more than one @ProtoFactory annotated method / constructor : " + m);
            }
            if (!m.isStatic()) {
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
            if (unknownFieldSetField != null || unknownFieldSetGetter != null || unknownFieldSetSetter != null) {
               throw new ProtoSchemaBuilderException("The @ProtoUnknownFieldSet annotation should not occur more than once in a class and its superclasses and superinterfaces : " + field);
            }
            unknownFieldSetField = field;
         } else {
            ProtoField annotation = field.getAnnotation(ProtoField.class);
            if (annotation != null) {
               if (field.isStatic()) {
                  throw new ProtoSchemaBuilderException("Static fields cannot be @ProtoField annotated: " + field);
               }
               if (factory == null && field.isFinal()) {
                  throw new ProtoSchemaBuilderException("Final fields cannot be @ProtoField annotated: " + field);
               }
               if (field.isPrivate()) {
                  throw new ProtoSchemaBuilderException("Private fields cannot be @ProtoField annotated: " + field);
               }
               if (annotation.number() == 0) {
                  throw new ProtoSchemaBuilderException("0 is not a valid Protobuf field number: " + field);
               }
               String fieldName = annotation.name();
               if (fieldName.isEmpty()) {
                  fieldName = field.getName();
               }

               Type protobufType = annotation.type();
               if (field.getType() == typeFactory.fromClass(byte[].class) && protobufType == Type.MESSAGE) {
                  // MESSAGE is the default, so stands for undefined too.
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
               if (!javaType.isArray() && !javaType.isPrimitive() && javaType.isAbstract()) {
                  throw new ProtoSchemaBuilderException("The type " + javaType.getCanonicalName() + " of field '" + fieldName + "' of " + clazz.getCanonicalName() + " should not be abstract.");
               }

               protobufType = getProtobufType(javaType, protobufType);

               Object defaultValue = getDefaultValue(clazz, fieldName, javaType, protobufType, annotation.defaultValue());

               if (!isRequired && !isRepeated && javaType.isPrimitive() && defaultValue == null) {
                  throw new ProtoSchemaBuilderException("Primitive field '" + fieldName + "' of " + clazz.getCanonicalName() + " is not nullable and should be either marked required or should have a default value.");
               }

               XClass collectionImplementation = getCollectionImplementation(clazz, field.getType(), getCollectionImplementationFromAnnotation(annotation), fieldName, isRepeated);
               if (isArray) {
                  collectionImplementation = typeFactory.fromClass(ArrayList.class);
               }

               ProtoTypeMetadata protoTypeMetadata = null;
               if (protobufType.getJavaType() == JavaType.ENUM || protobufType.getJavaType() == JavaType.MESSAGE) {
                  protoTypeMetadata = protoSchemaGenerator.scanAnnotations(javaType);
               }
               ProtoFieldMetadata fieldMetadata = new ProtoFieldMetadata(annotation.number(), fieldName, javaType, collectionImplementation,
                     protobufType, protoTypeMetadata, isRequired, isRepeated, isArray, defaultValue, field);

               ProtoFieldMetadata existing = fieldsByNumber.get(annotation.number());
               if (existing != null) {
                  throw new ProtoSchemaBuilderException("Duplicate field number definition. Found two field definitions with number " + annotation.number() + ": in "
                        + fieldMetadata.getLocation() + " and in " + existing.getLocation());
               }
               existing = fieldsByName.get(fieldMetadata.getName());
               if (existing != null) {
                  throw new ProtoSchemaBuilderException("Duplicate field name definition. Found two field definitions with name '" + fieldMetadata.getName() + "': in "
                        + fieldMetadata.getLocation() + " and in " + existing.getLocation());
               }

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
               if (method.getName().startsWith("set") && method.getName().length() >= 4) {
                  propertyName = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
               } else {
                  throw new ProtoSchemaBuilderException("Illegal setter method signature: " + method);
               }
               if (method.getParameterTypes().length != 1) {
                  throw new ProtoSchemaBuilderException("Illegal setter method signature: " + method);
               }
               unknownFieldSetSetter = method;
               unknownFieldSetGetter = findGetter(propertyName, method.getParameterTypes()[0]);
            } else {
               // this method is expected to be a getter
               if (method.getName().startsWith("get") && method.getName().length() >= 4) {
                  propertyName = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
               } else if (method.getName().startsWith("is") && method.getName().length() >= 3) {
                  propertyName = Character.toLowerCase(method.getName().charAt(2)) + method.getName().substring(3);
               } else {
                  throw new ProtoSchemaBuilderException("Illegal getter method signature: " + method);
               }
               unknownFieldSetGetter = method;
               unknownFieldSetSetter = findSetter(propertyName, unknownFieldSetGetter.getReturnType());
            }
         } else {
            ProtoField annotation = method.getAnnotation(ProtoField.class);
            if (annotation != null) {
               if (method.isPrivate()) {
                  throw new ProtoSchemaBuilderException("Private methods cannot be @ProtoField annotated: " + method);
               }
               if (method.isStatic()) {
                  throw new ProtoSchemaBuilderException("Static methods cannot be @ProtoField annotated: " + method);
               }
               String propertyName;
               XMethod getter;
               XMethod setter;
               // we can have the annotation present on either getter or setter but not both
               if (method.getReturnType() == typeFactory.fromClass(void.class)) {
                  // this method is expected to be a setter
                  if (method.getName().startsWith("set") && method.getName().length() >= 4) {
                     propertyName = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
                  } else {
                     throw new ProtoSchemaBuilderException("Illegal setter method signature: " + method);
                  }
                  if (method.getParameterTypes().length != 1) {
                     throw new ProtoSchemaBuilderException("Illegal setter method signature: " + method);
                  }
                  if (factory != null) {
                     throw new ProtoSchemaBuilderException("Setter methods should not be annotated with @ProtoField when @ProtoFactory is used by a class: " + method);
                  }
                  setter = method;
                  getter = findGetter(propertyName, method.getParameterTypes()[0]);
               } else {
                  // this method is expected to be a getter
                  if (method.getName().startsWith("get") && method.getName().length() >= 4) {
                     propertyName = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
                  } else if (method.getName().startsWith("is") && method.getName().length() >= 3) {
                     propertyName = Character.toLowerCase(method.getName().charAt(2)) + method.getName().substring(3);
                  } else {
                     throw new ProtoSchemaBuilderException("Illegal getter method signature: " + method);
                  }
                  getter = method;
                  if (factory == null) {
                     setter = findSetter(propertyName, getter.getReturnType());
                  } else {
                     setter = null;
                  }
               }
               if (annotation.number() == 0) {
                  throw new ProtoSchemaBuilderException("0 is not a valid Protobuf field number: " + method);
               }

               String fieldName = annotation.name();
               if (fieldName.isEmpty()) {
                  fieldName = propertyName;
               }

               Type protobufType = annotation.type();
               if (getter.getReturnType() == typeFactory.fromClass(byte[].class) && protobufType == Type.MESSAGE) {
                  // MESSAGE is the default, so stands for undefined too.
                  protobufType = Type.BYTES;
               }
               boolean isArray = isArray(getter.getReturnType(), protobufType);
               boolean isRepeated = isRepeated(getter.getReturnType(), protobufType);
               boolean isRequired = annotation.required();
               if (isRepeated && isRequired) {
                  throw new ProtoSchemaBuilderException("Repeated field '" + fieldName + "' of " + clazz.getCanonicalName() + " cannot be marked required.");
               }
               XClass javaType = getJavaTypeFromAnnotation(annotation);
               if (javaType == typeFactory.fromClass(void.class)) {
                  javaType = isRepeated ? getter.determineRepeatedElementType() : getter.getReturnType();
               }
               if (!javaType.isArray() && !javaType.isPrimitive() && javaType.isAbstract()) {
                  throw new ProtoSchemaBuilderException("The type " + javaType.getCanonicalName() + " of field '" + fieldName + "' of " + clazz.getCanonicalName() + " should not be abstract.");
               }

               protobufType = getProtobufType(javaType, protobufType);

               Object defaultValue = getDefaultValue(clazz, fieldName, javaType, protobufType, annotation.defaultValue());

               if (!isRequired && !isRepeated && javaType.isPrimitive() && defaultValue == null) {
                  throw new ProtoSchemaBuilderException("Primitive field '" + fieldName + "' of " + clazz.getCanonicalName() + " is not nullable and should be either marked required or should have a default value.");
               }

               XClass collectionImplementation = getCollectionImplementation(clazz, getter.getReturnType(), getCollectionImplementationFromAnnotation(annotation), fieldName, isRepeated);
               if (isArray) {
                  collectionImplementation = typeFactory.fromClass(ArrayList.class);
               }

               ProtoTypeMetadata protoTypeMetadata = null;
               if (protobufType.getJavaType() == JavaType.ENUM || protobufType.getJavaType() == JavaType.MESSAGE) {
                  protoTypeMetadata = protoSchemaGenerator.scanAnnotations(javaType);
               }

               ProtoFieldMetadata fieldMetadata = new ProtoFieldMetadata(annotation.number(), fieldName, javaType, collectionImplementation,
                     protobufType, protoTypeMetadata, isRequired, isRepeated, isArray, defaultValue,
                     propertyName, method, getter, setter);

               ProtoFieldMetadata existing = fieldsByNumber.get(annotation.number());
               if (existing != null) {
                  throw new ProtoSchemaBuilderException("Duplicate field definition. Found two field definitions with number " + annotation.number() + ": in "
                        + fieldMetadata.getLocation() + " and in " + existing.getLocation());
               }
               existing = fieldsByName.get(fieldMetadata.getName());
               if (existing != null) {
                  throw new ProtoSchemaBuilderException("Duplicate field definition. Found two field definitions with name '" + fieldMetadata.getName() + "': in "
                        + fieldMetadata.getLocation() + " and in " + existing.getLocation());
               }

               fieldsByNumber.put(annotation.number(), fieldMetadata);
               fieldsByName.put(fieldName, fieldMetadata);
            }
         }
      }
   }

   private XClass getCollectionImplementationFromAnnotation(ProtoField annotation) {
      try {
         return typeFactory.fromClass(annotation.collectionImplementation());
      } catch (MirroredTypeException e) {
         return typeFactory.fromTypeMirror(e.getTypeMirror());
      }
   }

   private XClass getJavaTypeFromAnnotation(ProtoField annotation) {
      try {
         return typeFactory.fromClass(annotation.javaType());
      } catch (MirroredTypeException e) {
         return typeFactory.fromTypeMirror(e.getTypeMirror());
      }
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
         if (fieldType.isAssignableTo(typeFactory.fromClass(Date.class))) {
            return Long.parseUnsignedLong(defaultValue);
         }
         if (fieldType.isAssignableTo(typeFactory.fromClass(Instant.class))) {
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

   private long parseLong(String value) {
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

   private int parseInt(String value) {
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
         if (collectionImplementation == javaUtilCollectionClass) {
            collectionImplementation = fieldType;
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
            throw new ProtoSchemaBuilderException("Specifying the collection implementation class is only allowed for repeated/collection fields: '" + fieldName + "' of " + clazz.getCanonicalName());
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
            } else if (javaType.isAssignableTo(typeFactory.fromClass(Date.class))) {
               return Type.FIXED64;
            } else if (javaType.isAssignableTo(typeFactory.fromClass(Instant.class))) {
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
                  && !javaType.isAssignableTo(typeFactory.fromClass(Date.class)) && !javaType.isAssignableTo(typeFactory.fromClass(Instant.class)))
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
      return javaType.isArray() || javaType.isAssignableTo(typeFactory.fromClass(Collection.class));
   }

   private XMethod findGetter(String propertyName, XClass propertyType) {
      String prefix = "get";
      if (propertyType == typeFactory.fromClass(boolean.class) || propertyType == typeFactory.fromClass(Boolean.class)) {
         prefix = "is";
      }
      String methodName = prefix + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
      XMethod getter = javaClass.getMethod(methodName);
      if (getter == null) {
         throw new ProtoSchemaBuilderException("No getter method found for property '" + propertyName
               + "' of type " + propertyType.getCanonicalName() + " in class " + javaClass.getCanonicalName());
      }
      if (getter.getReturnType() != propertyType) {
         throw new ProtoSchemaBuilderException("No suitable getter method found for property '" + propertyName
               + "' of type " + propertyType.getCanonicalName() + " in class " + javaClass.getCanonicalName()
               + ". The candidate method does not have a suitable return type: " + getter);
      }
      return getter;
   }

   private XMethod findSetter(String propertyName, XClass propertyType) {
      String methodName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
      XMethod setter = javaClass.getMethod(methodName, propertyType);
      if (setter == null) {
         throw new ProtoSchemaBuilderException("No setter method found for property '" + propertyName
               + "' of type " + propertyType.getCanonicalName() + " in class " + javaClass.getCanonicalName());
      }
      if (setter.getReturnType() != typeFactory.fromClass(void.class)) {
         throw new ProtoSchemaBuilderException("No suitable setter method found for property '" + propertyName
               + "' of type " + propertyType.getCanonicalName() + " in class " + javaClass.getCanonicalName()
               + ". The candidate method does not have a suitable return type: " + setter);
      }
      return setter;
   }
}
