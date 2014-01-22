package org.infinispan.protostream.impl;

import com.google.protobuf.Descriptors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author anistor@redhat.com
 * @since 1.0
 */
public final class MessageDescriptor {

   private final Descriptors.Descriptor messageDescriptor;

   private final Descriptors.FieldDescriptor[] fieldDescriptors;

   private final Map<String, Descriptors.FieldDescriptor> fieldsByName;

   public MessageDescriptor(Descriptors.Descriptor messageDescriptor) {
      this.messageDescriptor = messageDescriptor;
      List<Descriptors.FieldDescriptor> fields = messageDescriptor.getFields();
      fieldDescriptors = fields.toArray(new Descriptors.FieldDescriptor[fields.size()]);
      fieldsByName = new HashMap<String, Descriptors.FieldDescriptor>(fieldDescriptors.length);
      for (Descriptors.FieldDescriptor fd : fieldDescriptors) {
         fieldsByName.put(fd.getName(), fd);
      }
   }

   public Descriptors.Descriptor getMessageDescriptor() {
      return messageDescriptor;
   }

   public Descriptors.FieldDescriptor[] getFieldDescriptors() {
      return fieldDescriptors;
   }

   public Map<String, Descriptors.FieldDescriptor> getFieldsByName() {
      return fieldsByName;
   }
}
