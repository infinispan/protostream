package org.infinispan.protostream.schema;

/**
 * @since 5.0
 */
public interface ReservedContainer<T> extends GenericContainer {
   T addReserved(int... numbers);

   T addReservedRange(int from, int to);

   T addReserved(String name);
}
