package org.infinispan.protostream.impl.parser.mappers;

import com.squareup.protoparser.EnumElement;
import org.infinispan.protostream.descriptors.EnumDescriptor;

import static org.infinispan.protostream.impl.parser.mappers.Mappers.ENUM_VALUE_LIST_MAPPER;
import static org.infinispan.protostream.impl.parser.mappers.Mappers.OPTION_LIST_MAPPER;

/**
 * @author gustavonalle
 * @since 2.0
 */
final class EnumTypeMapper implements Mapper<EnumElement, EnumDescriptor> {

   @Override
   public EnumDescriptor map(EnumElement enumType) {
      return new EnumDescriptor.Builder()
            .withName(enumType.name())
            .withFullName(enumType.qualifiedName())
            .withValues(ENUM_VALUE_LIST_MAPPER.map(enumType.constants()))
            .withOptions(OPTION_LIST_MAPPER.map(enumType.options()))
            .withDocumentation(enumType.documentation())
            .build();
   }
}
