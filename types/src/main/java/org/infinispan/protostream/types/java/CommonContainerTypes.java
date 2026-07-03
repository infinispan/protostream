package org.infinispan.protostream.types.java;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoSchema;
import org.infinispan.protostream.types.java.arrays.BooleanArrayAdapter;
import org.infinispan.protostream.types.java.arrays.BoxedBooleanArrayAdapter;
import org.infinispan.protostream.types.java.arrays.BoxedByteArrayAdapter;
import org.infinispan.protostream.types.java.arrays.BoxedDoubleArrayAdapter;
import org.infinispan.protostream.types.java.arrays.BoxedFloatArrayAdapter;
import org.infinispan.protostream.types.java.arrays.BoxedIntegerArrayAdapter;
import org.infinispan.protostream.types.java.arrays.BoxedLongArrayAdapter;
import org.infinispan.protostream.types.java.arrays.BoxedShortArrayAdapter;
import org.infinispan.protostream.types.java.arrays.DoubleArrayAdapter;
import org.infinispan.protostream.types.java.arrays.FloatArrayAdapter;
import org.infinispan.protostream.types.java.arrays.IntArrayAdapter;
import org.infinispan.protostream.types.java.arrays.LongArrayAdapter;
import org.infinispan.protostream.types.java.arrays.ObjectArrayAdapter;
import org.infinispan.protostream.types.java.arrays.ShortArrayAdapter;
import org.infinispan.protostream.types.java.arrays.StringArrayAdapter;
import org.infinispan.protostream.types.java.collections.ArrayListAdapter;
import org.infinispan.protostream.types.java.collections.HashSetAdapter;
import org.infinispan.protostream.types.java.collections.LinkedHashSetAdapter;
import org.infinispan.protostream.types.java.collections.LinkedListAdapter;
import org.infinispan.protostream.types.java.collections.TreeSetAdapter;
import org.infinispan.protostream.types.java.util.MapAdapters;
import org.infinispan.protostream.types.java.util.MapEntryAdapter;

/**
 * Support for marshalling various {@link java.util.Collection} implementations and array or primitives.
 *
 * @author anistor@redhat.com
 * @since 4.4
 */
@ProtoSchema(
      className = "CommonContainerTypesSchema",
      schemaFileName = "common-java-container-types.proto",
      schemaFilePath = "/org/infinispan/protostream/types/java",
      schemaPackageName = "org.infinispan.protostream.commons",
      includeClasses = {
            // collections
            ArrayListAdapter.class,
            LinkedListAdapter.class,
            HashSetAdapter.class,
            LinkedHashSetAdapter.class,
            TreeSetAdapter.class,
            // arrays
            BooleanArrayAdapter.class,
            ShortArrayAdapter.class,
            IntArrayAdapter.class,
            LongArrayAdapter.class,
            FloatArrayAdapter.class,
            DoubleArrayAdapter.class,
            BoxedBooleanArrayAdapter.class,
            BoxedByteArrayAdapter.class,
            BoxedShortArrayAdapter.class,
            BoxedIntegerArrayAdapter.class,
            BoxedLongArrayAdapter.class,
            BoxedFloatArrayAdapter.class,
            BoxedDoubleArrayAdapter.class,
            StringArrayAdapter.class,
            ObjectArrayAdapter.class,

            // maps
            MapEntryAdapter.class,
            MapAdapters.HashMapAdapter.class,
            MapAdapters.ConcurrentHashMapAdapter.class,
            MapAdapters.LinkedHashMapAdapter.class,
            MapAdapters.TreeMapAdapter.class,
            MapAdapters.WeakHashMapAdapter.class,
            MapAdapters.IdentityHashMapAdapter.class,
            MapAdapters.ConcurrentSkipListMapAdapter.class,
            MapAdapters.HashtableAdapter.class,
            MapAdapters.PropertiesAdapter.class,
            MapAdapters.CollectionsEmptyMap.class,
            MapAdapters.CollectionSingletonMap.class,
      }
)
public interface CommonContainerTypes extends GeneratedSchema {
}
