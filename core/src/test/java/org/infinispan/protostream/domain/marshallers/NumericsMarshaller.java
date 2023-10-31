package org.infinispan.protostream.domain.marshallers;

import java.io.IOException;

import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.domain.Numerics;

public class NumericsMarshaller implements MessageMarshaller<Numerics> {

   @Override
   public Class<? extends Numerics> getJavaClass() {
      return Numerics.class;
   }

   @Override
   public String getTypeName() {
      return "sample_bank_account.Numerics";
   }

   @Override
   public Numerics readFrom(ProtoStreamReader reader) throws IOException {
      return new Numerics(
            reader.readInt("simpleByte").byteValue(),
            reader.readInt("simpleShort").shortValue(),
            reader.readInt("simpleInt"),
            reader.readLong("simpleLong"),
            reader.readFloat("simpleFloat"),
            reader.readDouble("simpleDouble")
      );
   }

   @Override
   public void writeTo(ProtoStreamWriter writer, Numerics numerics) throws IOException {
      writer.writeInt("simpleByte", numerics.simpleByte());
      writer.writeInt("simpleShort", numerics.simpleShort());
      writer.writeInt("simpleInt", numerics.simpleInt());
      writer.writeLong("simpleLong", numerics.simpleLong());
      writer.writeFloat("simpleFloat", numerics.simpleFloat());
      writer.writeDouble("simpleDouble", numerics.simpleDouble());
   }
}
