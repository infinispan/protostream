package org.infinispan.protostream.descriptors;

import java.util.List;

/**
 * Represents a extend in a proto file.
 *
 * @author gustavonalle
 * @since 2.0
 */
public final class ExtendDescriptor {

   private final String name;
   private final String fullName;
   private final List<FieldDescriptor> fields;
   private FileDescriptor fileDescriptor;
   private Descriptor extendedMessage;

   public ExtendDescriptor(Builder builder) {
      this.name = builder.name;
      this.fullName = builder.fullName;
      this.fields = builder.fields;
   }

   public String getName() {
      return name;
   }

   public String getFullName() {
      return fullName;
   }

   public List<FieldDescriptor> getFields() {
      return fields;
   }

   void setFileDescriptor(FileDescriptor fileDescriptor) {
      this.fileDescriptor = fileDescriptor;
   }

   public Descriptor getExtendedMessage() {
      return extendedMessage;
   }

   void setExtendedMessage(Descriptor extendedMessage) {
      this.extendedMessage = extendedMessage;
   }

   public FileDescriptor getFileDescriptor() {
      return fileDescriptor;
   }

   public static class Builder {
      private List<FieldDescriptor> fields;
      private String fullName;
      private String name;

      public Builder withFields(List<FieldDescriptor> fields) {
         this.fields = fields;
         return this;
      }

      public Builder withFullName(String fullName) {
         this.fullName = fullName;
         return this;
      }

      public Builder withName(String name) {
         this.name = name;
         return this;
      }

      public ExtendDescriptor build() {
         return new ExtendDescriptor(this);
      }
   }
}
