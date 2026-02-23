package org.infinispan.protostream.impl.json;

import java.util.List;
import java.util.Objects;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.descriptors.FieldDescriptor;

/**
 * Writer specialized for repeated fields.
 *
 * <p>
 * This writer handle repeated fields in an object and write them as an array to the JSON output. This writer will
 * push the array tokens at begin and at the end. The writing of each element is passed to the delegate. In case the
 * value is a primitive type, we invoke the {@link BaseJsonWriter} to write it.
 * </p>
 *
 * <p>
 * This writer follows the <code>ARRAY</code> state machine:
 * <pre>
 * ┌───────┐        ┌─────┐      ┌───────┐        ┌─────┐
 * │ ARRAY ├───────►│  [  ├─────►│ VALUE ├───┬───►│  ]  │
 * └───────┘        └─────┘  ▲   └───────┘   │    └─────┘
 *                           │               │
 *                           │   ┌───────┐   │
 *                           └───┤   ,   │◄──┘
 *                               └───────┘
 * </pre>
 * </p>
 *
 * <h2>ProtoStream parsing:</h2>
 * <p>
 * This writer is invoked from a reader of the ProtoStream byte stream, which means it follows how the bytes are
 * serialized. A repeated field is invoked multiple for each nested message. For a repeated field,
 * it will invoke {@link #onStartNested(int, FieldDescriptor)}, {@link #onTag(int, FieldDescriptor, Object)}, and
 * {@link #onEndNested(int, FieldDescriptor)} with the same field number and descriptor for each element in the stream.
 * </p>
 *
 * @author José Bolina
 * @see <a href="https://www.json.org/json-en.html">JSON specification.</a>
 */
final class ArrayJsonWriter extends BaseJsonWriter {
   private ObjectJsonWriter delegate = null;
   private boolean done;
   private boolean isMessageType;

   ArrayJsonWriter(ImmutableSerializationContext ctx, List<JsonTokenWriter> ast, FieldDescriptor descriptor) {
      super(ctx, ast, descriptor);
   }

   @Override
   public void onStartNested(int fieldNumber, FieldDescriptor fieldDescriptor) {
      if (fieldNumber != field())
         throw new IllegalStateException("Array handling incorrect field");

      // This happens when the end nested was invoked but there are additional elements in the stream.
      if (delegate != null && delegate.field() == fieldNumber) {
         delegate = null;
         replaceLastToken(JsonToken.RIGHT_BRACKET, JsonToken.COMMA);
         done = false;
      } else {
         // Otherwise, we are starting a fresh list.
         // We populate the property name if we're starting a list where it is still missing.
         if (JsonToken.followedByComma(lastToken())) {
            pushToken(JsonToken.COMMA);
            pushToken(JsonTokenWriter.string(fieldDescriptor.getName()));
            pushToken(JsonToken.COLON);
         }
         pushToken(JsonToken.LEFT_BRACKET);
      }

      delegate = (ObjectJsonWriter) objectWriter(fieldNumber, fieldDescriptor);
      isMessageType = fieldDescriptor.getMessageType() != null;
      if (isMessageType) {
         // For message types, always open a proper JSON object.
         delegate.onStartNested(fieldNumber, fieldDescriptor);
         // For simple messages (≤1 field), _type was not written by the delegate; write it
         // explicitly so the JSON remains round-trippable.
         if (!delegate.isComplexObject()) {
            writeType(fieldDescriptor);
         }
      }
   }

   @Override
   public void onTag(int fieldNumber, FieldDescriptor fieldDescriptor, Object tagValue) {
      Objects.requireNonNull(delegate, "Handling tag without starting nested");
      if (delegate.isComplexObject() || isMessageType) {
         delegate.onTag(fieldNumber, fieldDescriptor, tagValue);
         return;
      }

      writePrimitiveOrDispatch(fieldNumber, fieldDescriptor, tagValue, delegate);
   }

   @Override
   public void onEnd() {
      if (delegate.isComplexObject() || isMessageType)
         delegate.onEnd();

      if (!isDone())
         pushToken(JsonToken.RIGHT_BRACKET);
   }

   @Override
   public void onEndNested(int fieldNumber, FieldDescriptor fieldDescriptor) {
      if (delegate.isComplexObject() || isMessageType)
         delegate.onEndNested(fieldNumber, fieldDescriptor);

      pushToken(JsonToken.RIGHT_BRACKET);
      done = true;
   }

   @Override
   public boolean isDone() {
      return done;
   }

   @Override
   public boolean acceptField(int fieldNumber) {
      return field() == fieldNumber;
   }

   @Override
   protected boolean isRoot() {
      return false;
   }
}
