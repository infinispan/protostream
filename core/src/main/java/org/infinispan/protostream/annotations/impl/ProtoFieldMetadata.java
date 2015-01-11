package org.infinispan.protostream.annotations.impl;

import org.infinispan.protostream.descriptors.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
final class ProtoFieldMetadata {

   private static final Map<Type, String> typeNames = new HashMap<Type, String>();

   static {
      typeNames.put(Type.DOUBLE, "double");
      typeNames.put(Type.FLOAT, "float");
      typeNames.put(Type.INT32, "int32");
      typeNames.put(Type.INT64, "int64");
      typeNames.put(Type.FIXED32, "fixed32");
      typeNames.put(Type.FIXED64, "fixed64");
      typeNames.put(Type.BOOL, "bool");
      typeNames.put(Type.STRING, "string");
      typeNames.put(Type.BYTES, "bytes");
      typeNames.put(Type.UINT32, "uint32");
      typeNames.put(Type.UINT64, "uint64");
      typeNames.put(Type.SFIXED32, "sfixed32");
      typeNames.put(Type.SFIXED64, "sfixed64");
      typeNames.put(Type.SINT32, "sint32");
      typeNames.put(Type.SINT64, "sint64");
   }

   private final Class<?> declaringClass;
   private final int number;
   private final String name;
   private final Class<?> javaType;
   private final Class<?> collectionImplementation;
   private final Type protobufType;
   private final String documentation;
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
      this.documentation = DocumentationExtractor.getDocumentation(field);
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
      this.documentation = DocumentationExtractor.getDocumentation(getter, setter);
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

   public String getDocumentation() {
      return documentation;
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

   public void generateProto(IndentWriter iw) {
      if (documentation != null) {
         iw.append("/*\n");
         iw.append(documentation).append('\n');
         iw.append("*/\n");
      }
      if (isRepeated) {
         iw.append("repeated ");
      } else {
         iw.append(isRequired ? "required " : "optional ");
      }
      String typeName;
      if (protobufType == Type.ENUM || protobufType == Type.MESSAGE || protobufType == Type.GROUP) {
         typeName = protoTypeMetadata.getFullName();
      } else {
         typeName = typeNames.get(protobufType);
      }
      iw.append(typeName);
      iw.append(' ').append(name).append(" = ").append(String.valueOf(number));
      Object defaultValue = getDefaultValue();
      if (defaultValue != null) {
         String v = defaultValue instanceof ProtoEnumValueMetadata ?
               ((ProtoEnumValueMetadata) defaultValue).getProtoName() : defaultValue.toString();
         iw.append(" [default = ").append(v).append(']');
      }
      iw.append(" /* ");
      if (field != null) {
         iw.append("field = ").append(field.getName());
      } else {
         iw.append("getter = ").append(getter.getName()).append(", setter = ").append(setter.getName());
      }
      iw.append(" */;\n");
   }
}
