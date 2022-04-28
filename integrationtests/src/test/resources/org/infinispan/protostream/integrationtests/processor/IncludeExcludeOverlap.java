package test_include_exclude_overlap;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoField;

// includeClasses and excludeClasses are overlapping and must cause a compilation error

@AutoProtoSchemaBuilder(includeClasses = IncludeExcludeOverlap.Msg.class,
      excludeClasses = IncludeExcludeOverlap.Msg.class)
interface IncludeExcludeOverlap extends SerializationContextInitializer {

   class Msg {
      @ProtoField(1)
      public String txt;
   }
}
