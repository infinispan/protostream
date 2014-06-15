package org.infinispan.protostream.impl.parser.mappers;

import com.squareup.protoparser.EnumType;
import org.infinispan.protostream.descriptors.EnumValueDescriptor;

import static org.infinispan.protostream.impl.parser.mappers.Mappers.OPTION_LIST_MAPPER;


/**
 * @author gustavonalle
 * @since 2.0
 */
class EnumValueTypeMapper implements Mapper<EnumType.Value, EnumValueDescriptor> {
   @Override
   public EnumValueDescriptor map(EnumType.Value input) {
      return new EnumValueDescriptor.Builder()
              .withName(input.getName())
              .withTag(input.getTag())
              .withOptions(OPTION_LIST_MAPPER.map(input.getOptions()))
              .build();
   }
}
