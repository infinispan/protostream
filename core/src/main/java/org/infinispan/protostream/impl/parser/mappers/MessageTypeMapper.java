package org.infinispan.protostream.impl.parser.mappers;

import com.squareup.protoparser.*;
import org.infinispan.protostream.descriptors.Descriptor;

import java.util.List;

import static org.infinispan.protostream.impl.parser.mappers.Mappers.*;

/**
 * Mapper for Type.
 *
 * @author gustavonalle
 * @since 2.0
 */
class MessageTypeMapper implements Mapper<MessageType, Descriptor> {

   @Override
   public Descriptor map(MessageType type) {
      List<MessageType> nestedMessageTypes = filter(type.getNestedTypes(), MessageType.class);
      List<EnumType> enumTypes = filter(type.getNestedTypes(), EnumType.class);
      List<MessageType.Field> fields = type.getFields();
      List<Option> options = type.getOptions();
      return new Descriptor.Builder()
              .withFullName(type.getFullyQualifiedName())
              .withName(type.getName())
              .withFields(FIELD_LIST_MAPPER.map(fields))
              .withEnumTypes(ENUM_LIST_MAPPER.map(enumTypes))
              .withNestedTypes(MESSAGE_LIST_MAPPER.map(nestedMessageTypes))
              .withOptions(OPTION_LIST_MAPPER.map(options))
              .build();
   }
}
