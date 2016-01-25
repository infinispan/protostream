package org.infinispan.protostream.impl.parser.mappers;

import com.squareup.protoparser.EnumConstantElement;
import org.infinispan.protostream.descriptors.EnumValueDescriptor;

import static org.infinispan.protostream.impl.parser.mappers.Mappers.OPTION_LIST_MAPPER;


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
