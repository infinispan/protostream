package org.infinispan.protostream.impl.json;

import static org.infinispan.protostream.impl.json.JsonHelper.MAP_KEY_FIELD;
import static org.infinispan.protostream.impl.json.JsonHelper.MAP_VALUE_FIELD;

import java.util.List;
import java.util.Objects;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.descriptors.FieldDescriptor;

/**
 * Writer specialized for map fields.
 *
 * <p>
 * This writer is similar to the {@link ObjectJsonWriter}, but it is specialized into writing maps. ProtoStream write
 * maps as a repeated field. The elements of the map are written with field number {@link JsonHelper#MAP_KEY_FIELD}
 * to identify the key, and field number {@link JsonHelper#MAP_VALUE_FIELD} to identify the value. Since it is a
 * repeated field, it follows the approach as {@link ArrayJsonWriter}.
 * </p>
 *
 * <p>
 * Maps in proto have the restriction of only integral numbers or strings for the keys. The values can be any arbitrary
 * value. This way, we write the key in this writer and delegate when writing the value. If the object is complex, we
 * create a delegate handler, otherwise, we pass the primitive to the {@link BaseJsonWriter}.
 * </p>
 *
 * @author José Bolina
 * @see <a href="https://protobuf.dev/programming-guides/proto3/#maps">Proto3 Maps</a>
 */
final class MapJsonWriter extends BaseJsonWriter {

   private FieldAwareTagHandler delegate;

   // The descriptor of the value field of the current entry.
   // It is not-null only while the value is a nested message being written by the delegate.
   private FieldDescriptor valueField;
   private int lastField = 0;
   private boolean done;

   public MapJsonWriter(ImmutableSerializationContext ctx, List<JsonTokenWriter> ast, FieldDescriptor descriptor) {
      super(ctx, ast, descriptor);
   }

   @Override
   public void onStartNested(int fieldNumber, FieldDescriptor fieldDescriptor) {
      if (fieldNumber == field() && fieldDescriptor == descriptor) {
         // In case the end nested method was invoked previously but there were additional elements in the stream.
         // This will replace the last token with a comma and continue writing the content.
         if (done) {
            replaceLastToken(JsonToken.RIGHT_BRACE, JsonToken.COMMA);
            done = false;
            return;
         }

         // Otherwise, we are starting a fresh list.
         // We populate the property name if we're starting a list where it is still missing.
         if (JsonToken.followedByComma(lastToken())) {
            pushToken(JsonToken.COMMA);
            pushToken(JsonTokenWriter.string(fieldDescriptor.getName()));
            pushToken(JsonToken.COLON);
         } else if (JsonToken.isOpen(lastToken())) {
            pushToken(JsonTokenWriter.string(fieldDescriptor.getName()));
            pushToken(JsonToken.COLON);
         }
         return;
      }

      if (delegate != null) {
         // The value message is still open.
         // Any nested structure starting now belongs to it (or to a structure it contains), delegate it.
         delegate.onStartNested(fieldNumber, fieldDescriptor);
         return;
      }

      if (fieldNumber != MAP_VALUE_FIELD)
         throw new IllegalStateException("Maps only have nested objects for values");

      valueField = fieldDescriptor;
      delegate = createDelegate(fieldNumber, fieldDescriptor);
      pushToken(JsonToken.LEFT_BRACE);
   }

   @Override
   public void onTag(int fieldNumber, FieldDescriptor fieldDescriptor, Object tagValue) {
      if (delegate != null) {
         // The value message is still open.
         // This tag belongs to the value message (or to a structure it contains), delegate it.
         delegate.onTag(fieldNumber, fieldDescriptor, tagValue);
         lastField = delegate.field();
         return;
      }

      lastField = fieldNumber;
      if (fieldNumber == MAP_KEY_FIELD) {
         if (lastToken() == JsonToken.COLON)
            pushToken(JsonToken.LEFT_BRACE);

         pushToken(JsonTokenWriter.string(Objects.toString(tagValue)));
         pushToken(JsonToken.COLON);
         return;
      }

      // At this point, the object should be a primitive.
      // Otherwise, it should have created the delegate writer.
      writePrimitiveOrDispatch(fieldNumber, fieldDescriptor, tagValue, null);
   }

   @Override
   public void onEndNested(int fieldNumber, FieldDescriptor fieldDescriptor) {
      if (delegate != null) {
         // The value message is still open.
         // This end belongs either to a structure inside the value message, or to the value message itself.
         delegate.onEndNested(fieldNumber, fieldDescriptor);
         if (fieldDescriptor == valueField) {
            delegate = null;
            valueField = null;
            lastField = MAP_VALUE_FIELD;
         }
         return;
      }

      if (fieldDescriptor != descriptor)
         return;

      if (lastField == MAP_VALUE_FIELD)
         pushToken(JsonToken.RIGHT_BRACE);

      done = true;
   }

   @Override
   public boolean isDone() {
      return done;
   }

   @Override
   protected boolean isRoot() {
      return false;
   }

   private FieldAwareTagHandler createDelegate(int fieldNumber, FieldDescriptor fd) {
      if (delegate == null) {
         if (!fd.isRepeated()) return objectWriter(fieldNumber, fd);
         if (fd.isMap()) return mapWriter(fieldNumber, fd);
         return repeatedWriter(fieldNumber, fd);
      }

      if (delegate.field() == fieldNumber)
         return delegate;

      if (!delegate.isDone())
         return delegate;

      delegate.onEndNested(fieldNumber, fd);
      delegate = null;
      return createDelegate(fieldNumber, fd);
   }
}
