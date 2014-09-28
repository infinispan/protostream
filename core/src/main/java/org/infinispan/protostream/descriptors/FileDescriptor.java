package org.infinispan.protostream.descriptors;

import org.infinispan.protostream.DescriptorParserException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;

/**
 * Representation of a protofile, including dependencies.
 *
 * @author gustavonalle
 * @author anistor@redhat.com
 * @since 2.0
 */
public final class FileDescriptor {

   private final String name;
   private final String packageName;
   private final List<FileDescriptor> dependencies;
   private final List<FileDescriptor> publicDependencies;
   private final List<Option> options;
   private final List<Descriptor> messageTypes;
   private final List<FieldDescriptor> extensions;
   private final List<EnumDescriptor> enumTypes;
   private final List<ExtendDescriptor> extendTypes;
   private final Map<String, ExtendDescriptor> extendDescriptors = new HashMap<>();

   /**
    * All types defined in this file or visible from imported files.
    */
   private final Map<String, GenericDescriptor> typeRegistry = new HashMap<>();

   /**
    * Types defined in this file or defined in publicly imported files.
    */
   private final Map<String, GenericDescriptor> exportedTypes = new HashMap<>();

   /**
    * Types defined in this file.
    */
   private final Map<String, GenericDescriptor> types = new HashMap<>();

   private FileDescriptor(Builder builder) {
      this.name = builder.name;
      this.packageName = builder.packageName;
      this.dependencies = builder.dependencies;
      this.publicDependencies = builder.publicDependencies;
      this.options = unmodifiableList(builder.options);
      this.enumTypes = unmodifiableList(builder.enumTypes);
      this.messageTypes = unmodifiableList(builder.messageTypes);
      this.extensions = builder.extensions;
      this.extendTypes = unmodifiableList(builder.extendDescriptors);

      for (FileDescriptor dep : publicDependencies) {
         typeRegistry.putAll(dep.exportedTypes);
         exportedTypes.putAll(dep.exportedTypes);
      }
      for (FileDescriptor dep : dependencies) {
         typeRegistry.putAll(dep.exportedTypes);
      }

      for (Descriptor desc : messageTypes) {
         collectDescriptors(desc);
      }
      for (EnumDescriptor enumDesc : enumTypes) {
         collectEnumDescriptors(enumDesc);
      }
      for (ExtendDescriptor extendDescriptor : extendTypes) {
         collectExtensions(extendDescriptor);
      }

      for (Descriptor descriptor : messageTypes) {
         wireDescriptor(descriptor);
      }

      for (ExtendDescriptor extendDescriptor : extendTypes) {
         GenericDescriptor res = searchType(extendDescriptor.getName(), null);
         if (res == null) {
            throw new DescriptorParserException("Extension error: type " + extendDescriptor.getName() + " not found");
         }
         extendDescriptor.setExtendedMessage((Descriptor) res);  //todo [anistor] is it possible to extend an enum?
      }
   }

   private void collectDescriptors(Descriptor descriptor) {
      checkValidDefinition(descriptor);

      descriptor.setFileDescriptor(this);
      typeRegistry.put(descriptor.getFullName(), descriptor);
      types.put(descriptor.getFullName(), descriptor);
      exportedTypes.put(descriptor.getFullName(), descriptor);
      for (EnumDescriptor enumDescriptor : descriptor.getEnumTypes()) {
         collectEnumDescriptors(enumDescriptor);
      }
      for (Descriptor nested : descriptor.getNestedTypes()) {
         collectDescriptors(nested);
      }
   }

   private void collectEnumDescriptors(EnumDescriptor enumDescriptor) {
      checkValidDefinition(enumDescriptor);

      enumDescriptor.setFileDescriptor(this);
      typeRegistry.put(enumDescriptor.getFullName(), enumDescriptor);
      types.put(enumDescriptor.getFullName(), enumDescriptor);
      exportedTypes.put(enumDescriptor.getFullName(), enumDescriptor);
   }

   private void checkValidDefinition(GenericDescriptor descriptor) {
      if (descriptor.getName().indexOf('.') != -1) {
         throw new DescriptorParserException("Definition names should not be qualified : " + descriptor.getName());
      }
      GenericDescriptor existing = types.get(descriptor.getFullName());
      if (existing != null) {
         String location = existing.getFileDescriptor().getName();
         if (!location.equals(getName())) {
            location = location + ", " + getName();
         }
         throw new DescriptorParserException(descriptor.getFullName() + " is already defined in " + location);
      }
   }

   private void collectExtensions(ExtendDescriptor extendDescriptor) {
      extendDescriptor.setFileDescriptor(this);
      extendDescriptors.put(extendDescriptor.getFullName(), extendDescriptor);
   }

   private void wireDescriptor(Descriptor descriptor) {
      for (FieldDescriptor fieldDescriptor : descriptor.getFields()) {
         fieldDescriptor.setContainingMessage(descriptor);
         fieldDescriptor.setFileDescriptor(this);
         if (fieldDescriptor.getType() == null) {
            GenericDescriptor res = searchType(fieldDescriptor.getTypeName(), descriptor);
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

   private String getScopedName(String name) {
      if (packageName == null) return name;
      return packageName.concat(".").concat(name);
   }

   private GenericDescriptor searchType(String name, Descriptor scope) {
      GenericDescriptor fullyQualified = typeRegistry.get(getScopedName(name));
      if (fullyQualified != null) {
         return fullyQualified;
      }
      GenericDescriptor relativeName = typeRegistry.get(name);
      if (relativeName != null) {
         return relativeName;
      }

      if (scope != null) {
         String searchScope = scope.getFullName().concat(".").concat(name);
         GenericDescriptor o = typeRegistry.get(searchScope);
         if (o != null) {
            return o;
         }

         Descriptor containingType;
         while ((containingType = scope.getContainingType()) != null) {
            GenericDescriptor res = searchType(name, containingType);
            if (res != null) {
               return res;
            }
         }
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

   public Map<String, GenericDescriptor> getTypes() {
      return types;
   }

   public static final class Builder {

      private String name;
      private String packageName;
      private List<FileDescriptor> dependencies = new ArrayList<>();
      private List<FileDescriptor> publicDependencies = new ArrayList<>();
      private List<FieldDescriptor> extensions;
      private List<Option> options;
      private List<EnumDescriptor> enumTypes;
      private List<Descriptor> messageTypes;
      private List<ExtendDescriptor> extendDescriptors;

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

      public Builder withPublicDependencies(List<FileDescriptor> publicDependencies) {
         this.publicDependencies = publicDependencies;
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

      public Builder withExtensions(List<FieldDescriptor> extensions) {
         this.extensions = extensions;
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
