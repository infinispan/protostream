package org.infinispan.protostream.impl.parser.mappers;

import com.squareup.protoparser.EnumType;
import org.infinispan.protostream.descriptors.EnumDescriptor;

import static org.infinispan.protostream.impl.parser.mappers.Mappers.ENUM_VALUE_LIST_MAPPER;
import static org.infinispan.protostream.impl.parser.mappers.Mappers.OPTION_LIST_MAPPER;

/**
 * @author gustavonalle
 * @since 2.0
 */
class EnumTypeMapper implements Mapper<EnumType, EnumDescriptor> {

   @Override
   public EnumDescriptor map(EnumType am) {
      return new EnumDescriptor.Builder()
              .withName(am.getName())
              .withFullName(am.getFullyQualifiedName())
              .withValues(ENUM_VALUE_LIST_MAPPER.map(am.getValues()))
              .withOptions(OPTION_LIST_MAPPER.map(am.getOptions()))
              .build();
   }
}
