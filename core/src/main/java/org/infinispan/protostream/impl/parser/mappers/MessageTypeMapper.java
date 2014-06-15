package org.infinispan.protostream.impl.parser.mappers;

import com.squareup.protoparser.*;
import org.infinispan.protostream.descriptors.Descriptor;

import java.util.List;

import static org.infinispan.protostream.impl.parser.mappers.Mappers.*;

/**
 * Mapper for Type
 *
 * @author gustavonalle
 * @since 2.0
 */
class MessageTypeMapper implements Mapper<MessageType, Descriptor> {

   @Override
   public Descriptor map(MessageType type) {
      String fullyQualifiedName = type.getFullyQualifiedName();

      List<Type> nestedTypes = type.getNestedTypes();
      List<MessageType> nestedMessageTypes = filter(nestedTypes, MessageType.class);
      List<EnumType> enumTypes = filter(nestedTypes, EnumType.class);
      List<MessageType.Field> fields = type.getFields();
      List<Option> options = type.getOptions();
      String name = type.getName();
      return new Descriptor.Builder()
              .withFullName(fullyQualifiedName)
              .withName(name)
              .withFields(Mappers.FIELD_LIST_MAPPER.map(fields))
              .withEnumTypes(ENUM_LIST_MAPPER.map(enumTypes))
              .withNestedTypes(MESSAGE_LIST_MAPPER.map(nestedMessageTypes))
              .withOptions(OPTION_LIST_MAPPER.map(options))
              .build();
   }


}
