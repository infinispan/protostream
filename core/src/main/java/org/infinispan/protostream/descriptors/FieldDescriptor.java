package org.infinispan.protostream.descriptors;

import static org.infinispan.protostream.descriptors.FileDescriptor.fullName;

import java.util.ArrayList;
import java.util.List;

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
public class FieldDescriptor extends AnnotatedDescriptorImpl implements AnnotatedDescriptor {
   protected final int number;
   protected final Label label;
   protected final String typeName;
   protected final List<Option> options;
   protected Type type;
   protected FileDescriptor fileDescriptor;
   protected Descriptor containingMessage;
   protected Descriptor messageType;
   protected EnumDescriptor enumType;

   protected FieldDescriptor(Builder builder) {
      super(builder.name, null, builder.documentation);
      number = builder.number;
      label = builder.label;
      options = List.copyOf(builder.options);
      typeName = builder.typeName;
      type = Type.primitiveFromString(typeName);
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

   public Descriptor getMessageType() {
      return messageType;
   }

   public Label getLabel() {
      return label;
   }

   public Option getOptionByName(String name) {
      return options.stream().filter(o -> name.equals(o.getName())).findFirst().orElse(null);
   }

   public List<Option> getOptions() {
      return options;
   }

   public boolean isRepeated() {
      return label == Label.REPEATED;
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
      this.fullName = fullName(containingMessage.getFullName(),  name);
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
   protected AnnotationConfiguration getAnnotationConfig(AnnotationElement.Annotation annotation) {
      AnnotationConfiguration annotationConfiguration = getAnnotationsConfig().annotations().get(annotation.getName());
      if (annotationConfiguration == null) {
         return null;
      }
      if (annotation.getPackageName() != null && !annotation.getPackageName().equals(annotationConfiguration.packageName())) {
         return null;
      }
      for (AnnotationElement.AnnotationTarget t : annotationConfiguration.target()) {
         if (t == AnnotationElement.AnnotationTarget.FIELD) {
            return annotationConfiguration;
         }
      }
      throw new DescriptorParserException("Annotation '" + annotation + "' cannot be applied to fields.");
   }

   @Override
   public String toString() {
      return "FieldDescriptor{" +
            "label=" + label +
            ", typeName='" + typeName + '\'' +
            ", name='" + name + '\'' +
            ", number='" + number + '\'' +
            '}';
   }

   public boolean hasDefaultValue() {
      // Fields no longer have a default value since Protobuf 3
      return false;
   }

   public Object getDefaultValue() {
      // Fields no longer have a default value since Protobuf 3
      return null;
   }

   public static class Builder implements OptionContainer<Builder> {
      String typeName;
      int number;
      String name;
      Label label = Label.OPTIONAL;
      List<Option> options = new ArrayList<>();
      String documentation;

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

      public Builder withDocumentation(String documentation) {
         this.documentation = documentation;
         return this;
      }

      @Override
      public Builder addOption(Option option) {
         this.options.add(option);
         return this;
      }

      public FieldDescriptor build() {
         return new FieldDescriptor(this);
      }
   }
}
