package org.infinispan.protostream.descriptors.namespace;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.descriptors.GenericDescriptor;

/**
 * The types defined in a file or in the files it imports (publicly or privately).
 *
 * @author anistor@redhat.com
 * @since 3.1
 */
public final class FileNamespace extends CompositeNamespace {

   private final FileDescriptor fileDescriptor;

   private final Map<String, GenericDescriptor> localDefinitions = new LinkedHashMap<>();

   private final LocalNamespace localNamespace;

   private final Namespace exportedNamespace;

   private final Namespace[] namespaces;

   public FileNamespace(FileDescriptor fileDescriptor, Collection<FileDescriptor> publicDependencies, Collection<FileDescriptor> privateDependencies) {
      this.fileDescriptor = fileDescriptor;
      this.localNamespace = new LocalNamespace(localDefinitions);
      ImportedNamespace publicImports = new ImportedNamespace(publicDependencies);
      ImportedNamespace privateImports = new ImportedNamespace(privateDependencies);
      this.exportedNamespace = new ExportedNamespace(fileDescriptor, localNamespace, publicImports);
      namespaces = new Namespace[]{localNamespace, publicImports, privateImports};
   }

   public void put(String name, GenericDescriptor d) {
      GenericDescriptor existing = get(name);
      if (existing != null) {
         foundDuplicateDefinition(name, existing);
      }
      localDefinitions.put(name, d);
   }

   private void foundDuplicateDefinition(String name, GenericDescriptor existing) {
      List<String> locations = Arrays.asList(fileDescriptor.getName(), existing.getFileDescriptor().getName());
      if (locations.get(0).equals(locations.get(1))) {
         throw new DescriptorParserException("Duplicate definition of '" + name + "' in " + locations.get(0));
      }
      Collections.sort(locations); // sort names for more predictable error messages in unit tests
      throw new DescriptorParserException("Duplicate definition of '" + name + "' in " + locations.get(0) + " and " + locations.get(1));
   }

   /**
    * Types defined in this file.
    */
   public LocalNamespace getLocalNamespace() {
      return localNamespace;
   }

   /**
    * The types defined in this file or defined in publicly imported files.
    */
   public Namespace getExportedNamespace() {
      return exportedNamespace;
   }

   @Override
   protected Namespace[] getNamespaces() {
      return namespaces;
   }
}
