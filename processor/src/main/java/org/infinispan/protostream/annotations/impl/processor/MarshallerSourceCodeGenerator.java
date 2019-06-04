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
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.RawProtobufMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.impl.AbstractMarshallerCodeGenerator;
import org.infinispan.protostream.annotations.impl.GeneratedMarshallerBase;
import org.infinispan.protostream.annotations.impl.IndentWriter;
import org.infinispan.protostream.annotations.impl.ProtoEnumTypeMetadata;
import org.infinispan.protostream.annotations.impl.ProtoFieldMetadata;
import org.infinispan.protostream.annotations.impl.ProtoMessageTypeMetadata;
import org.infinispan.protostream.annotations.impl.ProtoTypeMetadata;
import org.infinispan.protostream.annotations.impl.processor.types.HasModelElement;
import org.infinispan.protostream.annotations.impl.types.UnifiedTypeFactory;
import org.infinispan.protostream.impl.BaseMarshallerDelegate;
import org.infinispan.protostream.impl.EnumMarshallerDelegate;
import org.infinispan.protostream.impl.Log;

/**
 * Generates almost identical code as MarshallerByteCodeGenerator but it generates it in compilable Java source form
 * rather than directly in bytecode.
 *
 * @author anistor@readhat.com
 * @since 4.3
 */
final class MarshallerSourceCodeGenerator extends AbstractMarshallerCodeGenerator {

   private static final Log log = Log.LogFactory.getLog(MarshallerSourceCodeGenerator.class);

   private final GeneratedFilesWriter generatedFilesWriter;

   private final Set<String> generatedClasses = new LinkedHashSet<>();

   MarshallerSourceCodeGenerator(GeneratedFilesWriter generatedFilesWriter, UnifiedTypeFactory typeFactory, String protobufSchemaPackage) {
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
      String hash = hashStrings(ptm.getJavaClass().getName(), makeQualifiedTypeName(ptm.getFullName()));
      return ptm.getJavaClass().getSimpleName() + "$___Marshaller" + hash;
   }

   /**
    * Computes the SHA-1 hash over the input strings and returns the result encoded as a base 16 integer with all
    * leading zeroes stripped down.
    */
   private static String hashStrings(String... strings) {
      try {
         MessageDigest md = MessageDigest.getInstance("SHA-1");
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
         throw new RuntimeException("Failed to compute SHA-1 digest of strings", e);
      }
   }

   private void generateEnumMarshaller(ProtoEnumTypeMetadata petm) throws IOException {
      String marshallerClassName = makeUniqueMarshallerClassName(petm);
      if (log.isTraceEnabled()) {
         log.tracef("Generating enum marshaller %s for %s", marshallerClassName, petm.getJavaClass().getName());
      }

      IndentWriter iw = new IndentWriter();
      String fqn;
      if (petm.getJavaClass().getPackageName() != null) {
         fqn = petm.getJavaClass().getPackageName() + '.' + marshallerClassName;
         iw.append("package ").append(petm.getJavaClass().getPackageName()).append(";\n\n");
      } else {
         fqn = marshallerClassName;
      }
      iw.append("public final class ").append(marshallerClassName)
            .append(" implements ").append(EnumMarshaller.class.getName()).append('<').append(petm.getJavaClassName()).append("> {\n\n");
      iw.inc();

      iw.append("@Override\npublic Class<").append(petm.getJavaClassName()).append("> getJavaClass() { return ").append(petm.getJavaClassName()).append(".class; }\n\n");

      iw.append("@Override\npublic String getTypeName() { return \"").append(makeQualifiedTypeName(petm.getFullName())).append("\"; }\n\n");

      String decodeSrc = generateEnumDecodeMethod(petm);
      String decodeSig = "public " + petm.getJavaClassName() + " decode(int $1)";
      if (log.isTraceEnabled()) {
         log.tracef("%s %s", decodeSig, decodeSrc);
      }
      iw.append("@Override\n").append(decodeSig).append(' ').append(decodeSrc).append('\n');

      String encodeSrc = generateEnumEncodeMethod(petm);
      String encodeSig = "public int encode(" + petm.getJavaClassName() + " $1) throws IllegalArgumentException";
      if (log.isTraceEnabled()) {
         log.tracef("%s %s", encodeSig, encodeSrc);
      }
      iw.append("@Override\n").append(encodeSig).append(' ').append(encodeSrc);

      iw.dec();
      iw.append("}\n");

      emitSource(fqn, iw.toString(), petm);
   }

   private void generateMessageMarshaller(ProtoMessageTypeMetadata pmtm) throws IOException {
      String marshallerClassName = makeUniqueMarshallerClassName(pmtm);
      if (log.isTraceEnabled()) {
         log.tracef("Generating message marshaller %s for %s", marshallerClassName, pmtm.getJavaClass().getName());
      }

      IndentWriter iw = new IndentWriter();
      String fqn;
      if (pmtm.getJavaClass().getPackageName() != null) {
         fqn = pmtm.getJavaClass().getPackageName() + '.' + marshallerClassName;
         iw.append("package ").append(pmtm.getJavaClass().getPackageName()).append(";\n\n");
      } else {
         fqn = marshallerClassName;
      }
      AutoProtoSchemaBuilderAnnotationProcessor.addGeneratedAnnotation(iw, pmtm.getJavaClassName());
      iw.append("@SuppressWarnings(\"unchecked\")\n");
      iw.append("public final class ").append(marshallerClassName)
            .append(" extends ").append(GeneratedMarshallerBase.class.getName())
            .append(" implements ").append(RawProtobufMarshaller.class.getName()).append('<').append(pmtm.getJavaClassName()).append('>')
            .append(" {\n\n");
      iw.inc();

      addMarshallerDelegateFields(iw, pmtm);

      iw.append("@Override\npublic Class<").append(pmtm.getJavaClassName()).append("> getJavaClass() { return ").append(pmtm.getJavaClassName()).append(".class; }\n\n");

      iw.append("@Override\npublic String getTypeName() { return \"").append(makeQualifiedTypeName(pmtm.getFullName())).append("\"; }\n\n");

      String readFromSrc = generateReadFromMethod(pmtm);
      String readFromSig = "public " + pmtm.getJavaClassName() + " readFrom("
            + ImmutableSerializationContext.class.getName() + " $1, "
            + RawProtoStreamReader.class.getName() + " $2) throws java.io.IOException";
      if (log.isTraceEnabled()) {
         log.tracef("%s %s", readFromSig, readFromSrc);
      }
      iw.append("@Override\n").append(readFromSig).append(' ').append(readFromSrc).append('\n');

      String writeToSrc = generateWriteToMethod(pmtm);
      String writeToSig = "public void writeTo("
            + ImmutableSerializationContext.class.getName() + " $1, "
            + RawProtoStreamWriter.class.getName() + " $2, "
            + pmtm.getJavaClassName() + " $3) throws java.io.IOException";
      if (log.isTraceEnabled()) {
         log.tracef("%s %s", writeToSig, writeToSrc);
      }
      iw.append("@Override\n").append(writeToSig).append(' ').append(writeToSrc);

      iw.dec();
      iw.append("}\n");

      emitSource(fqn, iw.toString(), pmtm);
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
