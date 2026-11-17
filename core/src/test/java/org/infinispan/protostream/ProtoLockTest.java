package org.infinispan.protostream;

import static org.infinispan.protostream.impl.parser.DescriptorsTest.asFile;
import static org.infinispan.protostream.impl.parser.DescriptorsTest.parseAndResolve;
import static org.infinispan.protostream.impl.parser.DescriptorsTest.resolve;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.descriptors.ProtoLock;
import org.junit.jupiter.api.Test;

public class ProtoLockTest {

   private static final List<String> PROTO_FILES = List.of(
         "org/infinispan/protostream/message-wrapping.proto",
         "org/infinispan/protostream/lib/base.proto",
         "org/infinispan/protostream/lib/base2.proto",
         "org/infinispan/protostream/test/message.proto",
         "sample_bank_account/bank.proto"
   );

   @Test
   public void testProtoLock() throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ProtoLock protoLock1 = createProtoLock();
      protoLock1.writeLockFile(baos);
      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      ProtoLock protoLock2 = ProtoLock.readLockFile(bais);
      resolve(protoLock2.descriptors());
      assertEquals(protoLock1.descriptors().keySet(), protoLock2.descriptors().keySet());
      for (Map.Entry<String, FileDescriptor> descriptor : protoLock1.descriptors().entrySet()) {
         FileDescriptor d1 = descriptor.getValue();
         FileDescriptor d2 = protoLock2.descriptors().get(descriptor.getKey());
         assertEquals(d1.getPackage(), d2.getPackage());
         assertEquals(d1.getDependants().keySet(), d2.getDependants().keySet());
         assertEquals(d1.getEnumTypes().size(), d2.getEnumTypes().size());
         assertEquals(d1.getMessageTypes().size(), d2.getMessageTypes().size());
      }
   }

   @Test
   public void testLockFileFormat() throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      createProtoLock().writeLockFile(baos);
      String actual = baos.toString(StandardCharsets.UTF_8);

      String expected;
      try (InputStream is = ProtoLockTest.class.getResourceAsStream("/protolock/proto.lock")) {
         expected = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      }
      assertEquals(expected, actual);
   }

   @Test
   public void testReadLockFileRoundTrip() throws IOException {
      byte[] expected;
      try (InputStream is = ProtoLockTest.class.getResourceAsStream("/protolock/proto.lock")) {
         expected = is.readAllBytes();
      }
      ProtoLock protoLock = ProtoLock.readLockFile(new ByteArrayInputStream(expected));
      resolve(protoLock.descriptors());
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      protoLock.writeLockFile(baos);
      assertEquals(new String(expected, StandardCharsets.UTF_8), baos.toString(StandardCharsets.UTF_8));
   }

   @Test
   public void testReadLockFileMissingFields() {
      assertInvalid("{}", "missing 'definitions' in the root object");
      assertInvalid("{\"definitions\":[{\"def\":{}}]}", "missing 'protopath' in definition 0");
      assertInvalid("{\"definitions\":[{\"protopath\":\"a.proto\"}]}", "missing 'def' in definition 'a.proto'");
      assertInvalid(lockFile("\"messages\":[{\"name\":\"M\",\"fields\":[{\"name\":\"f\",\"type\":\"int32\"}]}]"),
            "missing 'id' in field 'f' of message 'p.M' of 'a.proto'");
      assertInvalid(lockFile("\"messages\":[{\"name\":\"M\",\"maps\":[{\"key_type\":\"string\"}]}]"),
            "missing 'field' in a map of message 'p.M' of 'a.proto'");
      assertInvalid(lockFile("\"enums\":[{\"name\":\"E\",\"enum_fields\":[{\"integer\":1}]}]"),
            "missing 'name' in a value of enum 'E' of 'a.proto'");
      assertInvalid(lockFile("\"enums\":[{\"name\":\"M.E\",\"enum_fields\":[]}]"),
            "unknown message 'M' containing enum 'M.E' of 'a.proto'");
   }

   private static String lockFile(String def) {
      return "{\"definitions\":[{\"protopath\":\"a.proto\",\"def\":{\"package\":{\"name\":\"p\"}," + def + "}}]}";
   }

   private static void assertInvalid(String lockFile, String message) {
      IOException e = assertThrows(IOException.class,
            () -> ProtoLock.readLockFile(new ByteArrayInputStream(lockFile.getBytes(StandardCharsets.UTF_8))));
      assertEquals("Invalid proto.lock file: " + message, e.getMessage());
   }

   private static ProtoLock createProtoLock() throws IOException {
      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      for (String f : PROTO_FILES) {
         fileDescriptorSource.addProtoFile(f, asFile(f));
      }
      Map<String, FileDescriptor> parseResult = parseAndResolve(fileDescriptorSource, Configuration.builder().build());
      return new ProtoLock(parseResult.values());
   }
}
