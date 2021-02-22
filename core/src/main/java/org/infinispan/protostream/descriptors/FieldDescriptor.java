package org.infinispan.protostream.descriptors;

import static java.util.Collections.unmodifiableList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.config.AnnotationConfiguration;
import org.infinispan.protostream.impl.AnnotatedDescriptorImpl;

/**
 * Represents a field in a proto file.
 *
 * @author gustavonalle
 * @author anistor@redhat.com
 * @since 2.0
 */
public final class FieldDescriptor extends AnnotatedDescriptorImpl implements AnnotatedDescriptor {

   private static final String PACKED = "packed";
   private final int number;
   private final Label label;
   private final String typeName;
   private final String defaultValue;
   private final List<Option> options;
   private final Map<String, Object> optionByName = new HashMap<>();
   private final boolean isExtension;
   private Type type;
   private FileDescriptor fileDescriptor;
   private Descriptor containingMessage;
   private Descriptor messageType;
   private EnumDescriptor enumType;

   private FieldDescriptor(Builder builder) {
      super(builder.name, null, builder.documentation);
      this.number = builder.number;
      this.label = builder.label;
      this.options = unmodifiableList(builder.options);
      for (Option opt : options) {
         optionByName.put(opt.getName(), opt.getValue());
      }
      this.typeName = builder.typeName;
      this.defaultValue = builder.defaultValue;
      this.isExtension = builder.isExtension;
   }

   public int getNumber() {
      return number;
   }

   public int getWireTag() {
      return WireType.makeTag(number, type.getWireType());
   }

   public Type getType() {
      return type;
   }

   void setType(Type type) {
      this.type = type;
   }

   public Descriptor getMessageType() {
      return messageType;
   }

   public Label getLabel() {
      return label;
   }

   public Object getOptionByName(String name) {
      return optionByName.get(name);
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

   public boolean isRequired() {
      return label == Label.REQUIRED;
   }

   public boolean isRepeated() {
      return label == Label.REPEATED;
   }

   public boolean isPacked() {
      return optionByName.containsKey(PACKED);
   }

   public Object getDefaultValue() {
      if (getJavaType() == JavaType.MESSAGE) {
         throw new UnsupportedOperationException("FieldDescriptor.getDefaultValue() called on an embedded message field (only scalars can have a default value).");
      }
      if (!hasDefaultValue()) {
         return null;
      }
      return getJavaType().fromString(defaultValue);
   }

   public boolean isExtension() {
      return isExtension;
   }

   public boolean hasDefaultValue() {
      return defaultValue != null;
   }

   public JavaType getJavaType() {
      return getType().getJavaType();
   }

   public EnumDescriptor getEnumType() {
      return enumType;
   }

   public String getTypeName() {
      return typeName;
   }

   public FileDescriptor getFileDescriptor() {
      return fileDescriptor;
   }

   public Descriptor getContainingMessage() {
      return containingMessage;
   }

   void setContainingMessage(Descriptor containingMessage) {
      this.containingMessage = containingMessage;
      this.fullName = containingMessage.getFullName() + '.' + name;
   }

   void setMessageType(Descriptor descriptor) {
      this.messageType = descriptor;
      type = Type.MESSAGE;
   }

   void setEnumType(EnumDescriptor enumDescriptor) {
      this.enumType = enumDescriptor;
      type = Type.ENUM;
   }

   void setFileDescriptor(FileDescriptor fileDescriptor) {
      this.fileDescriptor = fileDescriptor;
   }

   @Override
   protected AnnotationConfiguration getAnnotationConfig(String annotationName) {
      AnnotationConfiguration annotationConfiguration = getAnnotationsConfig().annotations().get(annotationName);
      if (annotationConfiguration == null) {
         return null;
      }
      for (AnnotationElement.AnnotationTarget t : annotationConfiguration.target()) {
         if (t == AnnotationElement.AnnotationTarget.FIELD) {
            return annotationConfiguration;
         }
      }
      throw new DescriptorParserException("Annotation '" + annotationName + "' cannot be applied to fields.");
   }

   @Override
   public String toString() {
      return "FieldDescriptor{" +
            "label=" + label +
            ", typeName='" + typeName + '\'' +
            ", name='" + name + '\'' +
            ", number='" + number + '\'' +
            ", defaultValue=" + defaultValue +
            '}';
   }

   public static final class Builder {
      private String typeName;
      private int number;
      private String name;
      private Label label;
      private List<Option> options;
      private String defaultValue;
      private boolean isExtension;
      private String documentation;

      public Builder withNumber(int number) {
         this.number = number;
         return this;
      }

      public Builder withName(String name) {
         this.name = name;
         return this;
      }

      public Builder withTypeName(String typeName) {
         this.typeName = typeName;
         return this;
      }

      public Builder withLabel(Label label) {
         this.label = label;
         return this;
      }

      public Builder withOptions(List<Option> options) {
         this.options = options;
         return this;
      }

      public Builder withDefaultValue(String defaultValue) {
         this.defaultValue = defaultValue;
         return this;
      }

      public Builder withIsExtension(boolean isExtension) {
         this.isExtension = isExtension;
         return this;
      }

      public Builder withDocumentation(String documentation) {
         this.documentation = documentation;
         return this;
      }

      public FieldDescriptor build() {
         FieldDescriptor fieldDescriptor = new FieldDescriptor(this);
         try {
            Type fieldType = Type.valueOf(typeName.toUpperCase());
            fieldDescriptor.setType(fieldType);
         } catch (IllegalArgumentException ignored) {
            // TODO [anistor] This (harmless exception) happens because typeName is not a primitive but a user defined type. A nicer validation would be better.
         }
         return fieldDescriptor;
      }
   }
}
