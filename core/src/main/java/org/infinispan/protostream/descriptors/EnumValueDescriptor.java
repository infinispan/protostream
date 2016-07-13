package org.infinispan.protostream.descriptors;

import static java.util.Collections.unmodifiableList;

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
      this.options = unmodifiableList(builder.options);
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
      fullName = enumDescriptor.getFullName() + "." + name;
      scopedName = enumDescriptor.getFullName().substring(0, enumDescriptor.getFullName().length()
            - enumDescriptor.getName().length()) + name;
   }

   public FileDescriptor getFileDescriptor() {
      return fileDescriptor;
   }

   void setFileDescriptor(FileDescriptor fileDescriptor) {
      this.fileDescriptor = fileDescriptor;
   }

   public static final class Builder {
      private String name;
      private int number;
      private String documentation;
      private List<Option> options;

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

      public EnumValueDescriptor build() {
         return new EnumValueDescriptor(this);
      }
   }
}
