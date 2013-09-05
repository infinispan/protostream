package org.infinispan.protostream;

import com.google.protobuf.Descriptors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author anistor@redhat.com
 */
public abstract class MessageContext<E extends MessageContext> {

   /**
    * The context of the outer message or null if this is a top level message.
    */
   protected final E parentContext;

   /**
    * If this is a nested context this is the name of the outer field being processed. This is null for root context.
    */
   private final String fieldName;

   private String fullFieldName;

   /**
    * The descriptor of the current message.
    */
   protected final Descriptors.Descriptor messageDescriptor;

   /**
    * The map of field descriptors by name, for easier lookup (more efficient than Descriptors.Descriptor.findFieldByName()).
    */
   protected final Map<String, Descriptors.FieldDescriptor> fieldDescriptors;

   protected final Set<Integer> seenFields;

   protected MessageContext(E parentContext, String fieldName, Descriptors.Descriptor messageDescriptor) {
      if (messageDescriptor == null) {
         throw new IllegalArgumentException("messageDescriptor cannot be null");
      }
      if (parentContext != null && fieldName == null) {
         throw new IllegalArgumentException("fieldName cannot be null for nested contexts");
      }
      if (parentContext == null && fieldName != null) {
         throw new IllegalArgumentException("fieldName must be null for root contexts");
      }

      this.parentContext = parentContext;
      this.fieldName = fieldName;
      this.messageDescriptor = messageDescriptor;
      List<Descriptors.FieldDescriptor> fields = messageDescriptor.getFields();
      seenFields = new HashSet<Integer>(fields.size());
      fieldDescriptors = new HashMap<String, Descriptors.FieldDescriptor>(fields.size());
      for (Descriptors.FieldDescriptor fd : fields) {
         fieldDescriptors.put(fd.getName(), fd);
      }
   }

   public E getParentContext() {
      return parentContext;
   }

   /**
    * Gets the name of the nested field.
    *
    * @return the name of the nested field if any or  {@code null} if this is the root context
    */
   public String getFieldName() {
      return fieldName;
   }

   public String getFullFieldName() {
      if (fieldName == null) {
         return null;
      }
      if (fullFieldName == null) {
         String pfqn = null;
         if (parentContext != null) {
            pfqn = parentContext.getFullFieldName();
         }
         fullFieldName = pfqn != null ? pfqn + "." + fieldName : fieldName;
      }
      return fullFieldName;
   }

   public Descriptors.Descriptor getMessageDescriptor() {
      return messageDescriptor;
   }

   public Map<String, Descriptors.FieldDescriptor> getFieldDescriptors() {
      return fieldDescriptors;
   }

   public Descriptors.FieldDescriptor getFieldByName(String fieldName) {
      Descriptors.FieldDescriptor fd = fieldDescriptors.get(fieldName);
      if (fd == null) {
         throw new IllegalArgumentException("Unknown field : " + fieldName);   //todo [anistor] throw a better exception
      }
      return fd;
   }

   public Descriptors.FieldDescriptor getFieldByNumber(int fieldNumber) {
      Descriptors.FieldDescriptor fd = messageDescriptor.findFieldByNumber(fieldNumber);
      if (fd == null) {
         throw new IllegalArgumentException("Unknown field : " + fieldName);   //todo [anistor] throw a better exception
      }
      return fd;
   }

   public Set<Integer> getSeenFields() {
      return seenFields;
   }
}
