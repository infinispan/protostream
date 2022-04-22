package org.infinispan.protostream.annotations.impl;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.infinispan.protostream.annotations.impl.types.XClass;
import org.infinispan.protostream.annotations.impl.types.XExecutable;
import org.infinispan.protostream.annotations.impl.types.XField;
import org.infinispan.protostream.annotations.impl.types.XMember;
import org.infinispan.protostream.annotations.impl.types.XMethod;
import org.infinispan.protostream.annotations.impl.types.XTypeFactory;
import org.infinispan.protostream.descriptors.JavaType;
import org.infinispan.protostream.descriptors.Type;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public final class ProtoFieldMetadata implements HasProtoSchema {

   private final int number;
   private final String name;
   private final XClass javaType;
   private final XClass collectionImplementation;
   private final Type protobufType;
   private final String documentation;
   private final ProtoTypeMetadata protoTypeMetadata; // todo [anistor] it's unclear what this type actually is...
   private final boolean isRequired;
   private final boolean isRepeated;
   private final boolean isArray;
   private final Object defaultValue;

   private final String propertyName;
   private final XMember declaringMember;
   private final XField field;    // field or getter required (exclusively)
   private final XMethod getter;
   private final XMethod setter;  // setter is optional

   ProtoFieldMetadata(int number, String name, XClass javaType,
                      XClass collectionImplementation, Type protobufType, ProtoTypeMetadata protoTypeMetadata,
                      boolean isRequired, boolean isRepeated, boolean isArray, Object defaultValue,
                      XField field) {
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
      this.documentation = field.getDocumentation();
      this.propertyName = field.getName();
   }

   ProtoFieldMetadata(int number, String name, XClass javaType,
                      XClass collectionImplementation, Type protobufType, ProtoTypeMetadata protoTypeMetadata,
                      boolean isRequired, boolean isRepeated, boolean isArray, Object defaultValue,
                      String propertyName, XMethod definingMethod, XMethod getter, XMethod setter) {
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
      this.propertyName = propertyName;
      this.declaringMember = definingMethod;
      this.getter = getter;
      this.setter = setter;
      this.documentation = definingMethod.getDocumentation();
   }

   public int getNumber() {
      return number;
   }

   public String getName() {
      return name;
   }

   public String getPropertyName() {
      return propertyName;
   }

   /**
    * The Java type. If this field is repeatable then the collection/array <em>element type</em> is returned here.
    */
   public XClass getJavaType() {
      return javaType;
   }

   public String getJavaTypeName() {
      String canonicalName = javaType.getCanonicalName();
      return canonicalName != null ? canonicalName : javaType.getName();
   }

   public XClass getCollectionImplementation() {
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

   public XField getField() {
      return field;
   }

   public XMethod getGetter() {
      return getter;
   }

   public XMethod getSetter() {
      return setter;
   }

   public String getLocation() {
      return declaringMember instanceof XExecutable ? ((XExecutable) declaringMember).toGenericString() : declaringMember.toString();
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
      if (protobufType.getJavaType() == JavaType.ENUM || protobufType.getJavaType() == JavaType.MESSAGE) {
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
         } else if (defaultValue instanceof byte[]) {
            v = "\"" + new String(ProtoMessageTypeMetadata.cescape((byte[]) defaultValue), StandardCharsets.ISO_8859_1) + "\"";
         } else if (defaultValue instanceof String) {
            v = "\"" + defaultValue + "\"";
         } else {
            v = defaultValue.toString();
         }
         iw.append(" [default = ").append(v).append(']');
      }

      if (BaseProtoSchemaGenerator.generateSchemaDebugComments) {
         iw.append(" /* ");
         if (field != null) {
            iw.append("field = ").append(field.getDeclaringClass().getCanonicalName()).append('.').append(field.getName());
         } else {
            iw.append("getter = ").append(getter.getDeclaringClass().getCanonicalName()).append('.').append(getter.getName());
            if (setter != null) {
               iw.append(", setter = ").append(setter.getDeclaringClass().getCanonicalName()).append('.').append(setter.getName());
            }
         }
         iw.append(" */");
      }

      iw.append(";\n");
   }

   public boolean isPrimitive() {
      XTypeFactory factory = javaType.getFactory();
      return javaType == factory.fromClass(float.class) || javaType == factory.fromClass(double.class) ||
            javaType == factory.fromClass(long.class) || javaType == factory.fromClass(int.class) ||
            javaType == factory.fromClass(short.class) || javaType == factory.fromClass(byte.class) ||
            javaType == factory.fromClass(boolean.class) || javaType == factory.fromClass(char.class);
   }

   public boolean isBoxedPrimitive() {
      XTypeFactory factory = javaType.getFactory();
      return javaType == factory.fromClass(Float.class) || javaType == factory.fromClass(Double.class) ||
            javaType == factory.fromClass(Long.class) || javaType == factory.fromClass(Integer.class) ||
            javaType == factory.fromClass(Short.class) || javaType == factory.fromClass(Byte.class) ||
            javaType == factory.fromClass(Boolean.class) || javaType == factory.fromClass(Character.class);
   }

   @Override
   public String toString() {
      return "ProtoFieldMetadata{" +
            "number=" + number +
            ", name='" + name + '\'' +
            ", protobufType=" + protobufType +
            ", javaType=" + javaType +
            ", collectionImplementation=" + collectionImplementation +
            ", documentation='" + documentation + '\'' +
            ", protoTypeMetadata=" + (protoTypeMetadata != null ? protoTypeMetadata.getName() : null) +
            ", isRequired=" + isRequired +
            ", isRepeated=" + isRepeated +
            ", isArray=" + isArray +
            ", defaultValue=" + defaultValue +
            ", propertyName='" + propertyName + '\'' +
            ", declaringMember=" + declaringMember +
            ", field=" + field +
            ", getter=" + getter +
            ", setter=" + setter +
            '}';
   }
}
