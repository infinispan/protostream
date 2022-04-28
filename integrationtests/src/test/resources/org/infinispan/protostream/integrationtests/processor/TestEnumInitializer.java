package test_enum_initializer;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoEnumValue;

@AutoProtoSchemaBuilder
public interface TestEnumInitializer extends SerializationContextInitializer {
}

class OuterClass {

   enum InnerEnum {
      @ProtoEnumValue(1) OPTION_A,
      @ProtoEnumValue(2) OPTION_B
   }
}