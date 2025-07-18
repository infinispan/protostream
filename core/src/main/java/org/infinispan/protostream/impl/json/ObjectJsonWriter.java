package org.infinispan.protostream.impl.json;

import java.util.List;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.descriptors.FieldDescriptor;

/**
 * Writer specialized for JSON objects.
 *
 * <p>
 * This writer is the most general form. It can handle new fields added, nested messages starting and ending, primitive
 * fields, etc. The idea with this writer is to create the appropriate delegate when necessary, delegate primitives to
 * the {@link BaseJsonWriter}, but include the tokens for a JSON object. The writer follows the most general state machine:
 *
 * <pre>
 *                        ┌───────────────────┐
 *                        │                   ▼
 * ┌────────┐     ┌───┐   │     ┌────────┐  ┌───┐
 * │ OBJECT ├────►│ { ├───┴────►│ STRING │  │ } │
 * └────────┘     └───┘   ▲     └───┬────┘  └───┘
 *                        │         │         ▲
 *                        │         ▼         │
 *                        │       ┌───┐       │
 *                        │       │ : │       │
 *                        │       └─┬─┘       │
 *                        │         │         │
 *                        │         ▼         │
 *                        │      ┌───────┐    │
 *                        └──────┤ VALUE ├────┘
 *                               └───────┘
 * </pre>
 *
 * This writer must ensure that repeated fields utilize the correct delegate when dealing with multiple entries. Delegating
 * to another object usually involves a one shot delegate, that writes the nested message and finishes when the end
 * methods are invoked.
 * </p>
 *
 * @author José Bolina
 * @see <a href="https://www.json.org/json-en.html">JSON specification.</a>
 */
class ObjectJsonWriter extends BaseJsonWriter {
   private FieldAwareTagHandler delegate = null;
   private final boolean complexObject;

   ObjectJsonWriter(ImmutableSerializationContext ctx, List<JsonTokenWriter> ast, FieldDescriptor descriptor) {
      super(ctx, ast, descriptor);
      this.complexObject = BaseJsonWriter.isComplexType(descriptor);
   }

   @Override
   public void onStartNested(int fieldNumber, FieldDescriptor fieldDescriptor) {
      // Ignore unknown fields.
      if (fieldDescriptor == null) {
         return;
      }

      if (fieldDescriptor != descriptor) {
         delegate = createDelegate(fieldNumber, fieldDescriptor);
         delegate.onStartNested(fieldNumber, fieldDescriptor);
         return;
      }

      // This means it is missing the comma, the field name, and then it can start the nested object.
      boolean written = false;
      if (JsonToken.followedByComma(lastToken())) {
         pushToken(JsonToken.COMMA);
         pushToken(JsonTokenWriter.string(fieldDescriptor.getName()));
         pushToken(JsonToken.COLON);
         written = true;
      }

      pushToken(JsonToken.LEFT_BRACE);
      if (complexObject && !written) {
         writeType(fieldDescriptor);
      }
   }

   @Override
   public void onTag(int fieldNumber, FieldDescriptor fieldDescriptor, Object tagValue) {
      if (delegate != null && !delegate.isDone()) {
         delegate.onTag(fieldNumber, fieldDescriptor, tagValue);
         return;
      }

      super.onTag(fieldNumber, fieldDescriptor, tagValue);
   }

   @Override
   public void onEnd() {
      if (delegate != null && !delegate.isDone()) {
         delegate.onEnd();
         return;
      }

      super.onEnd();
   }

   @Override
   public void onEndNested(int fieldNumber, FieldDescriptor fieldDescriptor) {
      if (delegate != null && !delegate.isDone()) {
         delegate.onEndNested(fieldNumber, fieldDescriptor);
         return;
      }

      onEnd();
   }

   @Override
   protected boolean isRoot() {
      return false;
   }

   final boolean isComplexObject() {
      return complexObject;
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
