package org.infinispan.protostream.types.java.time;

import java.util.Date;

import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;

/**
 * Adapter to convert {@link Date} objects.
 *
 * <p>
 * To allow backwards compatibility, this converter uses the same names as the serialization embedded in the
 * {@link WrappedMessage}.
 * </p>
 */
@ProtoAdapter(Date.class)
public class DateAdapter {

   @ProtoFactory
   Date create(Long epoch) {
      return new Date(epoch);
   }

   @ProtoField(number = WrappedMessage.WRAPPED_DATE_MILLIS, type = Type.INT64, name = "wrappedDateMillis")
   Long getEpoch(Date date) {
      return date.getTime();
   }
}
