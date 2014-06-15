package org.infinispan.protostream.descriptors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;

/**
 * Represents an enum in a proto file
 *
 * @author gustavonalle
 * @since 2.0
 */
public final class EnumDescriptor {

   private final String name, fullName;
   private final List<Option> options;
   private final List<EnumValueDescriptor> values;
   private final Map<Integer, EnumValueDescriptor> valueByNumber = new HashMap<>();
   private final Map<String, EnumValueDescriptor> valueByName = new HashMap<>();
   private FileDescriptor fileDescriptor;

   private EnumDescriptor(Builder builder) {
      this.name = builder.name;
      this.fullName = builder.fullName;
      this.options = unmodifiableList(builder.options);
      this.values = unmodifiableList(builder.values);
      for (EnumValueDescriptor value : values) {
         valueByNumber.put(value.getNumber(), value);
         valueByName.put(value.getName(), value);
      }
   }

   public String getName() {
      return name;
   }

   public String getFullName() {
      return fullName;
   }

   public FileDescriptor getFileDescriptor() {
      return fileDescriptor;
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
      for (EnumValueDescriptor valueDescriptor : values)
         valueDescriptor.setFileDescriptor(fileDescriptor);
   }

   public static class Builder {
      private String name;
      private String fullName;
      private List<Option> options;
      private List<EnumValueDescriptor> values;

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

      public EnumDescriptor build() {
         return new EnumDescriptor(this);
      }
   }


}
