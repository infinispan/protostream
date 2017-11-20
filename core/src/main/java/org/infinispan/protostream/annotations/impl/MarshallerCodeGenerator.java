package org.infinispan.protostream.annotations.impl;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Date;

import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.Message;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.RawProtobufMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilderException;
import org.infinispan.protostream.descriptors.JavaType;
import org.infinispan.protostream.impl.BaseMarshallerDelegate;
import org.infinispan.protostream.impl.EnumMarshallerDelegate;
import org.infinispan.protostream.impl.Log;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;

// TODO [anistor] check which java classfile limits impose limits on the size of the supported protobuf schema
// TODO [anistor] what do we do with non-repeated fields that come repeated from stream?
// TODO [anistor] bounded streams should be checked to be exactly as the size indicated

/**
 * @author anistor@readhat.com
 * @since 3.0
 */
final class MarshallerCodeGenerator {

   private static final Log log = Log.LogFactory.getLog(MarshallerCodeGenerator.class);

   private static final String PROTOSTREAM_PACKAGE = SerializationContext.class.getPackage().getName();

   private static final String MARSHALLER_CLASS_NAME = "___ProtostreamGeneratedMarshaller";

   /**
    * A numeric id that is appended to generated class names to avoid potential collisions.
    */
   private static volatile long nextId = 0;

   private final ClassPool cp;
   private final CtClass ioException;
   private final CtClass enumMarshallerInterface;
   private final CtClass rawProtobufMarshallerInterface;
   private final CtClass generatedMarshallerBaseClass;
   private final CtClass baseMarshallerDelegateClass;
   private final CtClass enumMarshallerDelegateClass;
   private final CtMethod getJavaClassMethod;
   private final CtMethod getTypeNameMethod;
   private final CtMethod readFromMethod;
   private final CtMethod writeToMethod;
   private final CtMethod decodeMethod;
   private final CtMethod encodeMethod;
   private final String protobufSchemaPackage;

   public MarshallerCodeGenerator(String protobufSchemaPackage, ClassPool cp) throws NotFoundException {
      this.protobufSchemaPackage = protobufSchemaPackage;
      this.cp = cp;
      ioException = cp.getCtClass(IOException.class.getName());
      enumMarshallerInterface = cp.getCtClass(EnumMarshaller.class.getName());
      rawProtobufMarshallerInterface = cp.getCtClass(RawProtobufMarshaller.class.getName());
      generatedMarshallerBaseClass = cp.getCtClass(GeneratedMarshallerBase.class.getName());
      baseMarshallerDelegateClass = cp.getCtClass(BaseMarshallerDelegate.class.getName());
      enumMarshallerDelegateClass = cp.getCtClass(EnumMarshallerDelegate.class.getName());
      getJavaClassMethod = rawProtobufMarshallerInterface.getMethod("getJavaClass", "()Ljava/lang/Class;");
      getTypeNameMethod = rawProtobufMarshallerInterface.getMethod("getTypeName", "()Ljava/lang/String;");
      String rawProtobufInputStreamName = RawProtoStreamReader.class.getName().replace('.', '/');
      String rawProtobufOutputStreamName = RawProtoStreamWriter.class.getName().replace('.', '/');
      String serializationContextName = SerializationContext.class.getName().replace('.', '/');
      readFromMethod = rawProtobufMarshallerInterface.getMethod("readFrom", "(L" + serializationContextName + ";L" + rawProtobufInputStreamName + ";)Ljava/lang/Object;");
      writeToMethod = rawProtobufMarshallerInterface.getMethod("writeTo", "(L" + serializationContextName + ";L" + rawProtobufOutputStreamName + ";Ljava/lang/Object;)V");
      decodeMethod = enumMarshallerInterface.getMethod("decode", "(I)Ljava/lang/Enum;");
      encodeMethod = enumMarshallerInterface.getMethod("encode", "(Ljava/lang/Enum;)I");
   }

   /**
    * Generates a unique id to be used for generating unique class names.
    */
   private static synchronized long nextMarshallerClassId() {
      return nextId++;
   }

   public EnumMarshaller generateEnumMarshaller(ProtoEnumTypeMetadata petm) throws NotFoundException, CannotCompileException, IllegalAccessException, InstantiationException {
      CtClass enumClass = cp.get(petm.getJavaClass().getName());
      CtClass marshallerImpl = enumClass.makeNestedClass(MARSHALLER_CLASS_NAME + nextMarshallerClassId(), true);
      marshallerImpl.addInterface(enumMarshallerInterface);

      CtMethod ctGetJavaClassMethod = new CtMethod(getJavaClassMethod, marshallerImpl, null);
      ctGetJavaClassMethod.setModifiers(ctGetJavaClassMethod.getModifiers() | Modifier.FINAL);
      ctGetJavaClassMethod.setBody("{ return " + petm.getJavaClass().getName() + ".class; }");
      marshallerImpl.addMethod(ctGetJavaClassMethod);

      CtMethod ctGetTypeNameMethod = new CtMethod(getTypeNameMethod, marshallerImpl, null);
      ctGetTypeNameMethod.setModifiers(ctGetTypeNameMethod.getModifiers() | Modifier.FINAL);
      ctGetTypeNameMethod.setBody("{ return \"" + makeQualifiedTypeName(petm.getFullName()) + "\"; }");
      marshallerImpl.addMethod(ctGetTypeNameMethod);

      CtMethod ctDecodeMethod = new CtMethod(decodeMethod, marshallerImpl, null);
      ctDecodeMethod.setModifiers(ctDecodeMethod.getModifiers() | Modifier.FINAL);
      String decodeSrc = generateEnumDecodeMethod(petm);
      if (log.isTraceEnabled()) {
         log.tracef("%s %s", ctDecodeMethod, decodeSrc);
      }
      ctDecodeMethod.setBody(decodeSrc);
      marshallerImpl.addMethod(ctDecodeMethod);

      CtMethod ctEncodeMethod = new CtMethod(encodeMethod, marshallerImpl, null);
      ctEncodeMethod.setModifiers(ctEncodeMethod.getModifiers() | Modifier.FINAL);
      String encodeSrc = generateEnumEncodeMethod(petm);
      if (log.isTraceEnabled()) {
         log.tracef("%s %s", ctEncodeMethod, encodeSrc);
      }
      ctEncodeMethod.setBody(encodeSrc);
      marshallerImpl.addMethod(ctEncodeMethod);

      marshallerImpl.setModifiers(marshallerImpl.getModifiers() & ~Modifier.ABSTRACT | Modifier.FINAL);

      Class<?> generatedMarshallerClass = marshallerImpl.toClass();
      EnumMarshaller marshallerInstance = (EnumMarshaller) generatedMarshallerClass.newInstance();
      marshallerImpl.detach();
      return marshallerInstance;
   }

   private String generateEnumDecodeMethod(ProtoEnumTypeMetadata enumTypeMetadata) {
      IndentWriter iw = new IndentWriter();
      iw.append("{\n");
      iw.inc();
      iw.append("switch ($1) {\n");
      iw.inc();
      for (ProtoEnumValueMetadata value : enumTypeMetadata.getMembers().values()) {
         iw.append("case ").append(String.valueOf(value.getNumber())).append(": return ").append(enumTypeMetadata.getJavaClass().getName()).append(".").append(value.getEnumValue().name()).append(";\n");
      }
      iw.append("default: return null;\n");
      iw.dec();
      iw.append("}\n");
      iw.dec();
      iw.append("}\n");
      return iw.toString();
   }

   private String generateEnumEncodeMethod(ProtoEnumTypeMetadata enumTypeMetadata) {
      IndentWriter iw = new IndentWriter();
      iw.append("{\n");
      iw.inc();
      iw.append("switch ($1.ordinal()) {\n");
      iw.inc();
      for (ProtoEnumValueMetadata value : enumTypeMetadata.getMembers().values()) {
         iw.append("case ").append(String.valueOf(value.getEnumValue().ordinal())).append(": return ").append(String.valueOf(value.getNumber())).append(";\n");
      }
      iw.append("default: throw new IllegalArgumentException(\"Unexpected ").append(enumTypeMetadata.getJavaClass().getName()).append(" value : \" + $1.name());\n");
      iw.dec();
      iw.append("}\n");
      iw.dec();
      iw.append("}\n");
      return iw.toString();
   }

   private String makeQualifiedTypeName(String fullName) {
      if (protobufSchemaPackage != null) {
         return protobufSchemaPackage + "." + fullName;
      }
      return fullName;
   }

   private String makeFieldWasSetFlag(ProtoFieldMetadata fieldMetadata) {
      return "__wasSet$" + fieldMetadata.getName();
   }

   private String makeCollectionLocalVar(ProtoFieldMetadata fieldMetadata) {
      return "__c$" + fieldMetadata.getName();
   }

   private String makeMarshallerDelegateFieldName(ProtoFieldMetadata fieldMetadata) {
      return "__md$" + fieldMetadata.getJavaType().getCanonicalName().replace('.', '$');
   }

   public RawProtobufMarshaller generateMessageMarshaller(ProtoMessageTypeMetadata messageTypeMetadata) throws NotFoundException, CannotCompileException, IllegalAccessException, InstantiationException {
      CtClass entityClass = cp.get(messageTypeMetadata.getJavaClass().getName());
      CtClass marshallerImpl = entityClass.makeNestedClass(MARSHALLER_CLASS_NAME + nextMarshallerClassId(), true);
      marshallerImpl.addInterface(rawProtobufMarshallerInterface);
      marshallerImpl.setSuperclass(generatedMarshallerBaseClass);

      addMarshallerDelegateFields(marshallerImpl, messageTypeMetadata);

      CtMethod ctGetJavaClassMethod = new CtMethod(getJavaClassMethod, marshallerImpl, null);
      ctGetJavaClassMethod.setModifiers(ctGetJavaClassMethod.getModifiers() | Modifier.FINAL);
      ctGetJavaClassMethod.setBody("{ return " + entityClass.getName() + ".class; }");
      marshallerImpl.addMethod(ctGetJavaClassMethod);

      CtMethod ctGetTypeNameMethod = new CtMethod(getTypeNameMethod, marshallerImpl, null);
      ctGetTypeNameMethod.setModifiers(ctGetTypeNameMethod.getModifiers() | Modifier.FINAL);
      ctGetTypeNameMethod.setBody("{ return \"" + makeQualifiedTypeName(messageTypeMetadata.getFullName()) + "\"; }");
      marshallerImpl.addMethod(ctGetTypeNameMethod);

      CtMethod ctReadFromMethod = new CtMethod(readFromMethod, marshallerImpl, null);
      ctGetTypeNameMethod.setExceptionTypes(new CtClass[]{ioException});
      ctReadFromMethod.setModifiers(ctReadFromMethod.getModifiers() | Modifier.FINAL);
      String readFromSrc = generateReadFromMethod(messageTypeMetadata);
      if (log.isTraceEnabled()) {
         log.tracef("%s %s", ctReadFromMethod, readFromSrc);
      }
      ctReadFromMethod.setBody(readFromSrc);
      marshallerImpl.addMethod(ctReadFromMethod);

      CtMethod ctWriteToMethod = new CtMethod(writeToMethod, marshallerImpl, null);
      ctWriteToMethod.setExceptionTypes(new CtClass[]{ioException});
      ctWriteToMethod.setModifiers(ctWriteToMethod.getModifiers() | Modifier.FINAL);
      String writeToSrc = generateWriteToMethod(messageTypeMetadata);
      if (log.isTraceEnabled()) {
         log.tracef("%s %s", ctWriteToMethod, writeToSrc);
      }
      ctWriteToMethod.setBody(writeToSrc);
      marshallerImpl.addMethod(ctWriteToMethod);

      marshallerImpl.setModifiers(marshallerImpl.getModifiers() & ~Modifier.ABSTRACT | Modifier.FINAL);

      Class<?> generatedMarshallerClass = marshallerImpl.toClass();
      RawProtobufMarshaller marshallerInstance = (RawProtobufMarshaller) generatedMarshallerClass.newInstance();
      marshallerImpl.detach();
      return marshallerInstance;
   }

   private void addMarshallerDelegateFields(CtClass marshallerImpl, ProtoMessageTypeMetadata messageTypeMetadata) throws CannotCompileException {
      for (ProtoFieldMetadata fieldMetadata : messageTypeMetadata.getFields().values()) {
         switch (fieldMetadata.getProtobufType()) {
            case GROUP:
            case MESSAGE:
            case ENUM:
               String fieldName = makeMarshallerDelegateFieldName(fieldMetadata);
               try {
                  // add the field only if it does not already exist
                  marshallerImpl.getDeclaredField(fieldName);
               } catch (NotFoundException ex) {
                  marshallerImpl.addField(new CtField(fieldMetadata.getJavaType().isEnum() ? enumMarshallerDelegateClass : baseMarshallerDelegateClass, fieldName, marshallerImpl));
               }
               break;
         }
      }
   }

   private String generateReadFromMethod(ProtoMessageTypeMetadata messageTypeMetadata) {
      String getUnknownFieldSetFieldStatement = null;
      String setUnknownFieldSetFieldStatement = null;
      if (messageTypeMetadata.getUnknownFieldSetField() != null) {
         getUnknownFieldSetFieldStatement = "o." + messageTypeMetadata.getUnknownFieldSetField().getName();
         setUnknownFieldSetFieldStatement = "o." + messageTypeMetadata.getUnknownFieldSetField().getName() + " = u";
      } else if (messageTypeMetadata.getUnknownFieldSetGetter() != null) {
         getUnknownFieldSetFieldStatement = "o." + messageTypeMetadata.getUnknownFieldSetGetter().getName() + "()";
         setUnknownFieldSetFieldStatement = "o." + messageTypeMetadata.getUnknownFieldSetSetter().getName() + "(u)";
      } else if (Message.class.isAssignableFrom(messageTypeMetadata.getJavaClass())) {
         getUnknownFieldSetFieldStatement = "o.getUnknownFieldSet()";
         setUnknownFieldSetFieldStatement = "o.setUnknownFieldSet(u)";
      }

      IndentWriter iw = new IndentWriter();
      iw.append("{\n");
      iw.inc();
      iw.append("final ").append(messageTypeMetadata.getJavaClass().getName()).append(" o = new ").append(messageTypeMetadata.getJavaClass().getName()).append("();\n");
      int requiredFields = 0;
      for (ProtoFieldMetadata fieldMetadata : messageTypeMetadata.getFields().values()) {
         if (fieldMetadata.isRequired() && fieldMetadata.getDefaultValue() == null) {
            requiredFields++;
         }
         if (fieldMetadata.isRequired() || fieldMetadata.getDefaultValue() != null) {
            iw.append("boolean ").append(makeFieldWasSetFlag(fieldMetadata)).append(" = false;\n");
         }
         if (fieldMetadata.isRepeated()) {
            String c = makeCollectionLocalVar(fieldMetadata);
            String collectionImpl = fieldMetadata.isArray() ? "java.util.ArrayList" : fieldMetadata.getCollectionImplementation().getName();
            iw.append(collectionImpl).append(' ').append(c).append(" = null;\n");
         }
      }
      iw.append("boolean done = false;\n");
      iw.append("while (!done) {\n");
      iw.inc();
      iw.append("final int tag = $2.readTag();\n");
      iw.append("switch (tag) {\n");
      iw.inc();
      iw.append("case 0:\n");
      iw.inc();
      iw.append("done = true;\nbreak;\n");
      iw.dec();

      for (ProtoFieldMetadata fieldMetadata : messageTypeMetadata.getFields().values()) {
         iw.append("case ").append(String.valueOf(fieldMetadata.getNumber() << 3 | fieldMetadata.getProtobufType().getWireType())).append(":\n");
         iw.inc();
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
            case SINT64:
               iw.append("{\n");
               iw.inc();
               iw.append(fieldMetadata.getJavaType().getName()).append(" v = ").append(box(convert("$2." + makeStreamIOMethodName(fieldMetadata, false) + "()", fieldMetadata), fieldMetadata.getJavaType())).append(";\n");
               genSetField(iw, fieldMetadata);
               iw.dec();
               iw.append("}\n");
               break;
            case GROUP:
               iw.append("{\n");
               iw.inc();
               initMarshallerDelegateField(iw, fieldMetadata);
               iw.append(fieldMetadata.getJavaType().getName()).append(" v = (").append(fieldMetadata.getJavaType().getName()).append(") readMessage(").append(makeMarshallerDelegateFieldName(fieldMetadata)).append(", $2);\n");
               iw.append("$2.checkLastTagWas(").append(String.valueOf(fieldMetadata.getNumber() << 3 | org.infinispan.protostream.impl.WireFormat.WIRETYPE_END_GROUP)).append(");\n");
               genSetField(iw, fieldMetadata);
               iw.dec();
               iw.append("}\n");
               break;
            case MESSAGE:
               iw.append("{\n");
               iw.inc();
               initMarshallerDelegateField(iw, fieldMetadata);
               iw.append("int length = $2.readRawVarint32();\n");
               iw.append("int oldLimit = $2.pushLimit(length);\n");
               iw.append(fieldMetadata.getJavaType().getName()).append(" v = (").append(fieldMetadata.getJavaType().getName()).append(") readMessage(").append(makeMarshallerDelegateFieldName(fieldMetadata)).append(", $2);\n");
               iw.append("$2.checkLastTagWas(0);\n");
               iw.append("$2.popLimit(oldLimit);\n");
               genSetField(iw, fieldMetadata);
               iw.dec();
               iw.append("}\n");
               break;
            case ENUM:
               iw.append("{\n");
               iw.inc();
               initMarshallerDelegateField(iw, fieldMetadata);
               iw.append("int enumVal = $2.readEnum();\n");
               iw.append(fieldMetadata.getJavaType().getName()).append(" v = (").append(fieldMetadata.getJavaType().getName()).append(") ((").append(PROTOSTREAM_PACKAGE).append(".EnumMarshaller) $1.getMarshaller(").append(fieldMetadata.getJavaType().getName()).append(".class)).decode(enumVal);\n");
               iw.append("if (v == null) {\n");
               if (getUnknownFieldSetFieldStatement != null) {
                  iw.inc();
                  iw.append(PROTOSTREAM_PACKAGE).append(".UnknownFieldSet u = ").append(getUnknownFieldSetFieldStatement).append(";\n");
                  iw.append("if (u == null) { u = new ").append(PROTOSTREAM_PACKAGE).append(".impl.UnknownFieldSetImpl(); ").append(setUnknownFieldSetFieldStatement).append("; }\n");
                  iw.append("u.putVarintField(").append(String.valueOf(fieldMetadata.getNumber())).append(", enumVal);\n");
                  iw.dec();
               }
               iw.append("} else {\n");
               iw.inc();
               genSetField(iw, fieldMetadata);
               iw.dec();
               iw.append("}\n");
               iw.dec();
               iw.append("}\n");
               break;
            default:
               throw new IllegalStateException("Unknown field type " + fieldMetadata.getProtobufType());
         }
         iw.append("break;\n");
         iw.dec();
      }
      iw.append("default:\n");
      iw.inc();
      iw.append("{\n");
      iw.inc();
      if (getUnknownFieldSetFieldStatement != null) {
         iw.append(PROTOSTREAM_PACKAGE).append(".UnknownFieldSet u = ").append(getUnknownFieldSetFieldStatement).append(";\n");
         iw.append("if (u == null) u = new ").append(PROTOSTREAM_PACKAGE).append(".impl.UnknownFieldSetImpl();\n");
         iw.append("if (!u.readSingleField(tag, $2)) done = true;\n");
         iw.append("if (!u.isEmpty()) ").append(setUnknownFieldSetFieldStatement).append(";\n");
      } else {
         iw.append("$2.skipField(tag);\n");
      }
      iw.dec();
      iw.append("}\n");
      iw.dec();
      iw.dec();
      iw.append("}\n");
      iw.dec();
      iw.append("}\n");
      for (ProtoFieldMetadata fieldMetadata : messageTypeMetadata.getFields().values()) {
         Object defaultValue = fieldMetadata.getDefaultValue();
         if (defaultValue != null) {
            iw.append("if (!").append(makeFieldWasSetFlag(fieldMetadata)).append(") {\n");
            iw.inc();
            String v;
            if (Date.class.isAssignableFrom(fieldMetadata.getJavaType())) {
               v = box(defaultValue + "L", fieldMetadata.getJavaType());
            } else if (Instant.class.isAssignableFrom(fieldMetadata.getJavaType())) {
               v = box(defaultValue + "L", fieldMetadata.getJavaType());
            } else if (defaultValue instanceof ProtoEnumValueMetadata) {
               Enum enumValue = ((ProtoEnumValueMetadata) defaultValue).getEnumValue();
               v = enumValue.getDeclaringClass().getName() + "." + enumValue.name();
            } else if (defaultValue instanceof Long) {
               v = defaultValue + "L";
            } else if (defaultValue instanceof Double) {
               v = defaultValue + "D";
            } else if (defaultValue instanceof Float) {
               v = defaultValue + "F";
            } else if (defaultValue instanceof Character) {
               v = "'" + defaultValue + "'";
            } else if (defaultValue instanceof Short) {
               v = "(short) " + defaultValue;
            } else if (defaultValue instanceof Byte) {
               v = "(byte) " + defaultValue;
            } else {
               v = defaultValue.toString();
            }
            if (fieldMetadata.isRepeated()) {
               String c = makeCollectionLocalVar(fieldMetadata);
               String collectionImpl = fieldMetadata.isArray() ? "java.util.ArrayList" : fieldMetadata.getCollectionImplementation().getName();
               iw.append("if (").append(c).append(" == null) ").append(c).append(" = new ").append(collectionImpl).append("();\n");
               iw.append(c).append(".add(").append(box(v, defaultValue.getClass())).append(");\n");
            } else {
               iw.append("o.").append(createSetter(fieldMetadata, box(v, fieldMetadata.getJavaType()))).append(";\n");
            }
            iw.dec();
            iw.append("}\n");
         }
      }
      for (ProtoFieldMetadata fieldMetadata : messageTypeMetadata.getFields().values()) {
         if (fieldMetadata.isRepeated()) {
            String c = makeCollectionLocalVar(fieldMetadata);
            if (fieldMetadata.isArray()) {
               iw.append("if (").append(c).append(" != null) { ");
               if (fieldMetadata.getJavaType().isPrimitive()) {
                  iw.append(fieldMetadata.getJavaType().getName()).append("[] _c = new ").append(fieldMetadata.getJavaType().getName()).append("[").append(c).append(".size()]; ");
                  Class<?> boxedType = box(fieldMetadata.getJavaType());
                  iw.append("for (int i = 0; i < _c.length; i++) _c[i] = ").append(unbox("((" + boxedType.getName() + ")" + c + ".get(i))", boxedType)).append("; ");
                  c = "_c";
               } else {
                  c = "(" + fieldMetadata.getJavaType().getName() + "[])" + c + ".toArray(new " + fieldMetadata.getJavaType().getName() + "[" + c + ".size()])";
               }
            }
            iw.append("o.").append(createSetter(fieldMetadata, c)).append(';');
            if (fieldMetadata.isArray()) {
               iw.append(" }");
            }
            iw.append('\n');
         }
      }
      if (requiredFields > 0) {
         iw.append("if (!(");
         boolean first = true;
         for (ProtoFieldMetadata fieldMetadata : messageTypeMetadata.getFields().values()) {
            if (fieldMetadata.isRequired() && fieldMetadata.getDefaultValue() == null) {
               if (first) {
                  first = false;
               } else {
                  iw.append(" && ");
               }
               iw.append(makeFieldWasSetFlag(fieldMetadata));
            }
         }
         iw.append("))\n{\n");
         iw.inc();
         iw.append("StringBuilder missingFields = null;\n");
         for (ProtoFieldMetadata fieldMetadata : messageTypeMetadata.getFields().values()) {
            if (fieldMetadata.isRequired()) {
               iw.append("if (!").append(makeFieldWasSetFlag(fieldMetadata)).append(") { if (missingFields == null) missingFields = new StringBuilder(); else missingFields.append(\", \"); missingFields.append(\"").append(fieldMetadata.getName()).append("\"); }\n");
            }
         }
         iw.append("if (missingFields != null) throw new java.io.IOException(\"Required field(s) missing from input stream : \" + missingFields);\n");
         iw.dec();
         iw.append("}\n");
      }
      iw.append("return o;\n");
      iw.dec();
      iw.append("}\n");
      return iw.toString();
   }

   private void genSetField(IndentWriter iw, ProtoFieldMetadata fieldMetadata) {
      if (fieldMetadata.isRepeated()) {
         String c = makeCollectionLocalVar(fieldMetadata);
         String collectionImpl = fieldMetadata.isArray() ? "java.util.ArrayList" : fieldMetadata.getCollectionImplementation().getName();
         iw.append("if (").append(c).append(" == null) ").append(c).append(" = new ").append(collectionImpl).append("();\n");
         iw.append(c).append(".add(").append(box("v", box(fieldMetadata.getJavaType()))).append(");\n");
      } else {
         iw.append("o.").append(createSetter(fieldMetadata, "v")).append(";\n");
      }
      if (fieldMetadata.isRequired() || fieldMetadata.getDefaultValue() != null) {
         iw.append(makeFieldWasSetFlag(fieldMetadata)).append(" = true;\n");
      }
   }

   private String generateWriteToMethod(ProtoMessageTypeMetadata messageTypeMetadata) {
      String getUnknownFieldSetFieldStatement = null;
      if (messageTypeMetadata.getUnknownFieldSetField() != null) {
         getUnknownFieldSetFieldStatement = "o." + messageTypeMetadata.getUnknownFieldSetField().getName();
      } else if (messageTypeMetadata.getUnknownFieldSetGetter() != null) {
         getUnknownFieldSetFieldStatement = "o." + messageTypeMetadata.getUnknownFieldSetGetter().getName() + "()";
      } else if (Message.class.isAssignableFrom(messageTypeMetadata.getJavaClass())) {
         getUnknownFieldSetFieldStatement = "o.getUnknownFieldSet()";
      }

      IndentWriter iw = new IndentWriter();
      iw.append("{\n");
      iw.inc();
      iw.append("final ").append(messageTypeMetadata.getJavaClass().getName()).append(" o = (").append(messageTypeMetadata.getJavaClass().getName()).append(") $3;\n");
      for (ProtoFieldMetadata fieldMetadata : messageTypeMetadata.getFields().values()) {
         iw.append("{\n");
         iw.inc();
         final String v = fieldMetadata.isRepeated() ? "c" : "v";
         iw.append("final ");
         if (fieldMetadata.isRepeated()) {
            if (fieldMetadata.isArray()) {
               iw.append(fieldMetadata.getJavaType().getName()).append("[]");
            } else {
               iw.append("java.util.Collection");
            }
         } else {
            iw.append(fieldMetadata.getJavaType().getName());
         }
         iw.append(' ').append(v).append(" = o.").append(createGetter(fieldMetadata)).append(";\n");
         if (fieldMetadata.isRequired()) {
            boolean couldBeNull = fieldMetadata.isRepeated()
                  || fieldMetadata.getProtobufType().getJavaType() == JavaType.STRING
                  || fieldMetadata.getProtobufType().getJavaType() == JavaType.BYTE_STRING
                  || fieldMetadata.getProtobufType().getJavaType() == JavaType.ENUM
                  || fieldMetadata.getProtobufType().getJavaType() == JavaType.MESSAGE;
            if (couldBeNull) {
               iw.append("if (").append(v).append(" == null) throw new IllegalStateException(\"Required field must not be null : ").append(fieldMetadata.getName()).append("\");\n");
            }
         } else {
            if (!fieldMetadata.getJavaType().isPrimitive() || fieldMetadata.isRepeated()) {
               iw.append("if (").append(v).append(" != null) ");
            }
         }
         if (fieldMetadata.isRepeated()) {
            iw.append('\n');
            iw.inc();
            if (fieldMetadata.isArray()) {
               iw.append("for (int i = 0; i < c.length; i++) {\n");
               iw.inc();
               iw.append("final ").append(fieldMetadata.getJavaType().getName()).append(" v = c[i];\n");
            } else {
               iw.append("for (java.util.Iterator it = c.iterator(); it.hasNext(); ) {\n");
               iw.inc();
               iw.append("final ").append(fieldMetadata.getJavaType().getName()).append(" v = (").append(fieldMetadata.getJavaType().getName()).append(") it.next();\n");
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
            case SINT64:
               iw.append("$2.").append(makeStreamIOMethodName(fieldMetadata, true)).append("(").append(String.valueOf(fieldMetadata.getNumber())).append(", ").append(unbox("v", fieldMetadata.getJavaType())).append(");\n");
               break;
            case GROUP:
               iw.append("{\n");
               iw.inc();
               initMarshallerDelegateField(iw, fieldMetadata);
               iw.append("$2.writeTag(").append(String.valueOf(fieldMetadata.getNumber())).append(", ").append(PROTOSTREAM_PACKAGE).append(".impl.WireFormat.WIRETYPE_START_GROUP);\n");
               iw.append("writeMessage(").append(makeMarshallerDelegateFieldName(fieldMetadata)).append(", $2, v);\n");
               iw.append("$2.writeTag(").append(String.valueOf(fieldMetadata.getNumber())).append(", ").append(PROTOSTREAM_PACKAGE).append(".impl.WireFormat.WIRETYPE_END_GROUP);\n");
               iw.dec();
               iw.append("}\n");
               break;
            case MESSAGE:
               iw.append("{\n");
               iw.inc();
               initMarshallerDelegateField(iw, fieldMetadata);
               iw.append("writeNestedMessage(").append(makeMarshallerDelegateFieldName(fieldMetadata)).append(", $2, ").append(String.valueOf(fieldMetadata.getNumber())).append(", v);\n");
               iw.dec();
               iw.append("}\n");
               break;
            case ENUM:
               iw.append("{\n");
               iw.inc();
               initMarshallerDelegateField(iw, fieldMetadata);
               iw.append("$2.writeEnum(").append(String.valueOf(fieldMetadata.getNumber())).append(", ").append(makeMarshallerDelegateFieldName(fieldMetadata)).append(".getMarshaller().encode(v));\n");
               iw.dec();
               iw.append("}\n");
               break;
            default:
               throw new IllegalStateException("Unknown field type " + fieldMetadata.getProtobufType());
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
         iw.append("{\n");
         iw.inc();
         iw.append(PROTOSTREAM_PACKAGE).append(".UnknownFieldSet u = ").append(getUnknownFieldSetFieldStatement).append(";\nif (u != null && !u.isEmpty()) u.writeTo($2);\n");
         iw.dec();
         iw.append("}\n");
      }

      iw.dec();
      iw.append("}\n");
      return iw.toString();
   }

   private void initMarshallerDelegateField(IndentWriter iw, ProtoFieldMetadata fieldMetadata) {
      iw.append("if (").append(makeMarshallerDelegateFieldName(fieldMetadata)).append(" == null) ")
            .append(makeMarshallerDelegateFieldName(fieldMetadata)).append(" = ");
      if (fieldMetadata.getJavaType().isEnum()) {
         iw.append("(").append(PROTOSTREAM_PACKAGE).append(".impl.EnumMarshallerDelegate) ");
      }
      iw.append("((").append(PROTOSTREAM_PACKAGE)
            .append(".impl.SerializationContextImpl) $1).getMarshallerDelegate(")
            .append(fieldMetadata.getJavaType().getName()).append(".class);\n");
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
            suffix = "Bytes";
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
            throw new IllegalStateException("Unknown field type " + fieldMetadata.getProtobufType());
      }

      return (isWrite ? "write" : "read") + suffix;
   }

   private String convert(String v, ProtoFieldMetadata fieldMetadata) {
      if (fieldMetadata.getJavaType() == Character.class) {
         return "(char) " + v;
      } else if (fieldMetadata.getJavaType() == Short.class) {
         return "(short) " + v;
      }
      return v;
   }

   private Class<?> box(Class<?> clazz) {
      if (clazz == Float.TYPE) {
         return Float.class;
      } else if (clazz == Double.TYPE) {
         return Double.class;
      } else if (clazz == Boolean.TYPE) {
         return Boolean.class;
      } else if (clazz == Long.TYPE) {
         return Long.class;
      } else if (clazz == Integer.TYPE) {
         return Integer.class;
      } else if (clazz == Short.TYPE) {
         return Short.class;
      } else if (clazz == Byte.TYPE) {
         return Byte.class;
      } else if (clazz == Character.TYPE) {
         return Character.class;
      }
      // if no boxing is required then return null to indicate this
      return null;
   }

   private String box(String v, Class<?> clazz) {
      if (clazz != null) {
         if (Date.class.isAssignableFrom(clazz)) {
            try {
               // just check this type really has a constructor that accepts a long timestamp param
               clazz.getConstructor(Long.TYPE);
            } catch (NoSuchMethodException e) {
               throw new ProtoSchemaBuilderException("Type " + clazz + " is not a valid Date type because it does not have a constructor that accepts a 'long' timestamp parameter");
            }
            return "new " + clazz.getName() + "(" + v + ")";
         } else if (Instant.class.isAssignableFrom(clazz)) {
            return "java.time.Instant.ofEpochMilli(" + v + ")";
         } else if (clazz == Float.class) {
            return "new java.lang.Float(" + v + ")";
         } else if (clazz == Double.class) {
            return "new java.lang.Double(" + v + ")";
         } else if (clazz == Boolean.class) {
            return "new java.lang.Boolean(" + v + ")";
         } else if (clazz == Long.class) {
            return "new java.lang.Long(" + v + ")";
         } else if (clazz == Integer.class) {
            return "new java.lang.Integer(" + v + ")";
         } else if (clazz == Short.class) {
            return "new java.lang.Short(" + v + ")";
         } else if (clazz == Byte.class) {
            return "new java.lang.Byte(" + v + ")";
         } else if (clazz == Character.class) {
            return "new java.lang.Character(" + v + ")";
         }
      }
      return v;
   }

   private String unbox(String v, Class<?> clazz) {
      if (Date.class.isAssignableFrom(clazz)) {
         return v + ".getTime()";
      } else if (Instant.class.isAssignableFrom(clazz)) {
         return v + ".toEpochMilli()";
      } else if (clazz == Float.class) {
         return v + ".floatValue()";
      } else if (clazz == Double.class) {
         return v + ".doubleValue()";
      } else if (clazz == Boolean.class) {
         return v + ".booleanValue()";
      } else if (clazz == Long.class) {
         return v + ".longValue()";
      } else if (clazz == Integer.class) {
         return v + ".intValue()";
      } else if (clazz == Short.class) {
         return v + ".shortValue()";
      } else if (clazz == Byte.class) {
         return v + ".byteValue()";
      } else if (clazz == Character.class) {
         return v + ".charValue()";
      }
      return v;
   }

   private String createGetter(ProtoFieldMetadata fieldMetadata) {
      if (fieldMetadata.getField() != null) {
         return fieldMetadata.getField().getName();
      }
      return fieldMetadata.getGetter().getName() + "()";
   }

   private String createSetter(ProtoFieldMetadata fieldMetadata, String args) {
      if (fieldMetadata.getField() != null) {
         return fieldMetadata.getField().getName() + " = " + args;
      }
      return fieldMetadata.getSetter().getName() + "(" + args + ")";
   }
}
