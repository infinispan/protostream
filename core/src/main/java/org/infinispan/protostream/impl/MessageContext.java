package org.infinispan.protostream.impl;

import com.google.protobuf.Descriptors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author anistor@redhat.com
 */
abstract class MessageContext<E extends MessageContext> {

   /**
    * The descriptor of the current message.
    */
   final Descriptors.Descriptor messageDescriptor;

   /**
    * The map of field descriptors by name, for easier lookup (more efficient than Descriptors.Descriptor.findFieldByName()).
    */
   final Map<String, Descriptors.FieldDescriptor> fieldDescriptors;

   /**
    * The context of the outer message or null if this is a top level message.
    */
   final E parentContext;

   MessageContext(E parentContext, Descriptors.Descriptor messageDescriptor) {
      this.parentContext = parentContext;
      this.messageDescriptor = messageDescriptor;
      List<Descriptors.FieldDescriptor> fields = messageDescriptor.getFields();
      fieldDescriptors = new HashMap<String, Descriptors.FieldDescriptor>(fields.size());
      for (Descriptors.FieldDescriptor fd : fields) {
         fieldDescriptors.put(fd.getName(), fd);
      }
   }

   Descriptors.FieldDescriptor getFieldId(String fieldName) {
      Descriptors.FieldDescriptor fd = fieldDescriptors.get(fieldName);
      if (fd == null) {
         throw new IllegalArgumentException("Unknown field : " + fieldName);   //todo [anistor] throw a better exception
      }
      return fd;
   }
}
