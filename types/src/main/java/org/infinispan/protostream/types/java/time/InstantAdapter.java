package org.infinispan.protostream.types.java.time;

import java.time.Instant;

import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;

/**
 * Adapter to convert {@link Instant} objects.
 *
 * <p>
 * To allow backwards compatibility, this converter uses the same names as the serialization embedded in the
 * {@link WrappedMessage}.
 * </p>
 */
@ProtoAdapter(Instant.class)
public class InstantAdapter {

   @ProtoFactory
   Instant create(Long seconds, Integer nanos) {
      return Instant.ofEpochSecond(seconds, nanos);
   }

   @ProtoField(number = WrappedMessage.WRAPPED_INSTANT_SECONDS, type = Type.INT64, name = "wrappedInstantSeconds")
   Long getSeconds(Instant instant) {
      return instant.getEpochSecond();
   }

   @ProtoField(number = WrappedMessage.WRAPPED_INSTANT_NANOS, type = Type.INT32, name = "wrappedInstantNanos")
   Integer getNanos(Instant instant) {
      return instant.getNano();
   }
}
