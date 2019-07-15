package org.infinispan.protostream.annotations.impl;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.annotations.impl.types.XClass;
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

   ImportedProtoTypeMetadata(GenericDescriptor descriptor, BaseMarshaller<?> marshaller, XClass javaClass) {
      super(descriptor.getFullName(), javaClass);
      this.descriptor = descriptor;
      this.marshaller = marshaller;
   }

   @Override
   public String getFullName() {
      return descriptor.getFullName();
   }

   @Override
   public boolean isImported() {
      return true;
   }

   @Override
   public String getFileName() {
      return descriptor.getFileDescriptor().getName();
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
      Enum<?> enumConstant = ((EnumMarshaller) marshaller).decode(evd.getNumber());
      return new ProtoEnumValueMetadata(evd.getNumber(), name,
            enumConstant.ordinal(), enumConstant.getDeclaringClass().getName() + '.' + enumConstant.name(), null);
   }

   @Override
   public String toString() {
      return "ImportedProtoTypeMetadata{" +
            "name='" + name + '\'' +
            ", javaClass=" + javaClass +
            ", descriptor=" + descriptor +
            ", marshaller=" + marshaller.getClass() +
            '}';
   }
}
