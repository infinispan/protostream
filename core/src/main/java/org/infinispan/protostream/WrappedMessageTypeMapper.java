package org.infinispan.protostream;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
public interface WrappedMessageTypeMapper {

   default int mapTypeId(int typeId, boolean isReading, ImmutableSerializationContext ctx) {
      return typeId;
   }

   default String mapTypeName(String typeName, boolean isReading, ImmutableSerializationContext ctx) {
      return typeName;
   }
}
