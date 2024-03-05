package org.infinispan.protostream.integrationtests.compliance.handwritten;

import java.io.IOException;

import org.infinispan.protostream.MessageMarshaller;

public class CustomValueMarshaller implements MessageMarshaller<CustomValue> {
   @Override
   public Class<? extends CustomValue> getJavaClass() {
      return CustomValue.class;
   }

   @Override
   public String getTypeName() {
      return "org.infinispan.protostream.integrationtests.compliance.CustomValue";
   }

   @Override
   public CustomValue readFrom(ProtoStreamReader reader) throws IOException {
      return new CustomValue(reader.readInt("id"), reader.readString("s"));
   }

   @Override
   public void writeTo(ProtoStreamWriter writer, CustomValue mapsTest) throws IOException {
      writer.writeInt("id", mapsTest.id);
      writer.writeString("s", mapsTest.s);
   }
}
