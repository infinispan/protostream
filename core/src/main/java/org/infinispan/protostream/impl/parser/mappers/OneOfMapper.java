package org.infinispan.protostream.impl.parser.mappers;

import org.infinispan.protostream.descriptors.OneOfDescriptor;

import com.squareup.protoparser.OneOfElement;

/**
 * @author anistor@redhat.com
 * @since 3.1
 */
final class OneOfMapper implements Mapper<OneOfElement, OneOfDescriptor> {

   @Override
   public OneOfDescriptor map(OneOfElement oneof) {
      return new OneOfDescriptor.Builder()
            .withName(oneof.name())
            .withDocumentation(oneof.documentation())
            .withFields(Mappers.FIELD_LIST_MAPPER.map(oneof.fields()))
            .build();
   }
}
