package test_include_exclude_overlap;

import org.infinispan.protostream.*;
import org.infinispan.protostream.annotations.*;

// includeClasses and excludeClasses are overlapping and must cause a compilation error

@AutoProtoSchemaBuilder(includeClasses = IncludeExcludeOverlap.Msg.class,
      excludeClasses = IncludeExcludeOverlap.Msg.class)
interface IncludeExcludeOverlap extends SerializationContextInitializer {

   class Msg {
      @ProtoField(number = 1)
      public String txt;
   }
}
