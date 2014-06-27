package org.infinispan.protostream.descriptors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;

/**
 * Represents a field in a proto file
 *
 * @author gustavonalle
 * @since 2.0
 */
public final class FieldDescriptor {

   private static final String PACKED = "packed";
   private final int number;
   private final String name;
   private final Rule rule;
   private final List<Option> options;
   private final String typeName;
   private final String defaultValue;
   private final Map<String, Object> optionByName = new HashMap<>();
   private final boolean isExtension;
   private String fullName;
   private Type type;
   private FileDescriptor fileDescriptor;
   private Descriptor messageType;
   private Descriptor containingMessage;
   private EnumDescriptor enumDescriptor;

   private FieldDescriptor(Builder builder) {
      this.number = builder.number;
      this.name = builder.name;
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

   public String getName() {
      return name;
   }

   public String getFullName() {
      return fullName;
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

   public EnumDescriptor getEnumDescriptor() {
      return enumDescriptor;
   }

   public String getTypeName() {
      return typeName;
   }

   public void setEnumDescriptor(EnumDescriptor enumDescriptor) {
      this.enumDescriptor = enumDescriptor;
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
   }

   void setFileDescriptor(FileDescriptor fileDescriptor) {
      this.fileDescriptor = fileDescriptor;
      if (enumDescriptor != null) {
         enumDescriptor.setFileDescriptor(fileDescriptor);
      }
      if (messageType != null) {
         messageType.setFileDescriptor(fileDescriptor);
      }
   }

   public static class Builder {
      public String typeName;
      private int number;
      private String name;
      private Rule rule;
      private List<Option> options;
      private String defaultValue;
      public boolean isExtension;

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

      public Builder withIsExtensions(boolean isExtension) {
         this.isExtension = isExtension;
         return this;
      }

      public FieldDescriptor build() {
         FieldDescriptor fieldDescriptor = new FieldDescriptor(this);
         Type fieldType;
         try {
            fieldType = Type.valueOf(typeName.toUpperCase());
            fieldDescriptor.setType(fieldType);
         } catch (IllegalArgumentException ignored) {
         }
         return fieldDescriptor;
      }
   }

}
