package org.infinispan.protostream;

/**
 * Hook for re-mapping type ids during reading/writing of WrappedMessages. TypeIds are supposed to be constant over the
 * entire lifetime of a schema, but in case they need to be changed we provide this hook to smooth the transition.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
public interface WrappedMessageTypeMapper {

   /**
    * Called during reading.
    *
    * @param typeId
    * @param ctx
    * @return the same TypeId or a different one
    */
   default int mapTypeIdIn(int typeId, ImmutableSerializationContext ctx) {
      return typeId;
   }

   /**
    * Called during writing.
    *
    * @param typeId
    * @param ctx
    * @return the same TypeId, a different TypeId or -1 to force dropping the use of TypeIds for current WrappedMessage
    * and use type name instead
    */
   default int mapTypeIdOut(int typeId, ImmutableSerializationContext ctx) {
      return typeId;
   }
}
