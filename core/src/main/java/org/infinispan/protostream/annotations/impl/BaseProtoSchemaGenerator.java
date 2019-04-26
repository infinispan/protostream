package org.infinispan.protostream.annotations.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilderException;
import org.infinispan.protostream.annotations.impl.types.UnifiedTypeFactory;
import org.infinispan.protostream.annotations.impl.types.XClass;
import org.infinispan.protostream.impl.Log;

/**
 * This class is not to be directly invoked by users. See {@link org.infinispan.protostream.annotations.ProtoSchemaBuilder}
 * instead.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
public abstract class BaseProtoSchemaGenerator {

   private static final Log log = Log.LogFactory.getLog(BaseProtoSchemaGenerator.class);

   /**
    * Set this flag to {@code true} to enable output of debug comments in the generated Protobuf schema.
    */
   public static boolean generateSchemaDebugComments = false;

   protected final UnifiedTypeFactory typeFactory;

   protected final SerializationContext serializationContext;

   protected final String fileName;

   protected final String packageName;

   protected final Set<XClass> classes;

   /**
    * Indicates if class dependencies are automatically added when discovered or will generate an error.
    */
   protected final boolean autoImportClasses;

   /**
    * Known classes: the user-added classes ({@link #classes}) plus all their superclasses and superinterfaces. This is
    * only used when auto-import is off.
    */
   private final Set<XClass> knownClasses = new HashSet<>();

   /**
    * The imported schema files.
    */
   private final Set<String> imports = new HashSet<>();

   private final Map<XClass, ProtoTypeMetadata> metadataByClass = new HashMap<>();

   private final Map<String, ProtoTypeMetadata> metadataByTypeName = new HashMap<>();

   protected BaseProtoSchemaGenerator(UnifiedTypeFactory typeFactory, SerializationContext serializationContext, String fileName, String packageName, Set<XClass> classes, boolean autoImportClasses) {
      if (fileName == null) {
         throw new ProtoSchemaBuilderException("fileName cannot be null");
      }

      this.typeFactory = typeFactory;
      this.serializationContext = serializationContext;
      this.fileName = fileName;
      this.packageName = packageName;
      this.classes = classes;
      this.autoImportClasses = autoImportClasses;
   }

   public String generateAndRegister() throws ProtoSchemaBuilderException {
      if (!autoImportClasses) {
         // collect supers
         for (XClass c : classes) {
            collectKnownClasses(c);
         }
      }

      // scan initial classes
      for (XClass c : classes) {
         ProtoTypeMetadata protoTypeMetadata = c.isEnum() ? new ProtoEnumTypeMetadata(c) : new ProtoMessageTypeMetadata(this, c);
         defineMetadata(protoTypeMetadata);
      }

      // scan member annotations and possibly discover more classes being referenced
      while (true) {
         List<ProtoTypeMetadata> meta = new ArrayList<>(metadataByClass.values());
         for (ProtoTypeMetadata m : meta) {
            m.scanMemberAnnotations();
         }
         if (metadataByClass.size() == meta.size()) {
            // no new classes were discovered
            break;
         }
      }

      // establish the outer-inner relationship between definitions
      for (XClass c : metadataByClass.keySet()) {
         ProtoTypeMetadata m = metadataByClass.get(c);
         if (!m.isImported()) {
            ProtoMessageTypeMetadata outer = findOuterType(c);
            if (outer != null) {
               m.setOuterType(outer);
               outer.addInnerType(m);
            }
         }
      }

      IndentWriter iw = new IndentWriter();
      iw.append("// File name: ").append(fileName).append('\n');
      if (generateSchemaDebugComments) {
         iw.append("// Scanned classes:\n");
         //todo [anistor] this list of scanned classes should include all interfaces and base classes not just the ones for which a proto definition was generated
         for (ProtoTypeMetadata ptm : metadataByClass.values()) {
            if (!ptm.isImported()) {
               iw.append("//   ").append(ptm.getJavaClassName()).append('\n');
            }
         }
      }
      iw.append("\nsyntax = \"proto2\";\n");
      if (packageName != null) {
         iw.append("\npackage ").append(packageName).append(";\n\n");
      }
      for (String dependency : imports) {
         iw.append("import \"").append(dependency).append("\";\n");
      }

      // generate type definitions for top-level types
      for (XClass c : metadataByClass.keySet()) {
         ProtoTypeMetadata m = metadataByClass.get(c);
         if (m.getOuterType() == null) {
            m.generateProto(iw);
         }
      }

      String protoFile = iw.toString();

      if (log.isTraceEnabled()) {
         log.tracef("Generated proto file:\n%s", protoFile);
      }

      serializationContext.registerProtoFiles(FileDescriptorSource.fromString(fileName, protoFile));

      try {
         generateMarshallers();
      } catch (Exception e) {
         throw new ProtoSchemaBuilderException("Failed to generate marshaller implementation class", e);
      }

      return protoFile;
   }

   private ProtoMessageTypeMetadata findOuterType(XClass c) {
      ProtoTypeMetadata outer = null;
      XClass ec = c.getEnclosingClass();
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
      AbstractMarshallerCodeGenerator marshallerCodeGenerator = makeCodeGenerator();
      for (XClass c : metadataByClass.keySet()) {
         ProtoTypeMetadata ptm = metadataByClass.get(c);
         marshallerCodeGenerator.generateMarshaller(serializationContext, ptm);
      }
   }

   protected abstract AbstractMarshallerCodeGenerator makeCodeGenerator();

   protected ProtoTypeMetadata scanAnnotations(XClass javaType) {
      ProtoTypeMetadata protoTypeMetadata = metadataByClass.get(javaType);
      if (protoTypeMetadata != null) {
         // already seen and processed
         return protoTypeMetadata;
      }

      // try to import it from existing files before deciding we need to generate it based on annotations
      protoTypeMetadata = importProtoTypeMetadata(javaType);

      if (protoTypeMetadata != null) {
         // found it
         imports.add(protoTypeMetadata.getFileName());
      } else {
         // nope, we need to generate it
         protoTypeMetadata = makeProtoTypeMetadata(javaType);
      }

      defineMetadata(protoTypeMetadata);

      return protoTypeMetadata;
   }

   /**
    * Return an imported ProtoTypeMetadata implementation or null if it cannot be imported.
    */
   protected abstract ProtoTypeMetadata importProtoTypeMetadata(XClass javaType);

   protected ProtoTypeMetadata makeProtoTypeMetadata(XClass javaType) {
      return javaType.isEnum() ? new ProtoEnumTypeMetadata(javaType) : new ProtoMessageTypeMetadata(this, javaType);
   }

   private void defineMetadata(ProtoTypeMetadata protoTypeMetadata) {
      if (!autoImportClasses && !protoTypeMetadata.isImported() && !isKnownClass(protoTypeMetadata.getJavaClass())) {
         // autoImportClasses is off and we are just expanding the class set
         throw new ProtoSchemaBuilderException("Found a reference to class "
               + protoTypeMetadata.getJavaClassName() + " which was not explicitly added to the builder and 'autoImportClasses' is disabled.");
      }

      String fullName = protoTypeMetadata.getFullName();
      ProtoTypeMetadata existing = metadataByTypeName.get(fullName);
      if (existing != null) {
         throw new ProtoSchemaBuilderException("Found a duplicate type definition. Type '" + fullName + "' is defined by "
               + protoTypeMetadata.getJavaClassName() + " and also by " + existing.getJavaClassName());
      }
      metadataByTypeName.put(fullName, protoTypeMetadata);
      metadataByClass.put(protoTypeMetadata.getJavaClass(), protoTypeMetadata);
   }

   protected boolean isKnownClass(XClass c) {
      boolean isKnown;
      while (!(isKnown = knownClasses.contains(c))) {
         // try the outer class because inner classes are considered recursively added too
         c = c.getEnclosingClass();
         if (c == null) {
            break;
         }
      }
      return isKnown;
   }

   private void collectKnownClasses(XClass c) {
      knownClasses.add(c);
      if (c.getSuperclass() != null) {
         collectKnownClasses(c.getSuperclass());
      }
      for (XClass i : c.getInterfaces()) {
         collectKnownClasses(i);
      }
   }
}
