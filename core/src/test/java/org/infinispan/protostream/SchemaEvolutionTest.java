package org.infinispan.protostream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.infinispan.protostream.impl.parser.DescriptorsTest.asFile;
import static org.infinispan.protostream.impl.parser.DescriptorsTest.parseAndResolve;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
      List<String> errors = new ArrayList<>();
      v1.checkCompatibility(v2, true, errors);
      assertThat(errors).size().isEqualTo(12);
      assertThat(errors).containsExactlyInAnyOrder(
            "IPROTO000035: Field 'evolution.m1.f1' number was changed from 1 to 8",
            "IPROTO000036: Field 'evolution.m1.f2' was removed, but its name has not been reserved",
            "IPROTO000037: Field 'evolution.m1.f2' was removed, but its number 2 has not been reserved",
            "IPROTO000038: Field 'evolution.m1.f3''s type was changed from 'bool' to 'sfixed32'",
            "IPROTO000033: Type 'evolution.m1' no longer reserves field names '[f6]'",
            "IPROTO000034: Type 'evolution.m1' no longer reserves field numbers '{6, 7}'",
            "IPROTO000036: Field 'evolution.e1.V2' was removed, but its name has not been reserved",
            "IPROTO000037: Field 'evolution.e1.V2' was removed, but its number 2 has not been reserved",
            "IPROTO000033: Type 'evolution.e1' no longer reserves field names '[V4]'",
            "IPROTO000034: Type 'evolution.e1' no longer reserves field numbers '{4, 5}'",
            "IPROTO000040: Incompatible @ProtoTypeId in 'evolution.m1', from '10002' to 'null'",
            "IPROTO000040: Incompatible @ProtoTypeId in 'evolution.e1', from '10001' to '10003'"
      );
   }
}
