package org.infinispan.protostream.impl.parser.mappers;

import com.squareup.protoparser.MessageType;
import com.squareup.protoparser.Option;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.Rule;

import java.util.List;

import static org.infinispan.protostream.impl.parser.mappers.Mappers.OPTION_LIST_MAPPER;

/**
 * @author gustavonalle
 * @since 2.0
 */
final class FieldMapper implements Mapper<MessageType.Field, FieldDescriptor> {

   @Override
   public FieldDescriptor map(MessageType.Field am) {
      String defaultValue = am.getDefault();

      MessageType.Label label = am.getLabel();
      List<Option> options = am.getOptions();
      Rule rule = Rule.valueOf(label.name());

      return new FieldDescriptor.Builder()
              .withName(am.getName())
              .withNumber(am.getTag())
              .withTypeName(am.getType())
              .withDefaultValue(defaultValue)
              .withRule(rule)
              .withOptions(OPTION_LIST_MAPPER.map(options))
              .withDocumentation(am.getDocumentation())
              .build();
   }
}
