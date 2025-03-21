package org.infinispan.protostream.impl;

import org.infinispan.protostream.descriptors.FieldDescriptor;

final class JsonNestingLevel {

   private final JsonNestingLevel previous;
   private boolean isFirstField = true;
   private FieldDescriptor repeatedFieldDescriptor;
   private int indent;

   public JsonNestingLevel(JsonNestingLevel previous) {
      this.previous = previous;
      this.indent = previous != null ? previous.indent + 1 : 0;
   }

   public JsonNestingLevel previous() {
      return previous;
   }

   public boolean isFirstField() {
      return isFirstField;
   }

   public void setFirstField(boolean firstField) {
      isFirstField = firstField;
   }

   public int indent() {
      return indent;
   }

   public void incrIndent() {
      indent++;
   }

   public void decrIndent() {
      indent--;
   }

   public FieldDescriptor repeatedFieldDescriptor() {
      return repeatedFieldDescriptor;
   }

   public void setRepeatedFieldDescriptor(FieldDescriptor repeatedFieldDescriptor) {
      this.repeatedFieldDescriptor = repeatedFieldDescriptor;
   }
}
