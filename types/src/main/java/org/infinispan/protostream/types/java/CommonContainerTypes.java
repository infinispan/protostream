package org.infinispan.protostream.types.java;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.types.java.arrays.BooleanArrayAdapter;
import org.infinispan.protostream.types.java.arrays.ByteArrayAdapter;
import org.infinispan.protostream.types.java.arrays.DoubleArrayAdapter;
import org.infinispan.protostream.types.java.arrays.FloatArrayAdapter;
import org.infinispan.protostream.types.java.arrays.IntArrayAdapter;
import org.infinispan.protostream.types.java.arrays.IntegerArrayAdapter;
import org.infinispan.protostream.types.java.arrays.LongArrayAdapter;
import org.infinispan.protostream.types.java.arrays.ShortArrayAdapter;
import org.infinispan.protostream.types.java.arrays.StringArrayAdapter;
import org.infinispan.protostream.types.java.collections.ArrayListAdapter;
import org.infinispan.protostream.types.java.collections.HashSetAdapter;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@AutoProtoSchemaBuilder(
      schemaFileName = "common-container-types.proto",
      schemaFilePath = "/infinispan",
      schemaPackageName = "org.infinispan.protostream.commons",
      includeClasses = {
            IntArrayAdapter.class,
            BooleanArrayAdapter.class,
            ByteArrayAdapter.class,
            ShortArrayAdapter.class,
            IntegerArrayAdapter.class,
            LongArrayAdapter.class,
            FloatArrayAdapter.class,
            DoubleArrayAdapter.class,
            StringArrayAdapter.class,
            ArrayListAdapter.class,
            HashSetAdapter.class
      }
)
public interface CommonContainerTypes extends GeneratedSchema {
}
