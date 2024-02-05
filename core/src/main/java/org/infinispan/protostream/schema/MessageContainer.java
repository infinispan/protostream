package org.infinispan.protostream.schema;

/**
 * @since 5.0
 */
public interface MessageContainer extends GenericContainer {
   Message.Builder addMessage(String name);
}
