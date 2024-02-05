package org.infinispan.protostream.schema;

import java.lang.invoke.MethodHandles;

import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.impl.Log;
import org.infinispan.protostream.impl.parser.ProtostreamProtoParser;
import org.junit.Test;

public class ProtoBufSchemaTest {
   private static final Log log = Log.LogFactory.getLog(MethodHandles.lookup().lookupClass());

   @Test
   public void schemaTest() {
      Schema schema = new Schema.Builder("myschema.proto")
            .syntax(Syntax.PROTO3)
            .addImport("another.proto")
            .packageName("org.infinispan.protostream.test")

            .addMessage("Address")
               .addField(Type.Scalar.STRING, "street", 1)
               .addField(Type.Scalar.INT32, "number", 2)

            .addMessage("User")
               .addComment("@Indexed")
               .addField(Type.Scalar.INT32, "age", 1)
                  .addComment("@Field")
               .addField(Type.Scalar.STRING, "name", 2)
                  .addComment("@Field")
               .addRepeatedField(Type.create("Address"), "addresses", 3)
               .addNestedMessage("Nested",
                  m -> m.addComment("Nested comment").addField(Type.Scalar.BOOL, "is_it", 1)
               )
               .addOneOf("either",
                     o -> o.addOneOfField(Type.Scalar.STRING, "sub1", 4).addOneOfField(Type.Scalar.INT32, "sub2", 5)
               )
               .addMap(Type.Scalar.STRING, Type.Scalar.STRING, "properties", 6)
               .addEnum("gender")
                  .addComment("@Enum")
                  .addOption("allow_alias", false)
                  .addValue("male", 0)
                  .addValue("female", 1)
                     .addOption("custom_option", "hello")
                  .addValue("unspecified", 2)
               .addReservedRange(4, 10)
               .addReserved(15)
               .addReserved("dont_wanna_say")
            .build();
      ProtostreamProtoParser parser = new ProtostreamProtoParser(Configuration.builder().build());
      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile("file1.proto", schema.toString());
      if (log.isDebugEnabled()) {
         log.debug(schema);
      }
      parser.parse(fileDescriptorSource);
   }
}
