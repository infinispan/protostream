package test_exclude_classes_vs_base_packages_conflict;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoField;

// excludeClasses and basePackages are conflicting and must cause a compilation error

@AutoProtoSchemaBuilder(excludeClasses = ExcludeClassesVsBasePackagesConflict.Msg.class,
      basePackages = "org.infinispan.wrongPackage")
interface ExcludeClassesVsBasePackagesConflict extends SerializationContextInitializer {

   class Msg {
      @ProtoField(1)
      public String txt;
   }
}
