package org.infinispan.protostream.descriptors;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;

/**
 * Represents a message declaration in a proto file
 *
 * @author gustavonalle
 * @since 2.0
 */
public final class Descriptor {

   private final String name;
   private final String fullName;
   private final List<Option> options;
   private final List<FieldDescriptor> fields;
   private final List<Descriptor> nestedTypes;
   private final List<EnumDescriptor> enumTypes;
   private final Map<Integer, FieldDescriptor> fieldsByNumber = new HashMap<>();
   private final Map<String, FieldDescriptor> fieldsByName = new HashMap<>();
   private FileDescriptor fileDescriptor;
   private Descriptor containingType;

   private Descriptor(Builder builder) {
      this.name = builder.name;
      this.fullName = builder.fullName;
      this.options = unmodifiableList(builder.options);
      this.fields = unmodifiableList(builder.fields);
      for (FieldDescriptor fieldDescriptor : fields) {
         fieldsByName.put(fieldDescriptor.getName(), fieldDescriptor);
         fieldsByNumber.put(fieldDescriptor.getNumber(), fieldDescriptor);
         fieldDescriptor.setContainingMessage(this);
      }
      this.nestedTypes = unmodifiableList(builder.nestedTypes);
      this.enumTypes = unmodifiableList(builder.enumTypes);
   }

   public String getName() {
      return name;
   }

   public String getFullName() {
      return fullName;
   }

   public FileDescriptor getFile() {
      return fileDescriptor;
   }

   public List<Option> getOptions() {
      return options;
   }

   public List<FieldDescriptor> getFields() {
      return fields;
   }

   public List<Descriptor> getNestedTypes() {
      return nestedTypes;
   }

   public List<EnumDescriptor> getEnumTypes() {
      return enumTypes;
   }

   public FieldDescriptor findFieldByNumber(int number) {
      return fieldsByNumber.get(number);
   }

   public FieldDescriptor findFieldByName(String name) {
      return fieldsByName.get(name);
   }

   void setFileDescriptor(FileDescriptor fileDescriptor) {
      this.fileDescriptor = fileDescriptor;
      for (Descriptor nested : nestedTypes) {
         nested.setFileDescriptor(fileDescriptor);
         nested.setContainingType(this);
      }
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Descriptor that = (Descriptor) o;

      return fullName.equals(that.fullName);

   }

   @Override
   public int hashCode() {
      return fullName.hashCode();
   }

   public Descriptor getContainingType() {
      return containingType;
   }

   void setContainingType(Descriptor containingType) {
      this.containingType = containingType;
   }

   public static class Builder {
      private String name, fullName;
      private List<Option> options;
      private List<FieldDescriptor> fields;
      private List<Descriptor> nestedTypes = new LinkedList<>();
      private List<EnumDescriptor> enumTypes;

      public Builder withName(String name) {
         this.name = name;
         return this;
      }

      public Builder withFullName(String fullName) {
         this.fullName = fullName;
         return this;
      }

      public Builder withOptions(List<Option> options) {
         this.options = options;
         return this;
      }

      public Builder withFields(List<FieldDescriptor> fields) {
         this.fields = fields;
         return this;
      }

      public Builder withNestedTypes(List<Descriptor> nestedTypes) {
         this.nestedTypes = nestedTypes;
         return this;
      }

      public Builder withEnumTypes(List<EnumDescriptor> enumTypes) {
         this.enumTypes = enumTypes;
         return this;
      }

      public Descriptor build() {
         return new Descriptor(this);
      }
   }
}
