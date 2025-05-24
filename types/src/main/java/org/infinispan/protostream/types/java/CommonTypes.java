package org.infinispan.protostream.types.java;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoSchema;
import org.infinispan.protostream.types.java.math.BigDecimalAdapter;
import org.infinispan.protostream.types.java.math.BigIntegerAdapter;
import org.infinispan.protostream.types.java.time.DateAdapter;
import org.infinispan.protostream.types.java.time.InstantAdapter;
import org.infinispan.protostream.types.java.time.LocalDateAdapter;
import org.infinispan.protostream.types.java.time.LocalDateTimeAdapter;
import org.infinispan.protostream.types.java.time.LocalTimeAdapter;
import org.infinispan.protostream.types.java.time.MonthAdapter;
import org.infinispan.protostream.types.java.time.MonthDayAdapter;
import org.infinispan.protostream.types.java.time.OffsetTimeAdapter;
import org.infinispan.protostream.types.java.time.PeriodAdapter;
import org.infinispan.protostream.types.java.time.YearAdapter;
import org.infinispan.protostream.types.java.time.ZoneIdAdapter;
import org.infinispan.protostream.types.java.time.ZoneOffsetAdapter;
import org.infinispan.protostream.types.java.time.ZonedDateTimeAdapter;
import org.infinispan.protostream.types.java.util.BitSetAdapter;
import org.infinispan.protostream.types.java.util.UUIDAdapter;

/**
 * Support for marshalling some frequently used Java types from 'java.math' and java.util' packages.
 *
 * @author anistor@redhat.com
 * @since 4.4
 */
@ProtoSchema(
      className = "CommonTypesSchema",
      schemaFileName = "common-java-types.proto",
      schemaFilePath = "/org/infinispan/protostream/types",
      schemaPackageName = "org.infinispan.protostream.commons",
      includeClasses = {
            UUIDAdapter.class,
            BigIntegerAdapter.class,
            BigDecimalAdapter.class,
            BitSetAdapter.class,
            DateAdapter.class,
            InstantAdapter.class,
            LocalDateAdapter.class,
            LocalDateTimeAdapter.class,
            LocalTimeAdapter.class,
            MonthAdapter.class,
            MonthDayAdapter.class,
            OffsetTimeAdapter.class,
            PeriodAdapter.class,
            YearAdapter.class,
            ZonedDateTimeAdapter.class,
            ZoneIdAdapter.class,
            ZoneOffsetAdapter.class
      }
)
public interface CommonTypes extends GeneratedSchema {
}
