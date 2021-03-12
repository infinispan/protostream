package org.infinispan.protostream;

import java.io.IOException;
import java.util.BitSet;

import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;

/**
 * A nested message processing context.
 *
 * @author anistor@redhat.com
 * @since 1.0
 */
public class MessageContext<E extends MessageContext<E>> {

   /**
    * The context of the outer message or null if this is a top level message.
    */
   private final E parentContext;

   /**
    * If this is a nested context this is the outer field being processed. This is null for the root context.
    */
   private final FieldDescriptor fieldDescriptor;

   /**
    * Dot separated path of the field.
    */
   private String fieldPath;

   /**
    * The descriptor of the current message.
    */
   private final Descriptor messageDescriptor;

   private final BitSet seenFields;      //todo [anistor] need a sparse bitset here to avoid memory waste

   private int maxSeenFieldNumber = 0;

   public MessageContext(E parentContext, FieldDescriptor fieldDescriptor, Descriptor messageDescriptor) {
      if (messageDescriptor == null) {
         throw new IllegalArgumentException("messageDescriptor cannot be null");
      }
      if (parentContext != null && fieldDescriptor == null) {
         throw new IllegalArgumentException("fieldDescriptor cannot be null for nested contexts");
      }
      if (parentContext == null && fieldDescriptor != null) {
         throw new IllegalArgumentException("fieldDescriptor must be null for root contexts");
      }

      this.parentContext = parentContext;
      this.fieldDescriptor = fieldDescriptor;
      this.messageDescriptor = messageDescriptor;

      seenFields = new BitSet(messageDescriptor.getFields().size() + messageDescriptor.getOneOfs().size());
   }

   public E getParentContext() {
      return parentContext;
   }

   /**
    * Gets the nested field.
    *
    * @return the descriptor of the nested field or {@code null} if this is the root context
    */
   public FieldDescriptor getField() {
      return fieldDescriptor;
   }

   /**
    * Gets the full path of the nested field.
    *
    * @return the full path of the nested field or {@code null} if this is the root context
    */
   public String getFieldPath() {
      if (fieldDescriptor == null) {
         return null;
      }
      if (fieldPath == null) {
         String pfqn = null;
         if (parentContext != null) {
            pfqn = parentContext.getFieldPath();
         }
         fieldPath = pfqn != null ? pfqn + '.' + fieldDescriptor.getName() : fieldDescriptor.getName(); // todo [anistor] use fieldDescriptor.getFullName() ?
      }
      return fieldPath;
   }

   public Descriptor getMessageDescriptor() {
      return messageDescriptor;
   }

   public FieldDescriptor getFieldByName(String fieldName) throws IOException {
      FieldDescriptor fd = messageDescriptor.findFieldByName(fieldName);
      if (fd == null) {
         throw new IOException("Unknown field name : " + fieldName);
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
      seenFields.set(fieldNumber); //todo [anistor] this can cause the bitset grow dangerously
      if (maxSeenFieldNumber < fieldNumber) {
         maxSeenFieldNumber = fieldNumber;
      }
      return true;
   }

   public int getMaxSeenFieldNumber() {
      return maxSeenFieldNumber;
   }
}
