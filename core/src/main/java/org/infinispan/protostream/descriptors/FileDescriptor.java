package org.infinispan.protostream.descriptors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.impl.Log;

/**
 * Representation of a .proto file, including its dependencies.
 *
 * @author gustavonalle
 * @author anistor@redhat.com
 * @since 2.0
 */
public final class FileDescriptor {

   private static final Log log = Log.LogFactory.getLog(FileDescriptor.class);

   protected Configuration configuration;

   private final String name;
   private final String packageName;

   /**
    * The imports. These are not transitive.
    */
   private final List<String> dependencies;

   /**
    * The public imports. These generate transitive dependencies.
    */
   private final List<String> publicDependencies;

   private final List<Option> options;
   private final List<Descriptor> messageTypes;
   private final List<FieldDescriptor> extensions;
   private final List<EnumDescriptor> enumTypes;
   private final List<ExtendDescriptor> extendTypes;

   private final Map<String, ExtendDescriptor> extendDescriptors = new HashMap<String, ExtendDescriptor>();

   /**
    * Files that directly depend on this one.
    */
   private final Map<String, FileDescriptor> dependants = new HashMap<String, FileDescriptor>();

   public void setConfiguration(Configuration configuration) {
      this.configuration = configuration;
   }

   /**
    * The validation status of a .proto file. Only {@link Status#RESOLVED} files contribute to the current state (known
    * types) of the {@link org.infinispan.protostream.SerializationContext}.
    */
   private enum Status {

      /**
       * Not processed yet.
       */
      UNRESOLVED,

      /**
       * Processed successfully.
       */
      RESOLVED,

      /**
       * An error was encountered during processing.
       */
      ERROR
   }

   private Status status = Status.UNRESOLVED;

   /**
    * All types defined in this file or visible from imported files.
    */
   private final Map<String, GenericDescriptor> typeRegistry = new HashMap<String, GenericDescriptor>();

   /**
    * Types defined in this file or defined in publicly imported files.
    */
   private final Map<String, GenericDescriptor> exportedTypes = new HashMap<String, GenericDescriptor>();

   /**
    * Types defined in this file.
    */
   private final Map<String, GenericDescriptor> types = new LinkedHashMap<String, GenericDescriptor>();

   private FileDescriptor(Builder builder) {
      this.name = builder.name;
      this.packageName = builder.packageName;
      this.dependencies = Collections.unmodifiableList(builder.dependencies);
      this.publicDependencies = Collections.unmodifiableList(builder.publicDependencies);
      this.options = Collections.unmodifiableList(builder.options);
      this.enumTypes = Collections.unmodifiableList(builder.enumTypes);
      this.messageTypes = Collections.unmodifiableList(builder.messageTypes);
      this.extensions = builder.extensions;
      this.extendTypes = Collections.unmodifiableList(builder.extendDescriptors);
   }

   public Map<String, FileDescriptor> getDependants() {
      return dependants;
   }

   public boolean isResolved() {
      return status == Status.RESOLVED;
   }

   public void markUnresolved() {
      status = Status.UNRESOLVED;
   }

   /**
    * Transition form ERROR status to UNRESOLVED and propagate to all dependant FileDescriptors. All internal state used
    * during type reference resolution is cleared for this file and dependants.
    */
   public void clearErrors() {
      if (status != Status.RESOLVED) {
         status = Status.UNRESOLVED;
         typeRegistry.clear();
         exportedTypes.clear();
         types.clear();
         extendDescriptors.clear();

         for (FileDescriptor fd : dependants.values()) {
            fd.clearErrors();
         }
         dependants.clear();
      }
   }

   public boolean resolveDependencies(FileDescriptorSource.ProgressCallback progressCallback,
                                      Map<String, FileDescriptor> fileDescriptorMap,
                                      Map<String, GenericDescriptor> allTypes) throws DescriptorParserException {
      if (status == Status.UNRESOLVED) {
         resolveDependencies(progressCallback, fileDescriptorMap, allTypes, new HashSet<String>());
      }
      return status == Status.RESOLVED;
   }

   private void resolveDependencies(FileDescriptorSource.ProgressCallback progressCallback,
                                    Map<String, FileDescriptor> fileDescriptorMap,
                                    Map<String, GenericDescriptor> allTypes,
                                    Set<String> processedFiles) throws DescriptorParserException {
      if (status != Status.UNRESOLVED) {
         return;
      }

      if (log.isDebugEnabled()) {
         log.debugf("Resolving dependencies of %s", name);
      }

      try {
         List<FileDescriptor> pubDeps = resolveImports(progressCallback, fileDescriptorMap, allTypes, processedFiles, publicDependencies);
         if (pubDeps == null) {
            return;
         }
         List<FileDescriptor> deps = resolveImports(progressCallback, fileDescriptorMap, allTypes, processedFiles, dependencies);
         if (deps == null) {
            return;
         }

         for (FileDescriptor dep : pubDeps) {
            typeRegistry.putAll(dep.exportedTypes);
            exportedTypes.putAll(dep.exportedTypes);
         }
         for (FileDescriptor dep : deps) {
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
            resolveTypes(descriptor);
         }

         for (ExtendDescriptor extendDescriptor : extendTypes) {
            GenericDescriptor res = searchType(extendDescriptor.getName(), null);
            if (res == null) {
               throw new DescriptorParserException("Extension error: type " + extendDescriptor.getName() + " not found");
            }
            extendDescriptor.setExtendedMessage((Descriptor) res);  //todo [anistor] is it possible to extend an enum?
         }

         // check duplicate type definitions
         for (String typeName : types.keySet()) {
            GenericDescriptor existing = allTypes.get(typeName);
            if (existing != null) {
               List<String> locations = Arrays.asList(name, existing.getFileDescriptor().getName());
               Collections.sort(locations);
               throw new DescriptorParserException("Duplicate definition of " + typeName + " in " + locations.get(0) + " and " + locations.get(1));
            }
         }

         for (FileDescriptor fd : pubDeps) {
            fd.dependants.put(name, this);
         }
         for (FileDescriptor fd : deps) {
            fd.dependants.put(name, this);
         }
      } catch (DescriptorParserException dpe) {
         status = Status.ERROR;
         if (progressCallback != null) {
            log.debugf("File has errors : %s", name);
            progressCallback.handleError(name, dpe);
            return;
         } else {
            throw dpe;
         }
      }

      status = Status.RESOLVED;
      if (progressCallback != null) {
         log.debugf("File resolved successfully : %s", name);
         progressCallback.handleSuccess(name);
      }
   }

   private List<FileDescriptor> resolveImports(FileDescriptorSource.ProgressCallback progressCallback,
                                               Map<String, FileDescriptor> fileDescriptorMap,
                                               Map<String, GenericDescriptor> allTypes,
                                               Set<String> processedFiles,
                                               List<String> dependencies) throws DescriptorParserException {
      List<FileDescriptor> fileDescriptors = new ArrayList<FileDescriptor>(dependencies.size());
      Set<String> uniqueDependencies = new HashSet<String>(dependencies.size());
      for (String dependency : dependencies) {
         if (!uniqueDependencies.add(dependency)) {
            throw new DescriptorParserException("Duplicate import : " + dependency);
         }
         FileDescriptor fd = fileDescriptorMap.get(dependency);
         if (fd == null) {
            throw new DescriptorParserException("Import '" + dependency + "' not found");
         }
         if (fd.status == Status.UNRESOLVED) {
            if (!processedFiles.add(dependency)) {
               throw new DescriptorParserException("Cyclic import detected at " + name + ", import " + dependency);
            }
            fd.resolveDependencies(progressCallback, fileDescriptorMap, allTypes, processedFiles);
         }
         if (fd.status == Status.ERROR) {
            status = Status.ERROR;
            return null;
         }
         fileDescriptors.add(fd);
      }
      return fileDescriptors;
   }

   private void collectDescriptors(Descriptor descriptor) {
      descriptor.setFileDescriptor(this);
      checkValidDefinition(descriptor);

      typeRegistry.put(descriptor.getFullName(), descriptor);
      types.put(descriptor.getFullName(), descriptor);
      exportedTypes.put(descriptor.getFullName(), descriptor);

      for (EnumDescriptor enumDescriptor : descriptor.getEnumTypes()) {
         enumDescriptor.setContainingType(descriptor);
         collectEnumDescriptors(enumDescriptor);
      }

      for (Descriptor nested : descriptor.getNestedTypes()) {
         collectDescriptors(nested);
      }
   }

   private void collectEnumDescriptors(EnumDescriptor enumDescriptor) {
      enumDescriptor.setFileDescriptor(this);
      checkValidDefinition(enumDescriptor);

      typeRegistry.put(enumDescriptor.getFullName(), enumDescriptor);
      types.put(enumDescriptor.getFullName(), enumDescriptor);
      exportedTypes.put(enumDescriptor.getFullName(), enumDescriptor);
   }

   private void checkValidDefinition(GenericDescriptor descriptor) {
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

   private void resolveTypes(Descriptor descriptor) {
      for (FieldDescriptor fieldDescriptor : descriptor.getFields()) {
         if (fieldDescriptor.getType() == null) {
            GenericDescriptor res = searchType(fieldDescriptor.getTypeName(), descriptor);
            if (res instanceof EnumDescriptor) {
               fieldDescriptor.setEnumType((EnumDescriptor) res);
            } else if (res instanceof Descriptor) {
               fieldDescriptor.setMessageType((Descriptor) res);
            } else {
               throw new DescriptorParserException("Field type " + fieldDescriptor.getTypeName() + " not found");
            }
         }
      }

      for (Descriptor nested : descriptor.getNestedTypes()) {
         resolveTypes(nested);
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

   /**
    * Top level message types defined in this file.
    */
   public List<Descriptor> getMessageTypes() {
      return messageTypes;
   }

   public List<ExtendDescriptor> getExtensionsTypes() {
      return extendTypes;
   }

   /**
    * All types defined in this file (both message and enum).
    */
   public Map<String, GenericDescriptor> getTypes() {
      if (status != Status.RESOLVED) {
         throw new IllegalStateException("File " + name + " is not resolved yet");
      }
      return types;
   }

   public static final class Builder {

      private String name;
      private String packageName;
      private List<String> dependencies = new ArrayList<String>();
      private List<String> publicDependencies = new ArrayList<String>();
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

      public Builder withDependencies(List<String> dependencies) {
         this.dependencies = dependencies;
         return this;
      }

      public Builder withPublicDependencies(List<String> publicDependencies) {
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
