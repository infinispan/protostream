package org.infinispan.protostream.annotations.impl.processor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.JavaFileObject;

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
 * @author anistor@readhat.com
 * @since 4.3
 */
final class MarshallerSourceCodeGenerator extends AbstractMarshallerCodeGenerator {

   private static final Log log = Log.LogFactory.getLog(MarshallerSourceCodeGenerator.class);

   private final ProcessingEnvironment processingEnv;

   private final Set<String> generatedClasses;

   MarshallerSourceCodeGenerator(ProcessingEnvironment processingEnv, UnifiedTypeFactory typeFactory, String protobufSchemaPackage, Set<String> generatedClasses) {
      super(typeFactory, protobufSchemaPackage);
      this.processingEnv = processingEnv;
      this.generatedClasses = generatedClasses;
   }

   @Override
   public void generateMarshaller(SerializationContext serCtx, ProtoTypeMetadata ptm) throws IOException {
      if (ptm instanceof ProtoMessageTypeMetadata) {
         generateMessageMarshaller((ProtoMessageTypeMetadata) ptm);
      } else if (ptm instanceof ProtoEnumTypeMetadata) {
         generateEnumMarshaller((ProtoEnumTypeMetadata) ptm);
      }
   }

   @Override
   protected String makeUniqueMarshallerClassName() {
      return "_Marshaller" + nextMarshallerClassId();
   }

   private void generateEnumMarshaller(ProtoEnumTypeMetadata petm) throws IOException {
      String marshallerClassName = petm.getJavaClass().getSimpleName() + '$' + makeUniqueMarshallerClassName();
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

      writeSourceFile(fqn, iw.toString(), ((HasModelElement) petm.getJavaClass()).getElement());
   }

   private void generateMessageMarshaller(ProtoMessageTypeMetadata pmtm) throws IOException {
      String marshallerClassName = pmtm.getJavaClass().getSimpleName() + '$' + makeUniqueMarshallerClassName();
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
      AutoProtoSchemaBuilderAnnotationProcessor.addGeneratedBy(iw);
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

      writeSourceFile(fqn, iw.toString(), ((HasModelElement) pmtm.getJavaClass()).getElement());
   }

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
               break;
         }
      }
   }

   private void writeSourceFile(String className, String classSource, Element typeElement) throws IOException {
      JavaFileObject file = processingEnv.getFiler().createSourceFile(className, typeElement);
      generatedClasses.add(className);
      try (PrintWriter out = new PrintWriter(file.openWriter())) {
         out.print(classSource);
      }
   }
}
