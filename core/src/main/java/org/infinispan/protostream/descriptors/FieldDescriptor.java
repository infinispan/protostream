package org.infinispan.protostream.descriptors;

import org.infinispan.protostream.config.AnnotationConfig;
import org.infinispan.protostream.impl.AnnotatedDescriptorImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;

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
   private final Rule rule;
   private final List<Option> options;
   private final String typeName;
   private final String defaultValue;
   private final Map<String, Object> optionByName = new HashMap<String, Object>();
   private final boolean isExtension;
   private Type type;
   private FileDescriptor fileDescriptor;
   private Descriptor containingMessage;
   private Descriptor messageType;
   private EnumDescriptor enumType;

   private FieldDescriptor(Builder builder) {
      super(builder.name, null, builder.documentation);
      this.number = builder.number;
      this.rule = builder.rule;
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

   public Type getType() {
      return type;
   }

   void setType(Type type) {
      this.type = type;
   }

   public Descriptor getMessageType() {
      return messageType;
   }

   public Rule getRule() {
      return rule;
   }

   public Object getOptionByName(String name) {
      return optionByName.get(name);
   }

   public List<Option> getOptions() {
      return options;
   }

   public boolean isRequired() {
      return rule.equals(Rule.REQUIRED);
   }

   public boolean isRepeated() {
      return rule.equals(Rule.REPEATED);
   }

   public boolean isPacked() {
      return optionByName.containsKey(PACKED);
   }

   public Object getDefaultValue() {
      if (!hasDefaultValue()) {
         return null;
      }
      if (!getJavaType().isScalar()) {
         throw new UnsupportedOperationException("FieldDescriptor.getDefaultValue() called on an embedded message field.");
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
      this.fullName = containingMessage.getFullName().concat(".").concat(name);
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
   protected AnnotationConfig<FieldDescriptor> getAnnotationConfig(String annotationName) {
      return fileDescriptor.configuration.fieldAnnotations().get(annotationName);
   }

   public static class Builder {
      private String typeName;
      private int number;
      private String name;
      private Rule rule;
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

      public Builder withRule(Rule rule) {
         this.rule = rule;
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
