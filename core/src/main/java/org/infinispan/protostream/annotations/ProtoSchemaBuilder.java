package org.infinispan.protostream.annotations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.Version;
import org.infinispan.protostream.annotations.impl.ProtoSchemaGenerator;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.impl.Log;

/**
 * Generates a Protocol Buffers schema definition file based on a set of {@code @Proto*} annotated classes.
 * <p>
 * See {@link ProtoName}, {@link ProtoMessage}, {@link ProtoField}, {@link ProtoEnum}, {@link ProtoEnumValue}, {@link
 * ProtoDoc} and {@link ProtoUnknownFieldSet}.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
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
    */
   public static boolean generateSchemaDebugComments = false;

   private String fileName;

   private String packageName;

   private final Set<Class<?>> classes = new HashSet<>();

   public static void main(String[] args) throws Exception {
      CommandLine cmd = parseCommandLine(args);
      if (cmd == null) {
         return;
      }

      String packageName = cmd.getOptionValue(PACKAGE_LONG_OPT);

      Configuration config = Configuration.builder().build();
      SerializationContext ctx = ProtobufUtil.newSerializationContext(config);

      Properties schemas = cmd.getOptionProperties(SCHEMA_LONG_OPT);
      if (schemas != null) {
         for (String schema : schemas.stringPropertyNames()) {
            String file = schemas.getProperty(schema);
            try (FileInputStream in = new FileInputStream(file)) {
               ctx.registerProtoFiles(new FileDescriptorSource().addProtoFile(schema, in));
            }
         }
      }

      String[] marshallers = cmd.getOptionValues(MARSHALLER_LONG_OPT);
      if (marshallers != null) {
         for (String marshaller : marshallers) {
            BaseMarshaller<?> bm = (BaseMarshaller) Class.forName(marshaller).newInstance();
            ctx.registerMarshaller(bm);
         }
      }

      File file = cmd.hasOption(FILE_LONG_OPT) ? new File(cmd.getOptionValue(FILE_LONG_OPT)) : null;
      String fileName = file == null ? DEFAULT_GENERATED_SCHEMA_NAME : file.getName();

      ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder()
            .fileName(fileName)
            .packageName(packageName);

      for (String className : cmd.getArgs()) {
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

   private static CommandLine parseCommandLine(String[] args) throws ParseException {
      Option f = new Option(FILE_OPT, FILE_LONG_OPT, true, "Output schema file name (optional)");
      Option p = new Option(PACKAGE_OPT, PACKAGE_LONG_OPT, true, "The Protobuf package name of the generated schema (optional)");
      Option h = new Option(HELP_OPT, HELP_LONG_OPT, false, "Print usage information and exit immediately");
      Option m = new Option(MARSHALLER_OPT, MARSHALLER_LONG_OPT, true, "Register an existing marshaller class to be available for 'includes' (optional, multiple)");
      Option s = new Option(SCHEMA_OPT, SCHEMA_LONG_OPT, true, "Register an existing Protobuf schema to be available for 'includes' (optional, multiple)");
      s.setArgs(2);
      s.setValueSeparator('=');
      Options options = new Options();
      options.addOption(f);
      options.addOption(p);
      options.addOption(h);
      options.addOption(m);
      options.addOption(s);
      CommandLineParser parser = new GnuParser();
      CommandLine cmd = parser.parse(options, args);

      if (cmd.hasOption(HELP_OPT)) {
         HelpFormatter formatter = new HelpFormatter();
         formatter.setSyntaxPrefix("ProtoStream " + Version.getVersion() + " " + ProtoSchemaBuilder.class.getSimpleName());
         formatter.printHelp(200, " usage: java " + ProtoSchemaBuilder.class.getName() + " [options] <list of fully qualified class names to process>",
               "Options: ", options, "The list of class names is separated by whitespace.");
         return null;
      }

      return cmd;
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
         log.warnf("fileName '%s' should end with '.proto'", fileName);
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
    * Add a @ProtoXyz annotated class to be analyzed. Proto schema and marshaller will be generated for it.
    * <p>
    * Its superclass and superinterfaces will be also included in the analysis but no separate Protobuf types and
    * marshallers will be generated for them as Protobuf does not have any notion of type hierarchy and inheritance.
    * The fields defined by the superclass or superinterfaces will be just included in the schema of the derived class.
    * <p>
    * Its inner classes will also be automatically processed if they are referenced by the outer class. If you want to
    * make sure an inner class is processed regardless if referenced or not you will have to add it explicitly using
    * {@code addClass}.
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
    * Builds the Protocol Buffers schema file defining the types and generates marshaller implementations for these
    * types and registers everything with the given {@link SerializationContext}.
    *
    * @param serializationContext
    * @return the generated Protocol Buffers schema file text
    * @throws ProtoSchemaBuilderException
    * @throws IOException
    */
   public String build(SerializationContext serializationContext) throws ProtoSchemaBuilderException, IOException {
      return new ProtoSchemaGenerator(serializationContext, fileName, packageName, classes)
            .generateAndRegister();
   }
}
