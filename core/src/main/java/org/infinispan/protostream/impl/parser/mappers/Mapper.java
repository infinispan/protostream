package org.infinispan.protostream.impl.parser.mappers;

/**
 * Converter for objects
 *
 * @param <S> Source type
 * @param <T> converted type
 * @since 2.0
 */
interface Mapper<S, T> {

   T map(S input);

}
