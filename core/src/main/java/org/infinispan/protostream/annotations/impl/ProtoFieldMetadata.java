package org.infinispan.protostream.annotations.impl;

import org.infinispan.protostream.descriptors.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
final class ProtoFieldMetadata {

   private final Class<?> declaringClass;
   private final int number;
   private final String name;
   private final Class<?> javaType;
   private final Class<?> collectionImplementation;
   private final Type protobufType;
   private final ProtoTypeMetadata protoTypeMetadata;
   private final boolean isRequired;
   private final boolean isRepeated;
   private final Object defaultValue;

   private final String propertyName;
   private final Field field;
   private final Method getter;
   private final Method setter;

   public ProtoFieldMetadata(Class<?> declaringClass, int number, String name, Class<?> javaType,
                             Class<?> collectionImplementation, Type protobufType, ProtoTypeMetadata protoTypeMetadata,
                             boolean isRequired, boolean isRepeated, Object defaultValue,
                             Field field) {
      this.declaringClass = declaringClass;
      this.number = number;
      this.name = name;
      this.javaType = javaType;
      this.collectionImplementation = collectionImplementation;
      this.protoTypeMetadata = protoTypeMetadata;
      this.isRequired = isRequired;
      this.isRepeated = isRepeated;
      this.defaultValue = defaultValue;
      this.protobufType = protobufType;
      this.propertyName = field.getName();
      this.field = field;
      this.getter = null;
      this.setter = null;
   }

   public ProtoFieldMetadata(Class<?> declaringClass, int number, String name, Class<?> javaType,
                             Class<?> collectionImplementation, Type protobufType, ProtoTypeMetadata protoTypeMetadata,
                             boolean isRequired, boolean isRepeated, Object defaultValue,
                             String propertyName, Method getter, Method setter) {
      this.declaringClass = declaringClass;
      this.number = number;
      this.name = name;
      this.javaType = javaType;
      this.collectionImplementation = collectionImplementation;
      this.protoTypeMetadata = protoTypeMetadata;
      this.isRequired = isRequired;
      this.isRepeated = isRepeated;
      this.defaultValue = defaultValue;
      this.protobufType = protobufType;
      this.field = null;
      this.propertyName = propertyName;
      this.getter = getter;
      this.setter = setter;
   }

   public Class<?> getDeclaringClass() {
      return declaringClass;
   }

   public int getNumber() {
      return number;
   }

   public String getName() {
      return name;
   }

   public Class<?> getJavaType() {
      return javaType;
   }

   public Class<?> getCollectionImplementation() {
      return collectionImplementation;
   }

   public Type getProtobufType() {
      return protobufType;
   }

   public ProtoTypeMetadata getProtoTypeMetadata() {
      return protoTypeMetadata;
   }

   public boolean isRequired() {
      return isRequired;
   }

   public boolean isRepeated() {
      return isRepeated;
   }

   public Object getDefaultValue() {
      return defaultValue;
   }

   public Field getField() {
      return field;
   }

   public Method getGetter() {
      return getter;
   }

   public Method getSetter() {
      return setter;
   }

   public String getLocation() {
      return String.format("%s on property '%s' with tag number %d and name '%s'", declaringClass, propertyName, number, name);
   }
}
