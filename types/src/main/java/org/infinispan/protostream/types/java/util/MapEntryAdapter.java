package org.infinispan.protostream.types.java.util;

import java.util.Map;

import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * Adapter for {@link Map.Entry} wrapper used in map serialization.
 *
 * @author José Bolina
 * @since 6.0
 */
@SuppressWarnings("rawtypes")
@ProtoAdapter(AbstractMapAdapter.MapEntryWrapper.class)
public class MapEntryAdapter {

   @ProtoFactory
   AbstractMapAdapter.MapEntryWrapper create(WrappedMessage key, WrappedMessage value) {
      return (AbstractMapAdapter.MapEntryWrapper) AbstractMapAdapter.MapEntryWrapper.create(key.getValue(), value.getValue());
   }

   @ProtoField(number = 1)
   WrappedMessage getKey(Map.Entry entry) {
      return new WrappedMessage(entry.getKey());
   }

   @ProtoField(number = 2)
   WrappedMessage getValue(Map.Entry entry) {
      return new WrappedMessage(entry.getValue());
   }
}
