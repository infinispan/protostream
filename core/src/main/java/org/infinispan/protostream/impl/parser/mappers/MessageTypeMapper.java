package org.infinispan.protostream.impl.parser.mappers;

import com.squareup.protoparser.EnumElement;
import com.squareup.protoparser.MessageElement;
import org.infinispan.protostream.descriptors.Descriptor;

import java.util.List;

import static org.infinispan.protostream.impl.parser.mappers.Mappers.*;

/**
 * Mapper for Type.
 *
 * @author gustavonalle
 * @since 2.0
 */
final class MessageTypeMapper implements Mapper<MessageElement, Descriptor> {

   @Override
   public Descriptor map(MessageElement type) {
      List<MessageElement> nestedMessageTypes = filter(type.nestedElements(), MessageElement.class);
      List<EnumElement> enumTypes = filter(type.nestedElements(), EnumElement.class);
      return new Descriptor.Builder()
            .withFullName(type.qualifiedName())
            .withName(type.name())
            .withFields(FIELD_LIST_MAPPER.map(type.fields()))
            .withEnumTypes(ENUM_LIST_MAPPER.map(enumTypes))
            .withNestedTypes(MESSAGE_LIST_MAPPER.map(nestedMessageTypes))
            .withOptions(OPTION_LIST_MAPPER.map(type.options()))
            .withDocumentation(type.documentation())
            .build();
   }
}
