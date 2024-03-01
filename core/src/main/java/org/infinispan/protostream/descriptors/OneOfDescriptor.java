package org.infinispan.protostream.descriptors;

import java.util.ArrayList;
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
      this.fields = List.copyOf(fields);
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

   @Override
   public String toString() {
      return "OneOfDescriptor{name='" + name + '}';
   }

   public static final class Builder implements FieldContainer<Builder> {

      private String name;
      private String documentation;
      private List<FieldDescriptor> fields = new ArrayList<>();

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

      @Override
      public Builder addField(FieldDescriptor.Builder field) {
         this.fields.add(field.build());
         return this;
      }

      public OneOfDescriptor build() {
         return new OneOfDescriptor(name, documentation, fields);
      }
   }
}
