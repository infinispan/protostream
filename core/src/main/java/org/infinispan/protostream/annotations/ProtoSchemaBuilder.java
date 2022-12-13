package org.infinispan.protostream.annotations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.Version;
import org.infinispan.protostream.annotations.impl.BaseProtoSchemaGenerator;
import org.infinispan.protostream.annotations.impl.RuntimeProtoSchemaGenerator;
import org.infinispan.protostream.annotations.impl.types.ReflectionTypeFactory;
import org.infinispan.protostream.annotations.impl.types.XClass;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.impl.Log;

/**
 * Generates a Protocol Buffers schema definition file and the associated marshallers instances based on a set of given
 * {@code @Proto*} annotated classes. The generated schema and marshallers are registered to a
 * {@link SerializationContext} given during creation. The needed types, on which the currently generated types depend
 * on, are also looked up in the same {@link SerializationContext}.
 * <p>
 * See annotations {@link ProtoName}, {@link ProtoMessage}, {@link ProtoField}, {@link ProtoEnum}, {@link ProtoTypeId},
 * {@link ProtoEnumValue}, {@link ProtoDoc}, {@link ProtoDocs}, {@link ProtoUnknownFieldSet}, {@link ProtoFactory},
 * {@link ProtoReserved} and {@link ProtoReservedStatements}.
 * <p>
 * <b>NOTE:</b> This builder contains state that cannot be reset making it impossible to reuse properly. Please create
 * separate instances for each schema generation.
 * <p>
 * This class performs run-time generation. For a compile-time equivalent see {@link AutoProtoSchemaBuilder} and
 * {@link SerializationContextInitializer}.
 *
 * @author anistor@redhat.com
 * @since 3.0
 * @deprecated since 4.3.4. To be removed in version 5. Please use {@link AutoProtoSchemaBuilder} instead. See
 * <a href="https://issues.redhat.com/browse/IPROTO-159">IPROTO-159</a>.
 */
@Deprecated
public final class ProtoSchemaBuilder {

   private static final Log log = Log.LogFactory.getLog(ProtoSchemaBuilder.class);

   public static final String DEFAULT_GENERATED_SCHEMA_NAME = "generated.proto";
   public static final String FILE_OPT = "f";
   public static final String FILE_LONG_OPT = "file";
   public static final String PACKAGE_OPT = "p";
   public static final String PACKAGE_LONG_OPT = "package";
   public static final String HELP_OPT = "h";
   public static final String HELP_LONG_OPT = "help";
   public static final String MARSHALLER_OPT = "m";
   public static final String MARSHALLER_LONG_OPT = "marshaller";
   public static final String SCHEMA_OPT = "s";
   public static final String SCHEMA_LONG_OPT = "schema";

   /**
    * Set this flag to {@code true} to enable output of debug comments in the generated Protobuf schema.
    *
    * @deprecated
    */
   @Deprecated
   public static boolean generateSchemaDebugComments = false;

   private String fileName;

   private String packageName;

   private String generator;

   private final Set<Class<?>> classes = new LinkedHashSet<>();

   private boolean autoImportClasses = true;

   public static void main(String[] args) throws Exception {
      File file = null;
      String packageName = null;
      Map<String, String> schemas = new HashMap<>();
      List<String> marshallers = new ArrayList<>();
      List<String> classes = new ArrayList<>();

      for (int i = 0; i < args.length; i++) {
         switch (args[i]) {
            case "-h":
            case "--help":
               System.out.printf("ProtoStream %s %s%n", Version.getVersion(), ProtoSchemaBuilder.class.getSimpleName());
               return;
            case "-f":
            case "--file":
               file = new File(args[++i]);
               break;
            case "-p":
            case "--package":
               packageName = args[++i];
               break;
            case "-s":
            case "--schema":
               String[] parts = args[++i].split("=");
               schemas.put(parts[0], parts[1]);
               break;
            case "-m":
            case "--marshaller":
               marshallers.add(args[++i]);
               break;
            default:
               if (args[i].startsWith("-")) {
                  System.err.printf("Unknown option '%s'%n", args[i]);
                  return;
               } else {
                  classes.add(args[i]);
               }
         }
      }

      Configuration config = Configuration.builder().build();
      SerializationContext ctx = ProtobufUtil.newSerializationContext(config);

      for (Map.Entry<String, String> entry : schemas.entrySet()) {
         try (FileInputStream in = new FileInputStream(entry.getValue())) {
            ctx.registerProtoFiles(new FileDescriptorSource().addProtoFile(entry.getKey(), in));
         }
      }

      for (String marshallerClass : marshallers) {
         BaseMarshaller<?> bm = (BaseMarshaller<?>) Class.forName(marshallerClass).newInstance();
         ctx.registerMarshaller(bm);
      }

      String fileName = file == null ? DEFAULT_GENERATED_SCHEMA_NAME : file.getName();

      ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder()
            .fileName(fileName)
            .packageName(packageName);

      for (String className : classes) {
         protoSchemaBuilder.addClass(Class.forName(className));
      }

      String schemaFile = protoSchemaBuilder.build(ctx);

      if (file != null) {
         try (PrintStream out = new PrintStream(new FileOutputStream(file))) {
            out.print(schemaFile);
            out.flush();
         }
      } else {
         System.out.print(schemaFile);
      }
   }

   public ProtoSchemaBuilder() {
   }

   /**
    * Set the name of the Protobuf schema file to generate. This is mandatory. The resulting file will be registered in
    * the {@link SerializationContext} with this given name.
    *
    * @param fileName the name of the file to generate
    * @return itself
    */
   public ProtoSchemaBuilder fileName(String fileName) {
      if (fileName == null || fileName.trim().isEmpty()) {
         throw new IllegalArgumentException("fileName cannot be null or empty");
      }
      if (!fileName.endsWith(".proto")) {
         log.warnf("File name '%s' should end with '.proto'", fileName);
      }
      this.fileName = fileName;
      return this;
   }

   /**
    * Set the name of the Protobuf package to generate. This is optional.
    *
    * @param packageName the package name
    * @return itself, to help chaining calls
    */
   public ProtoSchemaBuilder packageName(String packageName) {
      if (packageName != null && packageName.trim().isEmpty()) {
         throw new IllegalArgumentException("packageName cannot be empty");
      }
      this.packageName = packageName;
      return this;
   }

   /**
    * Sets the 'generated by' comment. This is useful for documentation purposes.
    *
    * @param generator a string to be added as a comment in the generated schema
    */
   public ProtoSchemaBuilder generator(String generator) {
      this.generator = generator;
      return this;
   }

   /**
    * Add a @ProtoXyz annotated class to be analyzed. Proto schema and marshaller will be generated for it.
    * <p>
    * Its superclass and superinterfaces will be also included in the analysis but no separate Protobuf types and
    * marshallers will be generated for them as Protobuf does not have any notion of type hierarchy and inheritance. The
    * fields defined by the superclass or superinterfaces will be just included in the schema of the derived class.
    * <p>
    * Its inner classes will also be automatically processed if they are referenced by the outer class. If you want to
    * make sure an inner class is processed regardless if referenced or not you will have to add it explicitly using
    * {@code #addClass} or {@link #addClasses}.
    *
    * @param clazz the class to analyze
    * @return itself, to help chaining calls
    */
   public ProtoSchemaBuilder addClass(Class<?> clazz) {
      if (clazz == null) {
         throw new IllegalArgumentException("class argument cannot be null");
      }
      classes.add(clazz);
      return this;
   }

   /**
    * Add several @ProtoXyz annotated classes to be analyzed. Proto schema and marshaller will be generated for them.
    * <p>
    * Their superclasses and superinterfaces will be also included in the analysis but no separate Protobuf types and
    * marshallers will be generated for them as Protobuf does not have any notion of type hierarchy and inheritance. The
    * fields defined by the superclass or superinterfaces will be just included in the schema of the derived class.
    * <p>
    * Inner classes will also be automatically processed if they are referenced by the outer class. If you want to make
    * sure an inner class is processed regardless if referenced or not you will have to add it explicitly using
    * {@link #addClass} or {@code #addClasses}.
    *
    * @param classes the classes to analyze
    * @return itself, to help chaining calls
    */
   public ProtoSchemaBuilder addClasses(Class<?>... classes) {
      if (classes == null || classes.length == 0) {
         throw new IllegalArgumentException("classes argument cannot be null or empty");
      }
      Collections.addAll(this.classes, classes);
      return this;
   }

   /**
    * A flag to control processing of classes that were not directly added but were discovered by analyzing the
    * annotated fields/properties of the added classes. When such a class is found an error will be generated if
    * autoImportClasses is disabled. This flag is {@code true} by default to simplify usability (and also for backward
    * compatibility) but can be turned off whenever you need to be very specific about which classes are to be
    * processed. We encourage you to turn it off for maximum control.
    *
    * @param autoImportClasses
    * @return itself, to help chaining calls
    */
   public ProtoSchemaBuilder autoImportClasses(boolean autoImportClasses) {
      this.autoImportClasses = autoImportClasses;
      return this;
   }

   /**
    * Builds the Protocol Buffers schema file defining the types and generates marshaller implementations for these
    * types and registers everything with the given {@link SerializationContext}. The generated classes are defined in
    * the thread context ClassLoader.
    *
    * @param serializationContext
    * @return the generated Protocol Buffers schema file text
    * @throws ProtoSchemaBuilderException
    * @throws IOException
    */
   public String build(SerializationContext serializationContext) throws ProtoSchemaBuilderException, IOException {
      return build(serializationContext, null);
   }

   /**
    * Builds the Protocol Buffers schema file defining the types and generates marshaller implementations for these
    * types and registers everything with the given {@link SerializationContext}.The generated classes are defined in
    * the given ClassLoader.
    *
    * @param serializationContext
    * @param classLoader          the ClassLoader in which the generated classes will be defined. If {@code null}, this
    *                             behaves as {@link #build(SerializationContext)}
    * @return the generated Protocol Buffers schema file text
    * @throws ProtoSchemaBuilderException
    * @throws IOException
    */
   public String build(SerializationContext serializationContext, ClassLoader classLoader) throws ProtoSchemaBuilderException, IOException {
      ReflectionTypeFactory typeFactory = new ReflectionTypeFactory();
      Set<XClass> xclasses = classes.stream().map(typeFactory::fromClass).collect(Collectors.toCollection(LinkedHashSet::new));
      BaseProtoSchemaGenerator.generateSchemaDebugComments = generateSchemaDebugComments;
      return new RuntimeProtoSchemaGenerator(typeFactory, serializationContext, generator, fileName, packageName, xclasses, autoImportClasses, classLoader)
            .generateAndRegister();
   }
}
