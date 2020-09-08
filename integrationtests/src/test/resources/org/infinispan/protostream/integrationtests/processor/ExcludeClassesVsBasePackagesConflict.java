package test_exclude_classes_vs_base_packages_conflict;

import org.infinispan.protostream.*;
import org.infinispan.protostream.annotations.*;

// excludeClasses and basePackages are conflicting and must cause a compilation error

@AutoProtoSchemaBuilder(excludeClasses = ExcludeClassesVsBasePackagesConflict.Msg.class,
      basePackages = "org.infinispan.wrongPackage")
interface ExcludeClassesVsBasePackagesConflict extends SerializationContextInitializer {

   class Msg {
      @ProtoField(number = 1)
      public String txt;
   }
}
