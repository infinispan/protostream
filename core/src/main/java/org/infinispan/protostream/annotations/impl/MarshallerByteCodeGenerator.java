package org.infinispan.protostream.annotations.impl;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.RawProtobufMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.impl.types.UnifiedTypeFactory;
import org.infinispan.protostream.impl.BaseMarshallerDelegate;
import org.infinispan.protostream.impl.EnumMarshallerDelegate;
import org.infinispan.protostream.impl.Log;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;

// TODO [anistor] detect situations when we generate an identical marshaller class to a previously generated one (and in the same classloader) and reuse it
// TODO [anistor] check which java classfile limits impose limits on the size of the supported Protobuf schema
// TODO [anistor] what do we do with non-repeated fields that come repeated from stream?
// TODO [anistor] bounded streams should be checked to be exactly as the size indicated

/**
 * Generates bytecode for implementation classes of {@link EnumMarshaller} and {@link RawProtobufMarshaller}. This class
 * relies heavily on javassist library (and should be the only place where javassist is used throughout this project).
 *
 * @author anistor@readhat.com
 * @since 3.0
 */
final class MarshallerByteCodeGenerator extends AbstractMarshallerCodeGenerator {

   private static final Log log = Log.LogFactory.getLog(MarshallerByteCodeGenerator.class);

   /**
    * The prefix of class names of generated marshallers.
    */
   private static final String MARSHALLER_CLASS_NAME_PREFIX = "___ProtostreamGeneratedMarshaller_";

   /**
    * A numeric id that is appended to generated class names to avoid potential collisions.
    */
   private static long nextId = 0;

   private final ClassPool cp;
   private final CtClass ioExceptionClass;
   private final CtClass enumMarshallerInterface;
   private final CtClass rawProtobufMarshallerInterface;
   private final CtClass generatedMarshallerBaseClass;
   private final CtClass baseMarshallerDelegateClass;
   private final CtClass enumMarshallerDelegateClass;
   private final CtMethod readFromMethod;
   private final CtMethod writeToMethod;
   private final CtMethod decodeMethod;
   private final CtMethod encodeMethod;

   MarshallerByteCodeGenerator(UnifiedTypeFactory typeFactory, String protobufSchemaPackage, ClassPool cp) throws NotFoundException {
      super(typeFactory, protobufSchemaPackage);
      this.cp = cp;
      ioExceptionClass = cp.getCtClass(IOException.class.getName());
      enumMarshallerInterface = cp.getCtClass(EnumMarshaller.class.getName());
      rawProtobufMarshallerInterface = cp.getCtClass(RawProtobufMarshaller.class.getName());
      generatedMarshallerBaseClass = cp.getCtClass(GeneratedMarshallerBase.class.getName());
      baseMarshallerDelegateClass = cp.getCtClass(BaseMarshallerDelegate.class.getName());
      enumMarshallerDelegateClass = cp.getCtClass(EnumMarshallerDelegate.class.getName());
      String rawProtobufInputStreamName = RawProtoStreamReader.class.getName().replace('.', '/');
      String rawProtobufOutputStreamName = RawProtoStreamWriter.class.getName().replace('.', '/');
      String serializationContextName = ImmutableSerializationContext.class.getName().replace('.', '/');
      readFromMethod = rawProtobufMarshallerInterface.getMethod("readFrom", "(L" + serializationContextName + ";L" + rawProtobufInputStreamName + ";)Ljava/lang/Object;");
      writeToMethod = rawProtobufMarshallerInterface.getMethod("writeTo", "(L" + serializationContextName + ";L" + rawProtobufOutputStreamName + ";Ljava/lang/Object;)V");
      decodeMethod = enumMarshallerInterface.getMethod("decode", "(I)Ljava/lang/Enum;");
      encodeMethod = enumMarshallerInterface.getMethod("encode", "(Ljava/lang/Enum;)I");
   }

   /**
    * Generates a unique numeric id to be used for generating unique class names.
    */
   private static synchronized long nextMarshallerClassId() {
      return nextId++;
   }

   private static String makeUniqueMarshallerClassName() {
      return MARSHALLER_CLASS_NAME_PREFIX + nextMarshallerClassId();
   }

   @Override
   public void generateMarshaller(SerializationContext serializationContext, ProtoTypeMetadata ptm) throws Exception {
      Class<? extends BaseMarshaller> marshallerClass = null;
      if (ptm instanceof ProtoMessageTypeMetadata) {
         marshallerClass = generateMessageMarshaller((ProtoMessageTypeMetadata) ptm);
      } else if (ptm instanceof ProtoEnumTypeMetadata) {
         marshallerClass = generateEnumMarshaller((ProtoEnumTypeMetadata) ptm);
      }
      if (marshallerClass != null) {
         BaseMarshaller marshaller = marshallerClass.newInstance();
         serializationContext.registerMarshaller(marshaller);
      }
   }

   /**
    * Generates an implementation of EnumMarshaller as a static nested class in the Enum class to be marshalled. The
    * InnerClasses attribute of the outer class is not altered, so this is not officially considered a nested class.
    */
   private Class<EnumMarshaller> generateEnumMarshaller(ProtoEnumTypeMetadata petm) throws NotFoundException, CannotCompileException {
      String marshallerClassName = makeUniqueMarshallerClassName();
      CtClass enumClass = cp.get(petm.getJavaClass().getName());
      CtClass marshallerImpl = enumClass.makeNestedClass(marshallerClassName, true);
      if (log.isTraceEnabled()) {
         log.tracef("Generating enum marshaller %s for %s", marshallerImpl.getName(), petm.getJavaClass().getName());
      }
      marshallerImpl.addInterface(enumMarshallerInterface);
      marshallerImpl.setModifiers(marshallerImpl.getModifiers() & ~Modifier.ABSTRACT | Modifier.FINAL);

      marshallerImpl.addMethod(CtMethod.make("public final Class getJavaClass() { return " + petm.getJavaClass().getName() + ".class; }", marshallerImpl));
      marshallerImpl.addMethod(CtMethod.make("public final String getTypeName() { return \"" + makeQualifiedTypeName(petm.getFullName()) + "\"; }", marshallerImpl));

      CtMethod ctDecodeMethod = new CtMethod(decodeMethod, marshallerImpl, null);
      ctDecodeMethod.setModifiers(ctDecodeMethod.getModifiers() | Modifier.FINAL);
      String decodeSrc = generateEnumDecodeMethodBody(petm);
      if (log.isTraceEnabled()) {
         log.tracef("%s %s", ctDecodeMethod.getLongName(), decodeSrc);
      }
      ctDecodeMethod.setBody(decodeSrc);
      marshallerImpl.addMethod(ctDecodeMethod);

      CtMethod ctEncodeMethod = new CtMethod(encodeMethod, marshallerImpl, null);
      ctEncodeMethod.setModifiers(ctEncodeMethod.getModifiers() | Modifier.FINAL);
      String encodeSrc = generateEnumEncodeMethodBody(petm);
      if (log.isTraceEnabled()) {
         log.tracef("%s %s", ctEncodeMethod.getLongName(), encodeSrc);
      }
      ctEncodeMethod.setBody(encodeSrc);
      marshallerImpl.addMethod(ctEncodeMethod);

      Class<EnumMarshaller> generatedMarshallerClass = (Class<EnumMarshaller>) marshallerImpl.toClass();
      marshallerImpl.detach();

      return generatedMarshallerClass;
   }

   /**
    * Generates an implementation of {@link RawProtobufMarshaller} as a static nested class in the message class to be
    * marshalled. The InnerClasses attribute of the outer class is not altered, so this is not officially considered a
    * nested class.
    */
   private Class<RawProtobufMarshaller> generateMessageMarshaller(ProtoMessageTypeMetadata pmtm) throws NotFoundException, CannotCompileException {
      String marshallerClassName = makeUniqueMarshallerClassName();
      CtClass entityClass = cp.get(pmtm.getJavaClass().getName());
      CtClass marshallerImpl = entityClass.makeNestedClass(marshallerClassName, true);
      if (log.isTraceEnabled()) {
         log.tracef("Generating message marshaller %s for %s", marshallerImpl.getName(), pmtm.getJavaClass().getName());
      }
      marshallerImpl.addInterface(rawProtobufMarshallerInterface);
      marshallerImpl.setSuperclass(generatedMarshallerBaseClass);
      marshallerImpl.setModifiers(marshallerImpl.getModifiers() & ~Modifier.ABSTRACT | Modifier.FINAL);

      addMarshallerDelegateFields(marshallerImpl, pmtm);

      marshallerImpl.addMethod(CtMethod.make("public final Class getJavaClass() { return " + pmtm.getJavaClass().getName() + ".class; }", marshallerImpl));
      marshallerImpl.addMethod(CtMethod.make("public final String getTypeName() { return \"" + makeQualifiedTypeName(pmtm.getFullName()) + "\"; }", marshallerImpl));

      CtMethod ctReadFromMethod = new CtMethod(readFromMethod, marshallerImpl, null);
      ctReadFromMethod.setExceptionTypes(new CtClass[]{ioExceptionClass});
      ctReadFromMethod.setModifiers(ctReadFromMethod.getModifiers() | Modifier.FINAL);
      String readFromSrc = generateReadFromMethodBody(pmtm);
      if (log.isTraceEnabled()) {
         log.tracef("%s %s", ctReadFromMethod.getLongName(), readFromSrc);
      }
      ctReadFromMethod.setBody(readFromSrc);
      marshallerImpl.addMethod(ctReadFromMethod);

      CtMethod ctWriteToMethod = new CtMethod(writeToMethod, marshallerImpl, null);
      ctWriteToMethod.setExceptionTypes(new CtClass[]{ioExceptionClass});
      ctWriteToMethod.setModifiers(ctWriteToMethod.getModifiers() | Modifier.FINAL);
      String writeToSrc = generateWriteToMethodBody(pmtm);
      if (log.isTraceEnabled()) {
         log.tracef("%s %s", ctWriteToMethod.getLongName(), writeToSrc);
      }
      ctWriteToMethod.setBody(writeToSrc);
      marshallerImpl.addMethod(ctWriteToMethod);

      Class<RawProtobufMarshaller> generatedMarshallerClass = (Class<RawProtobufMarshaller>) marshallerImpl.toClass();
      marshallerImpl.detach();

      return generatedMarshallerClass;
   }

   /**
    * Add fields used to cache delegates to other marshalled types (message or enum). These fields are lazily
    * initialized.
    */
   private void addMarshallerDelegateFields(CtClass marshallerImpl, ProtoMessageTypeMetadata messageTypeMetadata) throws CannotCompileException {
      Set<String> addedFields = new HashSet<>();
      for (ProtoFieldMetadata fieldMetadata : messageTypeMetadata.getFields().values()) {
         switch (fieldMetadata.getProtobufType()) {
            case GROUP:
            case MESSAGE:
            case ENUM:
               String fieldName = makeMarshallerDelegateFieldName(fieldMetadata);
               // Add the field only if it does not already exist. If there is more than one usage of a marshaller then we could try to add it twice.
               if (addedFields.add(fieldName)) {
                  CtField marshallerDelegateField = new CtField(fieldMetadata.getJavaType().isEnum() ? enumMarshallerDelegateClass : baseMarshallerDelegateClass, fieldName, marshallerImpl);
                  marshallerDelegateField.setModifiers(Modifier.PRIVATE);
                  marshallerImpl.addField(marshallerDelegateField);
               }
         }
      }
   }
}
