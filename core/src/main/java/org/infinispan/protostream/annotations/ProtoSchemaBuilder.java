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
import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.impl.ProtoSchemaGenerator;
import org.infinispan.protostream.config.Configuration;

/**
 * Generates a Protocol Buffers schema definition file based on a set of @Proto* annotated classes.
 * <p/>
 * See {@link ProtoMessage}, {@link ProtoField}, {@link ProtoEnum}, {@link ProtoEnumValue}, {@link ProtoDoc} and {@link
 * ProtoUnknownFieldSet}.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
public final class ProtoSchemaBuilder {

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
    * Set this to {@code true} to enable output of debug comments in the generated protobuf schema.
    */
   public static boolean generateSchemaDebugComments = false;

   private String fileName;

   private String packageName;

   private final Set<Class<?>> classes = new HashSet<>();

   public static void main(String[] args) throws Exception {
      Option f = new Option(FILE_OPT, FILE_LONG_OPT, true, "output file name");
      Option p = new Option(PACKAGE_OPT, PACKAGE_LONG_OPT, true, "Protobuf package name");
      p.setRequired(true);
      Option h = new Option(HELP_OPT, HELP_LONG_OPT, false, "Print usage information");
      Option m = new Option(MARSHALLER_OPT, MARSHALLER_LONG_OPT, true, "Register custom marshaller class");
      Option s = new Option(SCHEMA_OPT, SCHEMA_LONG_OPT, true, "Register Protobuf schema");
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
         formatter.printHelp("java " + ProtoSchemaBuilder.class.getName(), "Arguments: ", options, "followed by a list of class names to process");
         return;
      }

      String packageName = cmd.getOptionValue(PACKAGE_LONG_OPT);

      Configuration config = Configuration.builder().build();
      SerializationContext ctx = ProtobufUtil.newSerializationContext(config);

      Properties schemas = cmd.getOptionProperties(SCHEMA_LONG_OPT);
      if (schemas != null) {
         for (String schema : schemas.stringPropertyNames()) {
            String file = schemas.getProperty(schema);
            FileInputStream in = null;
            try {
               in = new FileInputStream(file);
               ctx.registerProtoFiles(new FileDescriptorSource().addProtoFile(schema, in));
            } finally {
               if (in != null) {
                  in.close();
               }
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
      String fileName = file == null ? "generated.proto" : file.getName();

      ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder()
            .fileName(fileName)
            .packageName(packageName);

      for (String className : cmd.getArgs()) {
         protoSchemaBuilder.addClass(Class.forName(className));
      }

      String schema = protoSchemaBuilder.build(ctx);

      if (file != null) {
         try (PrintStream out = new PrintStream(new FileOutputStream(file))) {
            out.print(schema);
         }
      } else {
         System.out.print(schema);
      }
   }

   public ProtoSchemaBuilder() {
   }

   public ProtoSchemaBuilder fileName(String fileName) {
      this.fileName = fileName;
      return this;
   }

   public ProtoSchemaBuilder packageName(String packageName) {
      if (packageName.trim().isEmpty()) {
         throw new IllegalArgumentException("packageName cannot be empty");
      }
      this.packageName = packageName;
      return this;
   }

   public ProtoSchemaBuilder addClass(Class<?> clazz) {
      classes.add(clazz);
      return this;
   }

   /**
    * Builds the Protocol Buffers schema file and marshallers and registers them with  the given {@link
    * SerializationContext}.
    *
    * @param serializationContext
    * @return the generated Protocol Buffers schema file
    * @throws ProtoSchemaBuilderException
    * @throws IOException
    */
   public String build(SerializationContext serializationContext) throws ProtoSchemaBuilderException, IOException {
      if (fileName == null) {
         throw new ProtoSchemaBuilderException("fileName cannot be null");
      }
      if (classes.isEmpty()) {
         throw new ProtoSchemaBuilderException("At least one class must be specified");
      }
      String schema = new ProtoSchemaGenerator(serializationContext, fileName, packageName, classes).generateAndRegister();

      fileName = null;
      packageName = null;
      classes.clear();

      return schema;
   }
}
