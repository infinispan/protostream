package org.infinispan.protostream.annotations.impl;

import javassist.ClassPool;
import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.RawProtobufMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilderException;
import org.infinispan.protostream.impl.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//todo [anistor] revise all exception messages to better indicate the cause AND location (class +field/method)

//todo [anistor] need to detect type definition cycles?

//todo [anistor] generate debug comments in proto schema (list of initial classes and extra detected classes

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public final class ProtoSchemaGenerator {

   private static final Log log = Log.LogFactory.getLog(ProtoSchemaGenerator.class);

   private final SerializationContext serializationContext;

   private final String fileName;

   private final String packageName;

   private final Set<Class<?>> classes;

   private final Set<String> imports = new HashSet<String>();

   private final Map<Class<?>, ProtoTypeMetadata> metadataByClass = new HashMap<Class<?>, ProtoTypeMetadata>();

   private final Map<String, ProtoTypeMetadata> metadataByTypeName = new HashMap<String, ProtoTypeMetadata>();

   public ProtoSchemaGenerator(SerializationContext serializationContext, String fileName, String packageName, Set<Class<?>> classes) {
      this.serializationContext = serializationContext;
      this.fileName = fileName;
      this.packageName = packageName;
      this.classes = classes;
   }

   public String generateAndRegister() throws ProtoSchemaBuilderException, IOException {
      for (Class<?> c : classes) {
         ProtoTypeMetadata protoTypeMetadata;
         if (c.isEnum()) {
            ProtoEnumMetadataScanner enumScanner = new ProtoEnumMetadataScanner((Class<? extends Enum>) c);
            protoTypeMetadata = enumScanner.getProtoEnumMetadata();
         } else {
            ProtoMessageMetadataScanner messageScanner = new ProtoMessageMetadataScanner(this, serializationContext, c);
            protoTypeMetadata = messageScanner.getProtoMessageMetadata();
         }
         defineType(c, protoTypeMetadata);
      }

      IndentWriter iw = new IndentWriter();
      iw.append("// ").append(fileName).append("\n");
      if (packageName != null) {
         iw.append("package ").append(packageName).append(";\n\n");
      }
      for (String dependency : imports) {
         iw.append("import \"").append(dependency).append("\";\n");
      }

      // establish the outer-inner relationship between definitions
      for (Class<?> c : metadataByClass.keySet()) {
         ProtoTypeMetadata m = metadataByClass.get(c);
         ProtoMessageTypeMetadata outer = findOuterType(c);
         if (outer != null) {
            m.setOuterType(outer);
            outer.addInnerType(m);
         }
      }

      // generate type definitions
      for (Class<?> c : metadataByClass.keySet()) {
         ProtoTypeMetadata m = metadataByClass.get(c);
         if (m.isTopLevel()) {
            m.generateProto(iw);
         }
      }

      String protoFile = iw.toString();
      log.tracef("Generated proto file:\n%s", protoFile);

      serializationContext.registerProtoFiles(FileDescriptorSource.fromString(fileName, protoFile));

      try {
         generateMarshallers();
      } catch (Exception e) {
         throw new ProtoSchemaBuilderException("Failed to generate marshaller implementation class", e);
      }

      return protoFile;
   }

   private ProtoMessageTypeMetadata findOuterType(Class<?> c) {
      ProtoTypeMetadata outer = null;
      Class<?> ec = c.getEnclosingClass();
      while (ec != null) {
         outer = metadataByClass.get(ec);
         if (outer != null) {
            break;
         }
         ec = ec.getEnclosingClass();
      }
      return (ProtoMessageTypeMetadata) outer; //todo [anistor] a class defined inside an enum could lead to a CCE here. this is not a valid case anyway, but the error should be presented to the user more gracefully
   }

   private void generateMarshallers() throws Exception {
      ClassPool cp = ClassPool.getDefault();
      MarshallerCodeGenerator marshallerCodeGenerator = new MarshallerCodeGenerator(packageName, cp);
      for (Class<?> c : metadataByClass.keySet()) {
         ProtoTypeMetadata ptm = metadataByClass.get(c);
         if (ptm instanceof ProtoMessageTypeMetadata) {
            RawProtobufMarshaller marshaller = marshallerCodeGenerator.generateMessageMarshaller((ProtoMessageTypeMetadata) ptm);
            ptm.setMarshaller(marshaller);
            serializationContext.registerMarshaller(marshaller);
         } else if (ptm instanceof ProtoEnumTypeMetadata) {
            EnumMarshaller marshaller = marshallerCodeGenerator.generateEnumMarshaller((ProtoEnumTypeMetadata) ptm);
            ptm.setMarshaller(marshaller);
            serializationContext.registerMarshaller(marshaller);
         }
      }
   }

   ProtoTypeMetadata scanAnnotations(Class<?> javaType) {
      ProtoTypeMetadata protoTypeMetadata = metadataByClass.get(javaType);
      if (protoTypeMetadata != null) {
         // already seen
         return protoTypeMetadata;
      }

      if (serializationContext.canMarshall(javaType)) {
         // this is a known type, defined in another schema file that we'll need to import
         BaseMarshaller m = serializationContext.getMarshaller(javaType);
         protoTypeMetadata = new ProtoTypeMetadata(m);
         if (protoTypeMetadata.isEnum()) {
            imports.add(serializationContext.getEnumDescriptor(m.getTypeName()).getFileDescriptor().getName());
         } else {
            imports.add(serializationContext.getMessageDescriptor(m.getTypeName()).getFileDescriptor().getName());
         }
      } else if (javaType.isEnum()) {
         ProtoEnumMetadataScanner enumScanner = new ProtoEnumMetadataScanner((Class<? extends Enum>) javaType);
         protoTypeMetadata = enumScanner.getProtoEnumMetadata();
      } else {
         ProtoMessageMetadataScanner messageScanner = new ProtoMessageMetadataScanner(this, serializationContext, javaType);
         protoTypeMetadata = messageScanner.getProtoMessageMetadata();
      }

      defineType(javaType, protoTypeMetadata);
      return protoTypeMetadata;
   }

   private void defineType(Class<?> javaType, ProtoTypeMetadata protoTypeMetadata) {
      String fullName = protoTypeMetadata.getFullName();
      ProtoTypeMetadata existing = metadataByTypeName.get(fullName);
      if (existing != null) {
         throw new ProtoSchemaBuilderException("Duplicate type definition. Type '" + fullName + "' is defined by " + javaType + " and by " + existing.getJavaClass());
      }
      metadataByTypeName.put(fullName, protoTypeMetadata);
      metadataByClass.put(javaType, protoTypeMetadata);
   }
}
