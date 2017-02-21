package org.infinispan.protostream.descriptors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.namespace.FileNamespace;
import org.infinispan.protostream.descriptors.namespace.Namespace;
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
   private final List<EnumDescriptor> enumTypes;
   private final List<ExtendDescriptor> extendTypes;

   private final Map<String, ExtendDescriptor> extendDescriptors = new HashMap<>();

   /**
    * Files that directly depend on this one.
    */
   private final Map<String, FileDescriptor> dependants = new HashMap<>();

   /**
    * The types defined in this file or in the imported files.
    */
   private FileNamespace fileNamespace;

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
       * A possibly recoverable error was encountered during processing.
       */
      ERROR,

      /**
       * An unrecoverable parsing error was encountered during processing.
       */
      PARSING_ERROR;

      boolean isResolved() {
         return this == RESOLVED;
      }

      boolean isError() {
         return this == ERROR || this == PARSING_ERROR;
      }
   }

   private Status status;

   private DescriptorParserException parsingException;

   private FileDescriptor(Builder builder) {
      name = builder.name;
      packageName = builder.packageName;
      dependencies = Collections.unmodifiableList(builder.dependencies);
      publicDependencies = Collections.unmodifiableList(builder.publicDependencies);
      options = Collections.unmodifiableList(builder.options);
      enumTypes = Collections.unmodifiableList(builder.enumTypes);
      messageTypes = Collections.unmodifiableList(builder.messageTypes);
      extendTypes = Collections.unmodifiableList(builder.extendDescriptors);
      parsingException = builder.parsingException;
      status = parsingException != null ? Status.PARSING_ERROR : Status.UNRESOLVED;
   }

   public void setConfiguration(Configuration configuration) {
      this.configuration = configuration;
   }

   public Map<String, FileDescriptor> getDependants() {
      return dependants;
   }

   public boolean isResolved() {
      return status.isResolved();
   }

   public void markUnresolved() {
      status = Status.UNRESOLVED;
   }

   void markError() {
      // parsing errors are fatal and final, so should not be overwritten
      if (status != Status.PARSING_ERROR) {
         status = Status.ERROR;
      }
   }

   /**
    * Transition form ERROR status to UNRESOLVED and propagate to all dependant FileDescriptors. All internal state used
    * during type reference resolution is cleared for this file and dependants.
    */
   public void clearErrors() {
      if (status != Status.RESOLVED && status != Status.PARSING_ERROR) {
         markUnresolved();
         fileNamespace = null;
         extendDescriptors.clear();

         for (FileDescriptor fd : dependants.values()) {
            fd.clearErrors();
         }
         dependants.clear();
      }
   }

   public Namespace getExportedNamespace() {
      if (status != Status.RESOLVED) {
         throw new IllegalStateException("File " + name + " is not resolved yet");
      }
      return fileNamespace.getExportedNamespace();
   }

   /**
    * Resolve type references across files and report semantic errors like duplicate type declarations, duplicate type
    * ids or clashing enum value constants. Only {@link Status#UNRESOLVED} files are processed. Files with other states
    * are ignored.
    */
   public void resolveDependencies(ResolutionContext resolutionContext) throws DescriptorParserException {
      resolveDependencies(resolutionContext, new HashSet<>());
   }

   private void resolveDependencies(ResolutionContext resolutionContext, Set<String> processedFiles) throws DescriptorParserException {
      if (status == Status.PARSING_ERROR) {
         resolutionContext.handleError(this, parsingException);
         return;
      }

      if (status != Status.UNRESOLVED) {
         return;
      }

      if (log.isDebugEnabled()) {
         log.debugf("Resolving dependencies of %s", name);
      }

      try {
         List<FileDescriptor> pubDeps = resolveImports(publicDependencies, resolutionContext, processedFiles);
         List<FileDescriptor> deps = resolveImports(dependencies, resolutionContext, processedFiles);
         if (status.isError()) {
            // no point going further if any of the imported files have errors
            return;
         }

         fileNamespace = new FileNamespace(this, pubDeps, deps);

         for (FileDescriptor fd : pubDeps) {
            fd.dependants.put(name, this);
         }
         for (FileDescriptor fd : deps) {
            fd.dependants.put(name, this);
         }

         for (Descriptor desc : messageTypes) {
            collectDescriptors(desc, resolutionContext);
         }
         for (EnumDescriptor enumDesc : enumTypes) {
            collectEnumDescriptors(enumDesc, resolutionContext);
         }
         for (ExtendDescriptor extendDescriptor : extendTypes) {
            collectExtensions(extendDescriptor);
         }
         for (Descriptor descriptor : messageTypes) {
            resolveFieldTypes(descriptor);
         }
         for (ExtendDescriptor extendDescriptor : extendTypes) {
            resolveExtension(extendDescriptor);
         }

         status = Status.RESOLVED;
         resolutionContext.flush();
         resolutionContext.handleSuccess(this);
      } catch (DescriptorParserException dpe) {
         resolutionContext.handleError(this, dpe);
      } catch (Exception e) {
         resolutionContext.handleError(this, new DescriptorParserException(e));
      } finally {
         if (status != Status.RESOLVED) {
            resolutionContext.clear();
         }
      }
   }

   /**
    * Resolves imported file names to {@link FileDescriptor}s. Changes the status of current file to ERROR if any of the
    * imported files (directly or indirectly) have any errors.
    *
    * @return the list of resolved {@link FileDescriptor}s
    */
   private List<FileDescriptor> resolveImports(List<String> dependencies, ResolutionContext resolutionContext,
                                               Set<String> processedFiles) throws DescriptorParserException {
      List<FileDescriptor> fileDescriptors = new ArrayList<>(dependencies.size());
      Set<String> uniqueDependencies = new HashSet<>(dependencies.size());
      for (String dependency : dependencies) {
         if (!uniqueDependencies.add(dependency)) {
            resolutionContext.handleError(this, new DescriptorParserException("Duplicate import : " + dependency));
            continue;
         }
         FileDescriptor fd = resolutionContext.getFileDescriptorMap().get(dependency);
         if (fd == null) {
            resolutionContext.handleError(this, new DescriptorParserException("Import '" + dependency + "' not found"));
            continue;
         }
         if (fd.status == Status.UNRESOLVED) {
            if (!processedFiles.add(dependency)) {
               resolutionContext.handleError(this, new DescriptorParserException("Cyclic import detected at " + name + ", import " + dependency));
               continue;
            }
            fd.resolveDependencies(resolutionContext, processedFiles);
         }
         if (fd.status.isError()) {
            resolutionContext.handleError(this, new DescriptorParserException("File " + name + " imports a file (" + fd.getName() + ") that has errors"));
            continue;
         }
         fileDescriptors.add(fd);
      }
      return fileDescriptors;
   }

   private void collectDescriptors(Descriptor descriptor, ResolutionContext resolutionContext) {
      descriptor.setFileDescriptor(this);
      fileNamespace.put(descriptor.getFullName(), descriptor);
      resolutionContext.addGenericDescriptor(descriptor);

      for (Descriptor nested : descriptor.getNestedTypes()) {
         collectDescriptors(nested, resolutionContext);
      }
      for (EnumDescriptor enumDescriptor : descriptor.getEnumTypes()) {
         collectEnumDescriptors(enumDescriptor, resolutionContext);
      }
   }

   private void collectEnumDescriptors(EnumDescriptor enumDescriptor, ResolutionContext resolutionContext) {
      enumDescriptor.setFileDescriptor(this);
      fileNamespace.put(enumDescriptor.getFullName(), enumDescriptor);
      resolutionContext.addGenericDescriptor(enumDescriptor);
   }

   private void collectExtensions(ExtendDescriptor extendDescriptor) {
      extendDescriptor.setFileDescriptor(this);
      extendDescriptors.put(extendDescriptor.getFullName(), extendDescriptor);
   }

   private void resolveFieldTypes(Descriptor descriptor) {
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
         resolveFieldTypes(nested);
      }
   }

   private void resolveExtension(ExtendDescriptor extendDescriptor) {
      GenericDescriptor res = searchType(extendDescriptor.getName(), null);
      if (res == null) {
         throw new DescriptorParserException("Extension error: type " + extendDescriptor.getName() + " not found");
      }
      if (res instanceof EnumDescriptor) {
         throw new DescriptorParserException("Enumerations cannot be extended: " + extendDescriptor.getFullName());
      }
      extendDescriptor.setExtendedMessage((Descriptor) res);
   }

   private String getScopedName(String name) {
      return packageName == null ? name : packageName.concat(".").concat(name);
   }

   private GenericDescriptor searchType(String name, Descriptor scope) {
      GenericDescriptor fullyQualified = fileNamespace.get(getScopedName(name));
      if (fullyQualified != null) {
         return fullyQualified;
      }
      GenericDescriptor relativeName = fileNamespace.get(name);
      if (relativeName != null) {
         return relativeName;
      }

      if (scope != null) {
         String searchScope = scope.getFullName().concat(".").concat(name);
         GenericDescriptor o = fileNamespace.get(searchScope);
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
      return fileNamespace.getLocalNamespace().getTypes();
   }

   @Override
   public String toString() {
      return "FileDescriptor{name=" + name + '}';
   }

   public static final class Builder {

      private String name;
      private String packageName;
      private List<String> dependencies = Collections.emptyList();
      private List<String> publicDependencies = Collections.emptyList();
      private List<Option> options = Collections.emptyList();
      private List<EnumDescriptor> enumTypes = Collections.emptyList();
      private List<Descriptor> messageTypes = Collections.emptyList();
      private List<ExtendDescriptor> extendDescriptors = Collections.emptyList();
      private DescriptorParserException parsingException;

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

      public Builder withEnumTypes(List<EnumDescriptor> enumTypes) {
         this.enumTypes = enumTypes;
         return this;
      }

      public Builder withMessageTypes(List<Descriptor> messageTypes) {
         this.messageTypes = messageTypes;
         return this;
      }

      public Builder withParsingException(DescriptorParserException parsingException) {
         this.parsingException = parsingException;
         return this;
      }

      public FileDescriptor build() {
         return new FileDescriptor(this);
      }
   }
}
