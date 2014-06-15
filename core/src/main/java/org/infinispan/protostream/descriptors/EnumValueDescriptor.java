package org.infinispan.protostream.descriptors;

import java.util.List;

import static java.util.Collections.unmodifiableList;

/**
 * Represents each value of a enumeration in a .proto file
 *
 * @author gustavonalle
 * @since 2.0
 */
public final class EnumValueDescriptor {

   private final String name;
   private final int number;
   private final List<Option> options;
   private FileDescriptor fileDescriptor;

   private EnumValueDescriptor(final Builder builder) {
      this.name = builder.name;
      this.number = builder.number;
      this.options = unmodifiableList(builder.options);
   }

   public String getName() {
      return name;
   }

   public int getNumber() {
      return number;
   }

   public List<Option> getOptions() {
      return options;
   }

   public FileDescriptor getFileDescriptor() {
      return fileDescriptor;
   }

   void setFileDescriptor(FileDescriptor fileDescriptor) {
      this.fileDescriptor = fileDescriptor;
   }

   public static class Builder {
      private String name;
      private int number;
      private List<Option> options;

      public Builder withName(String name) {
         this.name = name;
         return this;
      }

      public Builder withTag(int tag) {
         this.number = tag;
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
