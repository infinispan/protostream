package org.infinispan.protostream;

import com.google.protobuf.Descriptors;
import org.infinispan.protostream.impl.FastIntegerSet;

import java.util.BitSet;
import java.util.Set;

/**
 * @author anistor@redhat.com
 * @since 1.0
 */
public abstract class MessageContext<E extends MessageContext> {

   /**
    * The context of the outer message or null if this is a top level message.
    */
   private final E parentContext;

   /**
    * If this is a nested context this is the name of the outer field being processed. This is null for root context.
    */
   private final String fieldName;

   private String fullFieldName;

   /**
    * The descriptor of the current message.
    */
   private final Descriptors.Descriptor messageDescriptor;

   private final BitSet seenFields;
   private final FastIntegerSet seenFieldsSet;
   private int maxSeenFieldNumber = 0;

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

      seenFields = new BitSet(messageDescriptor.getFields().size());
      seenFieldsSet = new FastIntegerSet(seenFields);
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

   public Descriptors.FieldDescriptor getFieldByName(String fieldName) {
      Descriptors.FieldDescriptor fd = messageDescriptor.findFieldByName(fieldName);
      if (fd == null) {
         throw new IllegalArgumentException("Unknown field : " + fieldName);   //todo [anistor] throw a better exception
      }
      return fd;
   }

   public Descriptors.FieldDescriptor getFieldByNumber(int fieldNumber) {
      Descriptors.FieldDescriptor fd = messageDescriptor.findFieldByNumber(fieldNumber);
      if (fd == null) {
         throw new IllegalArgumentException("Unknown field : " + fieldNumber);   //todo [anistor] throw a better exception
      }
      return fd;
   }

   public boolean isFieldMarked(int fieldNumber) {
      return seenFields.get(fieldNumber);
   }

   /**
    * Mark a field as seen.
    *
    * @param fieldNumber the field number
    * @return true if it was added, false if it was already there
    */
   public boolean markField(int fieldNumber) {
      if (seenFields.get(fieldNumber)) {
         return false;
      }
      seenFields.set(fieldNumber);
      if (maxSeenFieldNumber < fieldNumber) {
         maxSeenFieldNumber = fieldNumber;
      }
      return true;
   }

   public int getMaxSeenFieldNumber() {
      return maxSeenFieldNumber;
   }

   @Deprecated
   public Set<Integer> getSeenFields() {
      return seenFieldsSet;
   }
}
