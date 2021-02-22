package org.infinispan.protostream.annotations.impl.processor;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.ProtoStreamMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.impl.AbstractMarshallerCodeGenerator;
import org.infinispan.protostream.annotations.impl.GeneratedMarshallerBase;
import org.infinispan.protostream.annotations.impl.IndentWriter;
import org.infinispan.protostream.annotations.impl.ProtoEnumTypeMetadata;
import org.infinispan.protostream.annotations.impl.ProtoFieldMetadata;
import org.infinispan.protostream.annotations.impl.ProtoMessageTypeMetadata;
import org.infinispan.protostream.annotations.impl.ProtoTypeMetadata;
import org.infinispan.protostream.annotations.impl.processor.types.HasModelElement;
import org.infinispan.protostream.annotations.impl.types.XTypeFactory;
import org.infinispan.protostream.impl.BaseMarshallerDelegate;
import org.infinispan.protostream.impl.EnumMarshallerDelegate;
import org.infinispan.protostream.impl.Log;

/**
 * Generates almost identical code as MarshallerByteCodeGenerator but it generates it in compilable Java source form
 * rather than directly in bytecode. Also, the syntax is not limited to the subset supported by javassist.
 *
 * @author anistor@readhat.com
 * @since 4.3
 */
final class MarshallerSourceCodeGenerator extends AbstractMarshallerCodeGenerator {

   private static final Log log = Log.LogFactory.getLog(MarshallerSourceCodeGenerator.class);

   private static final String DIGEST_ALG = "SHA-256";

   private final GeneratedFilesWriter generatedFilesWriter;

   private final Set<String> generatedClasses = new LinkedHashSet<>();

   MarshallerSourceCodeGenerator(GeneratedFilesWriter generatedFilesWriter, XTypeFactory typeFactory, String protobufSchemaPackage) {
      super(typeFactory, protobufSchemaPackage);
      this.generatedFilesWriter = generatedFilesWriter;
   }

   @Override
   public void generateMarshaller(SerializationContext serCtx, ProtoTypeMetadata ptm) throws IOException {
      if (ptm instanceof ProtoMessageTypeMetadata) {
         generateMessageMarshaller((ProtoMessageTypeMetadata) ptm);
      } else if (ptm instanceof ProtoEnumTypeMetadata) {
         generateEnumMarshaller((ProtoEnumTypeMetadata) ptm);
      }
   }

   private String makeUniqueMarshallerClassName(ProtoTypeMetadata ptm) {
      String hash = hashStrings(ptm.getAnnotatedClass().getName(), makeQualifiedTypeName(ptm.getFullName()));
      return ptm.getAnnotatedClass().getSimpleName() + "$___Marshaller_" + hash;
   }

   /**
    * Computes the SHA-256 hash over the input strings and returns the resulting digest encoded as a base 16 integer
    * from which all leading zeroes are stripped down.
    */
   private static String hashStrings(String... strings) {
      try {
         MessageDigest md = MessageDigest.getInstance(DIGEST_ALG);
         for (int i = 0; i < strings.length; i++) {
            if (i > 0) {
               // add a null separator between strings
               md.update((byte) 0);
            }
            byte[] bytes = strings[i].getBytes(StandardCharsets.UTF_8);
            md.update(bytes);
         }
         byte[] digest = md.digest();
         return new BigInteger(1, digest).toString(16);
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException("Failed to compute " + DIGEST_ALG + " digest of strings", e);
      }
   }

   private void generateEnumMarshaller(ProtoEnumTypeMetadata petm) throws IOException {
      String marshallerClassName = makeUniqueMarshallerClassName(petm);
      if (log.isTraceEnabled()) {
         log.tracef("Generating enum marshaller %s for %s", marshallerClassName, petm.getJavaClass().getName());
      }

      IndentWriter iw = new IndentWriter();
      addFileHeader(iw, petm.getAnnotatedClassName());

      String fqn;
      if (petm.getAnnotatedClass().getPackageName() != null) {
         fqn = petm.getAnnotatedClass().getPackageName() + '.' + marshallerClassName;
         iw.append("package ").append(petm.getAnnotatedClass().getPackageName()).append(";\n\n");
      } else {
         fqn = marshallerClassName;
      }

      if (petm.getJavaClass().getPackageName() != null) {
         iw.append("import ").append(petm.getJavaClassName()).append(";\n\n");
      }

      iw.append("public final class ").append(marshallerClassName)
            .append(" implements ").append(EnumMarshaller.class.getName()).append('<').append(petm.getJavaClassName()).append("> {\n\n");
      iw.inc();

      iw.append("@Override\npublic Class<").append(petm.getJavaClassName()).append("> getJavaClass() { return ").append(petm.getJavaClassName()).append(".class; }\n\n");

      iw.append("@Override\npublic String getTypeName() { return \"").append(makeQualifiedTypeName(petm.getFullName())).append("\"; }\n\n");

      String decodeSrc = generateEnumDecodeMethodBody(petm);
      String decodeSig = "public " + petm.getJavaClassName() + " decode(int $1)";
      if (log.isTraceEnabled()) {
         log.tracef("%s %s", decodeSig, decodeSrc);
      }
      iw.append("@Override\n").append(decodeSig).append(' ').append(decodeSrc).append('\n');

      String encodeSrc = generateEnumEncodeMethodBody(petm);
      String encodeSig = "public int encode(" + petm.getJavaClassName() + " $1) throws IllegalArgumentException";
      if (log.isTraceEnabled()) {
         log.tracef("%s %s", encodeSig, encodeSrc);
      }
      iw.append("@Override\n").append(encodeSig).append(' ').append(encodeSrc);

      iw.dec();
      iw.append("}\n");

      emitSource(fqn, iw.toString(), petm);
   }

   private void addFileHeader(IndentWriter iw, String className) {
      iw.append("/*\n");
      iw.append(" Generated by ").append(getClass().getName()).append("\n");
      iw.append(" for class ").append(className).append("\n");
      iw.append("*/\n\n");
   }

   private void generateMessageMarshaller(ProtoMessageTypeMetadata pmtm) throws IOException {
      String marshallerClassName = makeUniqueMarshallerClassName(pmtm);
      if (log.isTraceEnabled()) {
         log.tracef("Generating message marshaller %s for %s", marshallerClassName, pmtm.getJavaClass().getName());
      }

      IndentWriter iw = new IndentWriter();
      addFileHeader(iw, pmtm.getAnnotatedClassName());

      String fqn;
      if (pmtm.getAnnotatedClass().getPackageName() != null) {
         fqn = pmtm.getAnnotatedClass().getPackageName() + '.' + marshallerClassName;
         iw.append("package ").append(pmtm.getAnnotatedClass().getPackageName()).append(";\n\n");
      } else {
         fqn = marshallerClassName;
      }

      if (pmtm.getJavaClass().getPackageName() != null) {
         iw.append("import ").append(pmtm.getJavaClassName()).append(";\n\n");
      }

      AutoProtoSchemaBuilderAnnotationProcessor.addGeneratedClassHeader(iw, true);
      iw.append("@SuppressWarnings(\"all\")\n");
      iw.append("public final class ").append(marshallerClassName)
            .append(" extends ").append(GeneratedMarshallerBase.class.getName())
            .append(" implements ").append(ProtoStreamMarshaller.class.getName()).append('<').append(pmtm.getJavaClassName()).append('>');
      iw.append(" {\n\n");
      iw.inc();

      if (pmtm.isAdapter()) {
         addAdapterField(iw, pmtm);
      }

      if (pmtm.isIndexedContainer()) {
         //iw.append(", ").append(IndexedContainerAdapter.class.getName()).append('<').append(pmtm.getJavaClassName()).append('>');
      } else if (pmtm.isIterableContainer()) {
         //iw.append(", ").append(IndexedContainerAdapter.class.getName()).append('<').append(pmtm.getJavaClassName()).append('>');
      }

      addMarshallerDelegateFields(iw, pmtm);

      iw.append("@Override\npublic Class<").append(pmtm.getJavaClassName()).append("> getJavaClass() { return ").append(pmtm.getJavaClassName()).append(".class; }\n\n");

      iw.append("@Override\npublic String getTypeName() { return \"").append(makeQualifiedTypeName(pmtm.getFullName())).append("\"; }\n\n");

      if (pmtm.isIndexedContainer()) {
         //iw.append("@Override\npublic int getContainerField() { return 1; }\n\n");
      } else if (pmtm.isIterableContainer()) {
         //iw.append("@Override\npublic int getContainerField() { return 1; }\n\n");
      }

      String readMethodSrc = generateReadMethodBody(pmtm);
      String readMethodSig = "public " + pmtm.getJavaClassName() + " read("
            + ProtoStreamMarshaller.ReadContext.class.getCanonicalName() + " $1) throws java.io.IOException";
      if (log.isTraceEnabled()) {
         log.tracef("%s %s", readMethodSig, readMethodSrc);
      }
      iw.append("@Override\n").append(readMethodSig).append(' ').append(readMethodSrc).append('\n');

      String writeMethodSrc = generateWriteMethodBody(pmtm);
      String writeMethodSig = "public void write("
            + ProtoStreamMarshaller.WriteContext.class.getCanonicalName() + " $1, "
            + pmtm.getJavaClassName() + " $2) throws java.io.IOException";
      if (log.isTraceEnabled()) {
         log.tracef("%s %s", writeMethodSig, writeMethodSrc);
      }
      iw.append("@Override\n").append(writeMethodSig).append(' ').append(writeMethodSrc);

      iw.dec();
      iw.append("}\n");

      emitSource(fqn, iw.toString(), pmtm);
   }

   private void addAdapterField(IndentWriter iw, ProtoMessageTypeMetadata messageTypeMetadata) {
      iw.append("private final ").append(messageTypeMetadata.getAnnotatedClassName()).append(' ')
            .append(ADAPTER_FIELD_NAME).append(" = new ")
            .append(messageTypeMetadata.getAnnotatedClassName()).append("();\n\n");
   }

   /**
    * Add fields used to cache delegates to other marshalled types (message or enum). These fields are lazily
    * initialized.
    */
   private void addMarshallerDelegateFields(IndentWriter iw, ProtoMessageTypeMetadata messageTypeMetadata) {
      Set<String> addedFields = new HashSet<>();
      for (ProtoFieldMetadata fieldMetadata : messageTypeMetadata.getFields().values()) {
         switch (fieldMetadata.getProtobufType()) {
            case GROUP:
            case MESSAGE:
            case ENUM:
               String fieldName = makeMarshallerDelegateFieldName(fieldMetadata);
               // add the field only if it does not already exist. if there is more than one usage of a marshaller then we could try to add it twice
               if (addedFields.add(fieldName)) {
                  Class<?> marshallerDelegateClass = fieldMetadata.getJavaType().isEnum() ? EnumMarshallerDelegate.class : BaseMarshallerDelegate.class;
                  iw.append("private ").append(marshallerDelegateClass.getName()).append(' ').append(fieldName).append(";\n\n");
               }
         }
      }
   }

   private void emitSource(String fqn, String source, ProtoTypeMetadata ptm) throws IOException {
      generatedFilesWriter.addMarshallerSourceFile(fqn, source, ((HasModelElement) ptm.getJavaClass()).getElement());
      generatedClasses.add(fqn);
   }

   public Set<String> getGeneratedClasses() {
      return generatedClasses;
   }
}
