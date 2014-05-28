package org.infinispan.protostream;

import com.google.protobuf.Descriptors.Descriptor;

import java.util.BitSet;

/**
 * @author anistor@redhat.com
 * @since 1.0
 */
public class MessageContext<E extends MessageContext> {

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
   private final Descriptor messageDescriptor;

   private final BitSet seenFields;
   private int maxSeenFieldNumber = 0;

   public MessageContext(E parentContext, String fieldName, Descriptor messageDescriptor) {
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

   public Descriptor getMessageDescriptor() {
      return messageDescriptor;
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
}
