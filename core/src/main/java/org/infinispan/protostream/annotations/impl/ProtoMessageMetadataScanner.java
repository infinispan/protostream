package org.infinispan.protostream.annotations.impl;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.RawProtobufMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoMessage;
import org.infinispan.protostream.annotations.ProtoSchemaBuilderException;
import org.infinispan.protostream.annotations.ProtoUnknownFieldSet;
import org.infinispan.protostream.descriptors.Type;
import org.infinispan.protostream.impl.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
final class ProtoMessageMetadataScanner {

   private static final Log log = Log.LogFactory.getLog(ProtoMessageMetadataScanner.class);

   private final ProtoSchemaGenerator protoSchemaGenerator;

   private final SerializationContext serializationContext;

   private final Class<?> cls;

   private final ProtoMessageTypeMetadata protoMessageMetadata;

   private Field unknownFieldSetField;

   private Method unknownFieldSetGetterMethod;

   private Method unknownFieldSetSetterMethod;

   public ProtoMessageMetadataScanner(ProtoSchemaGenerator protoSchemaGenerator, SerializationContext serializationContext, Class<?> cls) {
      this.protoSchemaGenerator = protoSchemaGenerator;
      this.serializationContext = serializationContext;
      this.cls = cls;

      if (Modifier.isAbstract(cls.getModifiers())) {
         throw new ProtoSchemaBuilderException("Abstract classes are not allowed: " + cls);
      }

      try {
         cls.getDeclaredConstructor();
      } catch (NoSuchMethodException e) {
         throw new ProtoSchemaBuilderException("The class " + cls + " must have a public no-argument constructor.");
      }

      Map<Integer, ProtoFieldMetadata> fields = discoverFields(cls);
      if (fields.isEmpty()) {
         throw new ProtoSchemaBuilderException("Class " + cls.getCanonicalName() + " does not have any @ProtoField annotated fields.");
      }

      ProtoMessage annotation = cls.getAnnotation(ProtoMessage.class);
      String name = annotation == null || annotation.name().isEmpty() ? cls.getSimpleName() : annotation.name();

      protoMessageMetadata = new ProtoMessageTypeMetadata(null, cls, name, fields,
                                                          unknownFieldSetField, unknownFieldSetGetterMethod, unknownFieldSetSetterMethod);
   }

   private Map<Integer, ProtoFieldMetadata> discoverFields(Class<?> cls) {
      Map<Integer, ProtoFieldMetadata> fieldsByNumber = new TreeMap<Integer, ProtoFieldMetadata>();  // this ensures ascending order by field number
      Map<String, ProtoFieldMetadata> fieldByName = new HashMap<String, ProtoFieldMetadata>();
      Set<Class<?>> examinedClasses = new HashSet<Class<?>>();
      discoverFields(cls, examinedClasses, fieldsByNumber, fieldByName);
      return fieldsByNumber;
   }

   public ProtoMessageTypeMetadata getProtoMessageMetadata() {
      return protoMessageMetadata;
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
            if (unknownFieldSetField != null || unknownFieldSetGetterMethod != null || unknownFieldSetSetterMethod != null) {
               throw new ProtoSchemaBuilderException("The @ProtoUnknownFieldSet annotation should not be used multiple times : " + field);
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
               if (!Modifier.isPublic(field.getModifiers())) {
                  throw new ProtoSchemaBuilderException("Non-public fields cannot be @ProtoField annotated: " + field);
               }
               String fieldName = annotation.name();
               if (fieldName.isEmpty()) {
                  fieldName = field.getName();
               }

               boolean isRepeated = isRepeated(field.getType());
               boolean isRequired = annotation.required();
               if (isRepeated && isRequired) {
                  throw new ProtoSchemaBuilderException("Repeated field '" + fieldName + "' of " + clazz + " cannot be marked required.");
               }
               Class<?> javaType = annotation.javaType();
               if (javaType == ProtoField.UNSPECIFIED_TYPE.class) {
                  if (isRepeated) {
                     javaType = determineElementType(field.getType(), field.getGenericType());
                  } else {
                     javaType = field.getType();
                  }
               }
               if (!javaType.isArray() && !javaType.isPrimitive() && Modifier.isAbstract(javaType.getModifiers())) {
                  throw new ProtoSchemaBuilderException("The class " + javaType.getName() + " of repeated field '" + fieldName + "' of " + clazz + " should not be abstract.");
               }

               Object defaultValue = getDefaultValue(clazz, fieldName, javaType, annotation.defaultValue());

               if (!isRequired && javaType.isPrimitive() && defaultValue == null) {
                  throw new ProtoSchemaBuilderException("Primitive field '" + fieldName + "' of " + clazz + " should be marked required or should have a default value.");
               }

               Class<?> collectionImplementation = getCollectionImplementation(clazz, field.getType(), annotation.collectionImplementation(), fieldName, isRepeated);

               Type protobufType = getProtobufType(javaType, annotation.type());
               ProtoTypeMetadata protoTypeMetadata = null;
               if (protobufType == Type.ENUM || protobufType == Type.MESSAGE || protobufType == Type.GROUP) {
                  protoTypeMetadata = protoSchemaGenerator.scanAnnotations(javaType);
               }
               ProtoFieldMetadata fieldMetadata = new ProtoFieldMetadata(clazz, annotation.number(), fieldName, javaType, collectionImplementation,
                                                                         protobufType, protoTypeMetadata, isRequired, isRepeated, defaultValue, field);

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

               fieldsByNumber.put(fieldMetadata.getNumber(), fieldMetadata);
            }
         }
      }

      for (Method method : clazz.getDeclaredMethods()) {
         if (method.getAnnotation(ProtoUnknownFieldSet.class) != null) {
            if (unknownFieldSetField != null || unknownFieldSetGetterMethod != null || unknownFieldSetSetterMethod != null) {
               throw new ProtoSchemaBuilderException("The @ProtoUnknownFieldSet annotation should not be used multiple times : " + method);
            }
            String propertyName;
            if (method.getReturnType().equals(Void.TYPE)) {
               // this method is expected to be a setter
               if (method.getName().startsWith("set") && method.getName().length() >= 4) {
                  propertyName = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
               } else {
                  throw new ProtoSchemaBuilderException("Illegal setter method signature: " + method);
               }
               if (method.getParameterTypes().length != 1) {
                  throw new ProtoSchemaBuilderException("Illegal setter method signature: " + method);
               }
               unknownFieldSetSetterMethod = method;
               unknownFieldSetGetterMethod = findGetter(propertyName, method.getParameterTypes()[0]);
            } else {
               // this method is expected to be a getter
               if (method.getName().startsWith("get") && method.getName().length() >= 4) {
                  propertyName = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
               } else if (method.getName().startsWith("is") && method.getName().length() >= 3) {
                  propertyName = Character.toLowerCase(method.getName().charAt(2)) + method.getName().substring(3);
               } else {
                  throw new ProtoSchemaBuilderException("Illegal getter method signature: " + method);
               }
               unknownFieldSetGetterMethod = method;
               unknownFieldSetSetterMethod = findSetter(propertyName, unknownFieldSetGetterMethod.getReturnType());
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
               if (method.getReturnType().equals(Void.TYPE)) {
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
               String fieldName = annotation.name();
               if (fieldName.isEmpty()) {
                  fieldName = propertyName;
               }

               boolean isRepeated = isRepeated(getter.getReturnType());
               boolean isRequired = annotation.required();
               if (isRepeated && isRequired) {
                  throw new ProtoSchemaBuilderException("Repeated field '" + fieldName + "' of " + clazz + " cannot be marked required.");
               }
               Class<?> javaType = annotation.javaType();
               if (javaType == ProtoField.UNSPECIFIED_TYPE.class) {
                  if (isRepeated) {
                     javaType = determineElementType(getter.getReturnType(), getter.getGenericReturnType());
                  } else {
                     javaType = getter.getReturnType();
                  }
               }
               if (!javaType.isArray() && !javaType.isPrimitive() && Modifier.isAbstract(javaType.getModifiers())) {
                  throw new ProtoSchemaBuilderException("The class " + javaType.getName() + " of repeated field '" + fieldName + "' of " + clazz + " should not be abstract.");
               }

               Object defaultValue = getDefaultValue(clazz, fieldName, javaType, annotation.defaultValue());

               if (!isRequired && javaType.isPrimitive() && defaultValue == null) {
                  throw new ProtoSchemaBuilderException("Primitive field '" + fieldName + "' of " + clazz + " should be marked required or should have a default value.");
               }

               Class<?> collectionImplementation = getCollectionImplementation(clazz, getter.getReturnType(), annotation.collectionImplementation(), fieldName, isRepeated);

               Type protobufType = getProtobufType(javaType, annotation.type());
               ProtoTypeMetadata protoTypeMetadata = null;
               if (protobufType == Type.ENUM || protobufType == Type.MESSAGE || protobufType == Type.GROUP) {
                  protoTypeMetadata = protoSchemaGenerator.scanAnnotations(javaType);
               }

               ProtoFieldMetadata fieldMetadata = new ProtoFieldMetadata(clazz, annotation.number(), fieldName, javaType, collectionImplementation,
                                                                         protobufType, protoTypeMetadata, isRequired, isRepeated, defaultValue,
                                                                         propertyName, getter, setter);

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
            }
         }
      }
   }

   // todo [anistor] implement default value for enum fields
   private Object getDefaultValue(Class<?> clazz, String fieldName, Class<?> fieldType, String defaultValue) {
      if (defaultValue == null || defaultValue.isEmpty()) {
         return null;
      }
      if (fieldType == String.class) {
         return "\"" + defaultValue + "\"";
      }
      if (fieldType == Character.class || fieldType == Character.TYPE) {
         if (defaultValue.length() > 1) {
            throw new ProtoSchemaBuilderException("Invalid default value for field '" + fieldName + "' of " + clazz + ": " + defaultValue);
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
      } catch (NumberFormatException e) {
         throw new ProtoSchemaBuilderException("Invalid default value for field '" + fieldName + "' of " + clazz + ": " + defaultValue, e);
      }

      throw new ProtoSchemaBuilderException("No default value is allowed for field '" + fieldName + "' of " + clazz);
   }

   private Class<?> getCollectionImplementation(Class<?> clazz, Class<?> fieldType, Class<?> configuredCollection, String fieldName, boolean isRepeated) {
      Class<?> collectionImplementation;
      if (isRepeated) {
         collectionImplementation = configuredCollection;
         if (collectionImplementation == ProtoField.UNSPECIFIED_COLLECTION.class) {
            collectionImplementation = fieldType;
         }
         if (!Collection.class.isAssignableFrom(collectionImplementation)) {
            throw new ProtoSchemaBuilderException("The collection class of repeated field '" + fieldName + "' of " + clazz + " must implement java.util.Collection.");
         }
         if (Modifier.isAbstract(collectionImplementation.getModifiers())) {
            throw new ProtoSchemaBuilderException("The collection class (" + collectionImplementation.getName() + ") of repeated field '" + fieldName + "' of " + clazz + " must not be abstract.");
         }
         try {
            collectionImplementation.getDeclaredConstructor();
         } catch (NoSuchMethodException e) {
            throw new ProtoSchemaBuilderException("The collection class ('" + collectionImplementation.getName() + "') of repeated field '"
                                                        + fieldName + "' of " + clazz + " must have a public no-argument constructor.");
         }
         if (!fieldType.isAssignableFrom(collectionImplementation)) {
            throw new ProtoSchemaBuilderException("The collection implementation class ('" + collectionImplementation.getName() + "') of repeated field '"
                                                        + fieldName + "' of " + clazz + " is not assignable to this field's type.");
         }
      } else {
         if (configuredCollection != ProtoField.UNSPECIFIED_COLLECTION.class) {
            throw new ProtoSchemaBuilderException("Specifying the collection class is only allowed for repeated fields:  '" + fieldName + "' of " + clazz);
         }
         collectionImplementation = null;
      }
      return collectionImplementation;
   }

   private Type getProtobufType(Class<?> javaType, Type type) {
      switch (type) {
         case MESSAGE:
            if (javaType.isEnum()) {
               ProtoTypeMetadata m = protoSchemaGenerator.scanAnnotations(javaType);
               if (!m.isEnum()) {
                  throw new ProtoSchemaBuilderException(javaType + " is not a protobuf marshallable enum type");
               }
               return Type.ENUM;
            } else if (javaType == String.class || javaType == Character.class || javaType == Character.TYPE) {
               return Type.STRING;
            } else if (javaType == Double.class || javaType == Double.TYPE) {
               return Type.DOUBLE;
            } else if (javaType == Float.class || javaType == Float.TYPE) {
               return Type.FLOAT;
            } else if (javaType == Long.class || javaType == Long.TYPE) {
               return Type.INT64;
            } else if (javaType == Integer.class || javaType == Integer.TYPE || javaType == Short.class || javaType == Short.TYPE) {
               return Type.INT32;
            } else if (javaType == Byte.class || javaType == Byte.TYPE) {
               return Type.INT32;
            } else if (javaType == Boolean.class || javaType == Boolean.TYPE) {
               return Type.BOOL;
            } else {
               ProtoTypeMetadata m = protoSchemaGenerator.scanAnnotations(javaType);
               if (m.isEnum()) {
                  throw new ProtoSchemaBuilderException(javaType + " is not a protobuf marshallable message type");
               }
            }
            break;
         case ENUM:
            if (!javaType.isEnum()) {
               throw new ProtoSchemaBuilderException(javaType + " is not a protobuf marshallable enum type");
            }
            break;
         case GROUP:
            protoSchemaGenerator.scanAnnotations(javaType);

            BaseMarshaller<?> marshaller = serializationContext.getMarshaller(javaType);
            if (!(marshaller instanceof MessageMarshaller || marshaller instanceof RawProtobufMarshaller)) {
               throw new ProtoSchemaBuilderException(javaType + " is not a protobuf marshallable message type");
            }
            break;
         case STRING:
            if (javaType != String.class)
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getName() + " vs " + type);
            break;
         case BYTES:
            if (javaType != byte[].class)
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getName() + " vs " + type);
            break;
         case DOUBLE:
            if (javaType != Double.class && javaType != Double.TYPE)
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getName() + " vs " + type);
            break;
         case FLOAT:
            if (javaType != Float.class && javaType != Float.TYPE)
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getName() + " vs " + type);
            break;
         case BOOL:
            if (javaType != Boolean.class && javaType != Boolean.TYPE)
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getName() + " vs " + type);
            break;
         case INT32:
         case UINT32:
         case FIXED32:
         case SFIXED32:
         case SINT32:
            if (javaType != Integer.class && javaType != Integer.TYPE)
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getName() + " vs " + type);
            break;
         case INT64:
         case UINT64:
         case FIXED64:
         case SFIXED64:
         case SINT64:
            if (javaType != Long.class && javaType != Long.TYPE)
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getName() + " vs " + type);
            break;
      }
      return type;
   }

   private boolean isRepeated(Class<?> type) {
      return Collection.class.isAssignableFrom(type);
   }

   private Method findGetter(String propertyName, Class<?> propertyType) {
      String prefix = "get";
      if (propertyType.equals(Boolean.TYPE) || propertyType.equals(Boolean.class)) {
         prefix = "is";
      }
      String methodName = prefix + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
      Method getter;
      try {
         getter = cls.getMethod(methodName);
      } catch (NoSuchMethodException e) {
         throw new ProtoSchemaBuilderException("No getter method found for property '" + propertyName
                                                     + "' of type " + propertyType + " in class " + cls.getName());
      }
      if (!getter.getReturnType().equals(propertyType)) {
         throw new ProtoSchemaBuilderException("No suitable getter method found for property '" + propertyName
                                                     + "' of type " + propertyType + " in class " + cls.getName()
                                                     + ". The candidate method does not have a suitable return type: " + getter);
      }
      return getter;
   }

   private Method findSetter(String propertyName, Class<?> propertyType) {
      String methodName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
      Method setter;
      try {
         setter = cls.getMethod(methodName, propertyType);
      } catch (NoSuchMethodException e) {
         throw new ProtoSchemaBuilderException("No setter method found for property '" + propertyName
                                                     + "' of type " + propertyType + " in class " + cls.getName());
      }
      if (!setter.getReturnType().equals(Void.TYPE)) {
         throw new ProtoSchemaBuilderException("No suitable setter method found for property '" + propertyName
                                                     + "' of type " + propertyType + " in class " + cls.getName()
                                                     + ". The candidate method does not have a suitable return type: " + setter);
      }
      return setter;
   }

   private static Class determineElementType(Class<?> type, java.lang.reflect.Type genericType) {
      if (Collection.class.isAssignableFrom(type)) {
         return determineCollectionElementType(genericType);
      }
      return null;
   }

   private static Class determineCollectionElementType(java.lang.reflect.Type genericType) {
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
