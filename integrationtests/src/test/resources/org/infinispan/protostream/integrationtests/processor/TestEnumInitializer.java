package test_enum_initializer;

import org.infinispan.protostream.*;
import org.infinispan.protostream.annotations.*;

@AutoProtoSchemaBuilder
public interface TestEnumInitializer extends SerializationContextInitializer {
}

class OuterClass {

   enum InnerEnum {
      @ProtoEnumValue(number = 1) OPTION_A,
      @ProtoEnumValue(number = 2) OPTION_B
   }
}