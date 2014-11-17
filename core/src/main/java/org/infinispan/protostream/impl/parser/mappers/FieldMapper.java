package org.infinispan.protostream.impl.parser.mappers;

import com.squareup.protoparser.MessageType;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.Rule;

import static org.infinispan.protostream.impl.parser.mappers.Mappers.OPTION_LIST_MAPPER;

/**
 * @author gustavonalle
 * @since 2.0
 */
final class FieldMapper implements Mapper<MessageType.Field, FieldDescriptor> {

   @Override
   public FieldDescriptor map(MessageType.Field am) {
      return new FieldDescriptor.Builder()
            .withName(am.getName())
            .withNumber(am.getTag())
            .withTypeName(am.getType())
            .withDefaultValue(am.getDefault())
            .withRule(Rule.valueOf(am.getLabel().name()))
            .withOptions(OPTION_LIST_MAPPER.map(am.getOptions()))
            .withDocumentation(am.getDocumentation())
            .build();
   }
}
