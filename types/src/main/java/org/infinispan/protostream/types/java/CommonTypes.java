package org.infinispan.protostream.types.java;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(
      schemaFileName = "common-types.proto",
      schemaFilePath = "/infinispan",
      schemaPackageName = "org.infinispan.protostream.commons",
      includeClasses = {
            UUIDAdapter.class,
            BigIntegerAdapter.class,
            BigDecimalAdapter.class
      }
)
public interface CommonTypes extends GeneratedSchema {
}
