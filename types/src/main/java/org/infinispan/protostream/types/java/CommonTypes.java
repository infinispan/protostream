package org.infinispan.protostream.types.java;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.types.java.math.BigDecimalAdapter;
import org.infinispan.protostream.types.java.math.BigIntegerAdapter;
import org.infinispan.protostream.types.java.util.UUIDAdapter;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
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
