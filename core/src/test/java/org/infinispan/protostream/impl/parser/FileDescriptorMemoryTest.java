package org.infinispan.protostream.impl.parser;

import java.util.Map;

import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.junit.Test;
import org.openjdk.jol.info.GraphLayout;

public class FileDescriptorMemoryTest {

   @Test
   public void testFileDescriptorMemoryFootprint() throws Exception {
      Configuration config = Configuration.builder().build();
      FileDescriptorSource source = FileDescriptorSource.fromResources("sample_bank_account/bank.proto");
      Map<String, FileDescriptor> descriptors = DescriptorsTest.parseAndResolve(source, config);
      FileDescriptor fileDescriptor = descriptors.get("sample_bank_account/bank.proto");

      GraphLayout layout = GraphLayout.parseInstance(fileDescriptor);
      System.out.println(layout.toFootprint());
      System.out.println("Total size: " + layout.totalSize() + " bytes");
   }
}
