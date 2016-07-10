package org.infinispan.protostream.impl.parser.mappers;

import static org.infinispan.protostream.impl.parser.mappers.Mappers.OPTION_LIST_MAPPER;

import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.Label;

import com.squareup.protoparser.FieldElement;

/**
 * @author gustavonalle
 * @since 2.0
 */
final class FieldMapper implements Mapper<FieldElement, FieldDescriptor> {

   @Override
   public FieldDescriptor map(FieldElement am) {
      return new FieldDescriptor.Builder()
            .withName(am.name())
            .withNumber(am.tag())
            .withTypeName(am.type().toString())
            .withDefaultValue(am.getDefault() != null ? (String) am.getDefault().value() : null)
            .withLabel(Label.valueOf(am.label().name()))
            .withOptions(OPTION_LIST_MAPPER.map(am.options()))
            .withDocumentation(am.documentation())
            .build();
   }
}
