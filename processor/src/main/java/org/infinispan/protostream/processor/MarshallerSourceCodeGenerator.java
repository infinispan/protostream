package org.infinispan.protostream.processor;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.Element;

import org.infinispan.protostream.BaseMarshallerDelegate;
import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.EnumMarshallerDelegate;
import org.infinispan.protostream.GeneratedMarshallerBase;
import org.infinispan.protostream.ProtobufTagMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.impl.AbstractMarshallerCodeGenerator;
import org.infinispan.protostream.annotations.impl.IndentWriter;
import org.infinispan.protostream.annotations.impl.ProtoEnumTypeMetadata;
import org.infinispan.protostream.annotations.impl.ProtoFieldMetadata;
import org.infinispan.protostream.annotations.impl.ProtoMapMetadata;
import org.infinispan.protostream.annotations.impl.ProtoMessageTypeMetadata;
import org.infinispan.protostream.annotations.impl.ProtoTypeMetadata;
import org.infinispan.protostream.annotations.impl.types.XClass;
import org.infinispan.protostream.annotations.impl.types.XTypeFactory;
import org.infinispan.protostream.containers.IndexedElementContainerAdapter;
import org.infinispan.protostream.containers.IterableElementContainerAdapter;
import org.infinispan.protostream.containers.MapElementContainerAdapter;
import org.infinispan.protostream.impl.Log;
import org.infinispan.protostream.processor.types.HasModelElement;

/**
 * Generates source code for the marshaller.
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
      StringWriter sw = new StringWriter();
      IndentWriter iw = new IndentWriter(sw);
      addFileHeader(iw, petm.getAnnotatedClassName());

      String fqn;
      if (petm.getAnnotatedClass().getPackageName() != null) {
         fqn = petm.getAnnotatedClass().getPackageName() + '.' + marshallerClassName;
         iw.printf("package %s;\n", petm.getAnnotatedClass().getPackageName());
         iw.println();
      } else {
         fqn = marshallerClassName;
      }

      if (petm.getJavaClass().getPackageName() != null) {
         iw.append("import ").append(petm.getJavaClassName()).append(";\n\n");
      }

      iw.append("public final class ").append(marshallerClassName)
            .append(" implements ").append(EnumMarshaller.class.getName()).append('<').append(petm.getJavaClassName()).append("> {\n\n");
      iw.inc();

      iw.println("@Override");
      iw.append("public Class<").append(petm.getJavaClassName()).append("> getJavaClass() { return ").append(petm.getJavaClassName()).append(".class; }\n\n");

      iw.println("@Override");
      iw.append("public String getTypeName() { return \"").append(makeQualifiedTypeName(petm.getFullName())).append("\"; }\n\n");

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

      emitSource(fqn, sw.toString(), petm);
   }

   private void addFileHeader(IndentWriter iw, String className) {
      iw.println("/*");
      iw.printf(" Generated by %s\n", getClass().getName());
      iw.printf(" for class %s\n", className);
      iw.println("*/");
      iw.println();
   }

   private void generateMessageMarshaller(ProtoMessageTypeMetadata pmtm) throws IOException {
      String marshallerClassName = makeUniqueMarshallerClassName(pmtm);
      if (log.isTraceEnabled()) {
         log.tracef("Generating message marshaller %s for %s", marshallerClassName, pmtm.getJavaClass().getName());
      }

      StringWriter sw = new StringWriter();
      IndentWriter iw = new IndentWriter(sw);
      addFileHeader(iw, pmtm.getAnnotatedClassName());

      String fqn;
      if (pmtm.getAnnotatedClass().getPackageName() != null) {
         fqn = pmtm.getAnnotatedClass().getPackageName() + '.' + marshallerClassName;
         iw.printf("package %s;\n", pmtm.getAnnotatedClass().getPackageName());
         iw.println();
      } else {
         fqn = marshallerClassName;
      }

      if (pmtm.getJavaClass().getPackageName() != null) {
         XClass toImport = pmtm.getJavaClass().isArray() ? pmtm.getJavaClass().getComponentType() : pmtm.getJavaClass();
         String toImportName = toImport.getCanonicalName();
         iw.printf("import %s;\n", toImportName != null ? toImportName : toImport.getName());
         iw.println();
      }

      ProtoSchemaAnnotationProcessor.addGeneratedClassHeader(iw);
      iw.println("@SuppressWarnings(\"unchecked\")");
      iw.printf("public final class %s extends %s implements %s<%s>", marshallerClassName, GeneratedMarshallerBase.class.getName(), ProtobufTagMarshaller.class.getName(), pmtm.getJavaClassName());
      String elementType = null;
      if (pmtm.isIndexedContainer()) {
         elementType = pmtm.getAnnotatedClass().getGenericInterfaceParameterTypes(IndexedElementContainerAdapter.class)[1];
         iw.printf(", %s<%s, %s>", IndexedElementContainerAdapter.class.getName(), pmtm.getJavaClassName(), elementType);
      } else if (pmtm.isMapContainer()) {
         String[] types = pmtm.getAnnotatedClass().getGenericInterfaceParameterTypes(MapElementContainerAdapter.class);
         String map = pmtm.getJavaClassName();
         String key = types[0];
         String value = types[1];
         elementType = Map.Entry.class.getCanonicalName();
         iw.printf(", %s<%s, %s, %s<%s, %s>>", MapElementContainerAdapter.class.getName(), key, value, map, key, value);
      } else if (pmtm.isIterableContainer()) {
         elementType = pmtm.getAnnotatedClass().getGenericInterfaceParameterTypes(IterableElementContainerAdapter.class)[1];
         iw.printf(", %s<%s, %s>", IterableElementContainerAdapter.class.getName(), pmtm.getJavaClassName(), elementType);
      }
      iw.println(" {");
      iw.println();
      iw.inc();

      if (pmtm.isAdapter()) {
         addAdapterField(iw, pmtm);
      }

      addMarshallerDelegateFields(iw, pmtm);
      iw.println("@Override");
      iw.printf("public Class<%s> getJavaClass() { return %s.class; }\n", pmtm.getJavaClassName(), pmtm.getJavaClassName());
      iw.println();
      iw.println("@Override");
      iw.printf("public String getTypeName() { return \"%s\"; }\n", makeQualifiedTypeName(pmtm.getFullName()));
      iw.println();
      String[] subClassNames = pmtm.getSubClassNames();
      if (subClassNames != null && subClassNames.length > 0) {
         iw.println("@Override");
         iw.println("public String[] getSubClassNames() {");
         iw.inc().print("return new String[] {");
         iw.print(Arrays.stream(subClassNames).map(s -> "\"" + s + "\"").collect(Collectors.joining(",")));
         iw.println("};");
         iw.dec().println("}");
      }

      if (pmtm.isIndexedContainer()) {
         if (pmtm.isAdapter()) {
            iw.println("@Override");
            iw.printf("public int getNumElements(%s container) { return %s.getNumElements(container); }\n", pmtm.getJavaClassName(), ADAPTER_FIELD_NAME);
            iw.println("@Override");
            iw.printf("public %s getElement(%s container, int index) { return %s.getElement(container, index); }\n", elementType, pmtm.getJavaClassName(), ADAPTER_FIELD_NAME);
            iw.println("@Override");
            iw.printf("public void setElement(%s container, int index, %s element) { %s.setElement(container, index, element); }\n", pmtm.getJavaClassName(), elementType, ADAPTER_FIELD_NAME);
         } else {
            iw.println("@Override");
            iw.printf("public int getNumElements(%s container) { return ((%s) container).getNumElements(); }\n", pmtm.getJavaClassName(), IndexedElementContainerAdapter.class.getName());
            iw.println("@Override");
            iw.printf("public %s getElement(%s container, int index) { return ((%s) container).getElement(index); }\n", elementType, pmtm.getJavaClassName(), IndexedElementContainerAdapter.class.getName());
            iw.println("@Override");
            iw.append("public void setElement(").append(pmtm.getJavaClassName()).append(" container, int index, ").append(elementType).append(" element) { ((").append(IndexedElementContainerAdapter.class.getName()).append(") container).setElement(index, element); }\n");
         }
      } else if (pmtm.isIterableContainer()) {
         if (pmtm.isAdapter()) {
            iw.println("@Override");
            iw.append("public int getNumElements(").append(pmtm.getJavaClassName()).append(" container) { return ").append(ADAPTER_FIELD_NAME).append(".getNumElements(container); }\n");
            iw.println("@Override");
            iw.append("public java.util.Iterator getElements(").append(pmtm.getJavaClassName()).append(" container) { return ").append(ADAPTER_FIELD_NAME).append(".getElements(container); }\n");
            iw.println("@Override");
            iw.append("public void appendElement(").append(pmtm.getJavaClassName()).append(" container, ").append(elementType).append(" element) { ").append(ADAPTER_FIELD_NAME).append(".appendElement(container, element); }\n");
         } else {
            iw.println("@Override");
            iw.append("public int getNumElements(").append(pmtm.getJavaClassName()).append(" container) { return ((").append(IterableElementContainerAdapter.class.getName()).append(") container).getNumElements(); }\n");
            iw.println("@Override");
            iw.append("public java.util.Iterator getElements(").append(pmtm.getJavaClassName()).append(" container) { return ((").append(IterableElementContainerAdapter.class.getName()).append(") container).getElements(); }\n");
            iw.println("@Override");
            iw.append("public void appendElement(").append(pmtm.getJavaClassName()).append(" container, ").append(elementType).append(" element) { ((").append(IterableElementContainerAdapter.class.getName()).append(") container).appendElement(element); }\n");
         }
      }

      iw.println("@Override");
      iw.printf("public %s read(%s $1) throws java.io.IOException {\n", pmtm.getJavaClassName(), ProtobufTagMarshaller.ReadContext.class.getCanonicalName());
      iw.inc();
      generateReadMethodBody(iw, pmtm);
      iw.dec();
      iw.println("}");
      iw.println();

      iw.println("@Override");
      iw.printf("public void write(%s $1, %s $2) throws java.io.IOException {\n", ProtobufTagMarshaller.WriteContext.class.getCanonicalName(), pmtm.getJavaClassName());
      iw.inc();
      generateWriteMethodBody(iw, pmtm);
      iw.dec();
      iw.println("}");

      iw.dec();
      iw.println("}");

      if (log.isTraceEnabled()) {
         log.trace(sw.toString());
      }

      emitSource(fqn, sw.toString(), pmtm);
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
         addMarshallerDelegateField(iw, fieldMetadata, addedFields);
      }
   }

   private void addMarshallerDelegateField(IndentWriter iw, ProtoFieldMetadata fieldMetadata, Set<String> addedFields) {
      String fieldName;
      switch (fieldMetadata.getProtobufType()) {
         case GROUP:
         case MESSAGE:
         case ENUM:
            fieldName = makeMarshallerDelegateFieldName(fieldMetadata);
            addMarshallerDelegateField(iw, fieldName, fieldMetadata, addedFields);
            break;
         case MAP:
            ProtoMapMetadata mapMetadata = (ProtoMapMetadata) fieldMetadata;
            fieldName = makeMarshallerDelegateFieldName(mapMetadata);
            // No need to process delegate field for Map key as the protobuf spec stimulates it must be a Scalar type
            addMarshallerDelegateField(iw, fieldName, mapMetadata.getValue(), addedFields);
            break;
      }
   }

   private void addMarshallerDelegateField(IndentWriter iw, String fieldName, ProtoFieldMetadata fieldMetadata, Set<String> addedFields) {
      // add the field only if it does not already exist. if there is more than one usage of a marshaller then we could try to add it twice
      if (addedFields.add(fieldName)) {
         Class<?> marshallerDelegateClass = fieldMetadata.getJavaType().isEnum() ? EnumMarshallerDelegate.class : BaseMarshallerDelegate.class;
         iw.append("private ").append(marshallerDelegateClass.getName()).append(' ').append(fieldName).append(";\n\n");
      }
   }

   private void emitSource(String fqn, String source, ProtoTypeMetadata ptm) throws IOException {
      Element originatingElement = ((HasModelElement) ptm.getJavaClass()).getElement();
      generatedFilesWriter.addMarshallerSourceFile(fqn, source, originatingElement);
      generatedClasses.add(fqn);
   }

   public Set<String> getGeneratedClasses() {
      return generatedClasses;
   }
}
