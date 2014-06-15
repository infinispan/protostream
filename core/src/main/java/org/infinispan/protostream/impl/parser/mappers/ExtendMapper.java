package org.infinispan.protostream.impl.parser.mappers;

import com.squareup.protoparser.ExtendDeclaration;
import org.infinispan.protostream.descriptors.ExtendDescriptor;

import static org.infinispan.protostream.impl.parser.mappers.Mappers.FIELD_LIST_MAPPER;

/**
 * @author gustavonalle
 * @since 2.0
 */
class ExtendMapper implements Mapper<ExtendDeclaration, ExtendDescriptor> {
   @Override
   public ExtendDescriptor map(ExtendDeclaration input) {
      return new ExtendDescriptor.Builder()
              .withName(input.getName())
              .withFullName(input.getFullyQualifiedName())
              .withFields(FIELD_LIST_MAPPER.map(input.getFields()))
              .build();
   }
}
