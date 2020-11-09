package test_marshall_bad_external_enum;

import java.util.UUID;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoEnumValue;


@AutoProtoSchemaBuilder(includeClasses = BadColorEnumAdapter.class, schemaFilePath = "/")
interface MarshallBadExternalEnum extends SerializationContextInitializer {
}

enum Color {
   RED
}

@ProtoAdapter(Color.class)
enum BadColorEnumAdapter {

   @ProtoEnumValue(number = 0, name = "red")
   RED,

   @ProtoEnumValue(number = 3, name = "pink")
   PINK
}
