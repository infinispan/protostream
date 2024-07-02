package org.infinispan.protostream.integrationtests.processor.marshaller;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchema;
import org.infinispan.protostream.annotations.ProtoSyntax;
import org.infinispan.protostream.exception.ProtoStreamException;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.FootballSchema;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.FootballSchemaImpl;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.FootballTeam;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.MapSchema;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.ModelWithMap;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.NullTestModel;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.Player;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.SimpleEnum;
import org.junit.Test;

public class GeneratedMarshallerTest {

   @Test
   public void testGenericMessage() {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();

      GeneratedSchema generatedSchema = new FootballSchemaImpl();
      generatedSchema.registerSchema(ctx);
      generatedSchema.registerMarshallers(ctx);

      assertTrue(generatedSchema.getProtoFile().contains("message Player"));

      FootballTeam footBallTeam = new FootballTeam();
      footBallTeam.setName("New-Team");

      Player player = new Player("fax4ever", footBallTeam, 9, 10, null);
      footBallTeam.setPlayers(Collections.singletonList(player));

      assertThatThrownBy(() -> ProtobufUtil.toWrappedByteArray(ctx, player))
            .isInstanceOf(ProtoStreamException.class)
            .hasMessageContaining("IPROTO000008");
   }

   @Test
   public void testMaps() throws IOException {
      var ctx = ProtobufUtil.newSerializationContext();
      MapSchema.INSTANCE.registerSchema(ctx);
      MapSchema.INSTANCE.registerMarshallers(ctx);

      var adapter = Map.of("a", UUID.randomUUID());
      var simple = Map.of("b", 1);
      var enumMap = Map.of("c", SimpleEnum.FIRST);
      var maps = new ModelWithMap();

      maps.setAdapterMap(adapter);
      maps.setSimpleMap(simple);
      maps.setEnumMap(enumMap);

      var bytes = ProtobufUtil.toWrappedByteArray(ctx, maps);
      ModelWithMap copy = ProtobufUtil.fromWrappedByteArray(ctx, bytes);

      assertEquals(maps.getAdapterMap(), copy.getAdapterMap());
      assertEquals(maps.getSimpleMap(), copy.getSimpleMap());
      assertEquals(maps.getEnumMap(), copy.getEnumMap());
   }

   @Test
   public void proto2NullFields() throws IOException {
      var ctx = ProtobufUtil.newSerializationContext();
      FootballSchema.INSTANCE.registerSchema(ctx);
      FootballSchema.INSTANCE.registerMarshallers(ctx);

      var bytes = ProtobufUtil.toWrappedByteArray(ctx, new Player(null, null, null, 0, null));
      Player player = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertNull(player.getName());
      assertNull(player.getShirtNumber());
      assertEquals(0, player.getMatchRating());
      assertNull(player.getBytes());
   }

   @Test
   public void proto3AllowNullFields() throws IOException {
      var ctx = ProtobufUtil.newSerializationContext();
      var schema = new NullsAllowedSchemaImpl();
      schema.registerSchema(ctx);
      schema.registerMarshallers(ctx);

      var bytes = ProtobufUtil.toWrappedByteArray(ctx, new NullTestModel());
      NullTestModel model = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertNull(model.string);
      assertNull(model.boxedInt);
      assertEquals(0, model.primitiveInt);
      assertNull(model.bytes);
      assertNull(model.simpleEnum);
   }

   @Test
   public void proto3NoNullFields() throws IOException {
      var ctx = ProtobufUtil.newSerializationContext();
      var schema = new NoNullsSchemaImpl();
      schema.registerSchema(ctx);
      schema.registerMarshallers(ctx);

      var bytes = ProtobufUtil.toWrappedByteArray(ctx, new NullTestModel());
      NullTestModel model = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertEquals("", model.string);
      assertEquals((Integer) 0, model.boxedInt);
      assertEquals(0, model.primitiveInt);
      assertEquals(0, model.bytes.length);
      assertEquals(SimpleEnum.FIRST, model.simpleEnum);
   }

   @ProtoSchema(
         includeClasses = {
               NullTestModel.class,
               SimpleEnum.class
         },
         schemaPackageName = "nonulls",
         schemaFilePath = "proto",
         schemaFileName = "nonulls.proto",
         syntax = ProtoSyntax.PROTO3
   )
   interface NoNullsSchema extends GeneratedSchema {
   }

   @ProtoSchema(
         allowNullFields = true,
         includeClasses = {
               NullTestModel.class,
               SimpleEnum.class
         },
         schemaPackageName = "allownulls",
         schemaFilePath = "proto",
         schemaFileName = "allownulls.proto",
         syntax = ProtoSyntax.PROTO3
   )
   interface NullsAllowedSchema extends GeneratedSchema {
   }
}
