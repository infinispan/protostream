package org.infinispan.protostream.descriptors;

import org.infinispan.protostream.config.AnnotationConfig;
import org.infinispan.protostream.impl.AnnotatedDescriptorImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;

/**
 * Represents an enum in a proto file.
 *
 * @author gustavonalle
 * @author anistor@redhat.com
 * @since 2.0
 */
public final class EnumDescriptor extends AnnotatedDescriptorImpl implements GenericDescriptor {

   private final List<Option> options;
   private final List<EnumValueDescriptor> values;
   private final Map<Integer, EnumValueDescriptor> valueByNumber = new HashMap<>();
   private final Map<String, EnumValueDescriptor> valueByName = new HashMap<>();
   private FileDescriptor fileDescriptor;
   private Descriptor containingType;

   private EnumDescriptor(Builder builder) {
      super(builder.name, builder.fullName, builder.documentation);
      this.options = unmodifiableList(builder.options);
      this.values = unmodifiableList(builder.values);
      for (EnumValueDescriptor value : values) {
         value.setContainingEnum(this);
         valueByNumber.put(value.getNumber(), value);
         valueByName.put(value.getName(), value);
      }
   }

   @Override
   protected AnnotationConfig<EnumDescriptor> getAnnotationConfig(String annotationName) {
      return fileDescriptor.configuration.enumAnnotations().get(annotationName);
   }

   @Override
   public FileDescriptor getFileDescriptor() {
      return fileDescriptor;
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
   }

   public static class Builder {
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
