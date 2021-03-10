package org.infinispan.protostream.annotations.impl;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.infinispan.protostream.Message;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.TagReader;
import org.infinispan.protostream.TagWriter;
import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.annotations.ProtoSchemaBuilderException;
import org.infinispan.protostream.annotations.impl.types.XClass;
import org.infinispan.protostream.annotations.impl.types.XConstructor;
import org.infinispan.protostream.annotations.impl.types.XExecutable;
import org.infinispan.protostream.annotations.impl.types.XTypeFactory;
import org.infinispan.protostream.descriptors.JavaType;
import org.infinispan.protostream.descriptors.Type;
import org.infinispan.protostream.descriptors.WireType;

/**
 * @author anistor@readhat.com
 * @since 4.3
 */
public abstract class AbstractMarshallerCodeGenerator {

   private static final String PROTOSTREAM_PACKAGE = SerializationContext.class.getPackage().getName();

   protected static final String ADAPTER_FIELD_NAME = "__a$";

   private final XTypeFactory typeFactory;

   /**
    * Do nullable fields that do not have a user defined default value get a default type specific value if missing
    * instead of just null? This is currently implemented just for arrays/collections. TODO Maybe numbers should also
    * receive a 0 default value and booleans a false value. But what about strings? Empty string does not sound like a
    * good fit. See the spec we do not fully implement here: https://developers.google.com/protocol-buffers/docs/proto#optional
    */
   private final boolean noDefaults = false;

   private final String protobufSchemaPackage;

   protected AbstractMarshallerCodeGenerator(XTypeFactory typeFactory, String protobufSchemaPackage) {
      this.typeFactory = typeFactory;
      this.protobufSchemaPackage = protobufSchemaPackage;
   }

   /**
    * Signature of generated method is:
    * <code>
    * public java.lang.Enum decode(int $1)
    * </code>
    */
   protected String generateEnumDecodeMethodBody(ProtoEnumTypeMetadata enumTypeMetadata) {
      IndentWriter iw = new IndentWriter();
      iw.append("{\n");
      iw.inc();
      iw.append("switch ($1) {\n");
      iw.inc();
      for (ProtoEnumValueMetadata value : enumTypeMetadata.getMembers().values()) {
         iw.append("case ").append(String.valueOf(value.getNumber())).append(": return ").append(value.getJavaEnumName()).append(";\n");
      }
      iw.append("default: return null;\n");
      iw.dec();
      iw.append("}\n");
      iw.dec();
      iw.append("}\n");
      return iw.toString();
   }

   /**
    * Signature of generated method is:
    * <code>
    * public int encode(java.lang.Enum $1)
    * </code>
    */
   protected String generateEnumEncodeMethodBody(ProtoEnumTypeMetadata enumTypeMetadata) {
      IndentWriter iw = new IndentWriter();
      iw.append("{\n");
      iw.inc();
      iw.append("switch ($1.ordinal()) {\n"); // use ordinal rather than enum constant because Javassist does not support enum syntax at all
      iw.inc();
      for (ProtoEnumValueMetadata value : enumTypeMetadata.getMembers().values()) {
         iw.append("case ").append(String.valueOf(value.getJavaEnumOrdinal())).append(": return ").append(String.valueOf(value.getNumber())).append(";\n");
      }
      iw.append("default: throw new IllegalArgumentException(\"Unexpected ").append(enumTypeMetadata.getJavaClassName()).append(" enum value : \" + $1.name());\n");
      iw.dec();
      iw.append("}\n");
      iw.dec();
      iw.append("}\n");
      return iw.toString();
   }

   /**
    * Returns the protobuf qualified type name, including the package name.
    */
   protected String makeQualifiedTypeName(String fullName) {
      if (protobufSchemaPackage != null) {
         return protobufSchemaPackage + '.' + fullName;
      }
      return fullName;
   }

   /**
    * Make boolean expression to test that at least one fields from a given collection of fields was not set.
    */
   private String makeTestFieldWasNotSet(Collection<ProtoFieldMetadata> fields, Map<String, Integer> trackedFields) {
      Map<Integer, Long> masks = new HashMap<>();
      for (ProtoFieldMetadata field : fields) {
         int fieldBitIndex = trackedFields.get(field.getName());
         masks.merge(fieldBitIndex >> 6, 1L << fieldBitIndex, (a, b) -> a | b);
      }
      StringBuilder sb = new StringBuilder();
      boolean first = true;
      for (int set : masks.keySet()) {
         if (first) {
            first = false;
         } else {
            sb.append(" || ");
         }
         long mask = masks.get(set);
         sb.append("(__bits$").append(set).append(" & ").append(mask).append("L) != ").append(mask).append('L');
      }
      return sb.toString();
   }

   /**
    * Make boolean expression to test that a field was not set.
    */
   private String makeTestFieldWasNotSet(ProtoFieldMetadata field, Map<String, Integer> trackedFields) {
      int fieldBitIndex = trackedFields.get(field.getName());
      return "((__bits$" + (fieldBitIndex >> 6) + " & " + (1L << fieldBitIndex) + "L) == 0)";
   }

   /**
    * Make statement to mark a field as set.
    */
   private String makeFieldWasSet(ProtoFieldMetadata field, Map<String, Integer> trackedFields) {
      int fieldBitIndex = trackedFields.get(field.getName());
      return "__bits$" + (fieldBitIndex >> 6) + " |= " + (1L << fieldBitIndex) + 'L';
   }

   /**
    * Make local variable name for a field.
    */
   private String makeFieldLocalVar(ProtoFieldMetadata field) {
      return "__v$" + field.getNumber();
   }

   /**
    * Make a collection local variable name for a repeatable/array field.
    */
   private String makeCollectionLocalVar(ProtoFieldMetadata field) {
      return "__c$" + field.getNumber();
   }

   private String makeArrayLocalVar(ProtoFieldMetadata field) {
      return "__a$" + field.getNumber();
   }

   /**
    * Make field name for caching a marshaller delegate for a related message.
    */
   protected String makeMarshallerDelegateFieldName(ProtoFieldMetadata field) {
      return "__md$" + field.getNumber();
   }

   /**
    * Signature of generated method is:
    * <code>
    * public java.lang.Object read(org.infinispan.protostream.ProtoStreamMarshaller.ReadContext $1,
    * java.lang.Object $2) throws java.io.IOException
    * </code>
    */
   protected String generateReadMethodBody(ProtoMessageTypeMetadata messageTypeMetadata) {
      //todo [anistor] handle unknown fields for adapters also
      String getUnknownFieldSetFieldStatement = null;
      String setUnknownFieldSetFieldStatement = null;
      if (messageTypeMetadata.getUnknownFieldSetField() != null) {
         getUnknownFieldSetFieldStatement = "o." + messageTypeMetadata.getUnknownFieldSetField().getName();
         setUnknownFieldSetFieldStatement = "o." + messageTypeMetadata.getUnknownFieldSetField().getName() + " = u";
      } else if (messageTypeMetadata.getUnknownFieldSetGetter() != null) {
         getUnknownFieldSetFieldStatement = "o." + messageTypeMetadata.getUnknownFieldSetGetter().getName() + "()";
         setUnknownFieldSetFieldStatement = "o." + messageTypeMetadata.getUnknownFieldSetSetter().getName() + "(u)";
      } else if (messageTypeMetadata.getJavaClass().isAssignableTo(Message.class)) {
         getUnknownFieldSetFieldStatement = "o.getUnknownFieldSet()";
         setUnknownFieldSetFieldStatement = "o.setUnknownFieldSet(u)";
      }

      IndentWriter iw = new IndentWriter();
      iw.append("{\n");
      iw.inc();
      iw.append("final ").append(TagReader.class.getName()).append(" $in = $1.getIn();\n");

      if (messageTypeMetadata.isContainer()) {
         iw.append("int __v$size = ((java.lang.Integer) $1.getParamValue(\"" + WrappedMessage.CONTAINER_SIZE_CONTEXT_PARAM + "\")).intValue();");
      }

      // if there is no factory then the class must have setters or the fields should be directly accessible and not be final
      final boolean noFactory = messageTypeMetadata.getFactory() == null;
      if (noFactory) {
         iw.append("final ").append(messageTypeMetadata.getJavaClassName())
               .append(" o = new ").append(messageTypeMetadata.getJavaClassName()).append("();\n");
      }

      // number of fields that are required and do not have a default value
      int mandatoryFields = 0;

      // fields that should be tracked for presence and be either initialized with defaults if missing at the end
      // or an exception thrown if no default exists
      Map<String, Integer> trackedFields = new LinkedHashMap<>();

      // First pass over fields. Count how many are mandatory and how many need to be tracked for presence.
      for (ProtoFieldMetadata fieldMetadata : messageTypeMetadata.getFields().values()) {
         if (fieldMetadata.isRequired() && fieldMetadata.getDefaultValue() == null) {
            mandatoryFields++;
         }

         if (fieldMetadata.isRequired() || fieldMetadata.getDefaultValue() != null && (noFactory || fieldMetadata.isRepeated() || fieldMetadata.getProtobufType() == Type.BYTES)) {
            int trackedFieldsSize = trackedFields.size();
            if (trackedFieldsSize % 64 == 0) {
               // declare a long variable to emulate a bitset in a long
               iw.append("long __bits$").append(String.valueOf(trackedFieldsSize >> 6)).append(" = 0;\n");
            }
            trackedFields.put(fieldMetadata.getName(), trackedFieldsSize);
         }
      }

      for (ProtoFieldMetadata fieldMetadata : messageTypeMetadata.getFields().values()) {
         if (fieldMetadata.isRepeated()) {
            // a collection local variable
            iw.append(fieldMetadata.getCollectionImplementation().getCanonicalName()).append(' ').append(makeCollectionLocalVar(fieldMetadata)).append(" = ");
            if (noDefaults || fieldMetadata.isArray()) {
               iw.append("null");
            } else {
               iw.append("new ").append(fieldMetadata.getCollectionImplementation().getCanonicalName()).append("()");
            }
            iw.append(";\n");
            if (!noFactory && fieldMetadata.isArray()) {
               // an array local variable
               iw.append(fieldMetadata.getJavaTypeName()).append("[] ").append(makeArrayLocalVar(fieldMetadata)).append(" = ");
               if (noDefaults) {
                  iw.append("null");
               } else {
                  iw.append("new ").append(fieldMetadata.getJavaTypeName()).append("[0]");
               }
               iw.append(";\n");
            }
         } else if (!noFactory) {
            // immutable messages need a per-field local variable initialized to default value if any
            iw.append(fieldMetadata.getJavaTypeName()).append(' ').append(makeFieldLocalVar(fieldMetadata));
            Object defaultValue = fieldMetadata.getDefaultValue();
            if (defaultValue != null && fieldMetadata.getProtobufType() != Type.BYTES) {
               // fields of type bytes get assigned default values only at the end to avoid a possibly useless byte[] allocation
               String val = toJavaLiteral(defaultValue, fieldMetadata.getJavaType());
               iw.append(" = ").append(box(val, fieldMetadata.getJavaType()));
            } else {
               if (fieldMetadata.isBoxedPrimitive()
                     || fieldMetadata.getProtobufType() == Type.BYTES
                     || fieldMetadata.getProtobufType().getJavaType() == JavaType.STRING
                     || fieldMetadata.getProtobufType().getJavaType() == JavaType.BYTE_STRING
                     || fieldMetadata.getProtobufType().getJavaType() == JavaType.ENUM
                     || fieldMetadata.getProtobufType().getJavaType() == JavaType.MESSAGE) {
                  iw.append(" = null");
               } else if (fieldMetadata.isPrimitive()) {
                  if (fieldMetadata.getProtobufType() == Type.BOOL) {
                     iw.append(" = false");
                  } else {
                     iw.append(" = 0");
                  }
               }
            }
            iw.append(";\n");
         }
      }

      iw.append("boolean done = false;\n");
      iw.append("while (!done) {\n");
      iw.inc();
      iw.append("final int tag = $in.readTag();\n");
      iw.append("switch (tag) {\n");
      iw.inc();
      iw.append("case 0: {\n");
      iw.inc();
      iw.append("done = true;\nbreak;\n");
      iw.dec();
      iw.append("}\n");
      for (ProtoFieldMetadata fieldMetadata : messageTypeMetadata.getFields().values()) {
         final String v = makeFieldLocalVar(fieldMetadata);
         iw.append("case ").append(makeFieldTag(fieldMetadata.getNumber(), fieldMetadata.getProtobufType().getWireType())).append(": {\n");
         iw.inc();
         if (BaseProtoSchemaGenerator.generateMarshallerDebugComments) {
            iw.append("// type = ").append(fieldMetadata.getProtobufType().toString()).append(", name = ").append(fieldMetadata.getName()).append('\n');
         }
         switch (fieldMetadata.getProtobufType()) {
            case DOUBLE:
            case FLOAT:
            case INT64:
            case UINT64:
            case INT32:
            case FIXED64:
            case FIXED32:
            case BOOL:
            case STRING:
            case BYTES:
            case UINT32:
            case SFIXED32:
            case SFIXED64:
            case SINT32:
            case SINT64: {
               if (noFactory || fieldMetadata.isRepeated()) {
                  iw.append(fieldMetadata.getJavaTypeName()).append(' ');
               }
               iw.append(v).append(" = ").append(box(convert("$in." + makeStreamIOMethodName(fieldMetadata, false) + "()", fieldMetadata), fieldMetadata.getJavaType())).append(";\n");
               genSetField(iw, fieldMetadata, trackedFields, messageTypeMetadata);
               break;
            }
            case GROUP: {
               String mdField = initMarshallerDelegateField(iw, fieldMetadata);
               if (noFactory || fieldMetadata.isRepeated()) {
                  iw.append(fieldMetadata.getJavaTypeName()).append(' ');
               }
               iw.append(v).append(" = (").append(fieldMetadata.getJavaTypeName()).append(") readMessage(").append(mdField).append(", $1);\n");
               iw.append("$in.checkLastTagWas(").append(makeFieldTag(fieldMetadata.getNumber(), WireType.END_GROUP)).append(");\n");
               genSetField(iw, fieldMetadata, trackedFields, messageTypeMetadata);
               break;
            }
            case MESSAGE: {
               String mdField = initMarshallerDelegateField(iw, fieldMetadata);
               iw.append("int length = $in.readRawVarint32();\n");
               iw.append("int oldLimit = $in.pushLimit(length);\n");
               if (noFactory || fieldMetadata.isRepeated()) {
                  iw.append(fieldMetadata.getJavaTypeName()).append(' ');
               }
               iw.append(v).append(" = (").append(fieldMetadata.getJavaTypeName()).append(") readMessage(").append(mdField).append(", $1);\n");
               iw.append("$in.checkLastTagWas(0);\n");
               iw.append("$in.popLimit(oldLimit);\n");
               genSetField(iw, fieldMetadata, trackedFields, messageTypeMetadata);
               break;
            }
            case ENUM: {
               String mdField = initMarshallerDelegateField(iw, fieldMetadata);
               iw.append("int enumVal = $in.readEnum();\n");
               if (noFactory || fieldMetadata.isRepeated()) {
                  iw.append(fieldMetadata.getJavaTypeName()).append(' ');
               }
               iw.append(v).append(" = (").append(fieldMetadata.getJavaTypeName()).append(") ").append(mdField).append(".getMarshaller().decode(enumVal);\n");
               iw.append("if (").append(v).append(" == null) {\n");
               if (getUnknownFieldSetFieldStatement != null) {
                  iw.inc();
                  iw.append(PROTOSTREAM_PACKAGE).append(".UnknownFieldSet u = ").append(getUnknownFieldSetFieldStatement).append(";\n");
                  iw.append("if (u == null) { u = new ").append(PROTOSTREAM_PACKAGE).append(".impl.UnknownFieldSetImpl(); ").append(setUnknownFieldSetFieldStatement).append("; }\n");
                  iw.append("u.putVarintField(").append(String.valueOf(fieldMetadata.getNumber())).append(", enumVal);\n");
                  iw.dec();
               }
               iw.append("} else {\n").inc();
               genSetField(iw, fieldMetadata, trackedFields, messageTypeMetadata);
               iw.dec().append("}\n");
               break;
            }
            default:
               throw new IllegalStateException("Unknown field type : " + fieldMetadata.getProtobufType());
         }
         iw.append("break;\n");
         iw.dec();
         iw.append("}\n");
      }
      iw.append("default: {\n");
      iw.inc();
      if (getUnknownFieldSetFieldStatement != null) {
         iw.append(PROTOSTREAM_PACKAGE).append(".UnknownFieldSet u = ").append(getUnknownFieldSetFieldStatement).append(";\n");
         iw.append("if (u == null) u = new ").append(PROTOSTREAM_PACKAGE).append(".impl.UnknownFieldSetImpl();\n");
         iw.append("if (!u.readSingleField(tag, $in)) done = true;\n");
         iw.append("if (!u.isEmpty()) ").append(setUnknownFieldSetFieldStatement).append(";\n");
      } else {
         iw.append("if (!$in.skipField(tag)) done = true;\n");
      }
      iw.dec().append("}\n");
      iw.dec().append("}\n");
      iw.dec().append("}\n");

      // assign defaults to missing fields
      if (BaseProtoSchemaGenerator.generateMarshallerDebugComments) {
         iw.append("\n// default values\n\n");
      }
      for (ProtoFieldMetadata fieldMetadata : messageTypeMetadata.getFields().values()) {
         Object defaultValue = fieldMetadata.getDefaultValue();
         if (defaultValue != null && (noFactory || fieldMetadata.isRepeated() || fieldMetadata.getProtobufType() == Type.BYTES)) {
            iw.append("if ").append(makeTestFieldWasNotSet(fieldMetadata, trackedFields)).append(" {\n");
            iw.inc();
            String val = toJavaLiteral(defaultValue, fieldMetadata.getJavaType());
            if (fieldMetadata.isRepeated()) {
               String c = makeCollectionLocalVar(fieldMetadata);
               if (noDefaults || fieldMetadata.isArray()) {
                  iw.append("if (").append(c).append(" == null) ").append(c).append(" = new ").append(fieldMetadata.getCollectionImplementation().getCanonicalName()).append("();\n");
               }
               iw.append(c).append(".add(").append(box(val, typeFactory.fromClass(defaultValue.getClass()))).append(");\n");
            } else {
               if (noFactory) {
                  iw.append(createSetPropExpr(messageTypeMetadata, fieldMetadata, "o", box(val, fieldMetadata.getJavaType()))).append(";\n");
               } else {
                  iw.append(makeFieldLocalVar(fieldMetadata)).append(" = ").append(box(val, fieldMetadata.getJavaType())).append(";\n");
               }
            }
            iw.dec();
            iw.append("}\n");
         }
      }

      for (ProtoFieldMetadata fieldMetadata : messageTypeMetadata.getFields().values()) {
         if (fieldMetadata.isRepeated()) {
            String c = makeCollectionLocalVar(fieldMetadata);
            if (fieldMetadata.isArray()) {
               if (fieldMetadata.getDefaultValue() == null) {
                  iw.append("if (").append(c).append(" != null) ");
               }
               iw.append("{\n").inc();
               String a = makeArrayLocalVar(fieldMetadata);
               if (fieldMetadata.getJavaType().isPrimitive()) {
                  if (noFactory) {
                     iw.append(fieldMetadata.getJavaTypeName()).append("[] ");
                  }
                  iw.append(a).append(" = new ").append(fieldMetadata.getJavaTypeName()).append("[").append(c).append(".size()];\n");
                  XClass boxedType = box(fieldMetadata.getJavaType());
                  iw.append("int _j = 0;\nfor (java.util.Iterator _it = ").append(c).append(".iterator(); _it.hasNext();) ").append(a).append("[_j++] = ").append(unbox("((" + boxedType.getName() + ") _it.next())", boxedType)).append(";\n");
                  c = a;
               } else {
                  c = "(" + fieldMetadata.getJavaTypeName() + "[])" + c + ".toArray(new " + fieldMetadata.getJavaTypeName() + "[0])";
               }
            }
            if (noFactory) {
               iw.append(createSetPropExpr(messageTypeMetadata, fieldMetadata, "o", c)).append(";\n");
            } else if (fieldMetadata.isArray() && !fieldMetadata.getJavaType().isPrimitive()) {
               iw.append(makeArrayLocalVar(fieldMetadata)).append(" = ").append(c).append(";\n");
            }
            if (fieldMetadata.isArray()) {
               iw.dec().append('}');
               if (!noDefaults && fieldMetadata.getDefaultValue() == null) {
                  c = "new " + fieldMetadata.getJavaTypeName() + "[0]";
                  iw.append(" else {\n").inc();
                  if (noFactory) {
                     iw.append(createSetPropExpr(messageTypeMetadata, fieldMetadata, "o", c)).append(";\n");
                  } else {
                     iw.append(makeArrayLocalVar(fieldMetadata)).append(" = ").append(c).append(";\n");
                  }
                  iw.dec().append("}\n");
               }
            }
            iw.append('\n');
         }
      }

      // complain about missing required fields
      if (mandatoryFields > 0) {
         List<ProtoFieldMetadata> mandatory = messageTypeMetadata.getFields().values()
               .stream()
               .filter(f -> f.isRequired() && f.getDefaultValue() == null)
               .collect(Collectors.toList());

         iw.append("if (").append(makeTestFieldWasNotSet(mandatory, trackedFields)).append(") {\n");
         iw.inc();
         iw.append("final StringBuilder missing = new StringBuilder();\n");
         boolean first = true;
         for (ProtoFieldMetadata fieldMetadata : messageTypeMetadata.getFields().values()) {
            if (fieldMetadata.isRequired()) {
               iw.append("if ").append(makeTestFieldWasNotSet(fieldMetadata, trackedFields)).append(" {\n");
               iw.inc();
               if (first) {
                  first = false;
               } else {
                  iw.append("if (missing.length() > 0) missing.append(\", \");\n");
               }
               iw.append("missing.append(\"").append(fieldMetadata.getName()).append("\");\n");
               iw.dec();
               iw.append("}\n");
            }
         }
         iw.append("throw new java.io.IOException(\"Required field(s) missing from input stream : \" + missing);\n");
         iw.dec();
         iw.append("}\n");
      }

      if (noFactory) {
         // return the instance
         iw.append("return o;\n");
      } else {
         // create and return the instance
         iw.append("return ");
         XExecutable factory = messageTypeMetadata.getFactory();
         if (factory instanceof XConstructor) {
            iw.append("new ").append(messageTypeMetadata.getJavaClassName());
         } else {
            if (factory.isStatic()) {
               iw.append(messageTypeMetadata.getAnnotatedClassName()).append('.').append(factory.getName());
            } else {
               iw.append(ADAPTER_FIELD_NAME).append('.').append(factory.getName());
            }
         }
         iw.append('(');
         boolean first = true;
         for (String paramName : factory.getParameterNames()) {
            if (first) {
               first = false;
               if (messageTypeMetadata.isContainer()) {
                  iw.append("__v$size");
                  continue;
               }
            } else {
               iw.append(", ");
            }
            boolean found = false;
            for (ProtoFieldMetadata fieldMetadata : messageTypeMetadata.getFields().values()) {
               if (fieldMetadata.getPropertyName().equals(paramName)) {
                  String var = fieldMetadata.isRepeated() ?
                        (fieldMetadata.isArray() ?
                              makeArrayLocalVar(fieldMetadata) : makeCollectionLocalVar(fieldMetadata))
                        : makeFieldLocalVar(fieldMetadata);
                  iw.append(var);
                  found = true;
                  break;
               }
            }
            if (!found) {
               throw new ProtoSchemaBuilderException("Parameter '" + paramName + "' of factory " + factory + " does not map to any Protobuf field");
            }
         }
         iw.append(");\n");
      }

      iw.dec().append("}\n");
      return iw.toString();
   }

   private static String makeFieldTag(int fieldNumber, WireType wireType) {
      return "(" + fieldNumber + " << "
            + PROTOSTREAM_PACKAGE + ".descriptors.WireType.TAG_TYPE_NUM_BITS | "
            + PROTOSTREAM_PACKAGE + ".descriptors.WireType.WIRETYPE_" + wireType.name() + ")";
   }

   /**
    * Converts a given {@code value} instance to a Java language literal of type {@code javaType}. The input value is
    * expected to conform to the given type (no checks are performed).
    */
   private String toJavaLiteral(Object value, XClass javaType) {
      String v;
      if (javaType.isAssignableTo(Date.class)) {
         v = value + "L";
      } else if (javaType.isAssignableTo(Instant.class)) {
         v = value + "L";
      } else if (value instanceof ProtoEnumValueMetadata) {
         v = ((ProtoEnumValueMetadata) value).getJavaEnumName();
      } else if (value instanceof Long) {
         v = value + "L";
      } else if (value instanceof Double) {
         v = value + "D";
      } else if (value instanceof Float) {
         v = value + "F";
      } else if (value instanceof Character) {
         v = "'" + value + "'";
      } else if (value instanceof String) {
         v = "\"" + value + "\"";
      } else if (value instanceof Short) {
         v = "(short) " + value;
      } else if (value instanceof Byte) {
         v = "(byte) " + value;
      } else if (value instanceof byte[]) {
         StringBuilder sb = new StringBuilder();
         sb.append("new byte[] {");
         byte[] bytes = (byte[]) value;
         for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
               sb.append(", ");
            }
            sb.append(bytes[i]);
         }
         sb.append('}');
         v = sb.toString();
      } else {
         v = value.toString();
      }
      return v;
   }

   private void genSetField(IndentWriter iw, ProtoFieldMetadata fieldMetadata, Map<String, Integer> trackedFields, ProtoMessageTypeMetadata messageTypeMetadata) {
      final String v = makeFieldLocalVar(fieldMetadata);
      if (fieldMetadata.isRepeated()) {
         String c = makeCollectionLocalVar(fieldMetadata);
         if (noDefaults || fieldMetadata.isArray()) {
            iw.append("if (").append(c).append(" == null) ").append(c).append(" = new ").append(fieldMetadata.getCollectionImplementation().getCanonicalName()).append("();\n");
         }
         iw.append(c).append(".add(").append(box(v, box(fieldMetadata.getJavaType()))).append(");\n");
      } else {
         if (messageTypeMetadata.getFactory() == null) {
            iw.append(createSetPropExpr(messageTypeMetadata, fieldMetadata, "o", v)).append(";\n");
         }
      }
      if (trackedFields.containsKey(fieldMetadata.getName())) {
         iw.append(makeFieldWasSet(fieldMetadata, trackedFields)).append(";\n");
      }
   }

   /**
    * Signature of generated method is:
    * <code>
    * public void write(org.infinispan.protostream.ProtoStreamMarshaller.WriteContext $1,
    * java.lang.Object $2) throws java.io.IOException
    * </code>
    */
   protected String generateWriteMethodBody(ProtoMessageTypeMetadata messageTypeMetadata) {
      //todo [anistor] handle unknown fields for adapters also
      String getUnknownFieldSetFieldStatement = null;
      if (messageTypeMetadata.getUnknownFieldSetField() != null) {
         getUnknownFieldSetFieldStatement = "o." + messageTypeMetadata.getUnknownFieldSetField().getName();
      } else if (messageTypeMetadata.getUnknownFieldSetGetter() != null) {
         getUnknownFieldSetFieldStatement = "o." + messageTypeMetadata.getUnknownFieldSetGetter().getName() + "()";
      } else if (messageTypeMetadata.getJavaClass().isAssignableTo(Message.class)) {
         getUnknownFieldSetFieldStatement = "o.getUnknownFieldSet()";
      }

      IndentWriter iw = new IndentWriter();
      iw.append("{\n");
      iw.inc();
      iw.append("final ").append(TagWriter.class.getName()).append(" $out = $1.getOut();\n");
      iw.append("final ").append(messageTypeMetadata.getJavaClassName()).append(" o = (").append(messageTypeMetadata.getJavaClassName()).append(") $2;\n");
      for (ProtoFieldMetadata fieldMetadata : messageTypeMetadata.getFields().values()) {
         iw.append("{\n");
         iw.inc();
         if (BaseProtoSchemaGenerator.generateMarshallerDebugComments) {
            iw.append("// type = ").append(fieldMetadata.getProtobufType().toString()).append(", name = ").append(fieldMetadata.getName()).append('\n');
         }
         final String v = makeFieldLocalVar(fieldMetadata);
         final String f = fieldMetadata.isRepeated() ? (fieldMetadata.isArray() ? makeArrayLocalVar(fieldMetadata) : makeCollectionLocalVar(fieldMetadata)) : v;
         iw.append("final ");
         if (fieldMetadata.isRepeated()) {
            if (fieldMetadata.isArray()) {
               iw.append(fieldMetadata.getJavaTypeName()).append("[]");
            } else {
               iw.append("java.util.Collection");
            }
         } else {
            iw.append(fieldMetadata.getJavaTypeName());
         }
         iw.append(' ').append(f).append(" = ").append(createGetPropExpr(messageTypeMetadata, fieldMetadata, "o")).append(";\n");
         if (fieldMetadata.isRequired()) {
            boolean couldBeNull = fieldMetadata.isRepeated()
                  || fieldMetadata.isBoxedPrimitive()
                  || fieldMetadata.getProtobufType() == Type.BYTES
                  || fieldMetadata.getProtobufType().getJavaType() == JavaType.STRING
                  || fieldMetadata.getProtobufType().getJavaType() == JavaType.BYTE_STRING
                  || fieldMetadata.getProtobufType().getJavaType() == JavaType.ENUM
                  || fieldMetadata.getProtobufType().getJavaType() == JavaType.MESSAGE;
            if (couldBeNull) {
               iw.append("if (").append(f).append(" == null) throw new IllegalStateException(\"Required field must not be null : ").append(fieldMetadata.getName()).append("\");\n");
            }
         } else {
            if (!fieldMetadata.getJavaType().isPrimitive() || fieldMetadata.isRepeated()) {
               iw.append("if (").append(f).append(" != null) ");
            }
         }
         if (fieldMetadata.isRepeated()) {
            iw.append('\n');
            iw.inc();
            if (fieldMetadata.isArray()) {
               iw.append("for (int i = 0; i < ").append(f).append(".length; i++) {\n");
               iw.inc();
               iw.append("final ").append(fieldMetadata.getJavaTypeName()).append(' ').append(v).append(" = ").append(f).append("[i];\n");
            } else {
               iw.append("for (java.util.Iterator it = ").append(f).append(".iterator(); it.hasNext(); ) {\n");
               iw.inc();
               iw.append("final ").append(fieldMetadata.getJavaTypeName()).append(' ').append(v).append(" = (").append(fieldMetadata.getJavaTypeName()).append(") it.next();\n");
            }
         }
         switch (fieldMetadata.getProtobufType()) {
            case DOUBLE:
            case FLOAT:
            case INT64:
            case UINT64:
            case INT32:
            case FIXED64:
            case FIXED32:
            case BOOL:
            case STRING:
            case BYTES:
            case UINT32:
            case SFIXED32:
            case SFIXED64:
            case SINT32:
            case SINT64: {
               iw.append("$out.").append(makeStreamIOMethodName(fieldMetadata, true)).append("(").append(String.valueOf(fieldMetadata.getNumber())).append(", ").append(unbox(v, fieldMetadata.getJavaType())).append(");\n");
               break;
            }
            case GROUP: {
               iw.append("{\n");
               iw.inc();
               String mdField = initMarshallerDelegateField(iw, fieldMetadata);
               iw.append("$out.writeTag(").append(String.valueOf(fieldMetadata.getNumber())).append(", ").append(PROTOSTREAM_PACKAGE).append(".impl.WireFormat.WIRETYPE_START_GROUP);\n");
               iw.append("writeMessage(").append(mdField).append(", $1, ").append(v).append(");\n");
               iw.append("$out.writeTag(").append(String.valueOf(fieldMetadata.getNumber())).append(", ").append(PROTOSTREAM_PACKAGE).append(".impl.WireFormat.WIRETYPE_END_GROUP);\n");
               iw.dec();
               iw.append("}\n");
               break;
            }
            case MESSAGE: {
               iw.append("{\n");
               iw.inc();
               String mdField = initMarshallerDelegateField(iw, fieldMetadata);
               iw.append("writeNestedMessage(").append(mdField).append(", $1, ").append(String.valueOf(fieldMetadata.getNumber())).append(", ").append(v).append(");\n");
               iw.dec();
               iw.append("}\n");
               break;
            }
            case ENUM: {
               iw.append("{\n");
               iw.inc();
               String mdField = initMarshallerDelegateField(iw, fieldMetadata);
               iw.append("$out.writeEnum(").append(String.valueOf(fieldMetadata.getNumber())).append(", ").append(mdField).append(".getMarshaller().encode(").append(v).append("));\n");
               iw.dec();
               iw.append("}\n");
               break;
            }
            default:
               throw new IllegalStateException("Unknown field type : " + fieldMetadata.getProtobufType());
         }
         if (fieldMetadata.isRepeated()) {
            iw.dec();
            iw.append("}\n");
            iw.dec();
         }
         iw.dec();
         iw.append("}\n");
      }

      if (getUnknownFieldSetFieldStatement != null) {
         iw.append("{\n").inc();
         iw.append(PROTOSTREAM_PACKAGE).append(".UnknownFieldSet u = ").append(getUnknownFieldSetFieldStatement).append(";\n");
         iw.append("if (u != null && !u.isEmpty()) u.writeTo($out);\n");
         iw.dec().append("}\n");
      }

      iw.dec();
      iw.append("}\n");
      return iw.toString();
   }

   private String initMarshallerDelegateField(IndentWriter iw, ProtoFieldMetadata fieldMetadata) {
      String fieldName = makeMarshallerDelegateFieldName(fieldMetadata);
      iw.append("if (").append(fieldName).append(" == null) ").append(fieldName).append(" = ");
      if (fieldMetadata.getJavaType().isEnum()) {
         iw.append("(").append(PROTOSTREAM_PACKAGE).append(".impl.EnumMarshallerDelegate) ");
      }
      iw.append("((").append(PROTOSTREAM_PACKAGE)
            .append(".impl.SerializationContextImpl) $1.getSerializationContext()).getMarshallerDelegate(")
            .append(fieldMetadata.getJavaTypeName()).append(".class);\n");
      return fieldName;
   }

   private String makeStreamIOMethodName(ProtoFieldMetadata fieldMetadata, boolean isWrite) {
      String suffix;
      switch (fieldMetadata.getProtobufType()) {
         case DOUBLE:
            suffix = "Double";
            break;
         case FLOAT:
            suffix = "Float";
            break;
         case INT64:
            suffix = "Int64";
            break;
         case UINT64:
            suffix = "UInt64";
            break;
         case INT32:
            suffix = "Int32";
            break;
         case FIXED64:
            suffix = "Fixed64";
            break;
         case FIXED32:
            suffix = "Fixed32";
            break;
         case BOOL:
            suffix = "Bool";
            break;
         case STRING:
            suffix = "String";
            break;
         case GROUP:
            suffix = "Group";
            break;
         case MESSAGE:
            suffix = "Message";
            break;
         case BYTES:
            suffix = isWrite ? "Bytes" : "ByteArray";
            break;
         case UINT32:
            suffix = "UInt32";
            break;
         case ENUM:
            suffix = "Enum";
            break;
         case SFIXED32:
            suffix = "SFixed32";
            break;
         case SFIXED64:
            suffix = "SFixed64";
            break;
         case SINT32:
            suffix = "SInt32";
            break;
         case SINT64:
            suffix = "SInt64";
            break;
         default:
            throw new IllegalStateException("Unknown field type : " + fieldMetadata.getProtobufType());
      }

      return (isWrite ? "write" : "read") + suffix;
   }

   /**
    * Cast the given value if necessary. This is usually needed for the types that we are forced to represent as 32 bit
    * integers because of Protobuf's lack of support for integral types of 8 and 16 bits.
    */
   private String convert(String v, ProtoFieldMetadata fieldMetadata) {
      if (fieldMetadata.getJavaType() == typeFactory.fromClass(Character.class) || fieldMetadata.getJavaType() == typeFactory.fromClass(char.class)) {
         return "(char) " + v;
      } else if (fieldMetadata.getJavaType() == typeFactory.fromClass(Short.class) || fieldMetadata.getJavaType() == typeFactory.fromClass(short.class)) {
         return "(short) " + v;
      } else if (fieldMetadata.getJavaType() == typeFactory.fromClass(Byte.class) || fieldMetadata.getJavaType() == typeFactory.fromClass(byte.class)) {
         return "(byte) " + v;
      }
      return v;
   }

   /**
    * Return the corresponding 'boxed' Class given a Class, or {@code null} if no type change is required.
    */
   private XClass box(XClass clazz) {
      if (clazz == typeFactory.fromClass(float.class)) {
         return typeFactory.fromClass(Float.class);
      } else if (clazz == typeFactory.fromClass(double.class)) {
         return typeFactory.fromClass(Double.class);
      } else if (clazz == typeFactory.fromClass(boolean.class)) {
         return typeFactory.fromClass(Boolean.class);
      } else if (clazz == typeFactory.fromClass(long.class)) {
         return typeFactory.fromClass(Long.class);
      } else if (clazz == typeFactory.fromClass(int.class)) {
         return typeFactory.fromClass(Integer.class);
      } else if (clazz == typeFactory.fromClass(short.class)) {
         return typeFactory.fromClass(Short.class);
      } else if (clazz == typeFactory.fromClass(byte.class)) {
         return typeFactory.fromClass(Byte.class);
      } else if (clazz == typeFactory.fromClass(char.class)) {
         return typeFactory.fromClass(Character.class);
      }
      // if no boxing is required then return null to indicate this
      return null;
   }

   /**
    * Boxes a given value. The Class parameter can be {@code null} to indicate that no boxing should actually be
    * performed.
    */
   private String box(String v, XClass clazz) {
      if (clazz != null) {
         if (clazz.isAssignableTo(Date.class)) {
            // just check this type really has a public constructor that accepts a long timestamp param
            XConstructor ctor = clazz.getDeclaredConstructor(typeFactory.fromClass(long.class));
            if (ctor == null || !ctor.isPublic()) {
               throw new ProtoSchemaBuilderException("Type " + clazz.getCanonicalName() + " is not a valid Date type because it does not have an accessible constructor that accepts a 'long' timestamp parameter");
            }
            return "new " + clazz.getName() + "(" + v + ")";
         } else if (clazz.isAssignableTo(Instant.class)) {
            return "java.time.Instant.ofEpochMilli(" + v + ")";
         } else if (clazz == typeFactory.fromClass(Float.class)) {
            return "new java.lang.Float(" + v + ")";
         } else if (clazz == typeFactory.fromClass(Double.class)) {
            return "new java.lang.Double(" + v + ")";
         } else if (clazz == typeFactory.fromClass(Boolean.class)) {
            return "new java.lang.Boolean(" + v + ")";
         } else if (clazz == typeFactory.fromClass(Long.class)) {
            return "new java.lang.Long(" + v + ")";
         } else if (clazz == typeFactory.fromClass(Integer.class)) {
            return "new java.lang.Integer(" + v + ")";
         } else if (clazz == typeFactory.fromClass(Short.class)) {
            return "new java.lang.Short(" + v + ")";
         } else if (clazz == typeFactory.fromClass(Byte.class)) {
            return "new java.lang.Byte(" + v + ")";
         } else if (clazz == typeFactory.fromClass(Character.class)) {
            return "new java.lang.Character(" + v + ")";
         }
      }
      return v;
   }

   private String unbox(String v, XClass clazz) {
      if (clazz.isAssignableTo(Date.class)) {
         return v + ".getTime()";
      } else if (clazz.isAssignableTo(Instant.class)) {
         return v + ".toEpochMilli()";
      } else if (clazz == typeFactory.fromClass(Float.class)) {
         return v + ".floatValue()";
      } else if (clazz == typeFactory.fromClass(Double.class)) {
         return v + ".doubleValue()";
      } else if (clazz == typeFactory.fromClass(Boolean.class)) {
         return v + ".booleanValue()";
      } else if (clazz == typeFactory.fromClass(Long.class)) {
         return v + ".longValue()";
      } else if (clazz == typeFactory.fromClass(Integer.class)) {
         return v + ".intValue()";
      } else if (clazz == typeFactory.fromClass(Short.class)) {
         return v + ".shortValue()";
      } else if (clazz == typeFactory.fromClass(Byte.class)) {
         return v + ".byteValue()";
      } else if (clazz == typeFactory.fromClass(Character.class)) {
         return v + ".charValue()";
      }
      return v;
   }

   private String createGetPropExpr(ProtoMessageTypeMetadata messageTypeMetadata, ProtoFieldMetadata fieldMetadata, String obj) {
      String thisTarget;
      String thisArg;
      if (messageTypeMetadata.isAdapter()) {
         thisTarget = ADAPTER_FIELD_NAME;
         thisArg = obj;
      } else {
         thisTarget = obj;
         thisArg = "";
      }

      StringBuilder readPropExpr = new StringBuilder();

      boolean isJUOptional = fieldMetadata.getGetter() != null && fieldMetadata.getGetter().getReturnType() == typeFactory.fromClass(Optional.class);
      if ((isJUOptional || fieldMetadata.getProtobufType().getJavaType() == JavaType.MESSAGE || fieldMetadata.getProtobufType().getJavaType() == JavaType.ENUM)
            && (fieldMetadata.isArray() || !fieldMetadata.isRepeated())) {
         readPropExpr.append("(").append(fieldMetadata.getJavaTypeName());
         if (fieldMetadata.isArray()) {
            readPropExpr.append("[]");
         }
         readPropExpr.append(") ");
      }
      if (fieldMetadata.getField() != null) {
         // TODO [anistor] complain if fieldMetadata.getProtoTypeMetadata().isAdapter() !
         readPropExpr.append(thisTarget).append('.').append(fieldMetadata.getField().getName());
      } else {
         if (isJUOptional) {
            readPropExpr.append('(');
         }
         readPropExpr.append(thisTarget).append('.').append(fieldMetadata.getGetter().getName()).append('(').append(thisArg).append(')');
         if (isJUOptional) {
            // TODO [anistor] simplify, use .orElse(null) instead of .isPresent() + .get()
            readPropExpr.append(".isPresent() ? ").append(thisTarget).append('.').append(fieldMetadata.getGetter().getName()).append('(').append(thisArg).append(").get() : null)");
         }
      }

      return readPropExpr.toString();
   }

   private String createSetPropExpr(ProtoMessageTypeMetadata messageTypeMetadata, ProtoFieldMetadata fieldMetadata, String obj, String value) {
      StringBuilder setPropExpr = new StringBuilder();
      setPropExpr.append(messageTypeMetadata.isAdapter() ? ADAPTER_FIELD_NAME : obj).append('.');

      if (fieldMetadata.getField() != null) {
         // TODO [anistor] complain if fieldMetadata.getProtoTypeMetadata().isAdapter() !
         setPropExpr.append(fieldMetadata.getField().getName()).append(" = ").append(value);
      } else {
         setPropExpr.append(fieldMetadata.getSetter().getName()).append('(');
         if (messageTypeMetadata.isAdapter()) {
            setPropExpr.append(obj).append(", ");
         }
         setPropExpr.append(value).append(')');
      }
      return setPropExpr.toString();
   }

   public abstract void generateMarshaller(SerializationContext serCtx, ProtoTypeMetadata ptm) throws Exception;
}
