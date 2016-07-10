package org.infinispan.protostream.impl.parser.mappers;

import static org.infinispan.protostream.impl.parser.mappers.Mappers.OPTION_LIST_MAPPER;

import org.infinispan.protostream.descriptors.EnumValueDescriptor;

import com.squareup.protoparser.EnumConstantElement;


/**
 * @author gustavonalle
 * @since 2.0
 */
final class EnumValueTypeMapper implements Mapper<EnumConstantElement, EnumValueDescriptor> {

   @Override
   public EnumValueDescriptor map(EnumConstantElement input) {
      return new EnumValueDescriptor.Builder()
            .withName(input.name())
            .withTag(input.tag())
            .withDocumentation(input.documentation())
            .withOptions(OPTION_LIST_MAPPER.map(input.options()))
            .build();
   }
}
