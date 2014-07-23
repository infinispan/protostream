package org.infinispan.protostream.descriptors;

import org.infinispan.protostream.DescriptorParserException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;

/**
 * Representation of a protofile, including dependencies
 *
 * @author gustavonalle
 * @since 2.0
 */
public final class FileDescriptor {

   private final String name;
   private final String packageName;
   private final String fullName;
   private final List<FileDescriptor> dependencies;
   private final List<Option> options;
   private final List<Descriptor> messageTypes;
   private final List<FieldDescriptor> extensions;
   private final List<EnumDescriptor> enumTypes;
   private final List<ExtendDescriptor> extendTypes;
   private final Map<String, ExtendDescriptor> extendDescriptors = new HashMap<>();
   private final Map<String, Object> typeRegistry = new HashMap<>();

   private FileDescriptor(Builder builder) {
      this.name = builder.name;
      this.packageName = builder.packageName;
      this.dependencies = builder.dependencies;
      this.options = unmodifiableList(builder.options);
      this.enumTypes = unmodifiableList(builder.enumTypes);
      this.messageTypes = unmodifiableList(builder.messageTypes);
      this.extensions = builder.extensions;
      this.extendTypes = unmodifiableList(builder.extendDescriptors);
      this.fullName = packageName != null ? packageName.replace('.', '/').concat("/").concat(name) : name;

      for (FileDescriptor dep : dependencies) {
         typeRegistry.putAll(dep.typeRegistry);
      }
      collectTypes();
      wireInternalReferences();
      
   }

   private void collectTypes() {
      for (Descriptor desc : messageTypes) {
         collectDescriptors(desc);
      }
      for (EnumDescriptor enumDesc : enumTypes) {
         collectEnumDescriptors(enumDesc);
      }
      for (ExtendDescriptor extendDescriptor : extendTypes) {
         collectExtensions(extendDescriptor);
      }
   }

   private void collectDescriptors(Descriptor descriptor) {
      typeRegistry.put(descriptor.getFullName(), descriptor);
      for (EnumDescriptor enumDescriptor : descriptor.getEnumTypes()) {
         enumDescriptor.setFileDescriptor(this);
         typeRegistry.put(enumDescriptor.getFullName(), enumDescriptor);
      }
      for (Descriptor nested : descriptor.getNestedTypes()) {
         collectDescriptors(nested);
      }
   }

   private void collectEnumDescriptors(EnumDescriptor enumDesc) {
      typeRegistry.put(enumDesc.getFullName(), enumDesc);
   }

   private void collectExtensions(ExtendDescriptor extendDescriptor) {
      extendDescriptors.put(extendDescriptor.getFullName(), extendDescriptor);
   }

   private void wireDescriptor(Descriptor descriptor) {
      descriptor.setFileDescriptor(this);
      for (FieldDescriptor fieldDescriptor : descriptor.getFields()) {
         fieldDescriptor.setContainingMessage(descriptor);
         fieldDescriptor.setFileDescriptor(this);
         if (fieldDescriptor.getType() == null) {
            Object res = searchType(fieldDescriptor.getTypeName(), descriptor);
            if (res instanceof EnumDescriptor) {
               fieldDescriptor.setType(Type.ENUM);
               fieldDescriptor.setEnumDescriptor((EnumDescriptor) res);
            } else if (res instanceof Descriptor) {
               fieldDescriptor.setType(Type.MESSAGE);
               fieldDescriptor.setMessageType((Descriptor) res);
            } else {
               throw new DescriptorParserException("Field type " + fieldDescriptor.getTypeName() + " not found");
            }
         }
      }

      for (Descriptor nested : descriptor.getNestedTypes()) {
         wireDescriptor(nested);
      }
   }

   private void wireInternalReferences() {
      for (Descriptor descriptor : messageTypes) {
         wireDescriptor(descriptor);
         for (Descriptor nested : descriptor.getNestedTypes()) {
            wireDescriptor(nested);
         }
      }
      for (EnumDescriptor enumDescriptor : enumTypes) {
         enumDescriptor.setFileDescriptor(this);
      }
      for (ExtendDescriptor extendDescriptor : extendTypes) {
         extendDescriptor.setFileDescriptor(this);
         Object res = searchType(extendDescriptor.getName(), null);
         if (res == null) {
            throw new DescriptorParserException("Extension error: type " + extendDescriptor.getName() + " not found");
         }
         extendDescriptor.setExtendedMessage((Descriptor) res);
      }
   }

   private String getScopedName(String name) {
      if (packageName == null) return name;
      return packageName.concat(".").concat(name);
   }

   private Object searchType(String name, Descriptor scope) {
      Object fullyQualified = typeRegistry.get(getScopedName(name));
      if (fullyQualified != null) {
         return fullyQualified;
      }
      Object relativeName = typeRegistry.get(name);
      if (relativeName != null) {
         return relativeName;
      }
      String scoped = scope.getFullName();
      String searchScope = scoped.concat(".").concat(name);
      Object o = typeRegistry.get(searchScope);
      if (o != null) {
         return o;
      }
      Descriptor containingType = scope.getContainingType();

      while (containingType != null) {
         Object res = searchType(name, containingType);
         if (res != null) {
            return res;
         }
         containingType = containingType.getContainingType();
      }

      return null;

   }

   public String getName() {
      return name;
   }

   public String getPackage() {
      return packageName;
   }

   public List<Option> getOptions() {
      return options;
   }

   public List<EnumDescriptor> getEnumTypes() {
      return enumTypes;
   }

   public List<Descriptor> getMessageTypes() {
      return messageTypes;
   }

   public List<ExtendDescriptor> getExtensionsTypes() {
      return extendTypes;
   }

   public String getFullName() {
      return fullName;
   }

   public static final class Builder {

      String name, packageName;
      List<FileDescriptor> dependencies = new ArrayList<>();
      List<FieldDescriptor> extensions;
      List<Option> options;
      List<EnumDescriptor> enumTypes;
      List<Descriptor> messageTypes;
      List<ExtendDescriptor> extendDescriptors;

      public Builder withName(String name) {
         this.name = name;
         return this;
      }

      public Builder withPackageName(String packageName) {
         this.packageName = packageName;
         return this;
      }

      public Builder withDependencies(List<FileDescriptor> dependencies) {
         this.dependencies = dependencies;
         return this;
      }

      public Builder withExtendDescriptors(List<ExtendDescriptor> extendDescriptors) {
         this.extendDescriptors = extendDescriptors;
         return this;
      }

      public Builder withOptions(List<Option> options) {
         this.options = options;
         return this;
      }

      public Builder withEnumTypes(List<EnumDescriptor> enumTypes) {
         this.enumTypes = enumTypes;
         return this;
      }

      public Builder withMessageTypes(List<Descriptor> messageTypes) {
         this.messageTypes = messageTypes;
         return this;
      }

      public FileDescriptor build() {
         return new FileDescriptor(this);
      }

   }

}
