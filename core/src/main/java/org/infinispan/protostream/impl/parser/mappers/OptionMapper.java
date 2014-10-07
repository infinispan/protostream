package org.infinispan.protostream.impl.parser.mappers;

import com.squareup.protoparser.Option;

/**
 * @author gustavonalle
 * @since 2.0
 */
final class OptionMapper implements Mapper<Option, org.infinispan.protostream.descriptors.Option> {

   @Override
   public org.infinispan.protostream.descriptors.Option map(Option input) {
      String name = input.getName();
      Object value = input.getValue();
      return new org.infinispan.protostream.descriptors.Option(name, value);
   }
}
