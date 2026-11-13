package org.infinispan.protostream;

import static org.infinispan.protostream.impl.parser.DescriptorsTest.asFile;
import static org.infinispan.protostream.impl.parser.DescriptorsTest.parseAndResolve;
import static org.infinispan.protostream.impl.parser.DescriptorsTest.resolve;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

   private static ProtoLock createProtoLock() throws IOException {
      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      for (String f : PROTO_FILES) {
         fileDescriptorSource.addProtoFile(f, asFile(f));
      }
      Map<String, FileDescriptor> parseResult = parseAndResolve(fileDescriptorSource, Configuration.builder().build());
      return new ProtoLock(parseResult.values());
   }
}
