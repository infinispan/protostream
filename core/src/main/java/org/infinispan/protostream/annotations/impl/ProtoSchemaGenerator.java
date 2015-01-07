package org.infinispan.protostream.annotations.impl;

import javassist.ClassPool;
import javassist.LoaderClassPath;
import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.RawProtobufMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilderException;
import org.infinispan.protostream.impl.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
      // scan initial classes
      for (Class<?> c : classes) {
         ProtoTypeMetadata protoTypeMetadata = c.isEnum() ? new ProtoEnumTypeMetadata((Class<? extends Enum>) c) : new ProtoMessageTypeMetadata(this, c);
         defineType(protoTypeMetadata);
      }

      while (true) {
         List<ProtoTypeMetadata> meta = new ArrayList<ProtoTypeMetadata>(metadataByClass.values());
         for (ProtoTypeMetadata m : meta) {
            m.scanMemberAnnotations();
         }
         if (metadataByClass.size() == meta.size()) {
            break;
         }
      }

      // establish the outer-inner relationship between definitions
      for (Class<?> c : metadataByClass.keySet()) {
         ProtoMessageTypeMetadata outer = findOuterType(c);
         if (outer != null) {
            ProtoTypeMetadata m = metadataByClass.get(c);
            m.setOuterType(outer);
            outer.addInnerType(m);
         }
      }

      IndentWriter iw = new IndentWriter();
      iw.append("// File name: ").append(fileName).append('\n');
      iw.append("// Scanned classes:\n");
      for (ProtoTypeMetadata ptm : metadataByClass.values()) {
         if (ptm instanceof ProtoEnumTypeMetadata || ptm instanceof ProtoMessageTypeMetadata) {
            iw.append("//   ").append(ptm.getJavaClass().getCanonicalName()).append('\n');
         }
      }
      if (packageName != null) {
         iw.append("\npackage ").append(packageName).append(";\n\n");
      }
      for (String dependency : imports) {
         iw.append("import \"").append(dependency).append("\";\n");
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
         if (ec.isEnum()) {
            throw new ProtoSchemaBuilderException("Classes defined inside an Enum are not allowed : " + c.getCanonicalName());
         }
         outer = metadataByClass.get(ec);
         if (outer != null) {
            break;
         }
         ec = ec.getEnclosingClass();
      }
      return (ProtoMessageTypeMetadata) outer;
   }

   private void generateMarshallers() throws Exception {
      ClassPool cp = new ClassPool(ClassPool.getDefault());
      cp.appendClassPath(new LoaderClassPath(getClass().getClassLoader()));

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

   protected ProtoTypeMetadata scanAnnotations(Class<?> javaType) {
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
         protoTypeMetadata = new ProtoEnumTypeMetadata((Class<? extends Enum>) javaType);
      } else {
         protoTypeMetadata = new ProtoMessageTypeMetadata(this, javaType);
      }

      defineType(protoTypeMetadata);
      return protoTypeMetadata;
   }

   private void defineType(ProtoTypeMetadata protoTypeMetadata) {
      String fullName = protoTypeMetadata.getFullName();
      ProtoTypeMetadata existing = metadataByTypeName.get(fullName);
      if (existing != null) {
         throw new ProtoSchemaBuilderException("Duplicate type definition. Type '" + fullName + "' is defined by " + protoTypeMetadata.getJavaClass() + " and by " + existing.getJavaClass());
      }
      metadataByTypeName.put(fullName, protoTypeMetadata);
      metadataByClass.put(protoTypeMetadata.getJavaClass(), protoTypeMetadata);
   }
}
