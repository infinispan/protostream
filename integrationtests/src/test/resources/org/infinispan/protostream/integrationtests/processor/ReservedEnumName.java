package test_marshall_externals;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.annotations.ProtoReserved;


@AutoProtoSchemaBuilder(includeClasses = {
      ColorEnumAdapter.class
   },
   schemaFilePath = "/"
)
interface ReservedEnumName extends SerializationContextInitializer {
}

enum ColorEnum {
   RED, GREEN, BLUE
}

@ProtoAdapter(ColorEnum.class)
@ProtoName("Color")
@ProtoReserved(names = "green")
enum ColorEnumAdapter {

   @ProtoEnumValue(number = 0, name = "red")
   RED,

   @ProtoEnumValue(number = 1, name = "green")
   GREEN,

   @ProtoEnumValue(number = 2, name = "blue")
   BLUE
}
