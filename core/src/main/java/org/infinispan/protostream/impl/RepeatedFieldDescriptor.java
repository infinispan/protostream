package org.infinispan.protostream.impl;

import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.Label;

public class RepeatedFieldDescriptor extends FieldDescriptor {

   protected RepeatedFieldDescriptor(FieldDescriptor descriptor) {
      super(toBuilder(descriptor));
   }

   private static Builder toBuilder(FieldDescriptor descriptor) {
      Builder builder = new Builder();
      builder.withName("_value");
      builder.withTypeName(descriptor.getTypeName());
      builder.withLabel(Label.REPEATED);
      return builder;
   }
}
