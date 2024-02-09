package org.infinispan.protostream.integrationtests.performance;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.domain.Address;
import org.infinispan.protostream.domain.Note;
import org.infinispan.protostream.domain.User;
import org.infinispan.protostream.domain.marshallers.NoteMarshaller;
import org.infinispan.protostream.domain.marshallers.UserMarshaller;
import org.infinispan.protostream.impl.Log;
import org.infinispan.protostream.test.AbstractProtoStreamTest;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
@Ignore
public class AnnotationsPerformanceTest extends AbstractProtoStreamTest {

   private static final Log log = Log.LogFactory.getLog(AnnotationsPerformanceTest.class);

   public static final int NUM_LOOPS = 10000000;

   @Test
   public void testReadWrite() throws Exception {
      SerializationContext ctx1 = createCtxWithHandWrittenMarshaller();
      SerializationContext ctx2 = createCtxWithGeneratedMarshaller();
      User user = new User();
      user.setId(1);
      user.setName("John");
      user.setSurname("Batman");
      user.setSalutation("Sir");
      user.setGender(User.Gender.MALE);
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3)));
      List<Address> addresses = new ArrayList<>();
      addresses.add(new Address("Old Street", "XYZ42", -12));
      addresses.add(new Address("Bond Street", "QQ42", 312));
      user.setAddresses(addresses);

      Note note = new Note();
      note.setText("Lorem Ipsum");
      note.setCreationDate(new Date());
      note.setDigest(new byte[10]);
      note.setBlurb(new byte[3]);
      note.setAuthor(user);

      Note note2 = new Note();
      note2.setText("Lorem Ipsum");
      note2.setAuthor(user);
      note2.setBlurb(new byte[3]);

      Note note3 = new Note();
      note3.setText("Lorem Ipsum");
      note3.setAuthor(user);
      note3.setBlurb(new byte[3]);

      note.note = note2;
      note.notes = Collections.singletonList(note3);

      byte[] bytes = writeWithProtoStream(ctx1, note);
      writeWithProtoStream(ctx2, note);

      long d1 = readWithProtoStream(ctx1, bytes);
      log.infof("ProtoStream read duration           = %d ns", d1);

      long d2 = readWithProtoStream(ctx2, bytes);
      log.infof("ProtoStream read duration           = %d ns", d2);
   }

   private long readWithProtoStream(SerializationContext ctx, byte[] bytes) throws IOException {
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      long tStart = System.nanoTime();
      for (int i = 0; i < NUM_LOOPS; i++) {
         ProtobufUtil.readFrom(ctx, bais, Note.class);
         bais.close();
         bais.reset();
      }
      long duration = System.nanoTime() - tStart;
      return duration / NUM_LOOPS;
   }

   private byte[] writeWithProtoStream(SerializationContext ctx, Note note) throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
      long tStart = System.nanoTime();
      for (int i = 0; i < NUM_LOOPS; i++) {
         ProtobufUtil.writeTo(ctx, out, note);
         if (i != NUM_LOOPS - 1) {
            out.reset();
         }
      }
      long duration = System.nanoTime() - tStart;
      log.infof("ProtoStream write duration          = %d ns", duration / NUM_LOOPS);
      return out.toByteArray();
   }

   private SerializationContext createCtxWithHandWrittenMarshaller() throws IOException {
      Configuration.Builder cfgBuilder = Configuration.builder()
            .setLogOutOfSequenceWrites(false)
            .setLogOutOfSequenceReads(false);
      SerializationContext ctx = createContext(cfgBuilder);

      String file = " package sample_bank_account;\n" +
            "import \"sample_bank_account/bank.proto\";\n" +
            "message Note {\n" +
            "    optional string text = 1;\n" +
            "    optional User author = 2;\n" +
            "    optional Note note = 3;\n" +
            "    repeated Note notes = 4;\n" +
            "    optional uint64 creationDate = 5 [default = 0];\n" +
            "    optional bytes digest = 6;\n" +
            "    required bytes blurb = 7;\n" +
            "}\n";

      ctx.registerProtoFiles(FileDescriptorSource.fromString("note.proto", file));
      ctx.registerMarshaller(new UserMarshaller());
      ctx.registerMarshaller(new NoteMarshaller());
      return ctx;
   }

   private SerializationContext createCtxWithGeneratedMarshaller() throws IOException {
      Configuration.Builder cfgBuilder = Configuration.builder()
            .setLogOutOfSequenceWrites(false)
            .setLogOutOfSequenceReads(false);
      SerializationContext ctx = createContext(cfgBuilder);
      NoteSchemaImpl schema = new NoteSchemaImpl();
      schema.registerSchema(ctx);
      schema.registerMarshallers(ctx);
      return ctx;
   }

   @AutoProtoSchemaBuilder(schemaFileName = "note.proto", schemaFilePath = "org/infinispan/protostream/generated_schemas", schemaPackageName = "sample_bank_account2",
         service = false,
         includeClasses = {
               Address.class,
               Note.class,
               User.class
         }
   )
   interface NoteSchema extends GeneratedSchema {
   }
}
