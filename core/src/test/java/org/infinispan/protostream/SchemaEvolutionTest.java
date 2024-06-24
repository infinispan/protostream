package org.infinispan.protostream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.infinispan.protostream.impl.parser.DescriptorsTest.asFile;
import static org.infinispan.protostream.impl.parser.DescriptorsTest.parseAndResolve;

import java.io.IOException;

import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.junit.Test;

public class SchemaEvolutionTest {

   @Test
   public void testSchemaEvolution() throws IOException {
      FileDescriptorSource v1s = new FileDescriptorSource();
      v1s.addProtoFile("evolution/v1.proto", asFile("evolution/v1.proto"));
      FileDescriptor v1 = parseAndResolve(v1s, Configuration.builder().build()).values().iterator().next();
      FileDescriptorSource v2s = new FileDescriptorSource();
      v2s.addProtoFile("evolution/v2.proto", asFile("evolution/v2.proto"));
      FileDescriptor v2 = parseAndResolve(v2s, Configuration.builder().build()).values().iterator().next();
      try {
         v1.checkCompatibility(v2, true);
      } catch (IllegalStateException e) {
         assertThat(e.getMessage()).contains("IPROTO000039");
         assertThat(e.getMessage()).contains("IPROTO000035: Field 'evolution.m1.f1' number was changed from 1 to 8");
         assertThat(e.getMessage()).contains("IPROTO000036: Field 'evolution.m1.f2' was removed, but its name has not been reserved");
         assertThat(e.getMessage()).contains("IPROTO000037: Field 'evolution.m1.f2' was removed, but its number 2 has not been reserved");
         assertThat(e.getMessage()).contains("IPROTO000038: Field 'evolution.m1.f3''s type was changed from 'bool' to 'sfixed32'");
         assertThat(e.getMessage()).contains("IPROTO000033: Type 'evolution.m1' no longer reserves field names '[f6]'");
         assertThat(e.getMessage()).contains("IPROTO000034: Type 'evolution.m1' no longer reserves field numbers '{6, 7}'");
         assertThat(e.getMessage()).contains("IPROTO000036: Field 'evolution.e1.V2' was removed, but its name has not been reserved");
         assertThat(e.getMessage()).contains("IPROTO000037: Field 'evolution.e1.V2' was removed, but its number 2 has not been reserved");
         assertThat(e.getMessage()).contains("IPROTO000033: Type 'evolution.e1' no longer reserves field names '[V4]'");
         assertThat(e.getMessage()).contains("IPROTO000034: Type 'evolution.e1' no longer reserves field numbers '{4, 5}'");
      }
   }
}
