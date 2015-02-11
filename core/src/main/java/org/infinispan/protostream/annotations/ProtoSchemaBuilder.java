package org.infinispan.protostream.annotations;

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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Generates a Protocol Buffers schema definition file based on a set of @Proto* annotated classes.
 * <p/>
 * See {@link ProtoMessage}, {@link ProtoField}, {@link ProtoEnum}, {@link ProtoEnumValue} and {@link
 * ProtoUnknownFieldSet}.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
public final class ProtoSchemaBuilder {

   /**
    * Set this to {@code true} to enable output of debug comments in the generated protobuf schema.
    */
   public static boolean generateSchemaDebugComments = false;

   private String fileName;

   private String packageName;

   private final Set<Class<?>> classes = new HashSet<Class<?>>();

   public static void main(String[] args) throws Exception {
      Option f = new Option("f", "file", true, "output file name");
      Option p = new Option("p", "package", true, "Protobuf package name");
      p.setRequired(true);
      Option h = new Option("h", "help", false, "Print usage information");
      Option m = new Option("m", "marshaller", true, "Register custom marshaller class");
      Option s = new Option("s", "schema", true, "Register Protobuf schema");
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

      if (cmd.hasOption('h')) {
         HelpFormatter formatter = new HelpFormatter();
         formatter.printHelp("java " + ProtoSchemaBuilder.class.getName(), "Arguments: ", options, "followed by an arbitrary list of class names to process");
         return;
      }

      String packageName = cmd.getOptionValue("package");

      Configuration config = new Configuration.Builder().build();
      SerializationContext ctx = ProtobufUtil.newSerializationContext(config);

      Properties schemas = cmd.getOptionProperties("schema");
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

      String[] marshallers = cmd.getOptionValues("marshaller");
      if (marshallers != null) {
         for (String marshaller : marshallers) {
            BaseMarshaller bm = (BaseMarshaller) Class.forName(marshaller).newInstance();
            ctx.registerMarshaller(bm);
         }
      }

      ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder()
            .fileName("generated.proto")
            .packageName(packageName);

      for (String className : cmd.getArgs()) {
         protoSchemaBuilder.addClass(Class.forName(className));
      }

      String schema = protoSchemaBuilder.build(ctx);

      if (cmd.hasOption("file")) {
         String file = cmd.getOptionValue("file");
         PrintStream out = new PrintStream(new FileOutputStream(file));
         try {
            out.println(schema);
         } finally {
            out.close();
         }

      } else {
         System.out.println(schema);
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
