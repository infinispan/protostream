package org.infinispan.protostream.descriptors;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

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

   public enum Syntax {
      PROTO2,
      PROTO3;

      public static Syntax fromString(String syntax) {
         return Syntax.valueOf(syntax.toUpperCase());
      }


      @Override
      public String toString() {
         return name().toLowerCase();
      }
   }

   private static final Log log = Log.LogFactory.getLog(MethodHandles.lookup().lookupClass());

   private final Syntax syntax;

   private Configuration configuration;

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

   /**
    * When {@link #status} is equal to {@link Status#PARSING_ERROR}, this exception provides the cause.
    */
   private final DescriptorParserException parsingException;

   private FileDescriptor(Builder builder) {
      // Default to proto2 if no syntax was specified
      syntax = builder.syntax == null ? Syntax.PROTO2 : builder.syntax;
      name = builder.name;
      packageName = builder.packageName;
      dependencies = List.copyOf(builder.dependencies);
      publicDependencies = List.copyOf(builder.publicDependencies);
      options = List.copyOf(builder.options);
      enumTypes = List.copyOf(builder.enumTypes);
      messageTypes = List.copyOf(builder.messageTypes);

      parsingException = builder.parsingException;
      status = parsingException != null ? Status.PARSING_ERROR : Status.UNRESOLVED;
   }

   public Configuration getConfiguration() {
      return configuration;
   }

   /**
    * This method is not part of the public API. May be removed in future versions.
    */
   public void setConfiguration(Configuration configuration) {
      this.configuration = configuration;
   }

   public Map<String, FileDescriptor> getDependants() {
      return dependants;
   }

   public Collection<String> getDependencies() {
      return Stream.concat(dependencies.stream(), publicDependencies.stream()).toList();
   }

   public boolean isResolved() {
      return status.isResolved();
   }

   public void markUnresolved() {
      status = Status.UNRESOLVED;
   }

   void markError() {
      // parsing errors are already considered fatal and final so should not be overwritten by a 'regular' error
      if (status != Status.PARSING_ERROR) {
         status = Status.ERROR;
      }
   }

   /**
    * Clear resolving errors of unresolved files. Parsing errors are not cleared. Transitions from ERROR status back to
    * UNRESOLVED and propagates this recursively to all dependant FileDescriptors. All internal state acquired during
    * type reference resolution is cleared for this file and dependants (recursively).
    */
   public void clearErrors() {
      if (status != Status.RESOLVED && status != Status.PARSING_ERROR) {
         markUnresolved();
         fileNamespace = null;

         for (FileDescriptor fd : dependants.values()) {
            fd.clearErrors();
         }
         dependants.clear();
      }
   }

   public Namespace getExportedNamespace() {
      if (status != Status.RESOLVED) {
         throw new IllegalStateException("File '" + name + "' is not resolved yet");
      }
      return fileNamespace.getExportedNamespace();
   }

   /**
    * Resolve type references across files and report semantic errors like duplicate type declarations, duplicate type
    * ids or clashing enum value constants. Only {@link FileDescriptor.Status#UNRESOLVED} files are processed. Files
    * with other states are ignored.
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
         for (Descriptor descriptor : messageTypes) {
            resolveFieldTypes(descriptor);
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
      fileNamespace.put(enumDescriptor.getFullName(), enumDescriptor);
      resolutionContext.addGenericDescriptor(enumDescriptor);
   }

   private void resolveFieldTypes(Descriptor descriptor) {
      for (FieldDescriptor fieldDescriptor : descriptor.getFields()) {
         if (fieldDescriptor.getType() == null ||
               fieldDescriptor.getType() == Type.GROUP ||
               fieldDescriptor.getType() == Type.MESSAGE ||
               fieldDescriptor.getType() == Type.ENUM) {
            GenericDescriptor res = searchType(fieldDescriptor.getTypeName(), descriptor);
            if (res instanceof EnumDescriptor) {
               fieldDescriptor.setEnumType((EnumDescriptor) res);
            } else if (res instanceof Descriptor) {
               fieldDescriptor.setMessageType((Descriptor) res);
            } else {
               throw new DescriptorParserException("Failed to resolve type of field \"" + fieldDescriptor.getFullName()
                     + "\" in \"" + name + "\". Type not found : " + fieldDescriptor.getTypeName());
            }
         }
      }

      for (Descriptor nested : descriptor.getNestedTypes()) {
         resolveFieldTypes(nested);
      }
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

   public Syntax getSyntax() {
      return syntax;
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

   public Option getOption(String name) {
      for (Option o : options) {
         if (o.getName().equals(name)) {
            return o;
         }
      }
      return null;
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

   /**
    * All types defined in this file (both message and enum).
    */
   public Map<String, GenericDescriptor> getTypes() {
      if (status != Status.RESOLVED) {
         throw new IllegalStateException("File '" + name + "' is not resolved yet");
      }
      return fileNamespace.getLocalNamespace().getTypes();
   }

   public void checkCompatibility(FileDescriptor that, boolean strict, List<String> errors) {
      for(Descriptor dThat : that.getMessageTypes()) {
         messageTypes.stream().filter(d -> d.getName().equals(dThat.getName())).findFirst().ifPresent(d -> d.checkCompatibility(dThat, strict, errors));
      }
      for(EnumDescriptor eThat : that.getEnumTypes()) {
         enumTypes.stream().filter(e -> e.getName().equals(eThat.getName())).findFirst().ifPresent(e -> e.checkCompatibility(eThat, strict, errors));
      }
   }

   public void checkCompatibility(FileDescriptor that, boolean strict) {
      List<String> errors = new ArrayList<>();
      checkCompatibility(that, strict, errors);
      if (!errors.isEmpty()) {
         throw Log.LOG.incompatibleSchemaChanges(String.join("\n", errors));
      }
   }

   public void parseAnnotations() {
      if (configuration == null) {
         throw new IllegalStateException("FileDescriptor.setConfiguration() must be invoked before parsing the annotations");
      }
      messageTypes.forEach(descriptor -> descriptor.setFileDescriptor(this));
      enumTypes.forEach(enumDescriptor -> enumDescriptor.setFileDescriptor(this));
   }

   @Override
   public String toString() {
      return "FileDescriptor{" +
            "name='" + name + '\'' +
            ", packageName='" + packageName + '\'' +
            ", status=" + status +
            '}';
   }

   public static String fullName(String parent, String name) {
      return parent == null ? name : parent + '.' + name;
   }

   public static final class Builder implements MessageContainer<Builder>, OptionContainer<Builder>, EnumContainer<Builder> {

      private Syntax syntax = Syntax.PROTO2;
      private String name;
      private String packageName;
      private List<String> dependencies = new ArrayList<>();
      private List<String> publicDependencies = new ArrayList<>();
      private List<Option> options = new ArrayList<>();
      private List<EnumDescriptor> enumTypes = new ArrayList<>();
      private List<Descriptor> messageTypes = new ArrayList<>();
      private DescriptorParserException parsingException;

      public Builder withSyntax(Syntax syntax) {
         this.syntax = syntax;
         return this;
      }

      public Builder withName(String name) {
         this.name = name;
         return this;
      }

      public Builder withPackageName(String packageName) {
         this.packageName = packageName;
         return this;
      }

      @Override
      public String getFullName() {
         return packageName;
      }

      public Builder withDependencies(List<String> dependencies) {
         this.dependencies = dependencies;
         return this;
      }

      public Builder addDependency(String dependency) {
         this.dependencies.add(dependency);
         return this;
      }

      public Builder withPublicDependencies(List<String> publicDependencies) {
         this.publicDependencies = publicDependencies;
         return this;
      }

      public Builder addPublicDependency(String dependency) {
         this.publicDependencies.add(dependency);
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

      public Builder withEnumTypes(List<EnumDescriptor> enumTypes) {
         this.enumTypes = enumTypes;
         return this;
      }

      @Override
      public Builder addEnum(EnumDescriptor.Builder enumDescriptor) {
         this.enumTypes.add(enumDescriptor.withFullName(fullName(packageName, enumDescriptor.getName())).build());
         return this;
      }

      public Builder withMessageTypes(List<Descriptor> messageTypes) {
         this.messageTypes = messageTypes;
         return this;
      }

      @Override
      public Builder addMessage(Descriptor.Builder message) {
         this.messageTypes.add(message.withFullName(fullName(packageName, message.getName())).build());
         return this;
      }

      public Builder withParsingException(DescriptorParserException parsingException) {
         this.parsingException = parsingException;
         return this;
      }

      public FileDescriptor build() {
         Set<String> optionNames = new HashSet<>(options.size());
         for (Option option : options) {
            if (!optionNames.add(option.getName())) {
               throw new DescriptorParserException(name + ": Option \"" + option.getName() + "\" was already set.");
            }
         }
         return new FileDescriptor(this);
      }
   }
}
