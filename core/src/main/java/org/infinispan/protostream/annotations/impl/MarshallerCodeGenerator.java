package org.infinispan.protostream.annotations.impl;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.Message;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.RawProtobufMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.descriptors.JavaType;
import org.infinispan.protostream.descriptors.Type;
import org.infinispan.protostream.impl.Log;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

// TODO [anistor] check which java classfile limits impose limits on the size of the supported protobuf schema
// TODO [anistor] what do we do with non-repeated fields that come repeated from stream?
// TODO [anistor] bounded streams should be checked to be exactly as the size indicated
// TODO [anistor] implement extensions registry?, MessageSet support?, packed fields?

/**
 * @author anistor@readhat.com
 * @since 3.0
 */
final class MarshallerCodeGenerator {

   private static final Log log = Log.LogFactory.getLog(MarshallerCodeGenerator.class);

   private static final String PROTOSTREAM_PACKAGE = SerializationContext.class.getPackage().getName();

   private static final String MARSHALLER_CLASS_NAME = "___ProtostreamGeneratedMarshaller";

   private static long nextId = 0;

   private static final Map<Type, String> protoTypeToFriendlyName = new HashMap<Type, String>();

   static {
      protoTypeToFriendlyName.put(Type.DOUBLE, "Double");
      protoTypeToFriendlyName.put(Type.FLOAT, "Float");
      protoTypeToFriendlyName.put(Type.INT64, "Int64");
      protoTypeToFriendlyName.put(Type.UINT64, "UInt64");
      protoTypeToFriendlyName.put(Type.INT32, "Int32");
      protoTypeToFriendlyName.put(Type.FIXED64, "Fixed64");
      protoTypeToFriendlyName.put(Type.FIXED32, "Fixed32");
      protoTypeToFriendlyName.put(Type.BOOL, "Bool");
      protoTypeToFriendlyName.put(Type.STRING, "String");
      protoTypeToFriendlyName.put(Type.GROUP, "Group");
      protoTypeToFriendlyName.put(Type.MESSAGE, "Message");
      protoTypeToFriendlyName.put(Type.BYTES, "Bytes");
      protoTypeToFriendlyName.put(Type.UINT32, "UInt32");
      protoTypeToFriendlyName.put(Type.ENUM, "Enum");
      protoTypeToFriendlyName.put(Type.SFIXED32, "SFixed32");
      protoTypeToFriendlyName.put(Type.SFIXED64, "SFixed64");
      protoTypeToFriendlyName.put(Type.SINT32, "SInt32");
      protoTypeToFriendlyName.put(Type.SINT64, "SInt64");
   }

   private final ClassPool cp;
   private final CtClass ioException;
   private final CtClass enumMarshallerInterface;
   private final CtClass rawProtobufMarshallerInterface;
   private final CtClass generatedMarshallerBaseClass;
   private final CtMethod getJavaClassMethod;
   private final CtMethod getTypeNameMethod;
   private final CtMethod readFromMethod;
   private final CtMethod writeToMethod;
   private final CtMethod decodeMethod;
   private final CtMethod encodeMethod;
   private final String protobufPackage;

   public MarshallerCodeGenerator(String protobufPackage, ClassPool cp) throws NotFoundException {
      this.protobufPackage = protobufPackage;
      this.cp = cp;
      ioException = cp.getCtClass(IOException.class.getName());
      enumMarshallerInterface = cp.getCtClass(EnumMarshaller.class.getName());
      rawProtobufMarshallerInterface = cp.getCtClass(RawProtobufMarshaller.class.getName());
      generatedMarshallerBaseClass = cp.getCtClass(GeneratedMarshallerBase.class.getName());
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
      ctGetTypeNameMethod.setBody("{ return \"" + makeQualifiedName(petm.getFullName()) + "\"; }");
      marshallerImpl.addMethod(ctGetTypeNameMethod);

      CtMethod ctDecodeMethod = new CtMethod(decodeMethod, marshallerImpl, null);
      ctDecodeMethod.setModifiers(ctDecodeMethod.getModifiers() | Modifier.FINAL);
      String decodeSrc = generateDecodeMethod(petm);
      log.tracef("%s %s", ctDecodeMethod, decodeSrc);
      ctDecodeMethod.setBody(decodeSrc);
      marshallerImpl.addMethod(ctDecodeMethod);

      CtMethod ctEncodeMethod = new CtMethod(encodeMethod, marshallerImpl, null);
      ctEncodeMethod.setModifiers(ctEncodeMethod.getModifiers() | Modifier.FINAL);
      String encodeSrc = generateEncodeMethod(petm);
      log.tracef("%s %s", ctEncodeMethod, encodeSrc);
      ctEncodeMethod.setBody(encodeSrc);
      marshallerImpl.addMethod(ctEncodeMethod);

      marshallerImpl.setModifiers(marshallerImpl.getModifiers() & ~Modifier.ABSTRACT | Modifier.FINAL);

      EnumMarshaller marshallerInstance = (EnumMarshaller) marshallerImpl.toClass().newInstance();
      marshallerImpl.detach();
      return marshallerInstance;
   }

   private String generateDecodeMethod(ProtoEnumTypeMetadata enumTypeMetadata) {
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

   private String generateEncodeMethod(ProtoEnumTypeMetadata enumTypeMetadata) {
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

   private String makeQualifiedName(String fullName) {
      if (protobufPackage != null) {
         return protobufPackage + "." + fullName;
      }
      return fullName;
   }

   public RawProtobufMarshaller generateMessageMarshaller(ProtoMessageTypeMetadata messageTypeMetadata) throws NotFoundException, CannotCompileException, IllegalAccessException, InstantiationException {
      CtClass entityClass = cp.get(messageTypeMetadata.getJavaClass().getName());
      CtClass marshallerImpl = entityClass.makeNestedClass(MARSHALLER_CLASS_NAME + nextMarshallerClassId(), true);
      marshallerImpl.addInterface(rawProtobufMarshallerInterface);
      marshallerImpl.setSuperclass(generatedMarshallerBaseClass);

      CtMethod ctGetJavaClassMethod = new CtMethod(getJavaClassMethod, marshallerImpl, null);
      ctGetJavaClassMethod.setModifiers(ctGetJavaClassMethod.getModifiers() | Modifier.FINAL);
      ctGetJavaClassMethod.setBody("{ return " + entityClass.getName() + ".class; }");
      marshallerImpl.addMethod(ctGetJavaClassMethod);

      CtMethod ctGetTypeNameMethod = new CtMethod(getTypeNameMethod, marshallerImpl, null);
      ctGetTypeNameMethod.setModifiers(ctGetTypeNameMethod.getModifiers() | Modifier.FINAL);
      ctGetTypeNameMethod.setBody("{ return \"" + makeQualifiedName(messageTypeMetadata.getFullName()) + "\"; }");
      marshallerImpl.addMethod(ctGetTypeNameMethod);

      CtMethod ctReadFromMethod = new CtMethod(readFromMethod, marshallerImpl, null);
      ctGetTypeNameMethod.setExceptionTypes(new CtClass[]{ioException});
      ctReadFromMethod.setModifiers(ctReadFromMethod.getModifiers() | Modifier.FINAL);
      String readFromSrc = generateReadFromMethod(messageTypeMetadata);
      log.tracef("%s %s", ctReadFromMethod, readFromSrc);
      ctReadFromMethod.setBody(readFromSrc);
      marshallerImpl.addMethod(ctReadFromMethod);

      CtMethod ctWriteToMethod = new CtMethod(writeToMethod, marshallerImpl, null);
      ctWriteToMethod.setExceptionTypes(new CtClass[]{ioException});
      ctWriteToMethod.setModifiers(ctWriteToMethod.getModifiers() | Modifier.FINAL);
      String writeToSrc = generateWriteToMethod(messageTypeMetadata);
      log.tracef("%s %s", ctWriteToMethod, writeToSrc);
      ctWriteToMethod.setBody(writeToSrc);
      marshallerImpl.addMethod(ctWriteToMethod);

      marshallerImpl.setModifiers(marshallerImpl.getModifiers() & ~Modifier.ABSTRACT | Modifier.FINAL);

      RawProtobufMarshaller marshallerInstance = (RawProtobufMarshaller) marshallerImpl.toClass().newInstance();
      marshallerImpl.detach();
      return marshallerInstance;
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
            iw.append("boolean ").append(fieldMetadata.getName()).append("WasSet = false;\n");
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
               iw.append(fieldMetadata.getJavaType().getName()).append(" v = ").append(box("$2.read" + protoTypeToFriendlyName.get(fieldMetadata.getProtobufType()) + "()", fieldMetadata.getJavaType())).append(";\n");
               genSetField(fieldMetadata, iw);
               iw.dec();
               iw.append("}\n");
               break;
            case GROUP:
               iw.append("{\n");
               iw.inc();
               iw.append(fieldMetadata.getJavaType().getName()).append(" v = (").append(fieldMetadata.getJavaType().getName()).append(") readMessage($1, $2, ").append(fieldMetadata.getJavaType().getName()).append(".class);\n");
               iw.append("$2.checkLastTagWas(").append(String.valueOf(fieldMetadata.getNumber() << 3 | org.infinispan.protostream.impl.WireFormat.WIRETYPE_END_GROUP)).append(");\n");
               genSetField(fieldMetadata, iw);
               iw.dec();
               iw.append("}\n");
               break;
            case MESSAGE:
               iw.append("{\n");
               iw.inc();
               iw.append("int length = $2.readRawVarint32();\n");
               iw.append("int oldLimit = $2.pushLimit(length);\n");
               iw.append(fieldMetadata.getJavaType().getName()).append(" v = (").append(fieldMetadata.getJavaType().getName()).append(") readMessage($1, $2, ").append(fieldMetadata.getJavaType().getName()).append(".class);\n");
               iw.append("$2.checkLastTagWas(0);\n");
               iw.append("$2.popLimit(oldLimit);\n");
               genSetField(fieldMetadata, iw);
               iw.dec();
               iw.append("}\n");
               break;
            case ENUM:
               iw.append("{\n");
               iw.inc();
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
               genSetField(fieldMetadata, iw);
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
         if (fieldMetadata.getDefaultValue() != null) {
            iw.append("if (!").append(fieldMetadata.getName()).append("WasSet) {\n");
            iw.inc();
            String v;
            if (fieldMetadata.getDefaultValue() instanceof ProtoEnumValueMetadata) {
               Enum enumValue = ((ProtoEnumValueMetadata) fieldMetadata.getDefaultValue()).getEnumValue();
               v = enumValue.getDeclaringClass().getName() + "." + enumValue.name();
            } else {
               v = fieldMetadata.getDefaultValue().toString();
            }
            if (fieldMetadata.isRepeated()) {
               iw.append("java.util.List c = o.").append(createGetter(fieldMetadata)).append(";\n");
               iw.append("if (c == null) { c = new ").append(fieldMetadata.getCollectionImplementation().getName()).append("(); o.").append(createSetter(fieldMetadata, "c")).append("; }\n");
               iw.append("c.add(").append(v).append(");\n");
            } else {
               iw.append("o.").append(createSetter(fieldMetadata, v)).append(";\n");
            }
            iw.dec();
            iw.append("}\n");
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
               iw.append(fieldMetadata.getName()).append("WasSet");
            }
         }
         iw.append("))\n{\n");
         iw.inc();
         iw.append("java.util.List missingFields = new java.util.ArrayList(").append(String.valueOf(requiredFields)).append(");\n");
         for (ProtoFieldMetadata fieldMetadata : messageTypeMetadata.getFields().values()) {
            if (fieldMetadata.isRequired()) {
               iw.append("if (!").append(fieldMetadata.getName()).append("WasSet) missingFields.add(\"").append(fieldMetadata.getName()).append("\");\n");
            }
         }
         iw.append("if (!missingFields.isEmpty()) throw new java.io.IOException(\"Required field(s) missing : \" + missingFields);\n");
         iw.dec();
         iw.append("}\n");
      }
      iw.append("return o;\n");
      iw.dec();
      iw.append("}\n");
      return iw.toString();
   }

   private void genSetField(ProtoFieldMetadata fieldMetadata, IndentWriter iw) {
      if (fieldMetadata.isRepeated()) {
         iw.append("java.util.List c = o.").append(createGetter(fieldMetadata)).append(";\n");
         iw.append("if (c == null) { c = new ").append(fieldMetadata.getCollectionImplementation().getName()).append("(); o.").append(createSetter(fieldMetadata, "c")).append("; }\n");
         iw.append("c.add(v);\n");
      } else {
         iw.append("o.").append(createSetter(fieldMetadata, "v")).append(";\n");
      }
      if (fieldMetadata.isRequired()) {
         iw.append(fieldMetadata.getName()).append("WasSet = true;\n");
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
         if (fieldMetadata.isRepeated()) {
            iw.append("java.util.List");
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
            if (!fieldMetadata.getJavaType().isPrimitive()) {
               iw.append("if (").append(v).append(" != null) ");
            }
         }
         if (fieldMetadata.isRepeated()) {
            iw.append('\n');
            iw.inc();
            iw.append("for (java.util.Iterator it = c.iterator(); it.hasNext(); ) {\n");
            iw.inc();
            iw.append(fieldMetadata.getJavaType().getName()).append(" v = (").append(fieldMetadata.getJavaType().getName()).append(") it.next();\n");
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
               iw.append("$2.write").append(protoTypeToFriendlyName.get(fieldMetadata.getProtobufType())).append("(").append(String.valueOf(fieldMetadata.getNumber())).append(", ").append(unbox("v", fieldMetadata.getJavaType())).append(");\n");
               break;
            case GROUP:
               iw.append("{\n");
               iw.inc();
               iw.append("$2.writeTag(").append(String.valueOf(fieldMetadata.getNumber())).append(", ").append(PROTOSTREAM_PACKAGE).append(".impl.WireFormat.WIRETYPE_START_GROUP);\n");
               iw.append("writeMessage($1, $2, ").append(fieldMetadata.getJavaType().getName()).append(".class, v);\n");
               iw.append("$2.writeTag(").append(String.valueOf(fieldMetadata.getNumber())).append(", ").append(PROTOSTREAM_PACKAGE).append(".impl.WireFormat.WIRETYPE_END_GROUP);\n");
               iw.dec();
               iw.append("}\n");
               break;
            case MESSAGE:
               iw.append("{\n");
               iw.inc();
               iw.append("writeNestedMessage($1, $2, ").append(fieldMetadata.getJavaType().getName()).append(".class, ").append(String.valueOf(fieldMetadata.getNumber())).append(", v);\n");
               iw.dec();
               iw.append("}\n");
               break;
            case ENUM:
               iw.append("$2.writeEnum(").append(String.valueOf(fieldMetadata.getNumber())).append(", ((").append(PROTOSTREAM_PACKAGE).append(".EnumMarshaller) $1.getMarshaller(").append(fieldMetadata.getJavaType().getName()).append(".class)).encode(v));\n");
               break;
            default:
               throw new IllegalStateException("Unknown field type " + fieldMetadata.getProtobufType());
         }
         if (fieldMetadata.isRepeated()) {
            iw.dec();
            iw.append("}\n");
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

   private String box(String v, Class<?> clazz) {
      if (clazz == Float.class) {
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
      }
      return v;
   }

   private String unbox(String v, Class<?> clazz) {
      if (clazz == Float.class) {
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
