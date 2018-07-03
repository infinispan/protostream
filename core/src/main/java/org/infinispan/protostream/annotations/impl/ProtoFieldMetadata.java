package org.infinispan.protostream.annotations.impl;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Date;

import org.infinispan.protostream.annotations.ProtoSchemaBuilder;
import org.infinispan.protostream.descriptors.Type;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
final class ProtoFieldMetadata implements HasProtoSchema {

   private final int number;
   private final String name;
   private final Class<?> javaType;
   private final Class<?> collectionImplementation;
   private final Type protobufType;
   private final String documentation;
   private final ProtoTypeMetadata protoTypeMetadata;
   private final boolean isRequired;
   private final boolean isRepeated;
   private final boolean isArray;
   private final Object defaultValue;

   private final Member declaringMember;
   private final Field field;
   private final Method getter;
   private final Method setter;

   ProtoFieldMetadata(int number, String name, Class<?> javaType,
                      Class<?> collectionImplementation, Type protobufType, ProtoTypeMetadata protoTypeMetadata,
                      boolean isRequired, boolean isRepeated, boolean isArray, Object defaultValue,
                      Field field) {
      this.number = number;
      this.name = name;
      this.javaType = javaType;
      this.collectionImplementation = collectionImplementation;
      this.protoTypeMetadata = protoTypeMetadata;
      this.isRequired = isRequired;
      this.isRepeated = isRepeated;
      this.isArray = isArray;
      this.defaultValue = defaultValue;
      this.protobufType = protobufType;
      this.declaringMember = field;
      this.field = field;
      this.getter = null;
      this.setter = null;
      this.documentation = DocumentationExtractor.getDocumentation(field);
   }

   ProtoFieldMetadata(int number, String name, Class<?> javaType,
                      Class<?> collectionImplementation, Type protobufType, ProtoTypeMetadata protoTypeMetadata,
                      boolean isRequired, boolean isRepeated, boolean isArray, Object defaultValue,
                      Method definingMethod, Method getter, Method setter) {
      this.number = number;
      this.name = name;
      this.javaType = javaType;
      this.collectionImplementation = collectionImplementation;
      this.protoTypeMetadata = protoTypeMetadata;
      this.isRequired = isRequired;
      this.isRepeated = isRepeated;
      this.isArray = isArray;
      this.defaultValue = defaultValue;
      this.protobufType = protobufType;
      this.field = null;
      this.declaringMember = definingMethod;
      this.getter = getter;
      this.setter = setter;
      this.documentation = DocumentationExtractor.getDocumentation(definingMethod);
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

   public boolean isArray() {
      return isArray;
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
      return declaringMember instanceof Executable ? ((Executable) declaringMember).toGenericString() : declaringMember.toString();
   }

   @Override
   public void generateProto(IndentWriter iw) {
      iw.append('\n');
      ProtoTypeMetadata.appendDocumentation(iw, documentation);
      if (isRepeated) {
         iw.append("repeated ");
      } else {
         iw.append(isRequired ? "required " : "optional ");
      }
      String typeName;
      if (protobufType == Type.ENUM || protobufType == Type.MESSAGE || protobufType == Type.GROUP) {
         typeName = protoTypeMetadata.getFullName();
      } else {
         switch (protobufType) {
            case DOUBLE:
               typeName = "double";
               break;
            case FLOAT:
               typeName = "float";
               break;
            case INT32:
               typeName = "int32";
               break;
            case INT64:
               typeName = "int64";
               break;
            case FIXED32:
               typeName = "fixed32";
               break;
            case FIXED64:
               typeName = "fixed64";
               break;
            case BOOL:
               typeName = "bool";
               break;
            case STRING:
               typeName = "string";
               break;
            case BYTES:
               typeName = "bytes";
               break;
            case UINT32:
               typeName = "uint32";
               break;
            case UINT64:
               typeName = "uint64";
               break;
            case SFIXED32:
               typeName = "sfixed32";
               break;
            case SFIXED64:
               typeName = "sfixed64";
               break;
            case SINT32:
               typeName = "sint32";
               break;
            case SINT64:
               typeName = "sint64";
               break;
            default:
               throw new IllegalStateException("Unknown field type " + protobufType);
         }
      }
      iw.append(typeName);
      iw.append(' ').append(name).append(" = ").append(String.valueOf(number));
      Object defaultValue = getDefaultValue();
      if (defaultValue != null) {
         String v;
         if (defaultValue instanceof ProtoEnumValueMetadata) {
            v = ((ProtoEnumValueMetadata) defaultValue).getProtoName();
         } else if (defaultValue instanceof Date) {
            v = Long.toString(((Date) defaultValue).getTime());
         } else if (defaultValue instanceof Character) {
            v = Integer.toString(((Character) defaultValue));
         } else {
            v = defaultValue.toString();
         }
         iw.append(" [default = ").append(v).append(']');
      }

      if (ProtoSchemaBuilder.generateSchemaDebugComments) {
         iw.append(" /* ");
         if (field != null) {
            iw.append("field = ").append(field.getDeclaringClass().getCanonicalName()).append('.').append(field.getName());
         } else {
            iw.append("getter = ").append(getter.getName()).append(", setter = ").append(setter.getName());
         }
         iw.append(" */");
      }

      iw.append(";\n");
   }
}
