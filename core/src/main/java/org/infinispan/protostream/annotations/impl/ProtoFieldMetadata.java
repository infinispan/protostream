package org.infinispan.protostream.annotations.impl;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.infinispan.protostream.annotations.ProtoSyntax;
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
public class ProtoFieldMetadata implements HasProtoSchema {

   private final int number;
   private final String name;
   private final String oneof;
   private final XClass javaType;
   private final XClass repeatedImplementation;
   private final Type protobufType;
   private final String documentation;
   private final ProtoTypeMetadata protoTypeMetadata; // todo [anistor] it's unclear what this type actually is...
   private final boolean isRequired;
   private final boolean isRepeated;
   private final boolean isArray;
   private final boolean isIterable;
   private final boolean isStream;
   private final Object defaultValue;
   private final String propertyName;
   private final XMember declaringMember;
   private final XField field;    // field or getter required (exclusively)
   private final XMethod getter;
   private final XMethod setter;  // setter is optional

   ProtoFieldMetadata(int number, String name, String oneof, XClass javaType,
                      XClass repeatedImplementation, Type protobufType, ProtoTypeMetadata protoTypeMetadata,
                      boolean isRequired, boolean isRepeated, boolean isArray, boolean isIterable, boolean isStream,
                      Object defaultValue, XField field) {
      this.number = number;
      this.name = name;
      this.oneof = oneof;
      this.javaType = javaType;
      this.repeatedImplementation = repeatedImplementation;
      this.protoTypeMetadata = protoTypeMetadata;
      this.isRequired = isRequired;
      this.isRepeated = isRepeated;
      this.isArray = isArray;
      this.isIterable = isIterable;
      this.isStream = isStream;
      this.defaultValue = defaultValue;
      this.protobufType = protobufType;
      this.declaringMember = field;
      this.field = field;
      this.getter = null;
      this.setter = null;
      this.documentation = field.getDocumentation();
      this.propertyName = field.getName();
   }

   ProtoFieldMetadata(int number, String name, String oneof, XClass javaType,
                      XClass repeatedImplementation, Type protobufType, ProtoTypeMetadata protoTypeMetadata,
                      boolean isRequired, boolean isRepeated, boolean isArray, boolean isIterable, boolean isStream,
                      Object defaultValue, String propertyName, XMethod definingMethod, XMethod getter, XMethod setter) {
      this.number = number;
      this.name = name;
      this.oneof = oneof;
      this.javaType = javaType;
      this.repeatedImplementation = repeatedImplementation;
      this.protoTypeMetadata = protoTypeMetadata;
      this.isRequired = isRequired;
      this.isRepeated = isRepeated;
      this.isArray = isArray;
      this.isIterable = isIterable;
      this.isStream = isStream;
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

   public String getOneof() {
      return oneof;
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

   public XClass getRepeatedImplementation() {
      return repeatedImplementation;
   }

   public Type getProtobufType() {
      return protobufType;
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

   public boolean isIterable() {
      return isIterable;
   }

   public boolean isStream() {
      return isStream;
   }

   public boolean isMap() {
      return false;
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
   public void generateProto(IndentWriter iw, ProtoSyntax syntax) {
      iw.append('\n');
      ProtoTypeMetadata.appendDocumentation(iw, documentation);
      if (oneof == null) {
         if (isRepeated) {
            iw.append("repeated ");
         } else {
            if (syntax == ProtoSyntax.PROTO2) {
               iw.append(isRequired ? "required " : "optional ");
            }
         }
      }
      String typeName = getTypeName();
      iw.append(typeName);
      iw.append(' ').append(name).append(" = ").append(String.valueOf(number));

      if (syntax == ProtoSyntax.PROTO2) {
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

   protected String getTypeName() {
      String typeName;
      if (protobufType.getJavaType() == JavaType.ENUM || protobufType.getJavaType() == JavaType.MESSAGE) {
         typeName = protoTypeMetadata.getFullName();
      } else {
         typeName = switch (protobufType) {
            case DOUBLE -> "double";
            case FLOAT -> "float";
            case INT32 -> "int32";
            case INT64 -> "int64";
            case FIXED32 -> "fixed32";
            case FIXED64 -> "fixed64";
            case BOOL -> "bool";
            case STRING -> "string";
            case BYTES -> "bytes";
            case UINT32 -> "uint32";
            case UINT64 -> "uint64";
            case SFIXED32 -> "sfixed32";
            case SFIXED64 -> "sfixed64";
            case SINT32 -> "sint32";
            case SINT64 -> "sint64";
            default -> throw new IllegalStateException("Unknown field type " + protobufType);
         };
      }
      return typeName;
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
            ", collectionImplementation=" + repeatedImplementation +
            ", oneof=" + oneof +
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
