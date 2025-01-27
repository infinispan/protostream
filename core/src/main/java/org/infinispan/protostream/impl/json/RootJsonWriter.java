package org.infinispan.protostream.impl.json;

import java.util.ArrayList;
import java.util.List;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.descriptors.FieldDescriptor;

class RootJsonWriter extends BaseJsonWriter {

   private final boolean writable;

   RootJsonWriter(ImmutableSerializationContext ctx) {
      this(ctx, new ArrayList<>(8));
   }

   RootJsonWriter(ImmutableSerializationContext ctx, List<JsonTokenWriter> ast) {
      super(ctx, ast);
      this.writable = false;
   }

   RootJsonWriter(ImmutableSerializationContext ctx, List<JsonTokenWriter> ast, FieldDescriptor descriptor, boolean writable) {
      super(ctx, ast, descriptor);
      this.writable = writable;
   }

   @Override
   public void onStartNested(int fieldNumber, FieldDescriptor fieldDescriptor) {
      if (fieldNumber != field())
         throw new IllegalStateException("Invalid field number for writer");

      pushToken(JsonToken.LEFT_BRACE);
      writeType(fieldDescriptor);
   }

   @Override
   public void onEndNested(int fieldNumber, FieldDescriptor fieldDescriptor) {
      if (fieldNumber != field())
         throw new IllegalStateException("Invalid field number for writer");

      pushToken(JsonToken.RIGHT_BRACE);
   }

   @Override
   protected void pushToken(JsonTokenWriter token) {
      if (writable) super.pushToken(token);
   }

   @Override
   protected boolean isRoot() {
      return true;
   }
}
