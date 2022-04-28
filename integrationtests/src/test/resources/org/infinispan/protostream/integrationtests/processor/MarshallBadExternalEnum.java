package test_marshall_bad_external_enum;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoName;


@AutoProtoSchemaBuilder(includeClasses = BadColorEnumAdapter.class, schemaFilePath = "/")
interface MarshallBadExternalEnum extends SerializationContextInitializer {
}

enum ColorEnum {
   RED
}

@ProtoAdapter(ColorEnum.class)
@ProtoName("Color")
enum BadColorEnumAdapter { // enum values do not have 1 to 1 correspondence ...

   @ProtoEnumValue(number = 0, name = "red")
   RED,

   @ProtoEnumValue(number = 3, name = "pink")
   PINK
}
