package org.infinispan.protostream;


import org.infinispan.protostream.descriptors.FieldDescriptor;

/**
 * An event based interface for consuming a (read only) protobuf stream containing exactly one top level message.
 *
 * @author anistor@redhat.com
 * @since 1.0
 */
public interface TagHandler {

   /**
    * Start of top level message. Do whatever required init here.
    */
   default void onStart() {
   }

   /**
    * A field which is a primitive (non-nested) value.
    *
    * @param fieldNumber     the field number
    * @param fieldDescriptor the field descriptor, or null if this is an unknown field.
    */
   default void onTag(int fieldNumber, FieldDescriptor fieldDescriptor, Object tagValue) {
   }

   /**
    * Start of a nested message.
    *
    * @param fieldNumber     the field number
    * @param fieldDescriptor a field which is guaranteed to be of type Descriptor, or null if this is an unknown field.
    */
   default void onStartNested(int fieldNumber, FieldDescriptor fieldDescriptor) {
   }

   /**
    * End of a nested message.
    *
    * @param fieldNumber     the field number
    * @param fieldDescriptor a field which is guaranteed to be of type Descriptor, or null if this is an unknown field.
    */
   default void onEndNested(int fieldNumber, FieldDescriptor fieldDescriptor) {
   }

   /**
    * End of top level message. Cleanup your mess!
    */
   default void onEnd() {
   }
}
