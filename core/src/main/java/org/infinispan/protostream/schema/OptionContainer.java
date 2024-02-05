package org.infinispan.protostream.schema;

/**
 * @since 5.0
 */
public interface OptionContainer<T> extends GenericContainer {
   T addOption(String name, Object value);
}
