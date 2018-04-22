package org.infinispan.protostream.annotations.impl;

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.lang.model.type.MirroredTypeException;

import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoMessage;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.annotations.ProtoSchemaBuilderException;
import org.infinispan.protostream.annotations.ProtoUnknownFieldSet;
import org.infinispan.protostream.annotations.impl.types.UnifiedTypeFactory;
import org.infinispan.protostream.annotations.impl.types.XClass;
import org.infinispan.protostream.annotations.impl.types.XConstructor;
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

   private Map<Integer, ProtoFieldMetadata> fields = null;

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
      appendDocumentation(iw, getDocumentation());
      iw.append("message ").append(name);
      if (BaseProtoSchemaGenerator.generateSchemaDebugComments) {
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

         Set<XClass> examinedClasses = new HashSet<>();
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
      if (javaClass.isLocal()) {
         throw new ProtoSchemaBuilderException("Local or anonymous classes are not allowed. The class " + getJavaClassName() + " must be instantiable using a non-private no-argument constructor.");
      }
      // ensure the class is not a non-static inner class
      if (javaClass.getEnclosingClass() != null && !Modifier.isStatic(javaClass.getModifiers())) {
         throw new ProtoSchemaBuilderException("Non-static inner classes are not allowed. The class " + getJavaClassName() + " must be instantiable using a non-private no-argument constructor.");
      }
      // ensure the class has a non-private no-argument constructor
      XConstructor ctor = javaClass.getDeclaredConstructor();  //todo [anistor] vs getConstructor()
      if (ctor == null || Modifier.isPrivate(ctor.getModifiers())) {
         throw new ProtoSchemaBuilderException("The class " + getJavaClassName() + " must must be instantiable using a non-private no-argument constructor.");
      }
   }

   private void discoverFields(XClass clazz, Set<XClass> examinedClasses, Map<Integer, ProtoFieldMetadata> fieldsByNumber, Map<String, ProtoFieldMetadata> fieldsByName) {
      if (!examinedClasses.add(clazz)) {
         // avoid re-examining classes due to multiple interface inheritance
         return;
      }

      if (clazz.getSuperclass() != null) {
         discoverFields(clazz.getSuperclass(), examinedClasses, fieldsByNumber, fieldsByName);
      }
      for (XClass i : clazz.getInterfaces()) {
         discoverFields(i, examinedClasses, fieldsByNumber, fieldsByName);
      }

      for (XField field : clazz.getDeclaredFields()) {
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
               if (!javaType.isArray() && !javaType.isPrimitive() && Modifier.isAbstract(javaType.getModifiers())) {
                  throw new ProtoSchemaBuilderException("The type " + javaType.getCanonicalName() + " of field '" + fieldName + "' of " + clazz.getCanonicalName() + " should not be abstract.");
               }

               Object defaultValue = getDefaultValue(clazz, fieldName, javaType, annotation.defaultValue());

               if (!isRequired && !isRepeated && javaType.isPrimitive() && defaultValue == null) {
                  throw new ProtoSchemaBuilderException("Primitive field '" + fieldName + "' of " + clazz.getCanonicalName() + " should be marked required or should have a default value.");
               }

               XClass collectionImplementation = getCollectionImplementation(clazz, field.getType(), getCollectionImplementationFromAnnotation(annotation), fieldName, isRepeated);

               protobufType = getProtobufType(javaType, protobufType);
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
               throw new ProtoSchemaBuilderException("The @ProtoUnknownFieldSet annotation should not be used multiple times in one class hierarchy : " + method);
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
               if (Modifier.isPrivate(method.getModifiers())) {
                  throw new ProtoSchemaBuilderException("Private methods cannot be @ProtoField annotated: " + method);
               }
               if (Modifier.isStatic(method.getModifiers())) {
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
               if (!javaType.isArray() && !javaType.isPrimitive() && Modifier.isAbstract(javaType.getModifiers())) {
                  throw new ProtoSchemaBuilderException("The type " + javaType.getCanonicalName() + " of field '" + fieldName + "' of " + clazz.getCanonicalName() + " should not be abstract.");
               }

               Object defaultValue = getDefaultValue(clazz, fieldName, javaType, annotation.defaultValue());

               if (!isRequired && !isRepeated && javaType.isPrimitive() && defaultValue == null) {
                  throw new ProtoSchemaBuilderException("Primitive field '" + fieldName + "' of " + clazz.getCanonicalName() + " should be marked required or should have a default value.");
               }

               XClass collectionImplementation = getCollectionImplementation(clazz, getter.getReturnType(), getCollectionImplementationFromAnnotation(annotation), fieldName, isRepeated);

               protobufType = getProtobufType(javaType, protobufType);
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

   private Object getDefaultValue(XClass clazz, String fieldName, XClass fieldType, String defaultValue) {
      if (defaultValue == null || defaultValue.isEmpty()) {
         return null;
      }
      if (fieldType == typeFactory.fromClass(String.class)) {
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
            return Integer.valueOf(defaultValue);
         }
         if (fieldType == typeFactory.fromClass(Long.class) || fieldType == typeFactory.fromClass(long.class)) {
            return Long.valueOf(defaultValue);
         }
         if (fieldType == typeFactory.fromClass(Short.class) || fieldType == typeFactory.fromClass(short.class)) {
            return Short.valueOf(defaultValue);
         }
         if (fieldType == typeFactory.fromClass(Double.class) || fieldType == typeFactory.fromClass(double.class)) {
            return Double.valueOf(defaultValue);
         }
         if (fieldType == typeFactory.fromClass(Float.class) || fieldType == typeFactory.fromClass(float.class)) {
            return Float.valueOf(defaultValue);
         }
         if (fieldType == typeFactory.fromClass(Byte.class) || fieldType == typeFactory.fromClass(byte.class)) {
            return Byte.valueOf(defaultValue);
         }
         if (fieldType.isAssignableTo(typeFactory.fromClass(Date.class))) {
            return Long.valueOf(defaultValue);
         }
         if (fieldType.isAssignableTo(typeFactory.fromClass(Instant.class))) {
            return Long.valueOf(defaultValue);
         }
      } catch (NumberFormatException e) {
         throw new ProtoSchemaBuilderException("Invalid default value for field '" + fieldName + "' of Java type " + fieldType.getCanonicalName() + " from class " + clazz.getCanonicalName() + ": " + defaultValue, e);
      }

      throw new ProtoSchemaBuilderException("No default value is allowed for field '" + fieldName + "' of Java type " + fieldType.getCanonicalName() + " from class " + clazz.getCanonicalName());
   }

   private XClass getCollectionImplementation(XClass clazz, XClass fieldType, XClass configuredCollection, String fieldName, boolean isRepeated) {
      XClass collectionImplementation;
      if (isRepeated && !fieldType.isArray()) {
         collectionImplementation = configuredCollection;
         if (collectionImplementation == typeFactory.fromClass(Collection.class)) {
            collectionImplementation = fieldType;
         }
         if (!collectionImplementation.isAssignableTo(typeFactory.fromClass(Collection.class))) {
            throw new ProtoSchemaBuilderException("The collection class of repeated field '" + fieldName + "' of " + clazz.getCanonicalName() + " must implement java.util.Collection.");
         }
         if (Modifier.isAbstract(collectionImplementation.getModifiers())) {
            throw new ProtoSchemaBuilderException("The collection class (" + collectionImplementation.getCanonicalName() + ") of repeated field '" + fieldName + "' of " + clazz.getCanonicalName() + " must not be abstract. Please specify an appropriate class in collectionImplementation member.");
         }
         XConstructor ctor = collectionImplementation.getDeclaredConstructor();   //todo [anistor] vs getConstructor()
         if (ctor == null) {
            throw new ProtoSchemaBuilderException("The collection class ('" + collectionImplementation.getCanonicalName() + "') of repeated field '"
                  + fieldName + "' of " + clazz.getCanonicalName() + " must have a public no-argument constructor.");
         }
         if (!collectionImplementation.isAssignableTo(fieldType)) {
            throw new ProtoSchemaBuilderException("The collection implementation class ('" + collectionImplementation.getCanonicalName() + "') of repeated field '"
                  + fieldName + "' of " + clazz.getCanonicalName() + " is not assignable to this field's type.");
         }
      } else {
         if (configuredCollection != typeFactory.fromClass(Collection.class)) {
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
