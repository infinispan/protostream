package org.infinispan.protostream.annotations.impl;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoSchemaBuilderException;
import org.infinispan.protostream.annotations.ProtoSyntax;
import org.infinispan.protostream.annotations.impl.types.XClass;
import org.infinispan.protostream.annotations.impl.types.XTypeFactory;
import org.infinispan.protostream.impl.Log;

/**
 * Generates a protobuf schema (proto file) based on annotations and also generates marshallers for the types.
 * <p>
 * This class is not to be directly invoked by users.
 * <p>
 * Generators are not expected to be stateless, so reuse must be done taking this into account.
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

   /**
    * Set this flag to {@code true} to enable output of debug comments in the generated marshaller source code.
    */
   public static boolean generateMarshallerDebugComments = false;

   protected final XTypeFactory typeFactory;

   protected final SerializationContext serializationContext;

   /**
    * Protobuf schema file name.
    */
   protected final String fileName;

   protected final String generator;

   /**
    * Protobuf schema package name.
    */
   protected final String packageName;

   /**
    * Initial set of classes.
    */
   protected final Set<XClass> classes;

   /**
    * Known classes: the user-added classes ({@link #classes}) plus all their superclasses and superinterfaces. This is
    * only used when auto-import is disabled.
    */
   private final Set<XClass> knownClasses = new HashSet<>();

   /**
    * The imported schema files.
    */
   private final Set<String> imports = new HashSet<>();

   private final Map<XClass, ProtoTypeMetadata> metadataByClass = new HashMap<>();

   private final Map<String, ProtoTypeMetadata> metadataByTypeName = new HashMap<>();
   private final ProtoSyntax syntax;
   private final boolean allowNullFields;

   protected BaseProtoSchemaGenerator(XTypeFactory typeFactory, SerializationContext serializationContext,
                                      String generator, String fileName, String packageName, Set<XClass> classes,
                                      ProtoSyntax syntax, boolean allowNullFields) {
      if (fileName == null) {
         throw new ProtoSchemaBuilderException("fileName cannot be null");
      }

      this.typeFactory = typeFactory;
      this.serializationContext = serializationContext;
      this.generator = generator;
      this.fileName = fileName;
      this.packageName = packageName;
      this.classes = classes;
      this.syntax = syntax;
      this.allowNullFields = allowNullFields;
   }

   public boolean allowNullFields() {
      return allowNullFields;
   }

   public ProtoSyntax syntax() {
      return syntax;
   }

   public String generateAndRegister() {
      // collect supers
      for (XClass c : classes) {
         collectKnownClasses(c);
      }

      // scan initial classes
      for (XClass c : classes) {
         collectMetadata(makeTypeMetadata(c));
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
      StringWriter sw = new StringWriter();
      IndentWriter iw = new IndentWriter(sw);
      iw.printf("// File name: %s\n", fileName);
      if (generator != null) {
         iw.printf("// Generated from : %s\n", fileName);
      }
      if (generateSchemaDebugComments) {
         iw.println("// Scanned classes:");
         //todo [anistor] this list of scanned classes should include all interfaces and base classes not just the ones for which a proto definition was generated
         for (ProtoTypeMetadata ptm : metadataByClass.values()) {
            if (!ptm.isImported()) {
               iw.append("//   ").append(ptm.getJavaClassName()).append('\n');
            }
         }
      }

      iw.printf("syntax = \"%s\";\n", syntax);

      if (packageName != null) {
         iw.append("package ").append(packageName).append(";\n\n");
      }
      for (String dependency : imports) {
         iw.append("import \"").append(dependency).append("\";\n");
      }

      // generate type definitions for all top-level types, except the ones found in imported files
      for (XClass c : metadataByClass.keySet()) {
         ProtoTypeMetadata m = metadataByClass.get(c);
         if (m.getOuterType() == null && !m.isImported()) {
            m.generateProto(iw, syntax);
         }
      }

      String protoFile = sw.toString();

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
      AbstractMarshallerCodeGenerator marshallerCodeGenerator = makeMarshallerCodeGenerator();
      for (XClass c : metadataByClass.keySet()) {
         ProtoTypeMetadata ptm = metadataByClass.get(c);
         if (!ptm.isImported()) {
            marshallerCodeGenerator.generateMarshaller(serializationContext, ptm);
         }
      }
   }

   public Set<XClass> getMarshalledClasses() {
      return metadataByClass.entrySet().stream()
            .filter(e -> !e.getValue().isImported())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
   }

   /**
    * Creates a code generator for marshallers.
    */
   protected abstract AbstractMarshallerCodeGenerator makeMarshallerCodeGenerator();

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
         protoTypeMetadata = makeTypeMetadata(javaType);
      }

      collectMetadata(protoTypeMetadata);

      return protoTypeMetadata;
   }

   /**
    * Return an imported ProtoTypeMetadata implementation or null if it cannot be imported.
    */
   protected abstract ProtoTypeMetadata importProtoTypeMetadata(XClass javaType);

   protected ProtoTypeMetadata makeTypeMetadata(XClass javaType) {
      return javaType.isEnum() ? makeEnumTypeMetadata(javaType) : makeMessageTypeMetadata(javaType);
   }

   protected ProtoTypeMetadata makeEnumTypeMetadata(XClass javaType) {
      return new ProtoEnumTypeMetadata(javaType, getTargetClass(javaType));
   }

   protected ProtoTypeMetadata makeMessageTypeMetadata(XClass javaType) {
      return new ProtoMessageTypeMetadata(this, javaType, getTargetClass(javaType));
   }

   void collectMetadata(ProtoTypeMetadata protoTypeMetadata) {
      boolean isUnknownClass = isUnknownClass(protoTypeMetadata.getJavaClass());

      if (isUnknownClass && !protoTypeMetadata.isImported()) {
         // autoImportClasses is off and we are attempting expanding the class set -> NOPE!
         throw new ProtoSchemaBuilderException("Found a reference to class "
               + protoTypeMetadata.getJavaClassName()
               + " which was not added to the builder and 'autoImportClasses' is disabled.");
      }

      ProtoTypeMetadata existingByClass = metadataByClass.get(protoTypeMetadata.getJavaClass());
      if (existingByClass != null) {
         throw new ProtoSchemaBuilderException("Found a duplicate type definition. Java type '" + protoTypeMetadata.getJavaClassName() + "' is defined by "
               + protoTypeMetadata.getAnnotatedClassName() + " and also by " + existingByClass.getAnnotatedClassName());
      }
      String fullName = protoTypeMetadata.getFullName();
      ProtoTypeMetadata existingByName = metadataByTypeName.get(fullName);
      if (existingByName != null) {
         throw new ProtoSchemaBuilderException("Found a duplicate type definition. Type '" + fullName + "' is defined by "
               + protoTypeMetadata.getAnnotatedClassName() + " and also by " + existingByName.getAnnotatedClassName());
      }
      metadataByTypeName.put(fullName, protoTypeMetadata);
      metadataByClass.put(protoTypeMetadata.getJavaClass(), protoTypeMetadata);
   }

   protected boolean isUnknownClass(XClass c) {
      boolean isKnown;
      while (!(isKnown = knownClasses.contains(c))) {
         // try the outer class because inner classes are considered to be implicitly recursively added also
         c = c.getEnclosingClass();
         if (c == null) {
            break;
         }
      }
      return !isKnown;
   }

   /**
    * Collect all superclasses and superinterfaces.
    */
   private void collectKnownClasses(XClass c) {
      XClass b = getAdapterFor(c);
      if (b != null) {
         knownClasses.add(b);
         // supers are not collected for adapters
         return;
      }

      knownClasses.add(c);
      if (c.getSuperclass() != null) {
         collectKnownClasses(c.getSuperclass());
      }
      for (XClass i : c.getInterfaces()) {
         collectKnownClasses(i);
      }
   }

   protected XClass getAdapterFor(XClass annotatedClass) {
      ProtoAdapter protoAdapter = annotatedClass.getAnnotation(ProtoAdapter.class);
      if (protoAdapter != null) {
         // TODO [anistor] also ensure that protoAdapter.value() is not part of current builder and is not scanned for @ProtoXyz annotations even if present
         XClass target = typeFactory.fromClass(protoAdapter.value());
         if (target == annotatedClass) {
            throw new ProtoSchemaBuilderException(annotatedClass.getName() + " has an invalid @ProtoAdapter annotation pointing to self");
         }
         return target;
      }
      return null;
   }

   /**
    * Get the marshalled class or enum. The marshalled class and the annotated class are not always the same, depending
    * on the presence of the ProtoAdapter annotation which may establish a new target.
    */
   protected XClass getTargetClass(XClass annotatedClass) {
      XClass target = getAdapterFor(annotatedClass);
      return target == null ? annotatedClass : target;
   }
}
