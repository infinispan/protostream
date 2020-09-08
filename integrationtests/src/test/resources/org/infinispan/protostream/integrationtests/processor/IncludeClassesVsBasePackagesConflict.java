package test_include_classes_vs_base_packages_conflict;

import org.infinispan.protostream.*;
import org.infinispan.protostream.annotations.*;

// includeClasses and basePackages are conflicting and must cause a compilation error

@AutoProtoSchemaBuilder(includeClasses = IncludeClassesVsBasePackagesConflict.Msg.class,
      basePackages = "org.infinispan.wrongPackage")
interface IncludeClassesVsBasePackagesConflict extends SerializationContextInitializer {

   class Msg {
      @ProtoField(number = 1)
      public String txt;
   }
}
