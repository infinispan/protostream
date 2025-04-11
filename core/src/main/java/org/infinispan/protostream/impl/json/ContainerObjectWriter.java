package org.infinispan.protostream.impl.json;

import static org.infinispan.protostream.impl.json.JsonHelper.JSON_VALUE_FIELD;

import java.util.List;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.TagHandler;
import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.GenericDescriptor;

/**
 * Specialized writer to handle container types.
 *
 * <p>
 * The container types are messages which aren't wrapped by a {@link WrappedMessage}. They are repeated elements written
 * directly to the byte stream. The elements don't have well-defined type that can be extracted from the container
 * descriptor. Therefore, writing the container's element require the types in the JSON output.
 * </p>
 *
 * <p>
 * The container serializer will follow a specific order in the byte stream. We expect this same order to happen here
 * when reading the fields. Diverging from the structure won't generate the appropriate JSON.
 * </p>
 *
 * @author José Bolina
 */
final class ContainerObjectWriter extends BaseJsonWriter {

   private int containerFields = 1;

   ContainerObjectWriter(ImmutableSerializationContext ctx, List<JsonTokenWriter> ast, FieldDescriptor descriptor) {
      super(ctx, ast, descriptor);
   }

   @Override
   public void onStartNested(int fieldNumber, FieldDescriptor fieldDescriptor) { }

   @Override
   public void onTag(int fieldNumber, FieldDescriptor fieldDescriptor, Object tagValue) {
      containerFields++;

      // A container message has 3 fields:
      // 1. The name, which as read before creating this object;
      // 2. The size, which defines the number of elements;
      // 3. A wrapped message, this message is supposedly always empty, since the generated code doesn't write anything.
      if (containerFields < 4) {
         super.onTag(fieldNumber, fieldDescriptor, tagValue);
         return;
      }

      // After reading all the fields, we create an array to populate with the actual elements coming next.
      // Each element in the container is a WrappedMessage.
      if (containerFields == 4) {
         if (JsonToken.followedByComma(lastToken())) {
            pushToken(JsonToken.COMMA);
         }
         pushToken(JsonTokenWriter.string(JSON_VALUE_FIELD));
         pushToken(JsonToken.COLON);
         pushToken(JsonToken.LEFT_BRACKET);
      }

      // In case the container has multiple elements, we separate them with a comma.
      if (JsonToken.followedByComma(lastToken()))
         pushToken(JsonToken.COMMA);

      // After reading the base fields in the container, we start to handle the actual elements.
      // The elements inside a container are all wrapped by the WrappedMessage.
      // This way they are either a wrapped primitive, or a wrapped object.
      switch (fieldNumber) {
         case WrappedMessage.WRAPPED_BYTE:
         case WrappedMessage.WRAPPED_SHORT:
         case WrappedMessage.WRAPPED_DOUBLE:
         case WrappedMessage.WRAPPED_FLOAT:
         case WrappedMessage.WRAPPED_INT64:
         case WrappedMessage.WRAPPED_UINT64:
         case WrappedMessage.WRAPPED_INT32:
         case WrappedMessage.WRAPPED_FIXED64:
         case WrappedMessage.WRAPPED_FIXED32:
         case WrappedMessage.WRAPPED_BOOL:
         case WrappedMessage.WRAPPED_STRING:
         case WrappedMessage.WRAPPED_BYTES:
         case WrappedMessage.WRAPPED_UINT32:
         case WrappedMessage.WRAPPED_SFIXED32:
         case WrappedMessage.WRAPPED_SFIXED64:
         case WrappedMessage.WRAPPED_SINT32:
         case WrappedMessage.WRAPPED_SINT64:
            // For wrapped primitives, we create a small object for each element:
            //      {"<primitive type>": <primitive value>}
            // Since the content of the container are not defined, we need to write the type of the primitive to
            // parse from the JSON later. For example, the string "Hello":
            //              {"string": "Hello"}
            writePrimitiveContainer(fieldDescriptor, tagValue);
            break;

         default: {
            // We retrieve the WrappedMessage descriptor to wrap the container element.
            GenericDescriptor descriptor = ctx.getDescriptorByTypeId(WrappedMessage.PROTOBUF_TYPE_ID);
            TagHandler delegate = new RootJsonWriter(ctx, ast);
            delegate.onStart(descriptor);
            delegate.onTag(fieldNumber, fieldDescriptor, tagValue);
            delegate.onEnd();
         }
      }
   }

   @Override
   protected boolean isRoot() {
      return false;
   }

   private void writePrimitiveContainer(FieldDescriptor fieldDescriptor, Object tagValue) {
      pushToken(JsonToken.LEFT_BRACE);
      pushToken(JsonTokenWriter.string(fieldDescriptor.getTypeName()));
      pushToken(JsonToken.COLON);
      writeTagValue(fieldDescriptor, tagValue);
      pushToken(JsonToken.RIGHT_BRACE);
   }

   @Override
   public void onEnd() {
      pushToken(JsonToken.RIGHT_BRACKET);
   }
}
