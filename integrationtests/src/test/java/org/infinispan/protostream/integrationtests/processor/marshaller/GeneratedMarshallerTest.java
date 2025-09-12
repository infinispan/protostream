package org.infinispan.protostream.integrationtests.processor.marshaller;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchema;
import org.infinispan.protostream.annotations.ProtoSyntax;
import org.infinispan.protostream.exception.ProtoStreamException;
import org.infinispan.protostream.impl.JsonUtils;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.FootballSchema;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.FootballSchemaImpl;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.FootballTeam;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.IterableModel;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.MapOfLong;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.MapOfMapOfUUID;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.MapOfString;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.MapOfUUID;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.MapOverlappingMarshallerDelegate;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.MapSchema;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.ModelWithMap;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.NullTestModel;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.Player;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.SimpleEnum;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.SimpleRecord;
import org.infinispan.protostream.integrationtests.processor.marshaller.model.StreamModel;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GeneratedMarshallerTest {
   static final UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");

   @Test
   public void testGenericMessage() {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();

      GeneratedSchema generatedSchema = new FootballSchemaImpl();
      generatedSchema.register(ctx);

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
   public void testRegisterTwice() {
      var ctx = ProtobufUtil.newSerializationContext();
      MapSchema.INSTANCE.register(ctx);
      MapSchema.INSTANCE.register(ctx);
   }

   @Test
   public void testMaps() throws IOException {
      var ctx = ProtobufUtil.newSerializationContext();
      MapSchema.INSTANCE.register(ctx);

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
   public void testMapsOverlappingMarshallerDelegate() throws IOException {
      var ctx = ProtobufUtil.newSerializationContext();
      MapSchema.INSTANCE.register(ctx);

      var overlappingMap =  new MapOverlappingMarshallerDelegate(BigInteger.ONE, Map.of(1, UUID.randomUUID()));
      var bytes = ProtobufUtil.toWrappedByteArray(ctx, overlappingMap);
      var copy = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertEquals(overlappingMap, copy);
   }

   @Test
   public void proto2NullFields() throws IOException {
      var ctx = ProtobufUtil.newSerializationContext();
      FootballSchema.INSTANCE.register(ctx);

      var bytes = ProtobufUtil.toWrappedByteArray(ctx, new Player(null, null, null, 0, null));
      Player player = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertNull(player.getName());
      assertNull(player.getShirtNumber());
      assertEquals(0, player.getMatchRating());
      assertNull(player.getBytes());

      bytes = ProtobufUtil.toWrappedByteArray(ctx, new SimpleRecord(null, null));
      SimpleRecord record = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertNull(record.string());
      assertNull(record.boxedInt());
   }

   @Test
   public void proto3AllowNullFields() throws IOException {
      var ctx = ProtobufUtil.newSerializationContext();
      var schema = new NullsAllowedSchemaImpl();
      schema.register(ctx);

      var bytes = ProtobufUtil.toWrappedByteArray(ctx, new NullTestModel());
      NullTestModel model = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertNull(model.string);
      assertNull(model.boxedInt);
      assertEquals(0, model.primitiveInt);
      assertNull(model.bytes);
      assertNull(model.simpleEnum);

      bytes = ProtobufUtil.toWrappedByteArray(ctx, new SimpleRecord(null, null));
      SimpleRecord record = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertNull(record.string());
      assertNull(record.boxedInt());
   }

   @Test
   public void proto3NoNullFields() throws IOException {
      var ctx = ProtobufUtil.newSerializationContext();
      var schema = new NoNullsSchemaImpl();
      schema.register(ctx);

      var bytes = ProtobufUtil.toWrappedByteArray(ctx, new NullTestModel());
      NullTestModel model = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertEquals("", model.string);
      assertEquals((Integer) 0, model.boxedInt);
      assertEquals(0, model.primitiveInt);
      assertEquals(0, model.bytes.length);
      assertEquals(SimpleEnum.FIRST, model.simpleEnum);

      bytes = ProtobufUtil.toWrappedByteArray(ctx, new SimpleRecord(null, null));
      SimpleRecord record = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertEquals("", record.string());
      assertEquals((Integer) 0, record.boxedInt());
   }

   @ProtoSchema(
         includeClasses = {
               NullTestModel.class,
               SimpleEnum.class,
               SimpleRecord.class
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
               SimpleEnum.class,
               SimpleRecord.class
         },
         schemaPackageName = "allownulls",
         schemaFilePath = "proto",
         schemaFileName = "allownulls.proto",
         syntax = ProtoSyntax.PROTO3
   )
   interface NullsAllowedSchema extends GeneratedSchema {}


   @Test
   public void testMapOfStringToJson() throws IOException {
      var ctx = ProtobufUtil.newSerializationContext();
      MapSchema.INSTANCE.register(ctx);

      var m = new MapOfString();
      m.data = Map.of("1", "2", "3", "4");

      var bytes = ProtobufUtil.toWrappedByteArray(ctx, m);
      String json = JsonUtils.toCanonicalJSON(ctx, bytes, false);
      assertJson(
            """
                  {
                     "_type":"generic.MapOfString",
                     "data":{"1":"2","3":"4"}
                  }
                  """,
            json
      );
      byte[] bytes2 = JsonUtils.fromCanonicalJSON(ctx, new StringReader(json));
      assertArrayEquals(bytes, bytes2);
   }

   @Test
   public void testMapOfUUIDToJson() throws IOException {
      var ctx = ProtobufUtil.newSerializationContext();
      MapSchema.INSTANCE.register(ctx);

      var m = new MapOfUUID();
      m.data = Map.of("1", uuid, "3", uuid);

      var bytes = ProtobufUtil.toWrappedByteArray(ctx, m);
      assertJson("""
                  {
                     "_type":"generic.MapOfUUID",
                     "data":{
                        "1":{"mostSigBitsFixed":0,"leastSigBitsFixed":0},
                        "3":{"mostSigBitsFixed":0,"leastSigBitsFixed":0}
                     }
                  }
                  """,
            JsonUtils.toCanonicalJSON(ctx, bytes, false));
   }

   @Test
   public void testMapOfMapOfUUIDToJson() throws IOException {
      var ctx = ProtobufUtil.newSerializationContext();
      MapSchema.INSTANCE.register(ctx);

      var m = new MapOfUUID();
      m.data = Map.of("1", uuid, "3", uuid);
      var m2 = new MapOfMapOfUUID();
      m2.data1 = Map.of("1", "2", "3", "4");
      m2.data2 = m;
      m2.data3 = SimpleEnum.SECOND;

      var bytes = ProtobufUtil.toWrappedByteArray(ctx, m2);
      MapOfMapOfUUID copy = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertNotNull(copy);
      String json = JsonUtils.toCanonicalJSON(ctx, bytes, false);
      assertJson("""
            {
               "_type":"generic.MapOfMapOfUUID",
               "data1":{
                  "3":"4",
                  "1":"2"
               },
               "data2":{
                  "data":{
                     "3":{"mostSigBitsFixed":0,"leastSigBitsFixed":0},
                     "1":{"mostSigBitsFixed":0,"leastSigBitsFixed":0}
                  }
               },
               "data3":"SECOND"
            }
            """, json);

      byte[] bytes2 = JsonUtils.fromCanonicalJSON(ctx, new StringReader(json));
      assertArrayEquals(bytes, bytes2);
   }

   @Test
   public void testMapOfLong() throws Exception {
      var ctx = ProtobufUtil.newSerializationContext();
      MapSchema.INSTANCE.register(ctx);

      var key = "1";
      var value = (Long) 1L;
      var m = new MapOfLong();
      m.data = Map.of(key, value);
      var bytes = ProtobufUtil.toWrappedByteArray(ctx, m);
      MapOfLong copy = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertEquals(value, copy.data.get(key));
   }

   @Test
   public void testMapOfMapToJson() throws IOException {
      var ctx = ProtobufUtil.newSerializationContext();
      MapSchema.INSTANCE.register(ctx);

      var adapter = Map.of("a", uuid);
      var simple = Map.of("b", 2);
      var enumMap = Map.of("c", SimpleEnum.SECOND);
      var maps = new ModelWithMap();

      maps.setAdapterMap(adapter);
      maps.setSimpleMap(simple);
      maps.setEnumMap(enumMap);

      var bytes = ProtobufUtil.toWrappedByteArray(ctx, maps);
      ModelWithMap copy = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertNotNull(copy);
      String json = JsonUtils.toCanonicalJSON(ctx, bytes, false);
      assertJson("""
              {
                   "_type": "generic.ModelWithMap",
                   "simpleMap": {
                       "b": 2
                   },
                   "adapterMap": {
                       "a": {"mostSigBitsFixed":0,"leastSigBitsFixed":0}
                   },
                   "enumMap": {
                       "c": "SECOND"
                   }
               }
              """, json);

      byte[] bytes2 = JsonUtils.fromCanonicalJSON(ctx, new StringReader(json));
      assertArrayEquals(bytes, bytes2);
   }

   @Test
   public void testIterableFields() throws IOException{
      var ctx = ProtobufUtil.newSerializationContext();
      IterableModel.Schema.INSTANCE.register(ctx);

      LinkedList<String> list = new LinkedList<>();
      list.add("entry1");
      list.add("entry2");
      var ccf = new IterableModel.CustomCollectionFactory(list);
      var bytes = ProtobufUtil.toWrappedByteArray(ctx, ccf);
      IterableModel.CustomCollectionFactory unmarshalledCCF = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertEquals(list.size(), unmarshalledCCF.strings.size());
      assertTrue(unmarshalledCCF.strings instanceof LinkedList);

      var dc = new IterableModel.DefaultCollectionFactory(list);
      bytes = ProtobufUtil.toWrappedByteArray(ctx, dc);
      IterableModel.DefaultCollectionFactory unmarshalledDC = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertEquals(list.size(), unmarshalledDC.strings.size());
      assertTrue(unmarshalledDC.strings instanceof ArrayList<String>);

      var gs = new IterableModel.GetterSetter();
      gs.setStrings(list);
      bytes = ProtobufUtil.toWrappedByteArray(ctx, gs);
      IterableModel.GetterSetter unmarshalledGS = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      int numEntries = 0;
      for (String s : unmarshalledGS.getStrings()) numEntries++;
      assertEquals(list.size(), numEntries);
      assertTrue(unmarshalledGS.strings instanceof ArrayList<String>);

      var factory = new IterableModel.IterableFactory(list);
      bytes = ProtobufUtil.toWrappedByteArray(ctx, factory);
      IterableModel.IterableFactory unmarshalledFactory = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertEquals(list.size(), unmarshalledFactory.strings.size());
      assertTrue(unmarshalledFactory.strings instanceof ArrayList<String>);
   }

   @Test
   public void testStreamFields() throws IOException {
      var ctx = ProtobufUtil.newSerializationContext();
      StreamModel.StreamSchema.INSTANCE.register(ctx);

      var factory = new StreamModel.Factory(Stream.of("entry1", "entry2"));
      var bytes = ProtobufUtil.toWrappedByteArray(ctx, factory);
      StreamModel.Factory unmarshalledFactory = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertEquals(2, unmarshalledFactory.strings.length);

      var gs = new StreamModel.GetterSetter("entry1");
      bytes = ProtobufUtil.toWrappedByteArray(ctx, gs);
      StreamModel.GetterSetter unmarshalledGS = ProtobufUtil.fromWrappedByteArray(ctx, bytes);
      assertEquals(1, unmarshalledGS.strings.length);
   }

   private static void assertJson(String j1, String j2) throws IOException {
      ObjectMapper mapper = new ObjectMapper();
      assertEquals(mapper.readTree(j1), mapper.readTree(j2));
   }
}
