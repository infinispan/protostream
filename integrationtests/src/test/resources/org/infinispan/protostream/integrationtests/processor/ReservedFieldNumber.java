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
interface ReservedFieldNumber extends SerializationContextInitializer {
}

@ProtoReserved(1)
class TestMessage {
   @ProtoField(1)
   String txt;
}
