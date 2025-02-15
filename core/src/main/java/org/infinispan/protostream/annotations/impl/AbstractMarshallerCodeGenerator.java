package org.infinispan.protostream.annotations.impl;

import java.io.StringWriter;
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
    * good fit. See the spec we do not fully implement here: <a href="https://developers.google.com/protocol-buffers/docs/proto#optional">Optional</a>
    */
   private final boolean noDefaults = false;

   private final String protobufSchemaPackage;

   private final boolean useGenerics = false;

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
      StringWriter sw = new StringWriter();
      IndentWriter iw = new IndentWriter(sw);
      iw.println("{");
      iw.inc();
      iw.println("switch ($1) {");
      iw.inc();
      for (ProtoEnumValueMetadata value : enumTypeMetadata.getMembers().values()) {
         iw.printf("case %d: return %s;\n", value.getNumber(), value.getJavaEnumName());
      }
      iw.println("default: return null;");
      iw.dec();
      iw.println("}");
      iw.dec();
      iw.println("}");
      return sw.toString();
   }

   /**
    * Signature of generated method is:
    * <code>
    * public int encode(java.lang.Enum $1)
    * </code>
    */
   protected String generateEnumEncodeMethodBody(ProtoEnumTypeMetadata enumTypeMetadata) {
      StringWriter sw = new StringWriter();
      IndentWriter iw = new IndentWriter(sw);
      iw.println("{");
      iw.inc();
      iw.println("switch ($1.ordinal()) {");
      iw.inc();
      for (ProtoEnumValueMetadata value : enumTypeMetadata.getMembers().values()) {
         iw.printf("case %d: return %d;\n", value.getJavaEnumOrdinal(), value.getNumber());
      }
      iw.printf("default: throw new IllegalArgumentException(\"Unexpected %s enum value : \" + $1.name());\n", enumTypeMetadata.getJavaClassName());
      iw.dec();
      iw.println("}");
      iw.dec();
      iw.println("}");
      return sw.toString();
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
      String name = "__md$" + field.getNumber();
      if (field instanceof ProtoMapMetadata mapMetadata) {
         field = mapMetadata.getValue();
      }
      return name + (field.getJavaType().isEnum() ? "e" : "");
   }

   /**
    * Signature of generated method is:
    * <code>
    * public java.lang.Object read(org.infinispan.protostream.ProtoStreamMarshaller.ReadContext $1,
    * java.lang.Object $2) throws java.io.IOException
    * </code>
    */
   protected void generateReadMethodBody(IndentWriter iw, ProtoMessageTypeMetadata messageTypeMetadata) {
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
      iw.printf("final %s $in = $1.getReader();\n", TagReader.class.getName());
      if (messageTypeMetadata.isContainer()) {
         iw.printf("Object __v$sizeParam = $1.getParam(\"%s\");\n", WrappedMessage.CONTAINER_SIZE_CONTEXT_PARAM);
         iw.println("int __v$size = ((java.lang.Integer) __v$sizeParam).intValue();");
      }

      // if there is no factory then the class must have setters or the fields should be directly accessible and not be final
      final boolean noFactory = messageTypeMetadata.getFactory() == null;
      if (noFactory) {
         iw.printf("final %s o = new %s();\n", messageTypeMetadata.getJavaClassName(), messageTypeMetadata.getJavaClassName());
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
               // declare a long variable to emulate a bitset in multiple long variables
               iw.printf("long __bits$%s = 0;\n", trackedFieldsSize >> 6);
            }
            trackedFields.put(fieldMetadata.getName(), trackedFieldsSize);
         }
      }

      for (ProtoFieldMetadata fieldMetadata : messageTypeMetadata.getFields().values()) {
         if (fieldMetadata.isRepeated()) {
            // a collection local variable
            iw.printf("%s %s = ", fieldMetadata.getRepeatedImplementation().getCanonicalName(), makeCollectionLocalVar(fieldMetadata));
            if (noDefaults || fieldMetadata.isArray()) {
               iw.print("null");
            } else if (fieldMetadata.isMap()) {
               iw.printf("new %s()",
                     fieldMetadata.getRepeatedImplementation().getCanonicalName()
               );
            } else {
               iw.printf("new %s()", fieldMetadata.getRepeatedImplementation().getCanonicalName());
            }
            iw.println(";");
            if (!noFactory && fieldMetadata.isArray()) {
               // an array local variable
               iw.printf("%s[] %s = ", fieldMetadata.getJavaTypeName(), makeArrayLocalVar(fieldMetadata));
               if (noDefaults) {
                  iw.print("null");
               } else {
                  iw.printf("new %s[0]", fieldMetadata.getJavaTypeName());
               }
               iw.println(";");
            }
         } else if (!noFactory) {
            // immutable messages need a per-field local variable initialized to default value if any
            iw.printf("%s %s", fieldMetadata.getJavaTypeName(), makeFieldLocalVar(fieldMetadata));
            Object defaultValue = fieldMetadata.getDefaultValue();
            if (defaultValue != null && fieldMetadata.getProtobufType() != Type.BYTES) {
               // fields of type bytes get assigned default values only at the end to avoid a possibly useless byte[] allocation
               String val = toJavaLiteral(defaultValue, fieldMetadata.getJavaType());
               iw.printf(" = %s", box(val, fieldMetadata.getJavaType()));
            } else {
               if (fieldMetadata.isBoxedPrimitive()
                     || fieldMetadata.getProtobufType() == Type.BYTES
                     || fieldMetadata.getProtobufType().getJavaType() == JavaType.STRING
                     || fieldMetadata.getProtobufType().getJavaType() == JavaType.BYTE_STRING
                     || fieldMetadata.getProtobufType().getJavaType() == JavaType.ENUM
                     || fieldMetadata.getProtobufType().getJavaType() == JavaType.MESSAGE
                     || fieldMetadata.getJavaType().getCanonicalName().equals(Date.class.getCanonicalName())
                     || fieldMetadata.getJavaType().getCanonicalName().equals(Instant.class.getCanonicalName())) {
                  iw.print(" = null");
               } else if (fieldMetadata.isPrimitive()) {
                  if (fieldMetadata.getProtobufType() == Type.BOOL) {
                     iw.print(" = false");
                  } else {
                     iw.print(" = 0");
                  }
               }
            }
            iw.println(";");
         }
      }
      iw.println("boolean done = false;");
      iw.println("while (!done) {");
      iw.inc();
      iw.println("final int tag = $in.readTag();");
      iw.println("switch (tag) {");
      iw.inc();
      iw.println("case 0: {");
      iw.inc();
      iw.println("done = true;");
      iw.println("break;");
      iw.dec();
      iw.println("}");
      for (ProtoFieldMetadata fieldMetadata : messageTypeMetadata.getFields().values()) {
         generateFieldReadMethod(messageTypeMetadata, fieldMetadata, iw, noFactory, trackedFields, getUnknownFieldSetFieldStatement, setUnknownFieldSetFieldStatement);
      }
      iw.println("default: {");
      iw.inc();
      if (getUnknownFieldSetFieldStatement != null) {
         iw.printf("%s.UnknownFieldSet u = %s;\n", PROTOSTREAM_PACKAGE, getUnknownFieldSetFieldStatement);
         iw.printf("if (u == null) u = new %s.impl.UnknownFieldSetImpl();\n", PROTOSTREAM_PACKAGE);
         iw.println("if (!u.readSingleField(tag, $in)) done = true;");
         iw.printf("if (!u.isEmpty()) %s;\n", setUnknownFieldSetFieldStatement);
      } else {
         iw.println("if (!$in.skipField(tag)) done = true;");
      }
      iw.dec().println("}");
      iw.dec().println("}");
      iw.dec().println("}");

      // assign defaults to missing fields
      if (BaseProtoSchemaGenerator.generateMarshallerDebugComments) {
         iw.println();
         iw.println("// default values");
         iw.println();
      }
      for (ProtoFieldMetadata fieldMetadata : messageTypeMetadata.getFields().values()) {
         Object defaultValue = fieldMetadata.getDefaultValue();
         if (defaultValue != null && (noFactory || fieldMetadata.isRepeated() || fieldMetadata.getProtobufType() == Type.BYTES)) {
            iw.printf("if %s {\n", makeTestFieldWasNotSet(fieldMetadata, trackedFields));
            iw.inc();
            String val = toJavaLiteral(defaultValue, fieldMetadata.getJavaType());
            if (fieldMetadata.isRepeated()) {
               String c = makeCollectionLocalVar(fieldMetadata);
               if (noDefaults || fieldMetadata.isArray()) {
                  iw.printf("if (%s == null) %s = new %s();\n", c, c, fieldMetadata.getRepeatedImplementation().getCanonicalName());
               }
               iw.printf("%s.add(%s);\n", c, box(val, typeFactory.fromClass(defaultValue.getClass())));
            } else {
               if (noFactory) {
                  iw.printf("%s;\n", createSetPropExpr(messageTypeMetadata, fieldMetadata, "o", box(val, fieldMetadata.getJavaType())));
               } else {
                  iw.printf("%s = %s;\n", makeFieldLocalVar(fieldMetadata), box(val, fieldMetadata.getJavaType()));
               }
            }
            iw.dec();
            iw.println("}");
         }
      }

      for (ProtoFieldMetadata fieldMetadata : messageTypeMetadata.getFields().values()) {
         if (fieldMetadata.isRepeated()) {
            String c = makeCollectionLocalVar(fieldMetadata);
            if (fieldMetadata.isArray()) {
               if (fieldMetadata.getDefaultValue() == null) {
                  iw.printf("if (%s != null)", c);
               }
               iw.println("{");
               iw.inc();
               String a = makeArrayLocalVar(fieldMetadata);
               if (fieldMetadata.getJavaType().isPrimitive()) {
                  if (noFactory) {
                     iw.printf("%s[] ", fieldMetadata.getJavaTypeName());
                  }
                  iw.printf("%s = new %s[%s.size()];\n", a, fieldMetadata.getJavaTypeName(), c);
                  XClass boxedType = box(fieldMetadata.getJavaType());
                  iw.println("int _j =0;");
                  iw.printf("for (java.util.Iterator _it = %s.iterator(); _it.hasNext();) %s[_j++] = %s;\n", c, a, unbox("((" + boxedType.getName() + ") _it.next())", boxedType));
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
                  iw.println(" else {");
                  iw.inc();
                  if (noFactory) {
                     iw.printf("%s;\n", createSetPropExpr(messageTypeMetadata, fieldMetadata, "o", c));
                  } else {
                     iw.printf("%s = %s;\n", makeArrayLocalVar(fieldMetadata), c);
                  }
                  iw.dec().println("}");
               }
            }
            iw.println();
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
         iw.println("throw new java.io.IOException(\"Required field(s) missing from input stream : \" + missing);");
         iw.dec();
         iw.println("}");
      }

      if (noFactory) {
         // return the instance
         iw.println("return o;");
      } else {
         // create and return the instance
         iw.print("return ");
         XExecutable factory = messageTypeMetadata.getFactory();
         if (factory instanceof XConstructor) {
            iw.printf("new %s", messageTypeMetadata.getJavaClassName());
         } else {
            if (factory.isStatic()) {
               iw.printf("%s.%s", messageTypeMetadata.getAnnotatedClassName(), factory.getName());
            } else {
               iw.printf("%s.%s", ADAPTER_FIELD_NAME, factory.getName());
            }
         }
         iw.print('(');
         boolean first = true;
         for (String paramName : factory.getParameterNames()) {
            if (first) {
               first = false;
               if (messageTypeMetadata.isContainer()) {
                  iw.print("__v$size");
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
                  if (fieldMetadata.isStream())
                     iw.append(".stream()");
                  found = true;
                  break;
               }
            }
            if (!found) {
               throw new ProtoSchemaBuilderException("Parameter '" + paramName + "' of factory " + factory + " does not map to any Protobuf field");
            }
         }
         iw.println(");");
      }
   }

   private void generateFieldReadMethod(ProtoMessageTypeMetadata messageTypeMetadata, ProtoFieldMetadata fieldMetadata, IndentWriter iw, boolean noFactory, Map<String, Integer> trackedFields, String getUnknownFieldSetFieldStatement, String setUnknownFieldSetFieldStatement) {
      final String v = makeFieldLocalVar(fieldMetadata);
      iw.printf("case %s: {\n", makeFieldTag(fieldMetadata.getNumber(), fieldMetadata.getProtobufType().getWireType()));
      iw.inc();
      if (BaseProtoSchemaGenerator.generateMarshallerDebugComments) {
         iw.printf("// type = %s, name = %s\n", fieldMetadata.getProtobufType(), fieldMetadata.getName());
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
               iw.printf("%s ", fieldMetadata.getJavaTypeName());
            }
            iw.printf("%s = %s;\n", v, box(convert("$in." + makeStreamIOMethodName(fieldMetadata, false) + "()", fieldMetadata), fieldMetadata.getJavaType()));
            genSetField(iw, fieldMetadata, trackedFields, messageTypeMetadata);
            break;
         }
         case GROUP: {
            String mdField = initMarshallerDelegateField(iw, fieldMetadata);
            if (noFactory || fieldMetadata.isRepeated()) {
               iw.printf("%s ", fieldMetadata.getJavaTypeName());
            }
            iw.printf("%s = (%s) readMessage(%s, $1);\n", v, fieldMetadata.getJavaTypeName(), mdField);
            iw.printf("$in.checkLastTagWas(%s);\n", makeFieldTag(fieldMetadata.getNumber(), WireType.END_GROUP));
            genSetField(iw, fieldMetadata, trackedFields, messageTypeMetadata);
            break;
         }
         case MESSAGE: {
            String mdField = initMarshallerDelegateField(iw, fieldMetadata);
            iw.println("int length = $in.readUInt32();");
            iw.println("int oldLimit = $in.pushLimit(length);");
            if (noFactory || fieldMetadata.isRepeated()) {
               iw.printf("%s ", fieldMetadata.getJavaTypeName());
            }
            iw.printf("%s = (%s) readMessage(%s, $1);\n", v, fieldMetadata.getJavaTypeName(), mdField);
            iw.println("$in.checkLastTagWas(0);");
            iw.println("$in.popLimit(oldLimit);");
            genSetField(iw, fieldMetadata, trackedFields, messageTypeMetadata);
            break;
         }
         case ENUM: {
            String mdField = initMarshallerDelegateField(iw, fieldMetadata);
            iw.println("int enumVal = $in.readEnum();");
            if (noFactory || fieldMetadata.isRepeated()) {
               iw.printf("%s ", fieldMetadata.getJavaTypeName());
            }
            iw.printf("%s = (%s) %s.getMarshaller().decode(enumVal);\n", v, fieldMetadata.getJavaTypeName(), mdField);
            iw.printf("if (%s == null) {\n", v);
            if (getUnknownFieldSetFieldStatement != null) {
               iw.inc();
               iw.printf("%s.UnknownFieldSet u = %s;\n", PROTOSTREAM_PACKAGE, getUnknownFieldSetFieldStatement);
               iw.printf("if (u == null) { u = new %s.impl.UnknownFieldSetImpl(); %s; }\n", PROTOSTREAM_PACKAGE, setUnknownFieldSetFieldStatement);
               iw.printf("u.putVarintField(%d, enumVal);\n", fieldMetadata.getNumber());
               iw.dec();
            }
            iw.println("} else {");
            iw.inc();
            genSetField(iw, fieldMetadata, trackedFields, messageTypeMetadata);
            iw.dec().println("}");
            break;
         }
         case MAP: {
            ProtoMapMetadata mapMetadata = (ProtoMapMetadata) fieldMetadata;
            iw.println("int $len = $in.readUInt32();");
            iw.println("int $limit = $in.pushLimit($len);");
            iw.println("int $t = $in.readTag();");
            String key = generateMapFieldReadMethod(mapMetadata, mapMetadata.getKey(), iw, true);
            String value = generateMapFieldReadMethod(mapMetadata, mapMetadata.getValue(), iw, false);
            iw.printf("%s.put(%s, %s);\n", makeCollectionLocalVar(mapMetadata), key, value);
            iw.println("$in.checkLastTagWas(0);");
            iw.println("$in.popLimit($limit);");
            break;
         }
         default:
            throw new IllegalStateException("Unknown field type : " + fieldMetadata.getProtobufType());
      }
      iw.println("break;");
      iw.dec().println("}");
   }

   private String generateMapFieldReadMethod(ProtoMapMetadata mapMetadata, ProtoFieldMetadata fieldMetadata, IndentWriter iw, boolean isKey) {
      final String v = "__mv$" + fieldMetadata.getNumber();
      iw.printf("%s %s = %s;\n", fieldMetadata.getJavaTypeName(), v, fieldMetadata.getProtobufType().getJavaType().defaultValueAsString());
      iw.printf("if ($t == %s) {\n", makeFieldTag(fieldMetadata.getNumber(), fieldMetadata.getProtobufType().getWireType()));
      iw.inc();
      if (BaseProtoSchemaGenerator.generateMarshallerDebugComments) {
         iw.printf("// type = %s, name = %s\n", fieldMetadata.getProtobufType(), fieldMetadata.getName());
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
            iw.printf("%s = %s;\n", v, box(convert("$in." + makeStreamIOMethodName(fieldMetadata, false) + "()", fieldMetadata), fieldMetadata.getJavaType()));
            break;
         }
         case MESSAGE: {
            // Type can only be MESSAGE for Map values
            assert !isKey;
            String fieldName = makeMarshallerDelegateFieldName(mapMetadata);
            String mdField = initMarshallerDelegateField(iw, fieldName, fieldMetadata);
            iw.println("int length = $in.readUInt32();");
            iw.println("int oldLimit = $in.pushLimit(length);");
            iw.printf("%s = (%s) readMessage(%s, $1);\n", v, fieldMetadata.getJavaTypeName(), mdField);
            iw.println("$in.checkLastTagWas(0);");
            iw.println("$in.popLimit(oldLimit);");
            break;
         }
         case ENUM: {
            // Type can only be ENUM for Map values
            assert !isKey;
            String fieldName = makeMarshallerDelegateFieldName(mapMetadata);
            String mdField = initMarshallerDelegateField(iw, fieldName, fieldMetadata);
            iw.println("int enumVal = $in.readEnum();");
            iw.printf("%s = (%s) %s.getMarshaller().decode(enumVal);\n", v, fieldMetadata.getJavaTypeName(), mdField);
            break;
         }
         default:
            throw new IllegalStateException("Unknown field type : " + fieldMetadata.getProtobufType());
      }
      if (isKey) {
         iw.println("$t = $in.readTag();");
      }
      iw.dec().println("}");
      return v;
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
      } else if (value instanceof byte[] bytes) {
         StringBuilder sb = new StringBuilder();
         sb.append("new byte[] {");
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
         if (!fieldMetadata.isMap()) {
            String c = makeCollectionLocalVar(fieldMetadata);
            if (noDefaults || fieldMetadata.isArray()) {
               iw.append("if (").append(c).append(" == null) ").append(c).append(" = new ").append(fieldMetadata.getRepeatedImplementation().getCanonicalName()).append("();\n");
            }
            iw.append(c).append(".add(").append(box(v, box(fieldMetadata.getJavaType()))).append(");\n");
         }
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
   protected void generateWriteMethodBody(IndentWriter iw, ProtoMessageTypeMetadata messageTypeMetadata) {
      //todo [anistor] handle unknown fields for adapters also
      String getUnknownFieldSetFieldStatement = null;
      if (messageTypeMetadata.getUnknownFieldSetField() != null) {
         getUnknownFieldSetFieldStatement = "o." + messageTypeMetadata.getUnknownFieldSetField().getName();
      } else if (messageTypeMetadata.getUnknownFieldSetGetter() != null) {
         getUnknownFieldSetFieldStatement = "o." + messageTypeMetadata.getUnknownFieldSetGetter().getName() + "()";
      } else if (messageTypeMetadata.getJavaClass().isAssignableTo(Message.class)) {
         getUnknownFieldSetFieldStatement = "o.getUnknownFieldSet()";
      }
      if (!messageTypeMetadata.getFields().isEmpty() || getUnknownFieldSetFieldStatement != null) {
         iw.print("var $out = $1.getWriter();\n");
         iw.printf("final %s o = (%s) $2;\n", messageTypeMetadata.getJavaClassName(), messageTypeMetadata.getJavaClassName());
         for (ProtoFieldMetadata fieldMetadata : messageTypeMetadata.getFields().values()) {
            iw.println("{");
            iw.inc();
            if (BaseProtoSchemaGenerator.generateMarshallerDebugComments) {
               iw.printf("// type = %s, name = %s\n", fieldMetadata.getProtobufType(), fieldMetadata.getName());
            }
            final String v = makeFieldLocalVar(fieldMetadata);
            final String f = fieldMetadata.isRepeated() ? (fieldMetadata.isArray() ? makeArrayLocalVar(fieldMetadata) : makeCollectionLocalVar(fieldMetadata)) : v;
            iw.print("final ");
            if (fieldMetadata.isRepeated()) {
               if (fieldMetadata.isArray()) {
                  iw.printf("%s[]", fieldMetadata.getJavaTypeName());
               } else if (fieldMetadata.isMap()) {
                  ProtoMapMetadata mapFieldMetadata = (ProtoMapMetadata) fieldMetadata;
                  iw.printf("java.util.Map<%s, %s>",
                        mapFieldMetadata.getKey().getJavaTypeName(),
                        mapFieldMetadata.getValue().getJavaTypeName()
                  );
               } else if (fieldMetadata.isIterable()) {
                  printFieldAssignmentWithGenerics("java.lang.Iterable", fieldMetadata, iw);
               } else if (fieldMetadata.isStream()) {
                  printFieldAssignmentWithGenerics("java.util.stream.Stream", fieldMetadata, iw);
               } else {
                  printFieldAssignmentWithGenerics("java.util.Collection", fieldMetadata, iw);
               }
            } else {
               iw.print(fieldMetadata.getJavaTypeName());
            }
            iw.printf(" %s = %s;\n", f, createGetPropExpr(messageTypeMetadata, fieldMetadata, "o"));

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
            } else if (fieldMetadata.isRepeated() || !fieldMetadata.getJavaType().isPrimitive()) {
               iw.append("if (").append(f).append(" != null) ");
            }

            if (fieldMetadata.isRepeated()) {
               iw.append('\n');
               iw.inc();
               if (fieldMetadata.isArray()) {
                  iw.printf("for (int i = 0; i < %s.length; i++) {\n", f);
                  iw.inc();
                  iw.printf("final %s %s = %s[i];\n", fieldMetadata.getJavaTypeName(), v, f);
               } else if (fieldMetadata.isMap()) {
                  ProtoMapMetadata mapFieldMetadata = (ProtoMapMetadata) fieldMetadata;
                  iw.printf("for (java.util.Iterator<java.util.Map.Entry<%s, %s>> it = %s.entrySet().iterator(); it.hasNext(); ) {\n",
                        mapFieldMetadata.getKey().getJavaTypeName(),
                        mapFieldMetadata.getValue().getJavaTypeName(),
                        f);
                  iw.inc();
                  iw.printf("final java.util.Map.Entry<%s, %s> %s = it.next();\n",
                        mapFieldMetadata.getKey().getJavaTypeName(),
                        mapFieldMetadata.getValue().getJavaTypeName(),
                        v
                  );
                  iw.printf("try (var $n = $out.subWriter(%d, true)) {\n", fieldMetadata.getNumber());
                  iw.inc();
                  writeFieldValue(mapFieldMetadata.getKey(), iw, v + ".getKey()", "$n");
                  writeMapFieldValue(mapFieldMetadata, mapFieldMetadata.getValue(), iw, v + ".getValue()", "$n");
                  iw.dec();
                  iw.println("}");
               } else {
                  if (useGenerics) {
                     iw.printf("for (java.util.Iterator<%s> it = %s.iterator(); it.hasNext(); ) {\n", fieldMetadata.getJavaTypeName(), f);
                     iw.inc();
                     iw.printf("final %s %s = it.next();\n", fieldMetadata.getJavaTypeName(), v);
                  } else {
                     iw.printf("for (java.util.Iterator it = %s.iterator(); it.hasNext(); ) {\n", f);
                     iw.inc();
                     iw.printf("final %s %s = (%s) it.next();\n", fieldMetadata.getJavaTypeName(), v, fieldMetadata.getJavaTypeName());
                  }
               }
            }
            if (!fieldMetadata.isMap()) {
               writeFieldValue(fieldMetadata, iw, v);
            }
            if (fieldMetadata.isRepeated()) {
               iw.dec().println("}");
               iw.dec();
            }
            iw.dec().println("}");
         }

         if (getUnknownFieldSetFieldStatement != null) {
            iw.println("{");
            iw.inc();
            iw.printf("%s.UnknownFieldSet u = %s;\n", PROTOSTREAM_PACKAGE, getUnknownFieldSetFieldStatement);
            iw.println("if (u != null && !u.isEmpty()) u.writeTo($out);");
            iw.dec().println("}");
         }
      }
   }

   private void printFieldAssignmentWithGenerics(String type, ProtoFieldMetadata fieldMeta, IndentWriter iw) {
      if (useGenerics) {
         iw.printf("%s<%s>", type, fieldMeta.getJavaTypeName());
      } else {
         iw.printf(type);
      }
   }

   private void writeFieldValue(ProtoFieldMetadata fieldMetadata, IndentWriter iw, String v) {
      writeFieldValue(fieldMetadata, iw, v, "$out");
   }

   private void writeFieldValue(ProtoFieldMetadata fieldMetadata, IndentWriter iw, String v, String out) {
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
            iw.printf("%s.%s(%d, %s);\n", out, makeStreamIOMethodName(fieldMetadata, true), fieldMetadata.getNumber(), unbox(v, fieldMetadata.getJavaType()));
            break;
         }
         case GROUP: {
            iw.println("{");
            iw.inc();
            String mdField = initMarshallerDelegateField(iw, fieldMetadata);
            iw.printf("%s.writeTag(%d, %s.impl.WireFormat.WIRETYPE_START_GROUP);\n", out, fieldMetadata.getNumber(), PROTOSTREAM_PACKAGE);
            iw.printf("writeMessage(%s, %s, %s);\n", mdField, out, v);
            iw.printf("%s.writeTag(%d, %s.impl.WireFormat.WIRETYPE_END_GROUP);\n", out, fieldMetadata.getNumber(), PROTOSTREAM_PACKAGE);
            iw.dec();
            iw.println("}");
            break;
         }
         case MESSAGE: {
            iw.println("{");
            iw.inc();
            String mdField = initMarshallerDelegateField(iw, fieldMetadata);
            iw.printf("writeNestedMessage(%s, (WriteContext) %s, %d, %s);\n", mdField, out, fieldMetadata.getNumber(), v);
            iw.dec();
            iw.println("}");
            break;
         }
         case ENUM: {
            iw.println("{");
            iw.inc();
            String mdField = initMarshallerDelegateField(iw, fieldMetadata);
            iw.printf("%s.writeEnum(%d, %s.getMarshaller().encode(%s));\n", out, fieldMetadata.getNumber(), mdField, v);
            iw.dec();
            iw.println("}");
            break;
         }
         default:
            throw new IllegalStateException("Unknown field type : " + fieldMetadata.getProtobufType());
      }
   }

   private void writeMapFieldValue(ProtoMapMetadata mapMetadata, ProtoFieldMetadata fieldMetadata, IndentWriter iw, String v, String out) {
      String fieldName, mdField;
      switch (fieldMetadata.getProtobufType()) {
         case MESSAGE:
            iw.println("{");
            iw.inc();
            fieldName = makeMarshallerDelegateFieldName(mapMetadata);
            mdField = initMarshallerDelegateField(iw, fieldName, fieldMetadata);
            iw.printf("writeNestedMessage(%s, (WriteContext) %s, %d, %s);\n", mdField, out, fieldMetadata.getNumber(), v);
            iw.dec();
            iw.println("}");
            break;
         case ENUM:
            iw.println("{");
            iw.inc();
            fieldName = makeMarshallerDelegateFieldName(mapMetadata);
            mdField = initMarshallerDelegateField(iw, fieldName, fieldMetadata);
            iw.printf("%s.writeEnum(%d, %s.getMarshaller().encode(%s));\n", out, fieldMetadata.getNumber(), mdField, v);
            iw.dec();
            iw.println("}");
            break;
         default:
            writeFieldValue(fieldMetadata, iw, v, out);
      }
   }

   private String initMarshallerDelegateField(IndentWriter iw, ProtoFieldMetadata fieldMetadata) {
      String fieldName = makeMarshallerDelegateFieldName(fieldMetadata);
      return initMarshallerDelegateField(iw, fieldName, fieldMetadata);
   }

   private String initMarshallerDelegateField(IndentWriter iw, String fieldName, ProtoFieldMetadata fieldMetadata) {
      iw.printf("if (%s == null) %s = ", fieldName, fieldName);
      if (fieldMetadata.getJavaType().isEnum()) {
         iw.printf("(%s.impl.EnumMarshallerDelegate)", PROTOSTREAM_PACKAGE);
      }
      iw.printf("((%s.impl.SerializationContextImpl) $1.getSerializationContext()).getMarshallerDelegate(%s.class);\n", PROTOSTREAM_PACKAGE, fieldMetadata.getJavaTypeName());
      return fieldName;
   }

   private String makeStreamIOMethodName(ProtoFieldMetadata fieldMetadata, boolean isWrite) {
      String suffix = switch (fieldMetadata.getProtobufType()) {
         case DOUBLE -> "Double";
         case FLOAT -> "Float";
         case INT64 -> "Int64";
         case UINT64 -> "UInt64";
         case INT32 -> "Int32";
         case FIXED64 -> "Fixed64";
         case FIXED32 -> "Fixed32";
         case BOOL -> "Bool";
         case STRING -> "String";
         case GROUP -> "Group";
         case MESSAGE -> "Message";
         case MAP -> "Map";
         case BYTES -> isWrite ? "Bytes" : "ByteArray";
         case UINT32 -> "UInt32";
         case ENUM -> "Enum";
         case SFIXED32 -> "SFixed32";
         case SFIXED64 -> "SFixed64";
         case SINT32 -> "SInt32";
         case SINT64 -> "SInt64";
      };

      return (isWrite ? "write" : "read") + suffix;
   }

   /**
    * Cast the given value if necessary. This is usually needed for the types that we are forced to represent as 32-bit
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
            return "java.lang.Float.valueOf(" + v + ")";
         } else if (clazz == typeFactory.fromClass(Double.class)) {
            return "java.lang.Double.valueOf(" + v + ")";
         } else if (clazz == typeFactory.fromClass(Boolean.class)) {
            return "java.lang.Boolean.valueOf(" + v + ")";
         } else if (clazz == typeFactory.fromClass(Long.class)) {
            return "java.lang.Long.valueOf(" + v + ")";
         } else if (clazz == typeFactory.fromClass(Integer.class)) {
            return "java.lang.Integer.valueOf(" + v + ")";
         } else if (clazz == typeFactory.fromClass(Short.class)) {
            return "java.lang.Short.valueOf(" + v + ")";
         } else if (clazz == typeFactory.fromClass(Byte.class)) {
            return "java.lang.Byte.valueOf(" + v + ")";
         } else if (clazz == typeFactory.fromClass(Character.class)) {
            return "java.lang.Character.valueOf(" + v + ")";
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
      } else if (fieldMetadata.getSetter() != null) {
         setPropExpr.append(fieldMetadata.getSetter().getName()).append('(');
         if (messageTypeMetadata.isAdapter()) {
            setPropExpr.append(obj).append(", ");
         }
         setPropExpr.append(value);
         if (fieldMetadata.isStream()) {
            setPropExpr.append(".stream()");
         }
         setPropExpr.append(')');
      } else {
         setPropExpr.append("FIXME"); //Map
      }
      return setPropExpr.toString();
   }

   public abstract void generateMarshaller(SerializationContext serCtx, ProtoTypeMetadata ptm) throws Exception;
}
