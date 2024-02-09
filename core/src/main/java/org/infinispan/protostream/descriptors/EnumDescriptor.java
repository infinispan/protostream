package org.infinispan.protostream.descriptors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.config.AnnotationConfiguration;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.impl.AnnotatedDescriptorImpl;
import org.infinispan.protostream.impl.Log;
import org.infinispan.protostream.impl.SparseBitSet;

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
   private final Map<Integer, EnumValueDescriptor> valueByNumber;
   private final Map<String, EnumValueDescriptor> valueByName;
   private FileDescriptor fileDescriptor;
   private Descriptor containingType;
   private final SparseBitSet reservedNumbers;
   private final Set<String> reservedNames;

   private EnumDescriptor(Builder builder) {
      super(builder.name, builder.fullName, builder.documentation);
      this.options = List.copyOf(builder.options);
      this.reservedNumbers = builder.reservedNumbers;
      this.reservedNames = Set.copyOf(builder.reservedNames);
      this.values = List.copyOf(builder.values);
      this.valueByNumber = new HashMap<>(values.size());
      this.valueByName = new HashMap<>(values.size());
      Option allowAlias = options.stream().filter(o -> o.getName().equals("allow_alias")).findFirst().orElse(null);
      if (allowAlias == null || !"true".equals(allowAlias.getValue())) {
         SparseBitSet numbers = new SparseBitSet();
         for (EnumValueDescriptor c : values) {
            if (numbers.get(c.getNumber())) {
               throw new IllegalStateException("Duplicate tag " + c.getNumber() + " in " + fullName);
            } else {
               numbers.set(c.getNumber());
            }
         }
      }
      for (EnumValueDescriptor value : values) {
         if (name.equals(value.getName())) {
            throw new DescriptorParserException("Enum constant '" + value.getName() + "' clashes with enum type name: " + fullName);
         }
         if (valueByName.containsKey(value.getName())) {
            throw new DescriptorParserException("Enum constant '" + value.getName() + "' is already defined in " + fullName);
         }
         value.setContainingEnum(this);
         checkReserved(value);
         valueByName.put(value.getName(), value);
         valueByNumber.putIfAbsent(value.getNumber(), value);
      }
   }

   private void checkReserved(EnumValueDescriptor value) {
      if (reservedNumbers.get(value.getNumber())) {
         throw Log.LOG.reservedNumber(value.getNumber(), value.getName(), fullName);
      }
      if (reservedNames.contains(value.getName())) {
         throw Log.LOG.reservedName(value.getName(), fullName);
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
         if (t == AnnotationElement.AnnotationTarget.ENUM) {
            return annotationConfiguration;
         }
      }
      throw new DescriptorParserException("Annotation '" + annotation + "' cannot be applied to enum types.");
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
      if (typeId != null && typeId < 0) {
         throw new DescriptorParserException("TypeId cannot be negative");
      }
   }

   @Override
   public String toString() {
      return "EnumDescriptor{fullName=" + getFullName() + '}';
   }

   public static final class Builder implements OptionContainer<Builder>, ReservedContainer<Builder> {
      private String name;
      private String fullName;
      private List<Option> options = new ArrayList<>();
      private List<EnumValueDescriptor> values = new ArrayList<>();
      private String documentation;
      private final SparseBitSet reservedNumbers = new SparseBitSet();
      private final Set<String> reservedNames = new HashSet<>();

      public Builder withName(String name) {
         this.name = name;
         return this;
      }

      String getName() {
         return name;
      }

      public Builder withFullName(String fullName) {
         this.fullName = fullName;
         return this;
      }

      public Builder withOptions(List<Option> options) {
         this.options = options;
         return this;
      }

      @Override
      public Builder addOption(Option option) {
         this.options.add(option);
         return this;
      }

      public Builder withValues(List<EnumValueDescriptor> values) {
         this.values = values;
         return this;
      }

      public Builder addValue(EnumValueDescriptor.Builder value) {
         this.values.add(value.build());
         return this;
      }

      public Builder withDocumentation(String documentation) {
         this.documentation = documentation;
         return this;
      }

      public Builder addReserved(int number) {
         reservedNumbers.set(number);
         return this;
      }

      public Builder addReserved(int from, int to) {
         reservedNumbers.set(from, to + 1);
         return this;
      }

      public Builder addReserved(String name) {
         reservedNames.add(name);
         return this;
      }

      public EnumDescriptor build() {
         return new EnumDescriptor(this);
      }
   }
}
