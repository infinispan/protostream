package org.infinispan.protostream.integrationtests.compliance.handwritten;

import java.io.IOException;
import java.util.HashMap;

import org.infinispan.protostream.MessageMarshaller;

public class MapsTestMarshaller implements MessageMarshaller<MapsTest> {
   @Override
   public Class<? extends MapsTest> getJavaClass() {
      return MapsTest.class;
   }

   @Override
   public String getTypeName() {
      return "org.infinispan.protostream.integrationtests.compliance.MapsTest";
   }

   @Override
   public MapsTest readFrom(ProtoStreamReader reader) throws IOException {
      MapsTest mapsTest = new MapsTest();
      mapsTest.stringmap = reader.readMap("stringmap", new HashMap<>(), String.class, String.class);
      mapsTest.custommap = reader.readMap("custommap", new HashMap<>(), Integer.class, CustomValue.class);
      return mapsTest;
   }

   @Override
   public void writeTo(ProtoStreamWriter writer, MapsTest mapsTest) throws IOException {
      writer.writeMap("stringmap", mapsTest.stringmap, String.class, String.class);
      writer.writeMap("custommap", mapsTest.custommap, Integer.class, CustomValue.class);
   }
}
