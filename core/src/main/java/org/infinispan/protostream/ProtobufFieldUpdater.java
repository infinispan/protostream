package org.infinispan.protostream;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.infinispan.protostream.descriptors.Descriptor;

/**
 * Partially updates protobuf-serialized message bytes at the field level, without requiring Java POJOs.
 * Supports scalar field SET (including nested dotted paths), and collection ADD/REMOVE on repeated fields.
 * Unmodified fields are passed through verbatim in the resulting bytes, and unknown fields are preserved.
 *
 * @since 6.0
 */
public final class ProtobufFieldUpdater {

   public enum OperationType {
      SET,
      ADD,
      REMOVE
   }

   public record UpdateOperation(OperationType type, String[] propertyPath, List<Object> values) {
      public UpdateOperation {
         Objects.requireNonNull(type);
         Objects.requireNonNull(propertyPath);
         if (propertyPath.length == 0) {
            throw new IllegalArgumentException("propertyPath must not be empty");
         }
         Objects.requireNonNull(values);
      }
   }

   private ProtobufFieldUpdater() {
   }

   /**
    * Apply a list of update operations to the inner message bytes (NOT wrapped in WrappedMessage).
    *
    * @param descriptor the message descriptor
    * @param messageBytes the raw protobuf bytes of the message
    * @param operations the update operations to apply
    * @return updated protobuf bytes
    */
   public static byte[] update(Descriptor descriptor, byte[] messageBytes, List<UpdateOperation> operations) throws IOException {
      return update(null, descriptor, messageBytes, operations);
   }

   /**
    * Apply a list of update operations to the inner message bytes (NOT wrapped in WrappedMessage).
    *
    * @param serCtx the serialization context, used to marshal/unmarshal message-typed values
    * @param descriptor the message descriptor
    * @param messageBytes the raw protobuf bytes of the message
    * @param operations the update operations to apply
    * @return updated protobuf bytes
    */
   public static byte[] update(ImmutableSerializationContext serCtx, Descriptor descriptor, byte[] messageBytes, List<UpdateOperation> operations) throws IOException {
      Objects.requireNonNull(descriptor, "descriptor cannot be null");
      Objects.requireNonNull(messageBytes, "messageBytes cannot be null");
      Objects.requireNonNull(operations, "operations cannot be null");
      ProtobufMessageEditor editor = ProtobufMessageEditor.of(serCtx, descriptor, messageBytes);
      for (UpdateOperation op : operations) {
         editor.apply(op);
      }
      return editor.toByteArray();
   }
}
