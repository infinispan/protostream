package org.infinispan.protostream.impl.parser.mappers;

import static org.infinispan.protostream.impl.parser.mappers.Mappers.FIELD_LIST_MAPPER;

import org.infinispan.protostream.descriptors.ExtendDescriptor;

import com.squareup.protoparser.ExtendElement;

/**
 * @author gustavonalle
 * @since 2.0
 */
final class ExtendMapper implements Mapper<ExtendElement, ExtendDescriptor> {

   @Override
   public ExtendDescriptor map(ExtendElement input) {
      return new ExtendDescriptor.Builder()
            .withName(input.name())
            .withFullName(input.qualifiedName())
            .withFields(FIELD_LIST_MAPPER.map(input.fields()))
            .build();
   }
}
