package org.infinispan.protostream.descriptors;

import static java.util.Collections.unmodifiableList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.config.AnnotationConfiguration;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.impl.AnnotatedDescriptorImpl;

/**
 * Represents a message type declaration in a proto file.
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
   private final List<Descriptor> nestedMessageTypes;
   private final List<EnumDescriptor> nestedEnumTypes;
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
      for (OneOfDescriptor oneOf : oneofs) {
         addFields(oneOf.getFields());
         oneOf.setContainingMessage(this);
      }
      this.nestedMessageTypes = unmodifiableList(builder.nestedMessageTypes);
      this.nestedEnumTypes = unmodifiableList(builder.nestedEnumTypes);
      for (Descriptor nested : nestedMessageTypes) {
         nested.setContainingType(this);
      }
      for (EnumDescriptor nested : nestedEnumTypes) {
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

   public Option getOption(String name) {
      for (Option o : options) {
         if (o.getName().equals(name)) {
            return o;
         }
      }
      return null;
   }

   public List<FieldDescriptor> getFields() {
      return fields;
   }

   public List<OneOfDescriptor> getOneOfs() {
      return oneofs;
   }

   public List<Descriptor> getNestedTypes() {
      return nestedMessageTypes;
   }

   public List<EnumDescriptor> getEnumTypes() {
      return nestedEnumTypes;
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
      for (Descriptor nested : nestedMessageTypes) {
         nested.setFileDescriptor(fileDescriptor);
      }
      for (EnumDescriptor nested : nestedEnumTypes) {
         nested.setFileDescriptor(fileDescriptor);
      }
      typeId = getProcessedAnnotation(Configuration.TYPE_ID_ANNOTATION);
      if (typeId != null && typeId < 0) {
         throw new DescriptorParserException("TypeId cannot be negative");
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
   public Integer getTypeId() {
      return typeId;
   }

   @Override
   public Descriptor getContainingType() {
      return containingType;
   }

   private void setContainingType(Descriptor containingType) {
      this.containingType = containingType;
      for (Descriptor nested : nestedMessageTypes) {
         nested.setContainingType(this);
      }
      for (EnumDescriptor nested : nestedEnumTypes) {
         nested.setContainingType(this);
      }
   }

   @Override
   protected AnnotationConfiguration getAnnotationConfig(AnnotationElement.Annotation annotation) {
      AnnotationConfiguration annotationConfiguration = getAnnotationsConfig().annotations().get(annotation.getName());
      if (annotationConfiguration == null) {
         return null;
      }
      if (annotation.getPackageName() != null && !annotation.getPackageName().equals(annotationConfiguration.packageName())) {
         return null;
      }
      for (AnnotationElement.AnnotationTarget t : annotationConfiguration.target()) {
         if (t == AnnotationElement.AnnotationTarget.MESSAGE) {
            return annotationConfiguration;
         }
      }
      throw new DescriptorParserException("Annotation '" + annotation + "' cannot be applied to message types.");
   }

   @Override
   public String toString() {
      return "Descriptor{fullName=" + getFullName() + '}';
   }

   public static final class Builder {
      private String name, fullName;
      private List<Option> options;
      private List<FieldDescriptor> fields;
      private List<OneOfDescriptor> oneofs;
      private List<Descriptor> nestedMessageTypes;
      private List<EnumDescriptor> nestedEnumTypes;
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

      public Builder withNestedTypes(List<Descriptor> nestedMessageTypes) {
         this.nestedMessageTypes = nestedMessageTypes;
         return this;
      }

      public Builder withEnumTypes(List<EnumDescriptor> nestedEnumTypes) {
         this.nestedEnumTypes = nestedEnumTypes;
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
