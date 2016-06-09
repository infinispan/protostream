package org.infinispan.protostream.descriptors;

import java.util.List;

/**
 * @author anistor@redhat.com
 * @since 3.1
 */
public final class OneOfDescriptor {

   private final String name;
   private final String documentation;
   private final List<FieldDescriptor> fields;
   private Descriptor containingMessage;

   private OneOfDescriptor(String name, String documentation, List<FieldDescriptor> fields) {
      this.name = name;
      this.documentation = documentation;
      this.fields = fields;
   }

   public String getName() {
      return name;
   }

   public String getDocumentation() {
      return documentation;
   }

   public List<FieldDescriptor> getFields() {
      return fields;
   }

   public Descriptor getContainingMessage() {
      return containingMessage;
   }

   void setContainingMessage(Descriptor containingMessage) {
      this.containingMessage = containingMessage;
   }

   public static final class Builder {

      private String name;
      private String documentation;
      private List<FieldDescriptor> fields;

      public Builder withName(String name) {
         this.name = name;
         return this;
      }

      public Builder withDocumentation(String documentation) {
         this.documentation = documentation;
         return this;
      }

      public Builder withFields(List<FieldDescriptor> fields) {
         this.fields = fields;
         return this;
      }

      public OneOfDescriptor build() {
         return new OneOfDescriptor(name, documentation, fields);
      }
   }
}
