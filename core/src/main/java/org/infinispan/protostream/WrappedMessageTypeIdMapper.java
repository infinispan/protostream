package org.infinispan.protostream;

/**
 * <b>Experimental</b> hook for re-mapping type ids during reading/writing of WrappedMessages. TypeIds are supposed to
 * be constant over the entire evolution of a schema, but in case they need to be changed we provide this hook to smooth
 * the transition.
 *
 * @author anistor@redhat.com
 * @since 4.3
 * @deprecated since 4.3.1 because it does not suit the intended purpose. Will be removed in 4.4.x with no replacement.
 */
@Deprecated
public interface WrappedMessageTypeIdMapper {

   /**
    * Called during decoding.
    *
    * @param typeId a positive integer, the type id of the message being decoded
    * @param ctx    the serialization context
    * @return the same TypeId or a different one
    */
   default int mapTypeIdIn(int typeId, ImmutableSerializationContext ctx) {
      return typeId;
   }

   /**
    * Called during encoding.
    *
    * @param typeId a positive integer
    * @param ctx    the serialization context
    * @return the same TypeId, a different TypeId, or -1 to force dropping the use of TypeIds for current WrappedMessage
    * and use the type name instead
    */
   default int mapTypeIdOut(int typeId, ImmutableSerializationContext ctx) {
      return typeId;
   }
}
