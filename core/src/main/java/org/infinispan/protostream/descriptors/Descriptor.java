package org.infinispan.protostream.descriptors;

import static java.util.Collections.unmodifiableList;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.config.AnnotationConfiguration;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.impl.AnnotatedDescriptorImpl;

/**
 * Represents a message declaration in a proto file.
 *
 * @author gustavonalle
 * @author anistor@redhat.com
 * @since 2.0
 */
public final class Descriptor extends AnnotatedDescriptorImpl implements GenericDescriptor {

   private Integer typeId;
   private final List<Option> options;
   private final List<FieldDescriptor> fields;
   private final List<OneOfDescriptor> oneofs;
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
      addFields(builder.fields);
      this.oneofs = unmodifiableList(builder.oneofs);
      for (OneOfDescriptor oneof : oneofs) {
         addFields(oneof.getFields());
         oneof.setContainingMessage(this);
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

   private void addFields(List<FieldDescriptor> fields) {
      for (FieldDescriptor fieldDescriptor : fields) {
         FieldDescriptor existing = fieldsByNumber.put(fieldDescriptor.getNumber(), fieldDescriptor);
         if (existing != null) {
            throw new IllegalStateException("Field number " + fieldDescriptor.getNumber()
                  + " has already been used in \"" + fullName + "\" by field \"" + existing.getName() + "\".");
         }
         existing = fieldsByName.put(fieldDescriptor.getName(), fieldDescriptor);
         if (existing != null) {
            throw new IllegalStateException("Field \"" + fieldDescriptor.getName()
                  + "\" is already defined in \"" + fullName + "\" with numbers "
                  + existing.getNumber() + " and " + fieldDescriptor.getNumber() + ".");
         }
         fieldDescriptor.setContainingMessage(this);
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

   public List<OneOfDescriptor> getOneOfs() {
      return oneofs;
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
      typeId = getProcessedAnnotation(Configuration.TYPE_ID_ANNOTATION);
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
   public Integer getTypeId() {
      return typeId;
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
   protected AnnotationConfiguration getAnnotationConfig(String annotationName) {
      AnnotationConfiguration annotationConfiguration = fileDescriptor.configuration.annotationsConfig().annotations().get(annotationName);
      if (annotationConfiguration == null) {
         return null;
      }
      for (AnnotationElement.AnnotationTarget t : annotationConfiguration.target()) {
         if (t == AnnotationElement.AnnotationTarget.MESSAGE) {
            return annotationConfiguration;
         }
      }
      throw new DescriptorParserException("Annotation '" + annotationName + "' cannot be applied to message types.");
   }

   public static final class Builder {
      private String name, fullName;
      private List<Option> options;
      private List<FieldDescriptor> fields;
      private List<OneOfDescriptor> oneofs;
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

      public Builder withOneOfs(List<OneOfDescriptor> oneofs) {
         this.oneofs = oneofs;
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
