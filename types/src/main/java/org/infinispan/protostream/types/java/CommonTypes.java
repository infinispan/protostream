package org.infinispan.protostream.types.java;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.types.java.math.BigDecimalAdapter;
import org.infinispan.protostream.types.java.math.BigIntegerAdapter;
import org.infinispan.protostream.types.java.util.UUIDAdapter;

/**
 * Support for marshalling some frequently used Java types from 'java.math' and java.util' packages.
 *
 * @author anistor@redhat.com
 * @since 4.4
 */
@AutoProtoSchemaBuilder(
      className = "CommonTypesSchema",
      schemaFileName = "common-java-types.proto",
      schemaFilePath = "/protostream",
      schemaPackageName = "org.infinispan.protostream.commons",
      includeClasses = {
            UUIDAdapter.class,
            BigIntegerAdapter.class,
            BigDecimalAdapter.class
      }
)
public interface CommonTypes extends GeneratedSchema {
}
