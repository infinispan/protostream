package org.infinispan.protostream.annotations.impl;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.ProtobufTagMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.impl.types.XClass;
import org.infinispan.protostream.annotations.impl.types.XTypeFactory;
import org.infinispan.protostream.containers.IndexedElementContainerAdapter;
import org.infinispan.protostream.containers.IterableElementContainerAdapter;
import org.infinispan.protostream.impl.BaseMarshallerDelegate;
import org.infinispan.protostream.impl.EnumMarshallerDelegate;
import org.infinispan.protostream.impl.Log;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javassist.bytecode.ClassFile;

// TODO [anistor] detect situations when we generate an identical marshaller class to a previously generated one (and in the same classloader) and reuse it
// TODO [anistor] check which java classfile limits impose limits on the size of the supported Protobuf schema
// TODO [anistor] what do we do with non-repeated fields that come repeated from stream?
// TODO [anistor] bounded streams should be checked to be exactly as the size indicated

/**
 * Generates bytecode for implementation classes of {@link EnumMarshaller} and {@link ProtobufTagMarshaller}. This class
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
   private final CtClass protoStreamMarshallerInterface;
   private final CtClass indexedContainerAdapterInterface;
   private final CtClass iterableContainerAdapterInterface;
   private final CtClass generatedMarshallerBaseClass;
   private final CtClass baseMarshallerDelegateClass;
   private final CtClass enumMarshallerDelegateClass;
   private final CtMethod readMethod;
   private final CtMethod writeMethod;
   private final CtMethod decodeMethod;
   private final CtMethod encodeMethod;

   MarshallerByteCodeGenerator(XTypeFactory typeFactory, String protobufSchemaPackage, ClassPool cp) throws NotFoundException {
      super(typeFactory, protobufSchemaPackage);
      this.cp = cp;
      ioExceptionClass = cp.getCtClass(IOException.class.getName());
      enumMarshallerInterface = cp.getCtClass(EnumMarshaller.class.getName());
      protoStreamMarshallerInterface = cp.getCtClass(ProtobufTagMarshaller.class.getName());
      indexedContainerAdapterInterface = cp.getCtClass(IndexedElementContainerAdapter.class.getName());
      iterableContainerAdapterInterface = cp.getCtClass(IterableElementContainerAdapter.class.getName());
      generatedMarshallerBaseClass = cp.getCtClass(GeneratedMarshallerBase.class.getName());
      baseMarshallerDelegateClass = cp.getCtClass(BaseMarshallerDelegate.class.getName());
      enumMarshallerDelegateClass = cp.getCtClass(EnumMarshallerDelegate.class.getName());
      String readContextName = ProtobufTagMarshaller.ReadContext.class.getName().replace('.', '/');
      String writeContextName = ProtobufTagMarshaller.WriteContext.class.getName().replace('.', '/');
      readMethod = protoStreamMarshallerInterface.getMethod("read", "(L" + readContextName + ";)Ljava/lang/Object;");
      writeMethod = protoStreamMarshallerInterface.getMethod("write", "(L" + writeContextName + ";Ljava/lang/Object;)V");
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
      CtClass annotatedClass = cp.get(petm.getAnnotatedClass().getName());
      CtClass marshallerImpl = annotatedClass.makeNestedClass(marshallerClassName, true);
      if (log.isTraceEnabled()) {
         log.tracef("Generating enum marshaller %s for %s", marshallerImpl.getName(), petm.getJavaClass().getName());
      }
      marshallerImpl.addInterface(enumMarshallerInterface);
      marshallerImpl.setModifiers(marshallerImpl.getModifiers() & ~Modifier.ABSTRACT | Modifier.FINAL);

      marshallerImpl.addMethod(CtNewMethod.make("public final Class getJavaClass() { return " + petm.getJavaClassName() + ".class; }", marshallerImpl));
      marshallerImpl.addMethod(CtNewMethod.make("public final String getTypeName() { return \"" + makeQualifiedTypeName(petm.getFullName()) + "\"; }", marshallerImpl));

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

      Class<EnumMarshaller> generatedMarshallerClass = (Class<EnumMarshaller>) toClass(marshallerImpl, petm.getAnnotatedClass());
      marshallerImpl.detach();

      return generatedMarshallerClass;
   }

   private static Class<?> toClass(CtClass ctClass, XClass metadataClass) throws CannotCompileException {
      if (ClassFile.MAJOR_VERSION > ClassFile.JAVA_8) {
         // Can only use this method with Java 9 or newer and is required for Java 17
         return ctClass.toClass(metadataClass.getClass());
      }
      return ctClass.toClass();
   }

   /**
    * Generates an implementation of {@link ProtobufTagMarshaller} as a static nested class in the message class to be
    * marshalled. The InnerClasses attribute of the outer class is not altered, so this is not officially considered a
    * nested class.
    */
   private Class<ProtobufTagMarshaller> generateMessageMarshaller(ProtoMessageTypeMetadata pmtm) throws NotFoundException, CannotCompileException {
      String marshallerClassName = makeUniqueMarshallerClassName();
      CtClass annotatedClass = cp.get(pmtm.getAnnotatedClass().getName());
      CtClass marshallerImpl = annotatedClass.makeNestedClass(marshallerClassName, true);
      if (log.isTraceEnabled()) {
         log.tracef("Generating message marshaller %s for %s", marshallerImpl.getName(), pmtm.getJavaClass().getName());
      }
      marshallerImpl.addInterface(protoStreamMarshallerInterface);
      marshallerImpl.setSuperclass(generatedMarshallerBaseClass);
      marshallerImpl.setModifiers(marshallerImpl.getModifiers() & ~Modifier.ABSTRACT | Modifier.FINAL);

      if (pmtm.isAdapter()) {
         addAdapterField(marshallerImpl, pmtm);
      }

      if (pmtm.isIndexedContainer()) {
         marshallerImpl.addInterface(indexedContainerAdapterInterface);
         if (pmtm.isAdapter()) {
            marshallerImpl.addMethod(CtNewMethod.make("public final int getNumElements(java.lang.Object container) { return " + ADAPTER_FIELD_NAME + ".getNumElements(container); }", marshallerImpl));
            marshallerImpl.addMethod(CtNewMethod.make("public final java.lang.Object getElement(java.lang.Object container, int index) { return " + ADAPTER_FIELD_NAME + ".getElement(container, index); }", marshallerImpl));
            marshallerImpl.addMethod(CtNewMethod.make("public final void setElement(java.lang.Object container, int index, java.lang.Object element) { " + ADAPTER_FIELD_NAME + ".setElement(container, index, element); }", marshallerImpl));
         } else {
            marshallerImpl.addMethod(CtNewMethod.make("public final int getNumElements(java.lang.Object container) { return ((" + indexedContainerAdapterInterface.getName() + ") container).getNumElements(); }", marshallerImpl));
            marshallerImpl.addMethod(CtNewMethod.make("public final java.lang.Object getElement(java.lang.Object container, int index) { return ((" + indexedContainerAdapterInterface.getName() + ") container).getElement(index); }", marshallerImpl));
            marshallerImpl.addMethod(CtNewMethod.make("public final void setElement(java.lang.Object container, int index, java.lang.Object element) { ((" + indexedContainerAdapterInterface.getName() + ") container).setElement(index, element); }", marshallerImpl));
         }
      } else if (pmtm.isIterableContainer()) {
         marshallerImpl.addInterface(iterableContainerAdapterInterface);
         if (pmtm.isAdapter()) {
            marshallerImpl.addMethod(CtNewMethod.make("public final int getNumElements(java.lang.Object container) { return " + ADAPTER_FIELD_NAME + ".getNumElements(container); }", marshallerImpl));
            marshallerImpl.addMethod(CtNewMethod.make("public final java.util.Iterator getElements(java.lang.Object container) { return " + ADAPTER_FIELD_NAME + ".getElements(container); }", marshallerImpl));
            marshallerImpl.addMethod(CtNewMethod.make("public final void appendElement(java.lang.Object container, java.lang.Object element) { " + ADAPTER_FIELD_NAME + ".appendElement(container, element); }", marshallerImpl));
         } else {
            marshallerImpl.addMethod(CtNewMethod.make("public final int getNumElements(java.lang.Object container) { return ((" + iterableContainerAdapterInterface.getName() + ") container).getNumElements(); }", marshallerImpl));
            marshallerImpl.addMethod(CtNewMethod.make("public final java.util.Iterator getElements(java.lang.Object container) { return ((" + iterableContainerAdapterInterface.getName() + ") container).getElements(); }", marshallerImpl));
            marshallerImpl.addMethod(CtNewMethod.make("public final void appendElement(java.lang.Object container, java.lang.Object element) { ((" + iterableContainerAdapterInterface.getName() + ") container).appendElement(element); }", marshallerImpl));
         }
      }

      addMarshallerDelegateFields(marshallerImpl, pmtm);

      marshallerImpl.addMethod(CtNewMethod.make("public final Class getJavaClass() { return " + pmtm.getJavaClass().getCanonicalName() + ".class; }", marshallerImpl));
      marshallerImpl.addMethod(CtNewMethod.make("public final String getTypeName() { return \"" + makeQualifiedTypeName(pmtm.getFullName()) + "\"; }", marshallerImpl));

      CtMethod ctReadMethod = new CtMethod(readMethod, marshallerImpl, null);
      ctReadMethod.setExceptionTypes(new CtClass[]{ioExceptionClass});
      ctReadMethod.setModifiers(ctReadMethod.getModifiers() | Modifier.FINAL);
      String readBody = generateReadMethodBody(pmtm);
      if (log.isTraceEnabled()) {
         log.tracef("%s %s", ctReadMethod.getLongName(), readBody);
      }
      ctReadMethod.setBody(readBody);
      marshallerImpl.addMethod(ctReadMethod);

      CtMethod ctWriteMethod = new CtMethod(writeMethod, marshallerImpl, null);
      ctWriteMethod.setExceptionTypes(new CtClass[]{ioExceptionClass});
      ctWriteMethod.setModifiers(ctWriteMethod.getModifiers() | Modifier.FINAL);
      String writeBody = generateWriteMethodBody(pmtm);
      if (log.isTraceEnabled()) {
         log.tracef("%s %s", ctWriteMethod.getLongName(), writeBody);
      }
      ctWriteMethod.setBody(writeBody);
      marshallerImpl.addMethod(ctWriteMethod);

      Class<ProtobufTagMarshaller> generatedMarshallerClass = (Class<ProtobufTagMarshaller>) toClass(marshallerImpl, pmtm.getAnnotatedClass());
      marshallerImpl.detach();

      return generatedMarshallerClass;
   }

   private void addAdapterField(CtClass marshallerImpl, ProtoMessageTypeMetadata messageTypeMetadata) throws CannotCompileException, NotFoundException {
      CtClass adapterClass = cp.getCtClass(messageTypeMetadata.getAnnotatedClass().getName());
      CtField adapterField = new CtField(adapterClass, ADAPTER_FIELD_NAME, marshallerImpl);
      adapterField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
      marshallerImpl.addField(adapterField);

      marshallerImpl.addConstructor(CtNewConstructor.make("public " + marshallerImpl.getSimpleName() + "() { "
            + ADAPTER_FIELD_NAME + " = new " + adapterClass.getName() + "(); }", marshallerImpl));
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
