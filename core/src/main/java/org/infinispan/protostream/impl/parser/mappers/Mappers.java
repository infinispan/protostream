package org.infinispan.protostream.impl.parser.mappers;

import com.squareup.protoparser.EnumType;
import com.squareup.protoparser.ExtendDeclaration;
import com.squareup.protoparser.MessageType;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.EnumValueDescriptor;
import org.infinispan.protostream.descriptors.ExtendDescriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.Option;

import java.util.LinkedList;
import java.util.List;

/**
 * Mappers and utilities used by the conversion of the protoparser model to protostream descriptors.
 *
 * @author gustavonalle
 * @since 2.0
 */
final class Mappers {

   // Single field mappers
   public static final FieldMapper FIELD_MAPPER = new FieldMapper();
   public static final EnumTypeMapper ENUM_MAPPER = new EnumTypeMapper();
   public static final EnumValueTypeMapper ENUM_VALUE_MAPPER = new EnumValueTypeMapper();
   public static final MessageTypeMapper MESSAGE_TYPE_MAPPER = new MessageTypeMapper();
   public static final ExtendMapper EXTEND_MAPPER = new ExtendMapper();
   public static final OptionMapper OPTION_MAPPER = new OptionMapper();

   // Collections mappers
   public static final ListMapper<MessageType.Field, FieldDescriptor> FIELD_LIST_MAPPER = ListMapper.forMapper(FIELD_MAPPER);
   public static final ListMapper<EnumType, EnumDescriptor> ENUM_LIST_MAPPER = ListMapper.forMapper(ENUM_MAPPER);
   public static final ListMapper<EnumType.Value, EnumValueDescriptor> ENUM_VALUE_LIST_MAPPER = ListMapper.forMapper(ENUM_VALUE_MAPPER);
   public static final ListMapper<ExtendDeclaration, ExtendDescriptor> EXTEND_LIST_MAPPER = ListMapper.forMapper(EXTEND_MAPPER);
   public static final ListMapper<MessageType, Descriptor> MESSAGE_LIST_MAPPER = ListMapper.forMapper(MESSAGE_TYPE_MAPPER);
   public static final ListMapper<com.squareup.protoparser.Option, Option> OPTION_LIST_MAPPER = ListMapper.forMapper(OPTION_MAPPER);

   @SuppressWarnings(value = "unchecked")
   static <T> List<T> filter(List<? super T> input, Class<T> ofType) {
      List<T> ts = new LinkedList<>();
      for (Object elem : input) {
         if (ofType.isAssignableFrom(elem.getClass())) {
            ts.add((T) elem);
         }
      }
      return ts;
   }
}
