package org.infinispan.protostream.descriptors;

import org.infinispan.protostream.config.AnnotationConfig;
import org.infinispan.protostream.impl.AnnotatedDescriptorImpl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;

/**
 * Represents a message declaration in a proto file.
 *
 * @author gustavonalle
 * @author anistor@redhat.com
 * @since 2.0
 */
public final class Descriptor extends AnnotatedDescriptorImpl implements GenericDescriptor {

   private final List<Option> options;
   private final List<FieldDescriptor> fields;
   private final List<Descriptor> nestedTypes;
   private final List<EnumDescriptor> enumTypes;
   private final Map<Integer, FieldDescriptor> fieldsByNumber = new HashMap<>();
   private final Map<String, FieldDescriptor> fieldsByName = new HashMap<>();
   private FileDescriptor fileDescriptor;
   private Descriptor containingType;

   private Descriptor(Builder builder) {
      super(builder.name, builder.fullName, builder.documentation);
      this.options = unmodifiableList(builder.options);
      this.fields = unmodifiableList(builder.fields);
      for (FieldDescriptor fieldDescriptor : fields) {
         fieldsByName.put(fieldDescriptor.getName(), fieldDescriptor);
         fieldsByNumber.put(fieldDescriptor.getNumber(), fieldDescriptor);
         fieldDescriptor.setContainingMessage(this);
      }
      this.nestedTypes = unmodifiableList(builder.nestedTypes);
      this.enumTypes = unmodifiableList(builder.enumTypes);
      for (Descriptor nested : nestedTypes) {
         nested.setContainingType(this);
      }
      for (EnumDescriptor nested : enumTypes) {
         nested.setContainingType(this);
      }
   }

   @Override
   public FileDescriptor getFileDescriptor() {
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
      for (FieldDescriptor fieldDescriptor : fields) {
         fieldDescriptor.setFileDescriptor(fileDescriptor);
      }
      for (Descriptor nested : nestedTypes) {
         nested.setFileDescriptor(fileDescriptor);
      }
      for (EnumDescriptor nested : enumTypes) {
         nested.setFileDescriptor(fileDescriptor);
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

   @Override
   public Descriptor getContainingType() {
      return containingType;
   }

   private void setContainingType(Descriptor containingType) {
      this.containingType = containingType;
      for (Descriptor nested : nestedTypes) {
         nested.setContainingType(this);
      }
      for (EnumDescriptor nested : enumTypes) {
         nested.setContainingType(this);
      }
   }

   @Override
   protected AnnotationConfig<Descriptor> getAnnotationConfig(String annotationName) {
      return fileDescriptor.configuration.messageAnnotations().get(annotationName);
   }

   public static class Builder {
      private String name, fullName;
      private List<Option> options;
      private List<FieldDescriptor> fields;
      private List<Descriptor> nestedTypes = new LinkedList<>();
      private List<EnumDescriptor> enumTypes;
      private String documentation;

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

      public Builder withDocumentation(String documentation) {
         this.documentation = documentation;
         return this;
      }

      public Descriptor build() {
         return new Descriptor(this);
      }
   }
}
