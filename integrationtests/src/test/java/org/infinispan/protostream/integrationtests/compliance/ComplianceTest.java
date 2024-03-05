package org.infinispan.protostream.integrationtests.compliance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.integrationtests.compliance.handwritten.CustomValue;
import org.infinispan.protostream.integrationtests.compliance.handwritten.CustomValueMarshaller;
import org.infinispan.protostream.integrationtests.compliance.handwritten.MapsTest;
import org.infinispan.protostream.integrationtests.compliance.handwritten.MapsTestMarshaller;
import org.junit.Test;


public class ComplianceTest {

   @Test
   public void testMap() throws IOException {
      Maps.MapsTest.Builder builder = Maps.MapsTest.newBuilder();
      builder.putAllStringmap(
            Map.of(
                  "k1", "v1",
                  "k2", "v2"
            )
      );
      builder.putAllCustommap(
            Map.of(
                  1, Maps.CustomValue.newBuilder().setId(1).setS("s1").build(),
                  2, Maps.CustomValue.newBuilder().setId(2).setS("s2").build()
            )
      );
      Maps.MapsTest pcMap = builder.build();
      ByteArrayOutputStream pcBaos = new ByteArrayOutputStream();
      pcMap.writeTo(pcBaos);

      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      ctx.registerProtoFiles(FileDescriptorSource.fromResources("/maps.proto"));
      ctx.registerMarshaller(new CustomValueMarshaller());
      ctx.registerMarshaller(new MapsTestMarshaller());

      MapsTest psMap = new MapsTest();
      psMap.stringmap = Map.of(
            "k1", "v1",
            "k2", "v2"
      );
      psMap.custommap = Map.of(
            1, new CustomValue(1, "s1"),
            2, new CustomValue(2, "s2")
      );
      ByteArrayOutputStream psBaos = new ByteArrayOutputStream();
      ProtobufUtil.writeTo(ctx, psBaos, psMap);
      assertThat(psBaos.toByteArray()).isEqualTo(pcBaos.toByteArray());

      Files.write(Path.of("/tmp/pc.data"), pcBaos.toByteArray());
      Files.write(Path.of("/tmp/ps.data"), psBaos.toByteArray());

      MapsTest pc2psMap = ProtobufUtil.fromByteArray(ctx, pcBaos.toByteArray(), MapsTest.class);
      assertThat(pc2psMap.stringmap).containsAllEntriesOf(psMap.stringmap);
      assertThat(pc2psMap.custommap).containsAllEntriesOf(psMap.custommap);
   }
}
