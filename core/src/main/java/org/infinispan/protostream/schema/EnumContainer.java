package org.infinispan.protostream.schema;

/**
 * @since 5.0
 */
public interface EnumContainer extends MessageContainer, GenericContainer {
   Enum.Builder addEnum(String name);
}
