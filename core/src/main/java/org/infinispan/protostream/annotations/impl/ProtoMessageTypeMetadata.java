package org.infinispan.protostream.annotations.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoMessage;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.annotations.ProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoSchemaBuilderException;
import org.infinispan.protostream.annotations.ProtoUnknownFieldSet;
import org.infinispan.protostream.descriptors.JavaType;
import org.infinispan.protostream.descriptors.Type;

/**
 * A {@link ProtoTypeMetadata} for a message type created based on annotations during the current execution of {@link
 * org.infinispan.protostream.annotations.ProtoSchemaBuilder}.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
final class ProtoMessageTypeMetadata extends ProtoTypeMetadata {

   private final ProtoSchemaGenerator protoSchemaGenerator;

   private Map<Integer, ProtoFieldMetadata> fields = null;

   private Field unknownFieldSetField;

   private Method unknownFieldSetGetter;

   private Method unknownFieldSetSetter;

   private final Map<Class<?>, ProtoTypeMetadata> innerTypes = new HashMap<>();

   ProtoMessageTypeMetadata(ProtoSchemaGenerator protoSchemaGenerator, Class<?> messageClass) {
      super(getProtoName(messageClass), messageClass, DocumentationExtractor.getDocumentation(messageClass));
      this.protoSchemaGenerator = protoSchemaGenerator;

      checkInstantiability();
   }

   private static String getProtoName(Class<?> messageClass) {
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

   public Field getUnknownFieldSetField() {
      scanMemberAnnotations();
      return unknownFieldSetField;
   }

   public Method getUnknownFieldSetGetter() {
      scanMemberAnnotations();
      return unknownFieldSetGetter;
   }

   public Method getUnknownFieldSetSetter() {
      scanMemberAnnotations();
      return unknownFieldSetSetter;
   }

   public Map<Integer, ProtoFieldMetadata> getFields() {
      scanMemberAnnotations();
      return fields;
   }

   protected void addInnerType(ProtoTypeMetadata typeMetadata) {
      innerTypes.put(typeMetadata.getJavaClass(), typeMetadata);
   }

   @Override
   public void generateProto(IndentWriter iw) {
      scanMemberAnnotations();  //todo [anistor] need to have a better place for this call

      iw.append("\n\n");
      appendDocumentation(iw, documentation);
      iw.append("message ").append(name);
      if (ProtoSchemaBuilder.generateSchemaDebugComments) {
         iw.append(" /* ").append(getJavaClassName()).append(" */");
      }
      iw.append(" {\n");

      if (!innerTypes.isEmpty()) {
         iw.inc();
         for (ProtoTypeMetadata t : innerTypes.values()) {
            t.generateProto(iw);
         }
         iw.dec();
      }

      iw.inc();
      for (ProtoFieldMetadata f : fields.values()) {
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
      if (fields == null) {
         // all the fields discovered in this class hierarchy, by number
         // use a TreeMap to ensure ascending order by field number
         fields = new TreeMap<>();

         // all the fields discovered in this class hierarchy, by name
         Map<String, ProtoFieldMetadata> fieldsByName = new HashMap<>();

         Set<Class<?>> examinedClasses = new HashSet<>();
         discoverFields(javaClass, examinedClasses, fields, fieldsByName);
         if (fields.isEmpty()) {
            throw new ProtoSchemaBuilderException("Class " + javaClass.getCanonicalName() + " does not have any @ProtoField annotated fields. The class should be either annotated or it should have a custom marshaller.");
         }
         checkInstantiability();
      }
   }

   private void checkInstantiability() {
      // ensure the class is not abstract
      if (Modifier.isAbstract(javaClass.getModifiers())) {
         throw new ProtoSchemaBuilderException("Abstract classes are not allowed: " + getJavaClassName());
      }
      // ensure it is not a local or anonymous class
      if (javaClass.getEnclosingMethod() != null || javaClass.getEnclosingConstructor() != null) {
         throw new ProtoSchemaBuilderException("Local or anonymous classes are not allowed. The class " + getJavaClassName() + " must be instantiable using a non-private no-argument constructor.");
      }
      // ensure the class is not a non-static inner class
      if (javaClass.getEnclosingClass() != null && !Modifier.isStatic(javaClass.getModifiers())) {
         throw new ProtoSchemaBuilderException("Non-static inner classes are not allowed. The class " + getJavaClassName() + " must be instantiable using a non-private no-argument constructor.");
      }
      // ensure the class has a non-private no-argument constructor
      Constructor<?> ctor = null;
      try {
         ctor = javaClass.getDeclaredConstructor();
      } catch (NoSuchMethodException ignored) {
      }
      if (ctor == null || Modifier.isPrivate(ctor.getModifiers())) {
         throw new ProtoSchemaBuilderException("The class " + getJavaClassName() + " must must be instantiable using a non-private no-argument constructor.");
      }
   }

   private void discoverFields(Class<?> clazz, Set<Class<?>> examinedClasses, Map<Integer, ProtoFieldMetadata> fieldsByNumber, Map<String, ProtoFieldMetadata> fieldsByName) {
      if (!examinedClasses.add(clazz)) {
         // avoid re-examining classes due to multiple interface inheritance
         return;
      }

      if (clazz.getSuperclass() != null) {
         discoverFields(clazz.getSuperclass(), examinedClasses, fieldsByNumber, fieldsByName);
      }
      for (Class<?> i : clazz.getInterfaces()) {
         discoverFields(i, examinedClasses, fieldsByNumber, fieldsByName);
      }

      for (Field field : clazz.getDeclaredFields()) {
         if (field.getAnnotation(ProtoUnknownFieldSet.class) != null) {
            if (unknownFieldSetField != null || unknownFieldSetGetter != null || unknownFieldSetSetter != null) {
               throw new ProtoSchemaBuilderException("The @ProtoUnknownFieldSet annotation should not be used multiple times in one class hierarchy : " + field);
            }
            unknownFieldSetField = field;
         } else {
            ProtoField annotation = field.getAnnotation(ProtoField.class);
            if (annotation != null) {
               if (Modifier.isStatic(field.getModifiers())) {
                  throw new ProtoSchemaBuilderException("Static fields cannot be @ProtoField annotated: " + field);
               }
               if (Modifier.isFinal(field.getModifiers())) {
                  throw new ProtoSchemaBuilderException("Final fields cannot be @ProtoField annotated: " + field);
               }
               if (Modifier.isPrivate(field.getModifiers())) {
                  throw new ProtoSchemaBuilderException("Private fields cannot be @ProtoField annotated: " + field);
               }
               if (annotation.number() == 0) {
                  throw new ProtoSchemaBuilderException("0 is not a valid Protobuf field number: " + field);
               }
               String fieldName = annotation.name();
               if (fieldName.isEmpty()) {
                  fieldName = field.getName();
               }

               boolean isArray = field.getType().isArray();
               boolean isRepeated = isRepeated(field.getType());
               boolean isRequired = annotation.required();
               if (isRepeated && isRequired) {
                  throw new ProtoSchemaBuilderException("Repeated field '" + fieldName + "' of " + clazz.getCanonicalName() + " cannot be marked required.");
               }
               Class<?> javaType = annotation.javaType();
               if (javaType == void.class) {
                  if (isRepeated) {
                     javaType = determineElementType(field.getType(), field.getGenericType());
                  } else {
                     javaType = field.getType();
                  }
               }
               if (!javaType.isArray() && !javaType.isPrimitive() && Modifier.isAbstract(javaType.getModifiers())) {
                  throw new ProtoSchemaBuilderException("The type " + javaType.getCanonicalName() + " of field '" + fieldName + "' of " + clazz.getCanonicalName() + " should not be abstract.");
               }

               Object defaultValue = getDefaultValue(clazz, fieldName, javaType, annotation.defaultValue());

               if (!isRequired && !isRepeated && javaType.isPrimitive() && defaultValue == null) {
                  throw new ProtoSchemaBuilderException("Primitive field '" + fieldName + "' of " + clazz.getCanonicalName() + " should be marked required or should have a default value.");
               }

               Class<?> collectionImplementation = getCollectionImplementation(clazz, field.getType(), annotation.collectionImplementation(), fieldName, isRepeated);

               Type protobufType = getProtobufType(javaType, annotation.type());
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

      for (Method method : clazz.getDeclaredMethods()) {
         if (method.getAnnotation(ProtoUnknownFieldSet.class) != null) {
            if (unknownFieldSetField != null || unknownFieldSetGetter != null || unknownFieldSetSetter != null) {
               throw new ProtoSchemaBuilderException("The @ProtoUnknownFieldSet annotation should not be used multiple times in one class hierarchy : " + method);
            }
            String propertyName;
            if (method.getReturnType() == Void.TYPE) {
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
               if (Modifier.isPrivate(method.getModifiers())) {
                  throw new ProtoSchemaBuilderException("Private methods cannot be @ProtoField annotated: " + method);
               }
               if (Modifier.isStatic(method.getModifiers())) {
                  throw new ProtoSchemaBuilderException("Static methods cannot be @ProtoField annotated: " + method);
               }
               String propertyName;
               Method getter;
               Method setter;
               // we can have the annotation present on either getter or setter but not both
               if (method.getReturnType() == Void.TYPE) {
                  // this method is expected to be a setter
                  if (method.getName().startsWith("set") && method.getName().length() >= 4) {
                     propertyName = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
                  } else {
                     throw new ProtoSchemaBuilderException("Illegal setter method signature: " + method);
                  }
                  if (method.getParameterTypes().length != 1) {
                     throw new ProtoSchemaBuilderException("Illegal setter method signature: " + method);
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
                  setter = findSetter(propertyName, getter.getReturnType());
               }
               if (annotation.number() == 0) {
                  throw new ProtoSchemaBuilderException("0 is not a valid Protobuf field number: " + method);
               }

               String fieldName = annotation.name();
               if (fieldName.isEmpty()) {
                  fieldName = propertyName;
               }

               boolean isArray = getter.getReturnType().isArray();
               boolean isRepeated = isRepeated(getter.getReturnType());
               boolean isRequired = annotation.required();
               if (isRepeated && isRequired) {
                  throw new ProtoSchemaBuilderException("Repeated field '" + fieldName + "' of " + clazz.getCanonicalName() + " cannot be marked required.");
               }
               Class<?> javaType = annotation.javaType();
               if (javaType == void.class) {
                  if (isRepeated) {
                     javaType = determineElementType(getter.getReturnType(), getter.getGenericReturnType());
                  } else {
                     javaType = getter.getReturnType();
                  }
               }
               if (!javaType.isArray() && !javaType.isPrimitive() && Modifier.isAbstract(javaType.getModifiers())) {
                  throw new ProtoSchemaBuilderException("The type " + javaType.getCanonicalName() + " of field '" + fieldName + "' of " + clazz.getCanonicalName() + " should not be abstract.");
               }

               Object defaultValue = getDefaultValue(clazz, fieldName, javaType, annotation.defaultValue());

               if (!isRequired && !isRepeated && javaType.isPrimitive() && defaultValue == null) {
                  throw new ProtoSchemaBuilderException("Primitive field '" + fieldName + "' of " + clazz.getCanonicalName() + " should be marked required or should have a default value.");
               }

               Class<?> collectionImplementation = getCollectionImplementation(clazz, getter.getReturnType(), annotation.collectionImplementation(), fieldName, isRepeated);

               Type protobufType = getProtobufType(javaType, annotation.type());
               ProtoTypeMetadata protoTypeMetadata = null;
               if (protobufType.getJavaType() == JavaType.ENUM || protobufType.getJavaType() == JavaType.MESSAGE) {
                  protoTypeMetadata = protoSchemaGenerator.scanAnnotations(javaType);
               }

               ProtoFieldMetadata fieldMetadata = new ProtoFieldMetadata(annotation.number(), fieldName, javaType, collectionImplementation,
                     protobufType, protoTypeMetadata, isRequired, isRepeated, isArray, defaultValue,
                     method, getter, setter);

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

   private Object getDefaultValue(Class<?> clazz, String fieldName, Class<?> fieldType, String defaultValue) {
      if (defaultValue == null || defaultValue.isEmpty()) {
         return null;
      }
      if (fieldType == String.class) {
         return "\"" + defaultValue + "\"";
      }
      if (fieldType.isEnum()) {
         ProtoTypeMetadata protoEnumTypeMetadata = protoSchemaGenerator.scanAnnotations(fieldType);
         ProtoEnumValueMetadata enumVal = protoEnumTypeMetadata.getEnumMemberByName(defaultValue);
         if (enumVal == null) {
            throw new ProtoSchemaBuilderException("Invalid default value for field '" + fieldName + "' of Java type " + fieldType.getCanonicalName() + " from class " + clazz.getCanonicalName() + ": " + defaultValue + " is not a member of " + protoEnumTypeMetadata.getFullName() + " enum");
         }
         return enumVal;
      }
      if (fieldType == Character.class || fieldType == Character.TYPE) {
         if (defaultValue.length() > 1) {
            throw new ProtoSchemaBuilderException("Invalid default value for field '" + fieldName + "' of Java type " + fieldType.getCanonicalName() + " from class " + clazz.getCanonicalName() + ": " + defaultValue);
         }
         return defaultValue.charAt(0);
      }
      if (fieldType == Boolean.class || fieldType == Boolean.TYPE) {
         return Boolean.valueOf(defaultValue);
      }
      try {
         if (fieldType == Integer.class || fieldType == Integer.TYPE) {
            return Integer.valueOf(defaultValue);
         }
         if (fieldType == Long.class || fieldType == Long.TYPE) {
            return Long.valueOf(defaultValue);
         }
         if (fieldType == Short.class || fieldType == Short.TYPE) {
            return Short.valueOf(defaultValue);
         }
         if (fieldType == Double.class || fieldType == Double.TYPE) {
            return Double.valueOf(defaultValue);
         }
         if (fieldType == Float.class || fieldType == Float.TYPE) {
            return Float.valueOf(defaultValue);
         }
         if (fieldType == Byte.class || fieldType == Byte.TYPE) {
            return Byte.valueOf(defaultValue);
         }
         if (Date.class.isAssignableFrom(fieldType)) {
            return Long.valueOf(defaultValue);
         }
         if (Instant.class.isAssignableFrom(fieldType)) {
            return Long.valueOf(defaultValue);
         }
      } catch (NumberFormatException e) {
         throw new ProtoSchemaBuilderException("Invalid default value for field '" + fieldName + "' of Java type " + fieldType.getCanonicalName() + " from class " + clazz.getCanonicalName() + ": " + defaultValue, e);
      }

      throw new ProtoSchemaBuilderException("No default value is allowed for field '" + fieldName + "' of Java type " + fieldType.getCanonicalName() + " from class " + clazz.getCanonicalName());
   }

   private Class<?> getCollectionImplementation(Class<?> clazz, Class<?> fieldType, Class<?> configuredCollection, String fieldName, boolean isRepeated) {
      Class<?> collectionImplementation;
      if (isRepeated && !fieldType.isArray()) {
         collectionImplementation = configuredCollection;
         if (collectionImplementation == Collection.class) {
            collectionImplementation = fieldType;
         }
         if (!Collection.class.isAssignableFrom(collectionImplementation)) {
            throw new ProtoSchemaBuilderException("The collection class of repeated field '" + fieldName + "' of " + clazz.getCanonicalName() + " must implement java.util.Collection.");
         }
         if (Modifier.isAbstract(collectionImplementation.getModifiers())) {
            throw new ProtoSchemaBuilderException("The collection class (" + collectionImplementation.getCanonicalName() + ") of repeated field '" + fieldName + "' of " + clazz.getCanonicalName() + " must not be abstract. Please specify an appropriate class in collectionImplementation member.");
         }
         try {
            collectionImplementation.getDeclaredConstructor();
         } catch (NoSuchMethodException e) {
            throw new ProtoSchemaBuilderException("The collection class ('" + collectionImplementation.getCanonicalName() + "') of repeated field '"
                  + fieldName + "' of " + clazz.getCanonicalName() + " must have a public no-argument constructor.");
         }
         if (!fieldType.isAssignableFrom(collectionImplementation)) {
            throw new ProtoSchemaBuilderException("The collection implementation class ('" + collectionImplementation.getCanonicalName() + "') of repeated field '"
                  + fieldName + "' of " + clazz.getCanonicalName() + " is not assignable to this field's type.");
         }
      } else {
         if (configuredCollection != Collection.class) {
            throw new ProtoSchemaBuilderException("Specifying the collection implementation class is only allowed for repeated/collection fields: '" + fieldName + "' of " + clazz.getCanonicalName());
         }
         collectionImplementation = null;
      }
      return collectionImplementation;
   }

   private Type getProtobufType(Class<?> javaType, Type declaredType) {
      switch (declaredType) {
         case MESSAGE:
            // MESSAGE means either 'unspecified' or MESSAGE
            if (javaType.isEnum()) {
               ProtoTypeMetadata m = protoSchemaGenerator.scanAnnotations(javaType);
               if (!m.isEnum()) {
                  throw new ProtoSchemaBuilderException(javaType.getCanonicalName() + " is not a Protobuf marshallable enum type");
               }
               return Type.ENUM;
            } else if (javaType == String.class) {
               return Type.STRING;
            } else if (javaType == Double.class || javaType == Double.TYPE) {
               return Type.DOUBLE;
            } else if (javaType == Float.class || javaType == Float.TYPE) {
               return Type.FLOAT;
            } else if (javaType == Long.class || javaType == Long.TYPE) {
               return Type.INT64;
            } else if (javaType == Integer.class || javaType == Integer.TYPE ||
                  javaType == Short.class || javaType == Short.TYPE ||
                  javaType == Byte.class || javaType == Byte.TYPE ||
                  javaType == Character.class || javaType == Character.TYPE) {
               return Type.INT32;
            } else if (javaType == Boolean.class || javaType == Boolean.TYPE) {
               return Type.BOOL;
            } else if (Date.class.isAssignableFrom(javaType)) {
               return Type.FIXED64;
            } else if (Instant.class.isAssignableFrom(javaType)) {
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
            if (javaType != String.class)
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getCanonicalName() + " vs " + declaredType);
            break;
         case BYTES:
            if (javaType != byte[].class)
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getCanonicalName() + " vs " + declaredType);
            break;
         case DOUBLE:
            if (javaType != Double.class && javaType != Double.TYPE)
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getCanonicalName() + " vs " + declaredType);
            break;
         case FLOAT:
            if (javaType != Float.class && javaType != Float.TYPE)
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getCanonicalName() + " vs " + declaredType);
            break;
         case BOOL:
            if (javaType != Boolean.class && javaType != Boolean.TYPE)
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getCanonicalName() + " vs " + declaredType);
            break;
         case INT32:
         case UINT32:
         case FIXED32:
         case SFIXED32:
         case SINT32:
            if (javaType != Integer.class && javaType != Integer.TYPE)
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getCanonicalName() + " vs " + declaredType);
            break;
         case INT64:
         case UINT64:
         case FIXED64:
         case SFIXED64:
         case SINT64:
            if (javaType != Long.class && javaType != Long.TYPE
                  && !Date.class.isAssignableFrom(javaType) && !Instant.class.isAssignableFrom(javaType))
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getCanonicalName() + " vs " + declaredType);
            break;
      }
      return declaredType;
   }

   private boolean isRepeated(Class<?> type) {
      return type.isArray() || Collection.class.isAssignableFrom(type);
   }

   private Method findGetter(String propertyName, Class<?> propertyType) {
      String prefix = "get";
      if (propertyType == Boolean.TYPE || propertyType == Boolean.class) {
         prefix = "is";
      }
      String methodName = prefix + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
      Method getter;
      try {
         getter = javaClass.getMethod(methodName);
      } catch (NoSuchMethodException e) {
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

   private Method findSetter(String propertyName, Class<?> propertyType) {
      String methodName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
      Method setter;
      try {
         setter = javaClass.getMethod(methodName, propertyType);
      } catch (NoSuchMethodException e) {
         throw new ProtoSchemaBuilderException("No setter method found for property '" + propertyName
               + "' of type " + propertyType.getCanonicalName() + " in class " + javaClass.getCanonicalName());
      }
      if (setter.getReturnType() != Void.TYPE) {
         throw new ProtoSchemaBuilderException("No suitable setter method found for property '" + propertyName
               + "' of type " + propertyType.getCanonicalName() + " in class " + javaClass.getCanonicalName()
               + ". The candidate method does not have a suitable return type: " + setter);
      }
      return setter;
   }

   private static Class<?> determineElementType(Class<?> type, java.lang.reflect.Type genericType) {
      if (type.isArray()) {
         return type.getComponentType();
      }
      if (Collection.class.isAssignableFrom(type)) {
         return determineCollectionElementType(genericType);
      }
      return null;
   }

   private static Class<?> determineCollectionElementType(java.lang.reflect.Type genericType) {
      if (genericType instanceof ParameterizedType) {
         ParameterizedType type = (ParameterizedType) genericType;
         java.lang.reflect.Type fieldArgType = type.getActualTypeArguments()[0];
         if (fieldArgType instanceof Class) {
            return (Class) fieldArgType;
         }
         return (Class) ((ParameterizedType) fieldArgType).getRawType();
      } else if (genericType instanceof Class) {
         Class c = (Class) genericType;
         if (c.getGenericSuperclass() != null && Collection.class.isAssignableFrom(c.getSuperclass())) {
            Class x = determineCollectionElementType(c.getGenericSuperclass());
            if (x != null) {
               return x;
            }
         }
         for (java.lang.reflect.Type t : c.getGenericInterfaces()) {
            if (t instanceof Class && Map.class.isAssignableFrom((Class<?>) t)
                  || t instanceof ParameterizedType && Collection.class.isAssignableFrom((Class) ((ParameterizedType) t).getRawType())) {
               Class x = determineCollectionElementType(t);
               if (x != null) {
                  return x;
               }
            }
         }
      }
      return null;
   }
}
