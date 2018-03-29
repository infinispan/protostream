package org.infinispan.protostream.descriptors;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.config.AnnotationConfiguration;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.impl.AnnotatedDescriptorImpl;

/**
 * Represents an enum in a proto file.
 *
 * @author gustavonalle
 * @author anistor@redhat.com
 * @since 2.0
 */
public final class EnumDescriptor extends AnnotatedDescriptorImpl implements GenericDescriptor {

   private Integer typeId;
   private final List<Option> options;
   private final List<EnumValueDescriptor> values;
   private final Map<Integer, EnumValueDescriptor> valueByNumber = new HashMap<>();
   private final Map<String, EnumValueDescriptor> valueByName = new HashMap<>();
   private FileDescriptor fileDescriptor;
   private Descriptor containingType;

   private EnumDescriptor(Builder builder) {
      super(builder.name, builder.fullName, builder.documentation);
      this.options = Collections.unmodifiableList(builder.options);
      this.values = Collections.unmodifiableList(builder.values);
      for (EnumValueDescriptor value : values) {
         if (name.equals(value.getName())) {
            throw new DescriptorParserException("Enum constant '" + value.getName() + "' clashes with enum type name: " + fullName);
         }
         if (valueByName.containsKey(value.getName())) {
            throw new DescriptorParserException("Enum constant '" + value.getName() + "' is already defined in " + fullName);
         }
         valueByName.put(value.getName(), value);
         valueByNumber.put(value.getNumber(), value);
         value.setContainingEnum(this);
      }
   }

   @Override
   protected AnnotationConfiguration getAnnotationConfig(String annotationName) {
      AnnotationConfiguration annotationConfiguration = fileDescriptor.configuration.annotationsConfig().annotations().get(annotationName);
      if (annotationConfiguration == null) {
         return null;
      }
      for (AnnotationElement.AnnotationTarget t : annotationConfiguration.target()) {
         if (t == AnnotationElement.AnnotationTarget.ENUM) {
            return annotationConfiguration;
         }
      }
      throw new DescriptorParserException("Annotation '" + annotationName + "' cannot be applied to enum types.");
   }

   @Override
   public FileDescriptor getFileDescriptor() {
      return fileDescriptor;
   }

   @Override
   public Integer getTypeId() {
      return typeId;
   }

   @Override
   public Descriptor getContainingType() {
      return containingType;
   }

   void setContainingType(Descriptor containingType) {
      this.containingType = containingType;
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

   public List<EnumValueDescriptor> getValues() {
      return values;
   }

   public EnumValueDescriptor findValueByNumber(int number) {
      return valueByNumber.get(number);
   }

   public EnumValueDescriptor findValueByName(String name) {
      return valueByName.get(name);
   }

   void setFileDescriptor(FileDescriptor fileDescriptor) {
      this.fileDescriptor = fileDescriptor;
      for (EnumValueDescriptor valueDescriptor : values) {
         valueDescriptor.setFileDescriptor(fileDescriptor);
      }
      typeId = getProcessedAnnotation(Configuration.TYPE_ID_ANNOTATION);
   }

   @Override
   public String toString() {
      return "EnumDescriptor{fullName=" + getFullName() + '}';
   }

   public static final class Builder {
      private String name;
      private String fullName;
      private List<Option> options;
      private List<EnumValueDescriptor> values;
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

      public Builder withValues(List<EnumValueDescriptor> values) {
         this.values = values;
         return this;
      }

      public Builder withDocumentation(String documentation) {
         this.documentation = documentation;
         return this;
      }

      public EnumDescriptor build() {
         return new EnumDescriptor(this);
      }
   }
}
