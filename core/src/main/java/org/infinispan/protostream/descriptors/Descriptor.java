package org.infinispan.protostream.descriptors;

import static org.infinispan.protostream.descriptors.FileDescriptor.fullName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.config.AnnotationConfiguration;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.impl.Log;
import org.infinispan.protostream.impl.SparseBitSet;

/**
 * Represents a message type declaration in a proto file.
 *
 * @author gustavonalle
 * @author anistor@redhat.com
 * @since 2.0
 */
public final class Descriptor extends ReservableDescriptor implements GenericDescriptor {
   private Integer typeId;
   private final List<Option> options;
   private final List<FieldDescriptor> fields;
   private final List<OneOfDescriptor> oneofs;
   private final List<Descriptor> nestedMessageTypes;
   private final List<EnumDescriptor> nestedEnumTypes;
   private final Map<Integer, FieldDescriptor> fieldsByNumber;
   private final Map<String, FieldDescriptor> fieldsByName;
   private FileDescriptor fileDescriptor;
   private Descriptor containingType;

   private Descriptor(Builder builder) {
      super(builder.name, builder.fullName, builder.documentation, builder.reservedNumbers, builder.reservedNames);
      this.options = List.copyOf(builder.options);
      this.fields = List.copyOf(builder.fields);
      this.oneofs = List.copyOf(builder.oneOfs);
      int totalFields = this.fields.size() + this.oneofs.size();
      fieldsByNumber = new HashMap<>(totalFields);
      fieldsByName = new HashMap<>(totalFields);
      addFields(builder.fields);
      for (OneOfDescriptor oneOf : oneofs) {
         addFields(oneOf.getFields());
         oneOf.setContainingMessage(this);
      }
      this.nestedMessageTypes = List.copyOf(builder.nestedMessageTypes);
      this.nestedEnumTypes = List.copyOf(builder.nestedEnumTypes);
      for (Descriptor nested : nestedMessageTypes) {
         nested.setContainingType(this);
      }
      for (EnumDescriptor nested : nestedEnumTypes) {
         nested.setContainingType(this);
      }
   }

   private void addFields(List<? extends FieldDescriptor> fields) {
      for (FieldDescriptor field : fields) {
         if (reservedNames.contains(field.getName())) {
            throw Log.LOG.reservedName(field.getName(), fullName);
         }
         if (reservedNumbers.get(field.getNumber())) {
            throw Log.LOG.reservedNumber(field.getNumber(), field.getName(), fullName);
         }
         FieldDescriptor existing = fieldsByNumber.put(field.getNumber(), field);
         if (existing != null) {
            throw new IllegalStateException("Field number " + field.getNumber()
                  + " has already been used in \"" + fullName + "\" by field \"" + existing.getName() + "\".");
         }
         existing = fieldsByName.put(field.getName(), field);
         if (existing != null) {
            throw new IllegalStateException("Field \"" + field.getName()
                  + "\" is already defined in \"" + fullName + "\" with numbers "
                  + existing.getNumber() + " and " + field.getNumber() + ".");
         }
         field.setContainingMessage(this);
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

   public boolean isReserved(String name) {
      return reservedNames.contains(name);
   }

   public boolean isReserved(int number) {
      return reservedNumbers.get(number);
   }

   public void checkCompatibility(Descriptor that, boolean strict, List<String> errors) {
      if ((typeId == null && that.typeId != null) || (typeId != null && that.typeId == null) || (typeId != null && !typeId.equals(that.typeId))) {
         errors.add(Log.LOG.incompatibleTypeIds(fullName, typeId, that.typeId));
      }
      for (FieldDescriptor thatField : that.fields) {
         if (reservedNumbers.get(thatField.getNumber())) {
            errors.add(Log.LOG.reservedNumber(thatField.getNumber(), thatField.getName(), that.getFullName()).getMessage());
         }
         if (reservedNames.contains(thatField.getName())) {
            errors.add(Log.LOG.reservedName(thatField.getName(), that.getFullName()).getMessage());
         }
      }
      for (FieldDescriptor thisField : this.fields) {
         FieldDescriptor thatField = that.fieldsByName.get(thisField.getName());
         if (thatField == null) {
            // Value was removed, make sure it has been reserved
            if (!that.reservedNames.contains(thisField.getName())) {
               errors.add(Log.LOG.removedFieldNotReserved(thisField.getFullName()));
            }
            if (!that.reservedNumbers.get(thisField.getNumber())) {
               errors.add(Log.LOG.removedFieldNotReserved(thisField.getFullName(), thisField.getNumber()));
            }
         } else {
            if (thisField.getNumber() != thatField.getNumber()) {
               errors.add(Log.LOG.modifiedFieldNumber(thisField.getFullName(), thisField.getNumber(), thatField.getNumber()));
            }
            if (!thisField.getTypeName().equals(thatField.getTypeName())) {
               errors.add(Log.LOG.modifiedFieldType(thisField.getFullName(), thisField.getTypeName(), thatField.getTypeName()));
            }
         }
      }
      checkReservation(that, strict, errors);
      for(Descriptor dThat : that.getNestedTypes()) {
         nestedMessageTypes.stream().filter(d -> d.getName().equals(dThat.getName())).findFirst().ifPresent(d -> d.checkCompatibility(dThat, strict, errors));
      }
      for(EnumDescriptor eThat : that.getEnumTypes()) {
         nestedEnumTypes.stream().filter(e -> e.getName().equals(eThat.getName())).findFirst().ifPresent(e -> e.checkCompatibility(eThat, strict, errors));
      }
   }

   public static final class Builder implements OptionContainer<Builder>, FieldContainer<Builder>, EnumContainer<Builder>, MessageContainer<Builder>, ReservedContainer<Builder> {
      private String name, fullName;
      private List<Option> options = new ArrayList<>();
      private List<FieldDescriptor> fields = new ArrayList<>();
      private List<OneOfDescriptor> oneOfs = new ArrayList<>();
      private List<Descriptor> nestedMessageTypes = new ArrayList<>();
      private List<EnumDescriptor> nestedEnumTypes = new ArrayList<>();
      private String documentation;
      private final SparseBitSet reservedNumbers = new SparseBitSet();
      private final Set<String> reservedNames = new HashSet<>();

      public Builder withName(String name) {
         this.name = name;
         return this;
      }

      public String getName() {
         return name;
      }

      public Builder withFullName(String fullName) {
         this.fullName = fullName;
         return this;
      }

      @Override
      public String getFullName() {
         return fullName;
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
         this.oneOfs = oneofs;
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

      @Override
      public Builder addField(FieldDescriptor.Builder field) {
         this.fields.add(field.build());
         return this;
      }

      public Builder addMap(MapDescriptor.Builder map) {
         this.fields.add(map.build());
         return this;
      }

      public Builder addOneOf(OneOfDescriptor.Builder oneOf) {
         this.oneOfs.add(oneOf.build());
         return this;
      }

      @Override
      public Builder addOption(Option option) {
         this.options.add(option);
         return this;
      }

      @Override
      public Builder addEnum(EnumDescriptor.Builder enumDescriptor) {
         this.nestedEnumTypes.add(enumDescriptor.withFullName(fullName(fullName, enumDescriptor.getName())).build());
         return this;
      }

      @Override
      public Builder addMessage(Descriptor.Builder message) {
         this.nestedMessageTypes.add(message.withFullName(fullName(fullName, message.getName())).build());
         return this;
      }

      @Override
      public Builder addReserved(int number) {
         this.reservedNumbers.set(number);
         return this;
      }

      @Override
      public Builder addReserved(int from, int to) {
         this.reservedNumbers.set(from, to + 1);
         return this;
      }

      @Override
      public Builder addReserved(String name) {
         this.reservedNames.add(name);
         return this;
      }

      public Descriptor build() {
         return new Descriptor(this);
      }
   }
}
