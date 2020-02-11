package org.infinispan.protostream.types.java;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(
      schemaFileName = "common-container-types.proto",
      schemaFilePath = "/infinispan",
      schemaPackageName = "org.infinispan.protostream.commons",
      includeClasses = {
            IntArrayAdapter.class,
            IntegerArrayAdapter.class,
            ArrayListAdapter.class,
            HashSetAdapter.class
      }
)
public interface CommonContainerTypes extends GeneratedSchema {
}
