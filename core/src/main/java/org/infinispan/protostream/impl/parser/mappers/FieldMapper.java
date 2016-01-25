package org.infinispan.protostream.impl.parser.mappers;

import com.squareup.protoparser.FieldElement;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.Rule;

import static org.infinispan.protostream.impl.parser.mappers.Mappers.OPTION_LIST_MAPPER;

/**
 * @author gustavonalle
 * @since 2.0
 */
final class FieldMapper implements Mapper<FieldElement, FieldDescriptor> {

   @Override
   public FieldDescriptor map(FieldElement am) {
      String defaultValue = am.getDefault() != null ? am.getDefault().value().toString() : null;     //todo [anistor]
      return new FieldDescriptor.Builder()
            .withName(am.name())
            .withNumber(am.tag())
            .withTypeName(am.type().toString())
            .withDefaultValue(defaultValue)   //todo [anistor]
            .withRule(Rule.valueOf(am.label().name()))
            .withOptions(OPTION_LIST_MAPPER.map(am.options()))
            .withDocumentation(am.documentation())
            .build();
   }
}
