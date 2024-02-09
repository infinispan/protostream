package org.infinispan.protostream.descriptors;

import static org.infinispan.protostream.descriptors.FileDescriptor.fullName;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents each constant value of a enumeration in a .proto file.
 *
 * @author gustavonalle
 * @author anistor@redhat.com
 * @since 2.0
 */
public final class EnumValueDescriptor {

   private final String name;
   private String fullName;
   private String scopedName; // the name of this enum value constant in its scope
   private final int number;
   private final String documentation;
   private final List<Option> options;
   private EnumDescriptor enumDescriptor;
   private FileDescriptor fileDescriptor;

   private EnumValueDescriptor(Builder builder) {
      this.name = builder.name;
      this.number = builder.number;
      this.documentation = builder.documentation;
      this.options = List.copyOf(builder.options);
   }

   public String getName() {
      return name;
   }

   public int getNumber() {
      return number;
   }

   public String getDocumentation() {
      return documentation;
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

   public String getFullName() {
      return fullName;
   }

   public String getScopedName() {
      return scopedName;
   }

   public EnumDescriptor getContainingEnum() {
      return enumDescriptor;
   }

   void setContainingEnum(EnumDescriptor enumDescriptor) {
      this.enumDescriptor = enumDescriptor;
      fullName = fullName(enumDescriptor.getFullName(), name);
      scopedName = enumDescriptor.getFullName().substring(0, enumDescriptor.getFullName().length()
            - enumDescriptor.getName().length()) + name;
   }

   public FileDescriptor getFileDescriptor() {
      return fileDescriptor;
   }

   void setFileDescriptor(FileDescriptor fileDescriptor) {
      this.fileDescriptor = fileDescriptor;
   }

   @Override
   public String toString() {
      return "EnumValueDescriptor{fullName=" + fullName + '}';
   }

   public static final class Builder implements OptionContainer<Builder> {
      private String name;
      private int number;
      private String documentation;
      private List<Option> options = new ArrayList<>();

      public Builder withName(String name) {
         this.name = name;
         return this;
      }

      public Builder withTag(int tag) {
         this.number = tag;
         return this;
      }

      public Builder withDocumentation(String documentation) {
         this.documentation = documentation;
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

      public EnumValueDescriptor build() {
         return new EnumValueDescriptor(this);
      }
   }
}
