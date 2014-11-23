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
final class ProtoMessageTypeMetadata extends ProtoTypeMetadata {

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

   private final Map<Integer, ProtoFieldMetadata> fields;

   private final Map<Class<?>, ProtoTypeMetadata> innerTypes = new HashMap<Class<?>, ProtoTypeMetadata>();

   private final Field unknownFieldSetField;

   private final Method unknownFieldSetGetter;

   private final Method unknownFieldSetSetter;

   public ProtoMessageTypeMetadata(ProtoMessageTypeMetadata outerType, Class<?> messageClass, String name, Map<Integer, ProtoFieldMetadata> fields,
                                   Field unknownFieldSetField, Method unknownFieldSetGetter, Method unknownFieldSetSetter) {
      super(outerType, null, name, messageClass);
      this.fields = fields;
      this.unknownFieldSetField = unknownFieldSetField;
      this.unknownFieldSetGetter = unknownFieldSetGetter;
      this.unknownFieldSetSetter = unknownFieldSetSetter;
   }

   public Field getUnknownFieldSetField() {
      return unknownFieldSetField;
   }

   public Method getUnknownFieldSetGetter() {
      return unknownFieldSetGetter;
   }

   public Method getUnknownFieldSetSetter() {
      return unknownFieldSetSetter;
   }

   public Map<Integer, ProtoFieldMetadata> getFields() {
      return fields;
   }

   public void addInnerType(ProtoTypeMetadata typeMetadata) {
      innerTypes.put(typeMetadata.getJavaClass(), typeMetadata);
   }

   @Override
   public void generateProto(IndentWriter iw) {
      iw.append("\n// ").append(javaClass.getCanonicalName()).append("\n");
      iw.append("message ").append(name).append(" {\n");

      if (!innerTypes.isEmpty()) {
         iw.inc();
         for (ProtoTypeMetadata t : innerTypes.values()) {
            t.generateProto(iw);
         }
         iw.dec();
      }

      iw.inc();
      for (ProtoFieldMetadata f : fields.values()) {
         iw.append("// ");
         if (f.getField() != null) {
            iw.append("field = ").append(f.getField().getName());
         } else {
            iw.append("getter = ").append(f.getGetter().getName()).append(", setter = ").append(f.getSetter().getName());
         }
         iw.append("\n");
         if (f.isRepeated()) {
            iw.append("repeated ");
         } else {
            iw.append(f.isRequired() ? "required " : "optional ");
         }
         iw.append(getTypeName(f));
         iw.append(' ').append(f.getName()).append(" = ").append(String.valueOf(f.getNumber()));
         if (f.getDefaultValue() != null) {
            iw.append(" [default = ").append(f.getDefaultValue().toString()).append(']');
         }
         iw.append(";\n");
      }
      iw.dec();

      iw.append("}\n");
   }

   private String getTypeName(ProtoFieldMetadata fieldMetadata) {
      if (fieldMetadata.getProtobufType() == Type.ENUM
            || fieldMetadata.getProtobufType() == Type.MESSAGE
            || fieldMetadata.getProtobufType() == Type.GROUP) {
         return fieldMetadata.getProtoTypeMetadata().getFullName();
      }
      return typeNames.get(fieldMetadata.getProtobufType());
   }

   @Override
   public boolean isEnum() {
      return false;
   }
}
