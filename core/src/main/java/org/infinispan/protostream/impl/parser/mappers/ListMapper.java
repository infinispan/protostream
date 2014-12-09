package org.infinispan.protostream.impl.parser.mappers;

import java.util.LinkedList;
import java.util.List;

/**
 * Converts the content of a list, given a mapper to convert its elements.
 *
 * @param <S> Type of the source list
 * @param <T> Type of the converted list
 * @author gustavonalle
 * @since 2.0
 */
final class ListMapper<S, T> implements Mapper<List<S>, List<T>> {

   private final Mapper<S, T> mapper;

   private ListMapper(Mapper<S, T> mapper) {
      this.mapper = mapper;
   }

   @Override
   public List<T> map(List<S> input) {
      List<T> mapped = new LinkedList<T>();
      for (S elem : input) {
         mapped.add(mapper.map(elem));
      }
      return mapped;
   }

   public static <S, T> ListMapper<S, T> forMapper(Mapper<S, T> elementMapper) {
      return new ListMapper<S, T>(elementMapper);
   }
}
