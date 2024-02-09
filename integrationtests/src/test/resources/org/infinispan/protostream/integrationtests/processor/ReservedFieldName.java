package test_basic_stuff;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoReserved;


@AutoProtoSchemaBuilder(includeClasses = {
      TestMessage.class
},
      schemaFilePath = "/"
)
interface ReservedFieldName extends SerializationContextInitializer {
}

@ProtoReserved(names = "txt")
class TestMessage {
   @ProtoField(1)
   String txt;
}
