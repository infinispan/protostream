package org.infinispan.protostream.integrationtests.transcoding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.Play;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.PlaySchemaImpl;
import org.infinispan.protostream.types.java.CommonContainerTypesSchema;
import org.junit.Test;

public class JSONTranscodingTest {

   @Test
   public void listOfStrings_rootObject() throws Exception {
      SerializationContext context = ProtobufUtil.newSerializationContext();

      new CommonContainerTypesSchema().registerSchema(context);
      new CommonContainerTypesSchema().registerMarshallers(context);

      ArrayList<String> matches = new ArrayList<>();
      matches.add("one");
      matches.add("two");

      byte[] proto = ProtobufUtil.toWrappedByteArray(context, matches);
      String json = ProtobufUtil.toCanonicalJSON(context, proto);
      assertThat(json).isEqualTo("{\"_type\":\"org.infinispan.protostream.commons.ArrayList\",\"wrappedContainerSize\":2,\"wrappedContainerMessage\":\"\",\"_value\":[{\"string\":\"one\"},{\"string\":\"two\"}]}");

      ArrayList<String> reloaded = ProtobufUtil.fromWrappedByteArray(context, proto);
      assertThat(reloaded).isEqualTo(matches);
   }

   @Test
   public void listOfStrings_asField() throws Exception {
      SerializationContext context = ProtobufUtil.newSerializationContext();

      new CommonContainerTypesSchema().registerSchema(context);
      new CommonContainerTypesSchema().registerMarshallers(context);
      new PlaySchemaImpl().registerSchema(context);
      new PlaySchemaImpl().registerMarshallers(context);

      ArrayList<String> matches = new ArrayList<>();
      matches.add("one");
      matches.add("two");
      Play play = new Play("game", matches);

      byte[] proto = ProtobufUtil.toWrappedByteArray(context, play);
      String json = ProtobufUtil.toCanonicalJSON(context, proto);
      assertThat(json).isEqualTo("{\"_type\":\"my.Play\",\"play_name\":\"game\",\"matches\":[\"one\",\"two\"]}");

      Play reloaded = ProtobufUtil.fromWrappedByteArray(context, proto);
      assertThat(reloaded).isEqualTo(play);
   }

   @Test
   public void wrappedString() throws Exception {
      SerializationContext context = ProtobufUtil.newSerializationContext();

      byte[] proto = ProtobufUtil.toWrappedByteArray(context, "play");
      String json = ProtobufUtil.toCanonicalJSON(context, proto);
      assertThat(json).isEqualTo("{\"_type\":\"string\",\"_value\":\"play\"}");

      String reloaded = ProtobufUtil.fromWrappedByteArray(context, proto);
      assertThat(reloaded).isEqualTo("play");
   }
}
