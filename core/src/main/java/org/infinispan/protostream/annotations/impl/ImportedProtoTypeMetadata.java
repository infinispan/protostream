package org.infinispan.protostream.annotations.impl;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.EnumValueDescriptor;
import org.infinispan.protostream.descriptors.GenericDescriptor;

/**
 * A {@link ProtoTypeMetadata} for a message or enum type that is defined in an external protobuf schema and its
 * definition was not created based on annotations during the current execution of a {@link
 * org.infinispan.protostream.annotations.ProtoSchemaBuilder}.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
final class ImportedProtoTypeMetadata extends ProtoTypeMetadata {

   private final GenericDescriptor descriptor;

   private final BaseMarshaller<?> marshaller;

   ImportedProtoTypeMetadata(GenericDescriptor descriptor, BaseMarshaller<?> marshaller) {
      super(descriptor.getFullName(), marshaller.getJavaClass(), null);
      this.descriptor = descriptor;
      this.marshaller = marshaller;
   }

   @Override
   public boolean isEnum() {
      return descriptor instanceof EnumDescriptor;
   }

   @Override
   public ProtoEnumValueMetadata getEnumMemberByName(String name) {
      if (!isEnum()) {
         throw new IllegalStateException(getFullName() + " is not an enum");
      }
      EnumValueDescriptor evd = ((EnumDescriptor) descriptor).findValueByName(name);
      if (evd == null) {
         return null;
      }
      Enum ev = ((EnumMarshaller) marshaller).decode(evd.getNumber());
      return new ProtoEnumValueMetadata(evd.getNumber(), name, ev, null);
   }
}
